package no.idporten.eidas.connector.matching.domain;

import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;

public record UserMatchRedirect(@NotNull String redirectUrl)
        implements UserMatchResponse, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
