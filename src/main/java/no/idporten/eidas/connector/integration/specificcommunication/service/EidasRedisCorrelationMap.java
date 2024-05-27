package no.idporten.eidas.connector.integration.specificcommunication.service;

import eu.eidas.auth.commons.light.ILightRequest;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.integration.specificcommunication.config.EidasCacheProperties;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EidasRedisCorrelationMap extends AbstractCorrelationMap<ILightRequest> {

    EidasRedisCorrelationMap(LightRedisCache lightRedisCache, EidasCacheProperties eidasCacheProperties) {
        super(lightRedisCache, eidasCacheProperties.getOidcRequestStateLifetimeSeconds());
    }
}
