package no.idporten.eidas.connector.integration.freggateway.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.commons.nullanalysis.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

@Data
@Validated
@NoArgsConstructor
@Slf4j
@ConfigurationProperties(prefix = "eidas.freg-gw")
public class FregProperties implements InitializingBean {

    @NotNull
    private String apiKey;

    @NotNull
    private URI baseUri;

    @Min(1)
    private long connectTimeoutMs;

    @Min(1)
    private long readTimeoutMs;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("FregProperties enabled and initialized with apiKey: {}, baseUri: {}, connectTimeoutMs: {}, readTimeoutMs: {}",
                maskString(apiKey), baseUri, connectTimeoutMs, readTimeoutMs);
    }

    public static String maskString(String input) {
        if (input == null || input.length() <= 3) {
            return input;
        }
        int length = input.length();
        return "*".repeat(length - 3) + input.substring(length - 3);
    }
}
