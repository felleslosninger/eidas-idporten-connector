package no.idporten.eidas.connector.integration.nobid.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProvider;
import no.idporten.eidas.connector.integration.nobid.service.NobidMatchingServiceClient;
import no.idporten.eidas.connector.integration.nobid.web.NobidSession;
import no.idporten.eidas.connector.logging.AuditService;
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

import java.util.Optional;

@Configuration
@EnableConfigurationProperties(NobidProperties.class)
@ConditionalOnProperty(prefix = "eidas.nobid", name = "enabled", havingValue = "true")
@Slf4j
public class NobidConfiguration {
    @Value("${spring.application.environment}")
    private String environment;

    @Bean
    RestClient.Builder nobidEndpointBuilder(NobidProperties nobidProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(nobidProperties.connectTimeout());
        requestFactory.setReadTimeout(nobidProperties.readTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "%s,%s".formatted(MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE))
                .defaultHeader("idporten-eidas-environment", environment));
    }

    @Bean
    OidcProvider nobidClaimsProvider(NobidProperties nobidProperties) {
        return nobidProperties.matchingService();
    }

    @Bean
    NobidMatchingServiceClient matchingService(
            OidcProvider nobidClaimsProvider,
            NobidSession nobidSession,
            ObjectMapper objectMapper,
            AuditService auditService,
            NobidProperties nobidProperties
    ) {
        return new NobidMatchingServiceClient(
                nobidClaimsProvider,
                nobidSession,
                objectMapper,
                auditService,
                new CountryCodeConverter(Optional.ofNullable(nobidProperties.demoCountryCodeMap())));
    }

}
