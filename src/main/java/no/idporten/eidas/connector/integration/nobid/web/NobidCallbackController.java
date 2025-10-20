package no.idporten.eidas.connector.integration.nobid.web;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
    @GetMapping("/callback/nobid")
    public ResponseEntity<String> handleCallback(@RequestParam("state") String state,
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
            //todo get the token and validate it
            String levelOfAssurance = nobidSession.getLevelOfAssurance() != null ? nobidSession.getLevelOfAssurance() : "http://eidas.europa.eu/LoA/low"; //tmp
            EidasUser eidasUser = nobidSession.getEidasUser();
            clearNobidSession();
            //to the thing.
            PushedAuthorizationRequest parRequest = (PushedAuthorizationRequest) request.getSession().getAttribute(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST);
            try {
                authorizationResponseHelper.returnAuthorizationCode(request, response, levelOfAssurance, parRequest, eidasUser, null);//todo
            } catch (IOException e) {
                throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Failed to return authorization code.", e);
            }
            return ResponseEntity.ok("ok");
        } finally {
            clearNobidSession();
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

    //todo when we get an idtoken from nobid, we should validate it and extract the pid from it.
    private String validateAndExtractPidFromIdToken(String idToken, Nonce expectedNonce) {
        try {
            JWTClaimsSet claims = SignedJWT.parse(idToken).getJWTClaimsSet();
            String nonceInToken = claims.getStringClaim("nonce");

            if (!Objects.equals(nonceInToken, expectedNonce.getValue())) {
                throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(),
                        "Invalid nonce in ID token");
            }

            return claims.getStringClaim("pid");
        } catch (Exception e) {
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(),
                    "Failed to validate nonce in ID token", e);
        }
    }
}

//http://eidas-idporten-connector:8088/nobid/test