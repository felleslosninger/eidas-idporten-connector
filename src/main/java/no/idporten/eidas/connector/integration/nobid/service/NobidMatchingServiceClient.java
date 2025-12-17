package no.idporten.eidas.connector.integration.nobid.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.langtag.LangTag;
import com.nimbusds.langtag.LangTagException;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.domain.EidasLoginHint;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.exceptions.ErrorCodes;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.nobid.domain.ClientIdAuthorizationGrant;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProtocolVerifiers;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProvider;
import no.idporten.eidas.connector.integration.nobid.web.NobidSession;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.eidas.connector.matching.domain.UserMatchError;
import no.idporten.eidas.connector.matching.domain.UserMatchRedirect;
import no.idporten.eidas.connector.matching.domain.UserMatchResponse;
import no.idporten.eidas.connector.matching.service.MatchingService;
import no.idporten.eidas.connector.service.CountryCodeConverter;
import no.idporten.eidas.connector.service.EIDASIdentifier;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
public class NobidMatchingServiceClient implements MatchingService {

    private final OidcProvider nobidClaimsProvider;
    private final NobidSession nobidSession;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;
    private final CountryCodeConverter countryCodeConverter;


    @Override
    public UserMatchResponse match(EidasUser eidasUser, Set<String> requestedScopes) {
        try {
            return createParAndReturnState(eidasUser, requestedScopes);
        } catch (Exception e) {
            log.warn("Nobid matching failed: {}", e.getMessage());
            return new UserMatchError(eidasUser, "internal_error", "No match found");
        }
    }

    private UserMatchResponse createParAndReturnState(EidasUser eidasUser, Set<String> requestedScopes) {
        OidcProtocolVerifiers protocolVerifiers = new OidcProtocolVerifiers("nobid");

        nobidSession.setOidcProtocolVerifiers(protocolVerifiers);
        nobidSession.setEidasUser(eidasUser);
        AuthenticationRequest parRequestToNobid = createNobidAuthenticationRequest(eidasUser, protocolVerifiers, requestedScopes);
        try {
            return sendPushedAuthorizationRequest(parRequestToNobid);
        } catch (Exception e) {
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Failed to send PAR-request: %s.".formatted(e.getMessage()), e);
        }
    }

    /**
     * Exchanges an authorization code for tokens at the Nobid token endpoint using Nimbus SDK.
     * Uses PKCE code_verifier stored in the current {@link NobidSession}.
     *
     * @param authorizationCode The authorization code received on the callback
     * @return OIDC token response containing ID token and access token on success
     */
    public OIDCTokenResponse getToken(String authorizationCode, OidcProtocolVerifiers verifiers) {
        try {
            if (authorizationCode == null || authorizationCode.isBlank()) {
                throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Missing authorization code from Nobid");
            }


            if (verifiers == null) {
                throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Missing protocol verifiers in session for Nobid token exchange");
            }
            ClientAuthentication clientAuthentication = ClientAuthenticationService.createClientAuthentication(nobidClaimsProvider);
            CodeVerifier codeVerifier = verifiers.codeVerifier();

            AuthorizationGrant codeGrant = new ClientIdAuthorizationGrant(
                    new AuthorizationCodeGrant(new AuthorizationCode(authorizationCode),
                            nobidClaimsProvider.redirectUri(),
                            codeVerifier),
                    clientAuthentication.getClientID()
            );

            TokenRequest tokenRequest = getTokenRequest(clientAuthentication, codeGrant);

            // Send HTTP request with timeouts
            HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
            clientAuthentication.applyTo(httpRequest);
            httpRequest.setConnectTimeout(getMillisAsIntSafely(nobidClaimsProvider.connectTimeout()));
            httpRequest.setReadTimeout(getMillisAsIntSafely(nobidClaimsProvider.readTimeout()));

            //log.info("Sending token request {}", httpRequest.getBodyAsFormParameters().toString());
            HTTPResponse httpResponse = sendHttpRequest(httpRequest);
            // log.info("Token response {} {}", httpResponse.getStatusCode(), httpResponse.getBodyAsJSONObject().toString());
            //nb: no access token returned.
            com.nimbusds.oauth2.sdk.TokenResponse parsed = OIDCTokenResponseParser.parse(httpResponse);
            if (parsed.indicatesSuccess()) {
                return (OIDCTokenResponse) parsed.toSuccessResponse();
            }

            ErrorObject error = parsed.toErrorResponse().getErrorObject();
            String description = error != null ? error.getDescription() : "Unknown error from token endpoint";
            log.warn("Nobid token request failed: {} - {}", error != null ? error.getCode() : "error", description);
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Nobid token request failed: %s".formatted(description));

        } catch (SpecificConnectorException e) {
            throw e;
        } catch (Exception e) {
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Failed to exchange authorization code at Nobid: %s".formatted(e.getMessage()), e);
        }
    }

    /**
     * Factory method to create the Nimbus {@link TokenRequest} for the token endpoint.
     * Extracted to facilitate testing and potential customization.
     */
    private TokenRequest getTokenRequest(ClientAuthentication clientAuthentication, AuthorizationGrant codeGrant) {
        return new TokenRequest(
                nobidClaimsProvider.tokenEndpoint(),
                clientAuthentication,
                codeGrant,
                null
        );
    }

    public AuthenticationRequest createNobidAuthenticationRequest(final EidasUser eidasUser, OidcProtocolVerifiers protocolVerifiers, Set<String> requestedScopes) {
        // forwarded scopes from rp union default scopes for claims provider
        List<LangTag> langTags = null;
        try {
            langTags = List.of(LangTag.parse("nb"));
        } catch (LangTagException e) {
            log.warn("Failed to parse lang tag nb", e);
        }

        Set<String> scopes = new HashSet<>(nobidClaimsProvider.scopes());
        if (!CollectionUtils.isEmpty(requestedScopes)) {
            Set<String> selectedOptional = new HashSet<>(nobidClaimsProvider.optionalScopes());
            selectedOptional.retainAll(requestedScopes);
            scopes.addAll(selectedOptional);
        }

        AuthenticationRequest.Builder requestBuilder = new AuthenticationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE),
                new Scope(scopes.toArray(String[]::new)),
                new ClientID(nobidClaimsProvider.clientId()),
                nobidClaimsProvider.redirectUri());
        requestBuilder
                .codeChallenge(protocolVerifiers.codeVerifier(), CodeChallengeMethod.S256)
                .responseMode(nobidClaimsProvider.responseMode())
                .endpointURI(nobidClaimsProvider.authorizationEndpoint())
                .state(protocolVerifiers.state())
                .nonce(protocolVerifiers.nonce())
                .uiLocales(langTags)
                .codeChallenge(protocolVerifiers.codeVerifier(), CodeChallengeMethod.S256);

        //for example in test map from demoland country code CA to SE
        String mappedCountryCode = countryCodeConverter.getMappedCountryCode(eidasUser.eidasIdentifier().getSubjectCountryCode());
        EidasUser mappedEidasUser = copyWithSubjectCountry(eidasUser, mappedCountryCode);
        EidasLoginHint loginHint = mappedEidasUser.toLoginHint();
        try {

            String serializedLoginHint = objectMapper.writeValueAsString(loginHint);
            log.info("Serialized login hint {}", serializedLoginHint);
            requestBuilder.loginHint(serializedLoginHint);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize login hint {}", loginHint, e);
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Failed to perform matching because of invalid login hint", e);
        }
        return requestBuilder.build();
    }


    public UserMatchResponse sendPushedAuthorizationRequest(AuthenticationRequest internalRequest) throws Exception {
        ClientAuthentication clientAuthentication = ClientAuthenticationService.createClientAuthentication(nobidClaimsProvider);
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(nobidClaimsProvider.pushedAuthorizationRequestEndpoint(),
                clientAuthentication,
                internalRequest);
        nobidSession.setPushedAuthorizationRequest(pushedAuthorizationRequest);
        auditService.auditSendPushedAuthorizationRequestToNobid(pushedAuthorizationRequest);
        HTTPRequest httpRequest = pushedAuthorizationRequest.toHTTPRequest();
        httpRequest.setConnectTimeout(getMillisAsIntSafely(nobidClaimsProvider.connectTimeout()));
        httpRequest.setReadTimeout(getMillisAsIntSafely(nobidClaimsProvider.readTimeout()));
        HTTPResponse httpResponse = sendHttpRequest(httpRequest);
        PushedAuthorizationResponse response = parsePushedAuthorizationResponse(httpResponse);
        //todo if (isDirectResponse(response))
        if (response.indicatesSuccess()) {
            PushedAuthorizationSuccessResponse successResponse = response.toSuccessResponse();
            auditService.auditPushedAuthorizationResponseFromNobid(internalRequest.getClientID() != null ? internalRequest.getClientID().getValue() : null, successResponse);

            return new UserMatchRedirect("%s/authorize?request_uri=%s&client_id=%s".formatted(nobidClaimsProvider.issuer().toURL().toString(), successResponse.getRequestURI().toString(), nobidClaimsProvider.clientId()));
        } else {
            ErrorObject errorObject = response.toErrorResponse().getErrorObject();
            if (errorObject != null) {
                log.warn("nobid rejected PAR request details: {}, {}, {}", errorObject.getCode(), errorObject.getDescription(), errorObject.toJSONObject().toJSONString());
            } else {
                log.error("Nobid rejected PAR request: {}, {}", response.toHTTPResponse().getStatusCode(), getJsonString(response));
            }
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Integration with nobidClaimsProvider failed");
        }
    }

    private static String getJsonString(PushedAuthorizationResponse response) throws ParseException {
        return response.toHTTPResponse().getBodyAsJSONObject().toJSONString();
    }

    protected HTTPResponse sendHttpRequest(HTTPRequest httpRequest) throws IOException {
        return httpRequest.send();
    }

    /**
     * Parse the pushed authorization response, adding support for direct responses
     */
    protected PushedAuthorizationResponse parsePushedAuthorizationResponse(HTTPResponse httpResponse) throws ParseException {
//       todo for directresponse if (httpResponse.getStatusCode() == 200) {
//            return PushedAuthorizationDirectResponse.parse(httpResponse);
//        }
        return PushedAuthorizationResponse.parse(httpResponse);
    }

    // Convert Duration to int for compatibility with APIs that require int values.
    private static int getMillisAsIntSafely(Duration duration) {
        long millis = duration.toMillis();
        if (millis > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (millis < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        } else {
            return (int) millis;
        }
    }

    //to change to mapped subject country code. only relevant in test environments
    public static EidasUser copyWithSubjectCountry(EidasUser user, String newSubjectCountryCode) {
        EIDASIdentifier oldId = user.eidasIdentifier();
        String formatted = "%s/%s/%s".formatted(
                newSubjectCountryCode,
                oldId.getSpCountryCode(),
                oldId.getForeignIdentifier()
        );
        return new EidasUser(new EIDASIdentifier(formatted), user.birthdate(), user.eidasClaims());
    }
}
