package no.idporten.eidas.connector.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

public record EidasLoginHint(@JsonProperty("foregin-identifier") String foreignIdentifier,
                             @JsonProperty("subject-country-code") String subjectCountryCode,
                             @JsonProperty("serviceprovider-country-code") String serviceProviderCountryCode,
                             @JsonProperty("birth-date") String birthDate
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
