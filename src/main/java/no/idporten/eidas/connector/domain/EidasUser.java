package no.idporten.eidas.connector.domain;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import no.idporten.eidas.connector.config.EidasClaims;
import no.idporten.eidas.connector.service.EIDASIdentifier;
import org.springframework.util.CollectionUtils;

import java.beans.Transient;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

//eidasClaims not used yet
public record EidasUser(@NotNull EIDASIdentifier eidasIdentifier, String birthdate,
                        @Nullable Map<String, String> eidasClaims) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Transient
    public EidasLoginHint toLoginHint() {
        EidasLoginHint.NaturalPerson naturalPerson = new EidasLoginHint.NaturalPerson(
                eidasIdentifier.getFormattedEidasIdentifier(),
                CollectionUtils.isEmpty(eidasClaims) ? null : eidasClaims.get(EidasClaims.IDPORTEN_EIDAS_GIVEN_NAME_CLAIM),
                CollectionUtils.isEmpty(eidasClaims) ? null : eidasClaims.get(EidasClaims.IDPORTEN_EIDAS_FAMILY_NAME_CLAIM),
                birthdate);
        return new EidasLoginHint(naturalPerson);
    }

    /*
      {
        "natural_person": {
          "person_identifier":"SE/NO/199210199320",
          "current_given_name":"Allslags",
          "current_family_name":"Lekeplass",
          "date_of_birth":"1992-10-19"
        }
      }
     */
}
