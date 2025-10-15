package no.idporten.eidas.connector.integration.nobid.config;


import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.integration.matching.MatchingService;
import no.idporten.eidas.connector.integration.nobid.service.NobidMatchingServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(NobidProperties.class)
@ConditionalOnProperty(prefix = "eidas.nobid", name = "enabled", havingValue = "true")
@Slf4j
public class NobidConfiguration {
    @Value("${spring.application.environment:prod}")
    private String environment;

    @Bean
    RestClient.Builder nobidEndpointBuilder(NobidProperties nobidProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(nobidProperties.connectTimeout());
        requestFactory.setReadTimeout(nobidProperties.readTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, "%s,%s".formatted(MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE))
                .defaultHeader("Client-Id", "eidas-idporten-connector-%s".formatted(environment));
    }

    @Bean
    MatchingService matchingService(RestClient.Builder nobidEndpointBuilder) {
        return new NobidMatchingServiceClient(nobidEndpointBuilder);
    }

}
