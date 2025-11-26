package no.idporten.eidas.connector.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;

/**
 * EIDAS login_hint JSON structure expected by Nobid claims provider.
 * <p>
 * Example:
 * {
 * "natural_person": {
 * "person_identifier":"SE/NO/199210199320",
 * "current_given_name":"Allslags",
 * "current_family_name":"Lekeplass",
 * "date_of_birth":"1992-10-19"
 * }
 * }
 */
public record EidasLoginHint(
        @JsonProperty("natural_person") NaturalPerson naturalPerson
) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NaturalPerson(
            @JsonProperty("person_identifier") String personIdentifier,
            @JsonProperty("current_given_name") String currentGivenName,
            @JsonProperty("current_family_name") String currentFamilyName,
            @JsonProperty("date_of_birth") String dateOfBirth
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
