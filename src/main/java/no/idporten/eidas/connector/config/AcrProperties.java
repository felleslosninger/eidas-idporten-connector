package no.idporten.eidas.connector.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

@Data
@Validated
@Configuration
@NoArgsConstructor
@Slf4j
@ConfigurationProperties(prefix = "eidas.acr")
public class AcrProperties implements InitializingBean {

    @NotEmpty
    private List<String> supportedAcrValues;

    @NotEmpty
    private Map<String, String> acrValueMap;

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Loaded Acr Proxy properties: supported {} and acrValueMap {}", this.supportedAcrValues, this.acrValueMap.entrySet());
    }
}
