package no.idporten.eidas.connector.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "eidas.countries")
@Data
@Validated
public class EUCountriesProperties {
    @NotEmpty
    List<String> included;
    boolean test;
    List<String> excluded;
}
