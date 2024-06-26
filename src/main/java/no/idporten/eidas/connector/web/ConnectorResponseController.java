package no.idporten.eidas.connector.web;

import eu.eidas.SimpleProtocol.utils.StatusCodeTranslator;
import eu.eidas.auth.commons.EidasParameterKeys;
import eu.eidas.auth.commons.attribute.AttributeDefinition;
import eu.eidas.auth.commons.light.ILightResponse;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.exceptions.ErrorCodes;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.connector.integration.specificcommunication.config.EidasCacheProperties;
import no.idporten.eidas.connector.integration.specificcommunication.service.SpecificCommunicationService;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.eidas.connector.service.SpecificConnectorService;
import no.idporten.eidas.lightprotocol.BinaryLightTokenHelper;
import no.idporten.eidas.lightprotocol.IncomingLightResponseValidator;
import no.idporten.eidas.lightprotocol.messages.LightResponse;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.Collection;

import static no.idporten.eidas.connector.web.SessionAttributes.SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ConnectorResponseController {

    private final SpecificCommunicationService specificCommunicationService;
    private final EidasCacheProperties eidasCacheProperties;
    private final OpenIDConnectIntegration openIDConnectSdk;
    private final SpecificConnectorService specificConnectorService;
    private final AuditService auditService;

    @RequestMapping(path = "/ConnectorResponse", method = {RequestMethod.GET, RequestMethod.POST})
    public String handleConnectorResponse(@Nonnull final HttpServletRequest request, @Nonnull final HttpServletResponse response) throws IOException {

        final ILightResponse iLightResponse = getIncomingiLightResponse(request, null);
        if (!IncomingLightResponseValidator.validate(iLightResponse)) {
            throw new SpecificConnectorException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Response is invalid. Rejecting request.");
        }
        if (iLightResponse instanceof LightResponse lightResponse) {
            auditService.auditLightResponse(lightResponse);
        } else {
            log.warn("Not instance of LightResponse");
        }

        CorrelatedRequestHolder cachedRequest = specificConnectorService.getCachedRequest(iLightResponse.getRelayState());
        if (cachedRequest == null || !iLightResponse.getInResponseToId().equals(cachedRequest.getiLightRequest().getId())) {
            throw new SpecificConnectorException(ErrorCodes.INVALID_REQUEST.getValue()
                    , "No request found for relay state %s and id %s."
                    .formatted(iLightResponse.getRelayState(), iLightResponse.getInResponseToId()));
        }
        final StatusCodeTranslator statusCodeTranslator = StatusCodeTranslator.fromEidasStatusCodeString(iLightResponse.getStatus().getStatusCode());
        if (StatusCodeTranslator.SUCCESS == statusCodeTranslator) {
            return returnAuthorizationCode(request, response, (LightResponse) iLightResponse);
        } else {
            throw new SpecificConnectorException(ErrorCodes.INVALID_REQUEST.getValue(), "IDP reported an error status: %s.".formatted(iLightResponse.getStatus()));
        }
    }

    private String returnAuthorizationCode(HttpServletRequest request, HttpServletResponse response, LightResponse lightResponse) throws IOException {
        PushedAuthorizationRequest authorizationRequest = (PushedAuthorizationRequest) request.getSession().getAttribute(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST);
        Authorization authorization = specificConnectorService.getAuthorization(lightResponse);
        AuthorizationResponse authorizationResponse = openIDConnectSdk.authorize(authorizationRequest, authorization);
        request.getSession().invalidate();
        sendHttpResponse(openIDConnectSdk.createClientResponse(authorizationResponse), response);
        return null;
    }


    private ILightResponse getIncomingiLightResponse(@Nonnull HttpServletRequest request, final Collection<AttributeDefinition<?>> registry) {
        final String lightTokenId = getLightTokenId(request);
        return specificCommunicationService.getAndRemoveResponse(lightTokenId, registry);
    }

    protected String getLightTokenId(HttpServletRequest request) {
        String tokenBase64 = BinaryLightTokenHelper.getBinaryToken(request, EidasParameterKeys.TOKEN.toString());
        return BinaryLightTokenHelper.getBinaryLightTokenId(tokenBase64, eidasCacheProperties.getResponseSecret(), eidasCacheProperties.getAlgorithm());
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
