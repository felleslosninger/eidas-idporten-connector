package no.idporten.eidas.connector.integration.nobid.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@Slf4j
@ConfigurationProperties(prefix = "eidas.nobid")
public record NobidProperties(
        boolean enabled,
        @Min(1) Duration connectTimeout,
        @Min(1) Duration readTimeout,
        @NotNull OidcProvider nobidMatchingService
) {
    @PostConstruct
    public void logInitialized() {
        long connectMs = connectTimeout != null ? connectTimeout.toMillis() : -1;
        long readMs = readTimeout != null ? readTimeout.toMillis() : -1;
        log.info("NobidProperties initialized with enabled={}, connectTimeout={}ms, readTimeout={}ms",
                enabled, connectMs, readMs);
    }
}
