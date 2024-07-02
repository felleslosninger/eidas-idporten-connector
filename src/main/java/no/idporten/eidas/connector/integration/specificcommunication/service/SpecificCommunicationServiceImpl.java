package no.idporten.eidas.connector.integration.specificcommunication.service;

import eu.eidas.auth.commons.attribute.AttributeDefinition;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.tx.BinaryLightToken;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.exceptions.ErrorCodes;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.specificcommunication.config.EidasCacheProperties;
import no.idporten.eidas.lightprotocol.BinaryLightTokenHelper;
import no.idporten.eidas.lightprotocol.LightRequestToXML;
import no.idporten.eidas.lightprotocol.LightResponseParser;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecificCommunicationServiceImpl implements SpecificCommunicationService {

    private final LightRedisCache<String, String> lightRedisCache;
    private final EidasCacheProperties eidasCacheProperties;


    @Override
    public BinaryLightToken putResponse(ILightResponse lightResponse) throws SpecificConnectorException {
        throw new UnsupportedOperationException("Not relevant for this service implementation");
    }

    @Override
    public ILightResponse getAndRemoveResponse(String lightTokenId, Collection<AttributeDefinition<?>> registry) throws SpecificConnectorException {
        log.debug("getAndRemoveResponse  {}", lightTokenId);
        String xmlMessage = lightRedisCache.get(eidasCacheProperties.getLightResponsePrefix(lightTokenId));
        log.debug("Got message from cache {}", xmlMessage);
        try {
            return LightResponseParser.parseXml(xmlMessage);
        } catch (JAXBException e) {
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Failed to parse message %s".formatted(e.getMessage()));
        }

    }

    /**
     * eidas-idporten-proxy does not send lightrequests
     */
    @Override
    public BinaryLightToken putRequest(ILightRequest lightRequest) throws SpecificConnectorException {
        String xmlRequest;
        try {
            xmlRequest = LightRequestToXML.toXml(lightRequest);
            log.debug("Storing xml lightRequest {}", xmlRequest);
        } catch (JAXBException e) {
            log.error("Failed to convert lightRequest to XML {}", e.getMessage());
            throw new SpecificConnectorException("Failed to convert lightRequest to XML", e);
        }
        BinaryLightToken binaryLightToken = BinaryLightTokenHelper
                .createBinaryLightToken(eidasCacheProperties.getRequestIssuerName(),
                        eidasCacheProperties.getRequestSecret(),
                        eidasCacheProperties.getAlgorithm());
        log.debug("putRequest {}", binaryLightToken.getToken().getId());
        lightRedisCache.set(eidasCacheProperties.getLightRequestPrefix(binaryLightToken.getToken().getId()), xmlRequest, Duration.ofSeconds(eidasCacheProperties.getLightRequestLifetimeSeconds()));
        return binaryLightToken;

    }

    @Override
    public ILightRequest getAndRemoveRequest(String tokenBase64, Collection<AttributeDefinition<?>> registry) throws SpecificConnectorException {
        throw new NotImplementedException("Not relevant for this service implementation");
    }
}
