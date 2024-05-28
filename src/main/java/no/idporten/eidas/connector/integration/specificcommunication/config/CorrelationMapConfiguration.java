package no.idporten.eidas.connector.integration.specificcommunication.config;

import no.idporten.eidas.connector.integration.specificcommunication.caches.LightningTokenRequestCache;
import no.idporten.eidas.connector.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.connector.integration.specificcommunication.service.LightRedisCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration

public class CorrelationMapConfiguration {
    @Bean("connectorRequestCache")
    public OIDCRequestCache specificConnectorRequestCorrelationMap(LightRedisCache lightRedisCache, EidasCacheProperties eidasCacheProperties) {
        return new OIDCRequestCache(lightRedisCache, eidasCacheProperties);
    }

    @Bean("lightningTokenResponseCache")
    public LightningTokenRequestCache tokenResponseCorrelationMap(LightRedisCache lightRedisCache, EidasCacheProperties eidasCacheProperties) {
        return new LightningTokenRequestCache(lightRedisCache, eidasCacheProperties);
    }
}
