package no.idporten.eidas.connector.exceptions;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.sdk.oidcserver.OAuth2Exception;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.ClientResponse;
import no.idporten.sdk.oidcserver.protocol.FormPostResponse;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.RedirectedResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;

import static no.idporten.eidas.connector.web.SessionAttributes.SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST;

/**
 * Exception handling for eidas-idporten-connector.
 */
@ControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class WebExceptionControllerAdvice {

    private final OpenIDConnectIntegration openIDConnectSdk;

    @ExceptionHandler(SpecificConnectorException.class)
    public String handleSpecificConnectorException(HttpServletResponse response, HttpSession httpSession, SpecificConnectorException e) {
        log.warn("SpecificConnectorException occurred for request: {} ", e.getMessage());
        try {
            sendHttpResponse(openIDConnectSdk.createClientResponse(openIDConnectSdk.errorResponse((PushedAuthorizationRequest) httpSession.getAttribute(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST), OAuth2Exception.SERVER_ERROR, e.getMessage())), response);
        } catch (IOException ex) {
            log.error("Error sending response: {}", ex.getMessage());
            return "error";
        }
        return null;

    }

    @ExceptionHandler(Exception.class)
    public String handleException(HttpServletResponse response, HttpSession httpSession, Exception e) {
        log.warn("Exception occurred :{}", e.getMessage());
        try {
            sendHttpResponse(openIDConnectSdk.createClientResponse(openIDConnectSdk.errorResponse((PushedAuthorizationRequest) httpSession.getAttribute(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST), OAuth2Exception.SERVER_ERROR, e.getMessage())), response);
        } catch (IOException ex) {
            log.error("Error sending response: {}", ex.getMessage());
            return "error";
        }
        return null;
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
            default ->
                    throw new IllegalStateException("Unexpected client response type %s".formatted(clientResponse.getClass().getName()));
        }
    }

}
