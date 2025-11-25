package no.idporten.eidas.connector.integration.nobid.domain;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.nobid.config.KeyStoreProperties;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreDirectAccess;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.time.Duration;
import java.util.*;

/**
 * Representation of an OIDC provider eidas connector is integrating with.
 */
public record OidcProvider(
        @NotEmpty String id,
        boolean enabled,
        List<String> acrValues,
        List<String> amr,
        @NotEmpty URI issuer,
        @NotEmpty URI authorizationEndpoint,
        URI pushedAuthorizationRequestEndpoint,
        @NotEmpty URI tokenEndpoint,
        URI jwksEndpoint,
        @DefaultValue("5m") Duration jwksCacheRefresh,
        @DefaultValue("60m") Duration jwksCacheLifetime,
        @DefaultValue("2s") Duration connectTimeout,
        @DefaultValue("5s") Duration readTimeout,
        @NotEmpty String clientId,
        @NotEmpty String clientSecret,
        @NotNull ClientAuthenticationMethod clientAuthenticationMethod,
        Set<String> scopes,
        @NotNull ResponseMode responseMode,
        Map<String, String> customParameters,
        Set<String> cancelErrorCodes,
        Set<String> requiredClaims,
        Map<String, String> claimsMapping,
        @NotEmpty Set<JWSAlgorithm> jwsAlgorithms,
        @NotEmpty URI redirectUri,
        KeyStoreProperties clientKeystore,
        String kid
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 141594090657324127L;

    public OidcProvider {
        acrValues = acrValues == null ? Collections.emptyList() : List.copyOf(acrValues);
        amr = amr == null ? Collections.emptyList() : List.copyOf(amr);
        scopes = scopes == null ? Collections.emptySet() : Set.copyOf(scopes);
        responseMode = responseMode == null ? ResponseMode.QUERY : responseMode;
        clientAuthenticationMethod = clientAuthenticationMethod == null ? ClientAuthenticationMethod.CLIENT_SECRET_BASIC : clientAuthenticationMethod;
        customParameters = customParameters == null ? Collections.emptyMap() : Map.copyOf(customParameters);
        cancelErrorCodes = cancelErrorCodes == null || cancelErrorCodes.isEmpty() ? Set.of("access_denied") : Set.copyOf(cancelErrorCodes);
        requiredClaims = requiredClaims == null ? Collections.emptySet() : Set.copyOf(requiredClaims);
        claimsMapping = claimsMapping == null ? Collections.emptyMap() : Map.copyOf(claimsMapping);
        jwsAlgorithms = (jwsAlgorithms == null || jwsAlgorithms.isEmpty()) ? Set.of(JWSAlgorithm.RS256) : Set.copyOf(jwsAlgorithms);
    }

    /**
     * Creates and load a keyprovider
     * Consider making this a bean
     * @return keyProvicer
     */
    public KeyProvider keyProvider() {
        if(clientKeystore == null) {
            throw new SpecificConnectorException("server_error", "Client keystore not configured correctly");
        }
        return KeystoreDirectAccess.createKeyProvider(
                clientKeystore.location(), // location: classpath:, file:, or base64:
                clientKeystore.type(),
                clientKeystore.password(),
                clientKeystore.keyAlias(),
                clientKeystore.keyPassword()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}