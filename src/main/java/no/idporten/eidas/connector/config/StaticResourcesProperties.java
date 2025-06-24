package no.idporten.eidas.connector.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

@Data
@ConfigurationProperties(prefix = "digdir.static.design")
@Slf4j
@Configuration
public class StaticResourcesProperties {

    private static final String LATEST_VERSION = "latest";
    private static final String DEFAULT_THEME = "idporten";

    private String version = LATEST_VERSION;
    private String themeId = DEFAULT_THEME;
    private String host = "https://static.idporten.no";

    public String getStaticResourcesBaseUri() {
        return UriComponentsBuilder.fromUriString(host)
                .path(version)
                .build()
                .toString();
    }

    @PostConstruct
    public void logConfig() {
        log.info("Loaded static resources properties: version {}, themeId {}", this.version, this.themeId);
    }
}
