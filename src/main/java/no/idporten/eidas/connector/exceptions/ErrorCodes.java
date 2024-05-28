package no.idporten.eidas.connector.exceptions;

import jakarta.annotation.Nonnull;

/**
 * Error codes for eIDAS proxy.
 * Values may change
 */
public enum ErrorCodes {

    INTERNAL_ERROR("internal_error"),
    INVALID_REQUEST("invalid_request"),
    INVALID_SESSION("invalid_session"),
    INVALID_TOKEN("invalid_token");

    private final transient String value;

    ErrorCodes(@Nonnull String value) {
        this.value = value;
    }

    @Nonnull
    public String getValue() {
        return value;
    }

    @Nonnull
    @Override
    public String toString() {
        return value;
    }
}
