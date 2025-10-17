package no.idporten.eidas.connector.matching.domain;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import no.idporten.eidas.connector.domain.EidasUser;

import java.io.Serial;
import java.io.Serializable;

public record UserMatchNotFound(@NotNull EidasUser eidasUser, @Nullable String message)
        implements UserMatchResponse, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
