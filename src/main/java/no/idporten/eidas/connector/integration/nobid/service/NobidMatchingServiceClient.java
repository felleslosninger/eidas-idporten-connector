package no.idporten.eidas.connector.integration.nobid.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.domain.EidasLoginHint;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.exceptions.ErrorCodes;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProtocolVerifiers;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProvider;
import no.idporten.eidas.connector.integration.nobid.web.NobidSession;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.eidas.connector.matching.domain.UserMatchError;
import no.idporten.eidas.connector.matching.domain.UserMatchRedirect;
import no.idporten.eidas.connector.matching.domain.UserMatchResponse;
import no.idporten.eidas.connector.matching.service.MatchingService;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
public class NobidMatchingServiceClient implements MatchingService {

    private final OidcProvider nobidClaimsProvider;
    private final NobidSession nobidSession;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;


    // Synchronous variant not used for true async flow; keep placeholder behavior
    @Override
    public UserMatchResponse match(EidasUser eidasUser) {
        try {
            return createParAndReturnState(eidasUser);
        } catch (Exception e) {
            log.warn("Nobid async matching failed: {}", e.getMessage());
            return new UserMatchError("internal_error", "No match found");
        }
    }


    private UserMatchResponse createParAndReturnState(EidasUser eidasUser) {
        OidcProtocolVerifiers protocolVerifiers = new OidcProtocolVerifiers("nobid");
        nobidSession.setOidcProtocolVerifiers(protocolVerifiers);
        nobidSession.setEidasUser(eidasUser);
        AuthenticationRequest parRequestToNobid = createNobidAuthenticationRequest(eidasUser, protocolVerifiers);
        try {
            return sendPushedAuthorizationRequest(parRequestToNobid);
        } catch (Exception e) {
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Failed to initiate matching.", e);
        }
    }

    public AuthenticationRequest createNobidAuthenticationRequest(EidasUser eidasUser, OidcProtocolVerifiers protocolVerifiers) {
        // forwarded scopes from rp union default scopes for claims provider
        Set<String> scopes = new HashSet<>(nobidClaimsProvider.scopes());
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
                .codeChallenge(protocolVerifiers.codeVerifier(), CodeChallengeMethod.S256);
        EidasLoginHint loginHint = eidasUser.toLoginHint();
        try {

            String serializedLoginHint = objectMapper.writeValueAsString(loginHint);
            log.info("Serialized login hint {}", serializedLoginHint);
            //todo requestBuilder.loginHint(serializedLoginHint);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize login hint {}", loginHint, e);
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Failed to perform matching because of invalid login hint", e);
        }
        return requestBuilder.build();
    }


    public UserMatchResponse sendPushedAuthorizationRequest(AuthenticationRequest internalRequest) throws Exception {
        ClientAuthentication clientAuthentication = ClientAuthenticationService.createClientAuthentication(nobidClaimsProvider);
        PushedAuthorizationRequest pushedAuthorizationRequest = new PushedAuthorizationRequest(nobidClaimsProvider.pushedAuthorizationRequestEndpoint(), clientAuthentication, internalRequest);
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

            return new UserMatchRedirect(successResponse.getRequestURI().toString());
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
    // Handles potential overflow and underflow by capping values at Integer.MAX_VALUE and Integer.MIN_VALUE.
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


}
