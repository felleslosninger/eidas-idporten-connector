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
import no.idporten.eidas.connector.matching.domain.*;
import no.idporten.eidas.connector.service.AuthorizationResponseHelper;
import no.idporten.eidas.connector.service.SpecificConnectorService;
import no.idporten.eidas.lightprotocol.BinaryLightTokenHelper;
import no.idporten.eidas.lightprotocol.IncomingLightResponseValidator;
import no.idporten.eidas.lightprotocol.messages.LightResponse;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import static no.idporten.eidas.connector.web.SessionAttributes.SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ConnectorResponseController {

    private final SpecificCommunicationService specificCommunicationService;
    private final EidasCacheProperties eidasCacheProperties;

    private final SpecificConnectorService specificConnectorService;
    private final AuthorizationResponseHelper authorizationResponseHelper;
    private final AuditService auditService;


    @RequestMapping(path = "/ConnectorResponse", method = {RequestMethod.GET, RequestMethod.POST})
    public String handleConnectorResponse(@Nonnull final HttpServletRequest request,
                                          @Nonnull final HttpServletResponse response) throws IOException {

        final ILightResponse iLightResponse = getIncomingiLightResponse(request, null);

        if (!IncomingLightResponseValidator.validate(iLightResponse)) {
            throw new SpecificConnectorException(ErrorCodes.INVALID_REQUEST.getValue(), "Incoming Light Response is invalid. Rejecting request.");
        }
        if (iLightResponse instanceof LightResponse lightResponse) {
            CorrelatedRequestHolder cachedRequest = specificConnectorService.getCachedRequest(iLightResponse.getRelayState());
            if (cachedRequest == null || !iLightResponse.getInResponseToId().equals(cachedRequest.getiLightRequest().getId())) {
                auditService.auditLightResponse(lightResponse, null);
                throw new SpecificConnectorException(ErrorCodes.INVALID_REQUEST.getValue()
                        , "No request found for relay state %s and id %s."
                        .formatted(iLightResponse.getRelayState(), iLightResponse.getInResponseToId()));
            }
            auditService.auditLightResponse(lightResponse, cachedRequest.getAuthenticationRequest().getRequestTraceId());

        } else {
            throw new SpecificConnectorException(ErrorCodes.INVALID_REQUEST.getValue(), "Response not instance of LightResponse");
        }
        final StatusCodeTranslator statusCodeTranslator = StatusCodeTranslator.fromEidasStatusCodeString(iLightResponse.getStatus().getStatusCode());
        if (StatusCodeTranslator.SUCCESS == statusCodeTranslator) {
            return handleConnectorResponse(request, response, (LightResponse) iLightResponse);
        } else {
            throw new SpecificConnectorException(ErrorCodes.INVALID_REQUEST.getValue(), "IDP reported an error status: %s.".formatted(iLightResponse.getStatus()));
        }
    }

    private String handleConnectorResponse(HttpServletRequest request, HttpServletResponse response, LightResponse lightResponse) throws IOException {
        PushedAuthorizationRequest parRequest = (PushedAuthorizationRequest) request.getSession().getAttribute(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST);
        UserMatchResponse userMatchResponse = specificConnectorService.matchUser(lightResponse, new HashSet(parRequest.getScope()));
        switch (userMatchResponse) {
            case UserMatchFound f -> {
                return authorizationResponseHelper.returnAuthorizationCode(request, response, lightResponse.getLevelOfAssurance(), parRequest, f.eidasUser(), f.pid());
            }
            case UserMatchNotFound nf -> {
                return authorizationResponseHelper.returnAuthorizationCode(request, response, lightResponse.getLevelOfAssurance(), parRequest, nf.eidasUser(), null);
            }
            case UserMatchRedirect r -> {
                return "redirect:%s".formatted(r.redirectUrl());
            }
            case UserMatchError error -> { //ignore errors and carry on
                return authorizationResponseHelper.returnAuthorizationCode(request, response, lightResponse.getLevelOfAssurance(), parRequest, error.eidasUser(), null);
            }
            default ->
                    throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Unexpected user match response");
        }
    }


    private ILightResponse getIncomingiLightResponse(@Nonnull HttpServletRequest request, final Collection<AttributeDefinition<?>> registry) {
        final String lightTokenId = getLightTokenId(request);
        return specificCommunicationService.getAndRemoveResponse(lightTokenId, registry);
    }

    protected String getLightTokenId(HttpServletRequest request) {
        String tokenBase64 = BinaryLightTokenHelper.getBinaryToken(request, EidasParameterKeys.TOKEN.toString());
        return BinaryLightTokenHelper.getBinaryLightTokenId(tokenBase64, eidasCacheProperties.getResponseSecret(), eidasCacheProperties.getAlgorithm());
    }


}
