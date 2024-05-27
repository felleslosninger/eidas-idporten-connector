package no.idporten.eidas.connector.integration.specificcommunication.service;

import eu.eidas.auth.commons.attribute.AttributeDefinition;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.tx.BinaryLightToken;
import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.specificcommunication.BinaryLightTokenHelper;
import no.idporten.eidas.connector.integration.specificcommunication.config.EidasCacheProperties;
import no.idporten.eidas.lightprotocol.LightRequestToXML;
import no.idporten.eidas.lightprotocol.LightResponseParser;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpecificCommunicationServiceImpl implements SpecificCommunicationService {

    private final LightRedisCache lightRedisCache;
    private final EidasCacheProperties eidasCacheProperties;

    public ILightRequest getAndRemoveRequest(String lightTokenId, Collection<AttributeDefinition<?>> registry) {
        log.info("getAndRemoveRequest {}", lightTokenId);
        return (ILightRequest) lightRedisCache.get(eidasCacheProperties.getLightRequestPrefix(lightTokenId));

    }

    @Override
    public BinaryLightToken putResponse(ILightResponse lightResponse) throws SpecificConnectorException {
        throw new UnsupportedOperationException("Not relevant for this service implementation");
    }

    @Override
    public ILightResponse getAndRemoveResponse(String lightTokenId, Collection<AttributeDefinition<?>> registry) throws SpecificConnectorException {
        log.info("getAndRemoveResponse  {}", lightTokenId);
        String xmlMessage = (String) lightRedisCache.get(eidasCacheProperties.getLightResponsePrefix(lightTokenId));
        log.info("Got message from cache {}", xmlMessage);
        try {
            return LightResponseParser.parseXml(xmlMessage);
        } catch (JAXBException e) {
            log.error("Failed to parse message. We ignore it for now and carry on {}", e.getMessage());
        }
        log.info("Can't parse message yet. We ignore it for now and carry on");
        return null;
    }

    /**
     * eidas-idporten-proxy does not send lightrequests
     */
    @Override
    public BinaryLightToken putRequest(ILightRequest lightRequest) throws SpecificConnectorException {
        String xmlRequest;
        try {
            xmlRequest = LightRequestToXML.toXml(lightRequest);
            log.info("Storing xml lightRequest {}", xmlRequest);
        } catch (JAXBException e) {
            log.error("Failed to convert lightRequest to XML {}", e.getMessage());
            throw new SpecificConnectorException("Failed to convert lightRequest to XML", e);
        }
        BinaryLightToken binaryLightToken = BinaryLightTokenHelper
                .createBinaryLightToken(eidasCacheProperties.getRequestIssuerName(),
                        eidasCacheProperties.getRequestSecret(),
                        eidasCacheProperties.getAlgorithm());
        log.info("putRequest {}", binaryLightToken.getToken().getId());
        lightRedisCache.set(eidasCacheProperties.getLightRequestPrefix(binaryLightToken.getToken().getId()), xmlRequest, Duration.ofSeconds(eidasCacheProperties.getLightRequestLifetimeSeconds()));
        return binaryLightToken;


    }
}
