package no.idporten.eidas.connector.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "eidas.countries")
@NoArgsConstructor
@Validated
@Setter
public class EUCountriesProperties {
    @NotEmpty
    private List<String> included;
    @Getter
    private boolean test;
    private List<String> excluded;

    public String included() {
        if (CollectionUtils.isEmpty(this.included)) {
            return "";
        }
        return String.join(",", this.included.stream().map(String::toLowerCase).toList());
    }

    public String excluded() {
        if (CollectionUtils.isEmpty(this.excluded)) {
            return "";
        }
        return String.join(",", this.excluded.stream().map(String::toLowerCase).toList());
    }
}
