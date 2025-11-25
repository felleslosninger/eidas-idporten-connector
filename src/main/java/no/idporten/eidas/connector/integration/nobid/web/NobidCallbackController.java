package no.idporten.eidas.connector.integration.nobid.web;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ciba.CIBATokenDelivery;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.Nonce;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.exceptions.ErrorCodes;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProtocolVerifiers;
import no.idporten.eidas.connector.integration.nobid.service.NobidMatchingServiceClient;
import no.idporten.eidas.connector.matching.domain.UserMatchRedirect;
import no.idporten.eidas.connector.matching.domain.UserMatchResponse;
import no.idporten.eidas.connector.service.AuthorizationResponseHelper;
import no.idporten.eidas.connector.service.EIDASIdentifier;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.text.ParseException;
import java.util.Objects;

import static no.idporten.eidas.connector.web.SessionAttributes.SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST;

@Slf4j
@Controller
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "eidas.nobid", name = "enabled", havingValue = "true")
public class NobidCallbackController {

    private final AuthorizationResponseHelper authorizationResponseHelper;
    private final NobidSession nobidSession;
    private final NobidMatchingServiceClient matchingServiceClient;

    /**
     * Callback endpoint invoked by Nobid matching service after PAR/authorization flow.
     * Accepts a correlation state and an optional matched national identifier.
     */
    @GetMapping(value = {"/callback/nobid", "nobid/callback"})
    public ResponseEntity<String> handleCallback(@RequestParam("state") String state,
                                                 @RequestParam(value = "code", required = false) String code,
                                                 @Nonnull final HttpServletRequest request,
                                                 @Nonnull final HttpServletResponse response) {
        log.info("Received Nobid callback for state={}", state);

        try {
            OidcProtocolVerifiers protocolVerifiers = nobidSession.getOidcProtocolVerifiers();
            if (protocolVerifiers == null) {
                throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Matching session missing at callback.");
            }
            if (!Objects.equals(protocolVerifiers.state().getValue(), state)) {
                throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Invalid state in protocol verifies when integrating with nobid. Expected %s, got %s".formatted(protocolVerifiers.state(), state));
            }
            String pid = null; //may or may not find a match
            // If no authorization code, then no match probably
            if (code == null) {
                throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "No authorization code received from Nobid.");
            }else {
                OIDCTokenResponse tokenResponse = matchingServiceClient.getToken(code, protocolVerifiers);

                JWT idToken = tokenResponse.getOIDCTokens().getIDToken();
                if (idToken == null) {
                    throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "IDToken from nobid was missing in the token response");
                }
                log.info("Received tokens from Nobid. ID Token present: {}", idToken);
                pid = validateAndExtractPidFromIdToken(idToken.serialize(), protocolVerifiers.nonce());
                log.info("Extracted pid from ID token: {}", pid);
            }
            String levelOfAssurance = nobidSession.getLevelOfAssurance() != null ? nobidSession.getLevelOfAssurance() : "http://eidas.europa.eu/LoA/low"; //tmp
            EidasUser eidasUser = nobidSession.getEidasUser();

            //Fetch the cached par request and return the code to idporten
            PushedAuthorizationRequest originalParRequest = (PushedAuthorizationRequest) request.getSession().getAttribute(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST);
            try {
                authorizationResponseHelper.returnAuthorizationCode(request, response, levelOfAssurance, originalParRequest, eidasUser, pid);
            } catch (IOException e) {
                throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Failed to return authorization code.", e);
            }
            return ResponseEntity.ok("ok");

        } finally {
            //clearNobidSession();
        }
    }

    private void clearNobidSession() {
        nobidSession.setOidcProtocolVerifiers(null);
        nobidSession.setEidasUser(null);
        nobidSession.setPushedAuthorizationRequest(null);
        nobidSession.setLevelOfAssurance(null);
    }

    private void validateSessionState(NobidSession session, String expectedState) {
        if (session.getOidcProtocolVerifiers() == null) {
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(),
                    "Session state missing");
        }
        if (!Objects.equals(session.getOidcProtocolVerifiers().state().getValue(), expectedState)) {
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(),
                    "Invalid session state");
        }
    }

    @GetMapping("/nobid/test")
    public String triggerTest(@Nonnull final HttpServletRequest request,
                              @Nonnull final HttpServletResponse response) {
        //trigger a test
        UserMatchResponse result = matchingServiceClient.match(new EidasUser(new EIDASIdentifier("SE/NO/1634736525341-3"), "2000-01-01", null));
        if (result instanceof UserMatchRedirect(String redirectUrl)) {
            return "redirect:http://localhost:7070/authorize?client_id=democlient1&request_uri=%s".formatted(redirectUrl);
        } else {
            return "foo";
        }
    }
    private String validateAndExtractPidFromIdToken(String idToken, Nonce expectedNonce) {
        try {
            JWTClaimsSet claims = SignedJWT.parse(idToken).getJWTClaimsSet();
            String nonceInToken = claims.getStringClaim("nonce");

            if (!Objects.equals(nonceInToken, expectedNonce.getValue())) {
                throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(),
                        "Invalid nonce in ID token");
            }

            return claims.getStringClaim("sub");
        } catch (Exception e) {
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(),
                    "Failed to validate nonce in ID token", e);
        }
    }
}
