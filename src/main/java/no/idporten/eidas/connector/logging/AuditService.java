package no.idporten.eidas.connector.logging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.eidas.lightprotocol.messages.LightResponse;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditIdentifier;
import no.idporten.logging.audit.AuditLogger;
import no.idporten.sdk.oidcserver.audit.OpenIDConnectAuditLogger;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService implements OpenIDConnectAuditLogger {

    private static final String RELATED_TRACE_ID = "related_trace_id";
    private final AuditLogger auditLogger;

    @Getter
    @AllArgsConstructor
    enum AuditIdPattern {

        OIDC_CLIENT_AUTHENTICATION("%s-AUTHENTICATE-CLIENT"),
        OIDC_PAR_REQUEST("%s-RECEIVE-PUSHED-AUTHORIZATION-REQUEST"),
        OIDC_REDIRECTED_AUTHORIZATION_REQUEST("%s-RECEIVE-REDIRECTED-AUTHORIZATION-REQUEST"),
        OIDC_PAR_RESPONSE("%s-SEND-PUSHED-AUTHORIZATION-RESPONSE"),
        OIDC_AUTHORIZATION_REQUEST("%s-RECEIVE-AUTHORIZATION-REQUEST"),
        OIDC_AUTHORIZE_USER("%s-AUTHORIZE-USER"),
        OIDC_AUTHORIZATION_RESPONSE("%s-SEND-AUTHORIZATION-RESPONSE"),
        OIDC_TOKEN_REQUEST("%s-RECEIVE-TOKEN-REQUEST"),
        OIDC_TOKEN_RESPONSE("%s-SEND-TOKEN-RESPONSE"),
        OIDC_USERINFO_REQUEST("%s-RECEIVE-USERINFO-REQUEST"),
        OIDC_USERINFO_RESPONSE("%s-SEND-USERINFO-RESPONSE"),
        EIDAS_CONNECTOR_COUNTRY_SELECTION("%s-SELECT-COUNTRY"),
        EIDAS_LIGHT_REQUEST("%s-LIGHT-REQUEST"),
        EIDAS_LIGHT_RESPONSE("%s-LIGHT-RESPONSE");

        private final String pattern;

        AuditIdentifier auditIdentifier() {
            return () -> String.format(getPattern(), "EIDAS-IDPORTEN-CONNECTOR");
        }
    }

    private void log(AuditIdPattern auditIdPattern, String auditDataAttribute, AuditDataProvider auditDataProvider) {
        log(auditIdPattern, auditDataAttribute, auditDataProvider, null);
    }

    private void log(AuditIdPattern auditIdPattern, String auditDataAttribute, AuditDataProvider auditDataProvider, String relatedTraceId) {
        auditLogger.log(AuditEntry.builder()
                .auditId(auditIdPattern.auditIdentifier())
                .logNullAttributes(false)
                .attribute(RELATED_TRACE_ID, relatedTraceId)
                .attribute(auditDataAttribute, auditDataProvider.getAuditData().getAttributes())
                .build());
    }

    @Override
    public void auditClientAuthentication(ClientAuthentication clientAuthentication) {
        log(AuditIdPattern.OIDC_CLIENT_AUTHENTICATION, "client_authentication", clientAuthentication);
    }

    @Override
    public void auditPushedAuthorizationRequest(PushedAuthorizationRequest pushedAuthorizationRequest) {
        log(AuditIdPattern.OIDC_PAR_REQUEST, "pushed_authorization_request", pushedAuthorizationRequest);
    }

    // The OIDC server SDK does not support redirected authorization request.  TestId has implemented this on top of the SDK.
    public void auditRedirectedAuthorizationRequest(PushedAuthorizationRequest pushedAuthorizationRequest) {
        log(AuditIdPattern.OIDC_REDIRECTED_AUTHORIZATION_REQUEST, "authorization_request", pushedAuthorizationRequest);
    }

    @Override
    public void auditPushedAuthorizationResponse(PushedAuthorizationResponse pushedAuthorizationResponse) {
        log(AuditIdPattern.OIDC_PAR_RESPONSE, "pushed_authorization_response", pushedAuthorizationResponse);
    }

    @Override
    public void auditAuthorizationRequest(AuthorizationRequest authorizationRequest) {
        log(AuditIdPattern.OIDC_AUTHORIZATION_REQUEST, "authorization_request", authorizationRequest);
    }

    @Override
    public void auditAuthorizationResponse(AuthorizationResponse authorizationResponse) {
        log(AuditIdPattern.OIDC_AUTHORIZATION_RESPONSE, "authorization_response", authorizationResponse);
    }

    @Override
    public void auditAuthorization(Authorization authorization) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditIdPattern.OIDC_AUTHORIZE_USER.auditIdentifier())
                .logNullAttributes(false)
                .attribute("person_identifier", authorization.getSub())
                .attribute("authorization", authorization.getAuditData().getAttributes())
                .build());
    }

    @Override
    public void auditTokenRequest(TokenRequest tokenRequest) {
        log(AuditIdPattern.OIDC_TOKEN_REQUEST, "token_request", tokenRequest);
    }

    @Override
    public void auditTokenResponse(TokenResponse tokenResponse) {
        log(AuditIdPattern.OIDC_TOKEN_RESPONSE, "token_response", tokenResponse);
    }

    @Override
    public void auditUserInfoRequest(UserInfoRequest userInfoRequest) {
        log(AuditIdPattern.OIDC_USERINFO_REQUEST, "userinfo_request", userInfoRequest);
    }

    @Override
    public void auditUserInfoResponse(UserInfoResponse userInfoResponse) {
        log(AuditIdPattern.OIDC_USERINFO_RESPONSE, "userinfo_response", userInfoResponse);
    }

    public void auditCountrySelection(String countryId) {
        auditLogger.log(AuditEntry.builder()
                .auditId(AuditIdPattern.EIDAS_CONNECTOR_COUNTRY_SELECTION.auditIdentifier())
                .logNullAttributes(false)
                .attribute("selected_country", countryId)
                .build());
    }

    public void auditLightRequest(LightRequest lightRequest) {
        log(AuditIdPattern.EIDAS_LIGHT_REQUEST, "light_request", lightRequest);
    }

    public void auditLightResponse(LightResponse lightResponse, String relatedTraceid) {
        log(AuditIdPattern.EIDAS_LIGHT_RESPONSE, "light_response", lightResponse, relatedTraceid);
    }

}
