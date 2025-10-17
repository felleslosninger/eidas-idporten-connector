package no.idporten.eidas.connector.integration.freggateway.config;


import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.integration.freggateway.service.FregGwMatchingServiceClient;
import no.idporten.eidas.connector.service.CountryCodeConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

@Configuration
@EnableConfigurationProperties(FregProperties.class)
@ConditionalOnProperty(prefix = "eidas.freg-gw", name = "enabled", havingValue = "true")
@Slf4j
public class FregConfiguration {
    @Value("${spring.application.environment:prod}")
    private String environment;


    @Bean
    RestClient.Builder fregGatewayEndpointBuilder(FregProperties fregProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(fregProperties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(fregProperties.getReadTimeoutMs()));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "%s,%s".formatted(MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE))
                .defaultHeader("X-API-KEY", fregProperties.getApiKey())
                .defaultHeader("Client-Id", "eidas-idporten-connector-%s".formatted(environment))
                .baseUrl(fregProperties.getBaseUri().toString());
    }


    @Bean
    FregGwMatchingServiceClient matchingService(RestClient.Builder fregGatewayEndpointBuilder, FregProperties fregProperties) {
        return new FregGwMatchingServiceClient(fregGatewayEndpointBuilder, new CountryCodeConverter(Optional.ofNullable(fregProperties.getDemoCountryCodeMap())));
    }

    @PostConstruct
    void logMe() {
        log.info("FregGateway integration enabled");
    }

}
