package no.idporten.eidas.connector.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

import static no.idporten.eidas.connector.web.SessionAttributes.SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IDPortenConnectorController {


    private final OpenIDConnectIntegration openIDConnectSdk;
    private final AuditService auditService;
    @GetMapping(value = "/")
    public String redirectToDiscovery() {
        return "redirect:" + openIDConnectSdk.getSDKConfiguration().getOidcDiscoveryEndpoint();
    }

    @PostMapping(value = "/par",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<PushedAuthorizationResponse> par(@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok(openIDConnectSdk.process(new PushedAuthorizationRequest(headers, parameters)));
    }


    @GetMapping("/authorize")
    public String authorize(@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> parameters, HttpServletRequest request, HttpServletResponse response, Model model) throws IOException {
        PushedAuthorizationRequest authorizationRequest = new PushedAuthorizationRequest(headers, parameters);
        ClientMetadata clientMetadata = openIDConnectSdk.findClient(authorizationRequest.getClientId());
        try {
            openIDConnectSdk.validate(authorizationRequest, clientMetadata);
            auditService.auditRedirectedAuthorizationRequest(authorizationRequest);
        } catch (OAuth2Exception e) {
            log.warn("Failed to validate authorization request", e);
            sendHttpResponse(openIDConnectSdk.createClientResponse(openIDConnectSdk.errorResponse(authorizationRequest, e.error(), e.errorDescription())), response);
            return null;
        }
        return authorize(authorizationRequest, request);
    }

    protected void sendHttpResponse(ClientResponse clientResponse, HttpServletResponse response) throws IOException {
        switch (clientResponse) {
            case RedirectedResponse redirectedResponse ->
                response.sendRedirect(redirectedResponse.toQueryRedirectUri().toString());

            case FormPostResponse formPostResponse -> {
                response.setContentType("text/html;charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(formPostResponse.getRedirectForm());
            }
            default -> throw new IllegalStateException("Unexpected client response type %s".formatted(clientResponse.getClass().getName()));
        }
    }

    @GetMapping(value = "/authorize", params = "request_uri")
    public String authorizePAR(@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> parameters, HttpServletRequest request, Model model) {
        request.getSession().invalidate();
        try {
            PushedAuthorizationRequest pushedAuthorizationRequest = openIDConnectSdk.process(new AuthorizationRequest(headers, parameters));
            return authorize(pushedAuthorizationRequest, request);
        } catch (OAuth2Exception e) {
            log.warn("Failed to process authorization request", e);
            return "error";
        }
    }

    public String authorize(PushedAuthorizationRequest authorizationRequest,
                            HttpServletRequest request) {
        request.getSession().invalidate();
        request.getSession(true).setAttribute(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST, authorizationRequest);
        return "redirect:/citizencountry";
    }


    @GetMapping("/cancel")
    public void cancel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PushedAuthorizationRequest authorizationRequest = (PushedAuthorizationRequest) request.getSession().getAttribute(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST);
        AuthorizationResponse authorizationResponse = openIDConnectSdk.errorResponse(authorizationRequest, "access_denied", "End user cancel.");
        request.getSession().invalidate();
        sendHttpResponse(openIDConnectSdk.createClientResponse(authorizationResponse), response);
    }

    @PostMapping(value = "/token",
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<TokenResponse> token(@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok(openIDConnectSdk.process(new TokenRequest(headers, parameters)));
    }

    @GetMapping(value = {"/jwk", "/jwks", "/.well-known/jwks.json"}, produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<String> jwks() {
        return ResponseEntity.ok(openIDConnectSdk.getPublicJWKSet().toString());
    }

    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<OpenIDProviderMetadataResponse> openidConfiguration() {
        return ResponseEntity.ok(openIDConnectSdk.getOpenIDProviderMetadata());
    }

    @GetMapping(value = "/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<UserInfoResponse> userInfoGet(@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok(openIDConnectSdk.process(new UserInfoRequest(headers, parameters)));
    }

    @PostMapping(value = "/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    @CrossOrigin(origins = "*")
    public ResponseEntity<UserInfoResponse> userInfoPost(@RequestHeader HttpHeaders headers, @RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok(openIDConnectSdk.process(new UserInfoRequest(headers, parameters)));
    }

    @ExceptionHandler(OAuth2Exception.class)
    public ResponseEntity<Map<String, Object>> handleError(HttpSession session, OAuth2Exception exception) {
        session.invalidate();
        log.warn(exception.getMessage(), exception);
        return ResponseEntity.status(exception.getHttpStatusCode()).body(exception.errorResponse().toJsonObject());
    }

}
