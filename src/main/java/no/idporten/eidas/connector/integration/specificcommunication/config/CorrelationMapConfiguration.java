package no.idporten.eidas.connector.integration.specificcommunication.config;

import eu.eidas.auth.commons.light.ILightRequest;
import no.idporten.eidas.connector.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.connector.integration.specificcommunication.caches.LightningTokenRequestCache;
import no.idporten.eidas.connector.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.connector.integration.specificcommunication.service.LightRedisCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration

public class CorrelationMapConfiguration {
    @Bean("connectorRequestCache")
    public OIDCRequestCache specificConnectorRequestCorrelationMap(LightRedisCache<String, CorrelatedRequestHolder> lightRedisCache, EidasCacheProperties eidasCacheProperties) {
        return new OIDCRequestCache(lightRedisCache, eidasCacheProperties);
    }

    @Bean("lightningTokenRequestCache")
    public LightningTokenRequestCache tokenResponseCorrelationMap(LightRedisCache<String, ILightRequest> lightRedisCache, EidasCacheProperties eidasCacheProperties) {
        return new LightningTokenRequestCache(lightRedisCache, eidasCacheProperties);
    }
}
