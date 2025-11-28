package no.idporten.eidas.connector.integration.nobid.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Map;

@Validated
@Slf4j
@ConfigurationProperties(prefix = "eidas.nobid")
public record NobidProperties(
        boolean enabled,
        @DefaultValue("5s")
        Duration connectTimeout,
        @DefaultValue("5s")
        Duration readTimeout,
        @NotNull OidcProvider matchingService,
        Map<String, String> demoCountryCodeMap
) {
    @PostConstruct
    public void logInitialized() {
        log.info("Nobid integration enabled: {}", enabled);
        log.info("Nobid connectTimeout: {}", connectTimeout.toMillis());
        log.info("Nobid readTimeout: {}", readTimeout.toMillis());
        log.info("Nobid matchingService: {}", matchingService);
        log.info("Nobid country code map: {}", demoCountryCodeMap);
    }
}
