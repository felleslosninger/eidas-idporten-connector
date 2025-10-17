package no.idporten.eidas.connector.domain;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import no.idporten.eidas.connector.service.EIDASIdentifier;

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
        return new EidasLoginHint(eidasIdentifier.getForeignIdentifier(), eidasIdentifier.getSubjectCountryCode(), eidasIdentifier.getSpCountryCode(), birthdate);
    }
}
