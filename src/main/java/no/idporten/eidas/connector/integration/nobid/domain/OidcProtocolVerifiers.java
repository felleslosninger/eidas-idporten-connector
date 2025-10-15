package no.idporten.eidas.connector.integration.nobid.domain;

import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.Nonce;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable verification objects for OIDC/OAuth2 protocols.  Generates state, nonce and code verifier when created.
 */
public record OidcProtocolVerifiers(String providerId, State state, Nonce nonce, CodeVerifier codeVerifier,
                                    Instant timestamp) implements Serializable {

    public OidcProtocolVerifiers(String providerId) {
        this(providerId, new State(), new Nonce(), new CodeVerifier(), Instant.now());
    }

    public OidcProtocolVerifiers {
        Objects.requireNonNull(providerId, "providerId must have value");
        Objects.requireNonNull(state, "state must have value");
        Objects.requireNonNull(nonce, "nonce must have value");
        Objects.requireNonNull(codeVerifier, "codeVerifier must have value");
        Objects.requireNonNull(timestamp, "timestamp must have value");
    }

}
