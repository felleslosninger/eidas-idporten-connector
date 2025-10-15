package no.idporten.eidas.connector.integration.nobid.domain;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.ResponseMode;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.jarm.JARMValidator;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * Abstract, INTERNAL representation of an oidc provider ID-porten is integrating with.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OidcProvider implements Serializable {

    @Serial
    private static final long serialVersionUID = 141594090657324127L;
    @NotEmpty
    @EqualsAndHashCode.Include
    private String id;
    /**
     * Disables or enables the eid. Default is true.
     */
    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private List<String> acrValues = new ArrayList<>();

    /**
     * List of amr values to be used for this eid.  Uses amr claim from id_token if not set.  Last resort is to
     * use {@link #id}.
     */
    @Builder.Default
    private List<String> amr = new ArrayList<>();

    /**
     * The name to be displayed in the selector
     */
    @NotEmpty
    private String displayName;

    /**
     * The issuer of the eid
     */
    @NotEmpty
    private String issuer;

    /**
     * The authorization path of the eid
     */
    @NotEmpty
    private String authorizationEndpoint;

    /**
     * The URI to the pushed authorization endpoint.  Optional.
     */
    private String pushedAuthorizationRequestEndpoint;

    /**
     * The token path of the eid
     */
    @NotEmpty
    private String tokenEndpoint;

    /**
     * The jwks path of the eid
     * Not mandatory..yet
     */
    private String jwksEndpoint;

    /**
     * JWKS cache refresh in minutes
     */
    @Min(1)
    @Builder.Default
    private int jwksCacheRefreshMinutes = 5;
    /**
     * JWKS cache lifetime in minutes
     */
    @Min(1)
    @Builder.Default
    private int jwksCacheLifetimeMinutes = 60;

    /**
     * The connect timeout in millis for backchannel endpoint HTTP requests
     */
    @Min(1)
    private int connectTimeoutMs;

    /**
     * The read timeout in millis for backchannel endpoint HTTP requests
     */
    @Min(1)
    private int readTimeoutMs;

    /**
     * The selectors credentials towards the oidc provider.
     * Might consider storing these in a KeyVault later
     * DO NOT expose externally
     */
    @NotEmpty
    private String clientId;

    @NotEmpty
    private String clientSecret;

    @Builder.Default
    @NotNull
    private ClientAuthenticationMethod clientAuthenticationMethod = ClientAuthenticationMethod.CLIENT_SECRET_BASIC;

    @Builder.Default
    private Set<String> scopes = new HashSet<>();

    @Builder.Default
    @NotNull
    private ResponseMode responseMode = ResponseMode.QUERY;

    /**
     * The custom parameters for the selectors to use towards the eid.
     * Do not expose externally
     */
    @Builder.Default
    private Map<String, String> customParameters = new HashMap<>();

    /**
     * A set of error codes that signals user cancel in the claims provider.  Default code "access_denied" always included.
     */
    @Builder.Default
    private Set<String> cancelErrorCodes = new HashSet<>(List.of("access_denied"));

    /**
     * Required claims in returned id_token from oidc provider.
     */
    @Builder.Default
    private Set<String> requiredClaims = new HashSet<>();
    /**
     * Claims extraction mapping from returned id_token to internal claims.
     * The keys are names of claims in the target structure.
     * The values are names of claims in the id_token issued by oidc provider.
     */
    @Builder.Default
    private Map<String, String> claimsMapping = new HashMap<>();

    /**
     * Algorithms for id_token validation.
     */
    @NotEmpty
    @Builder.Default
    private Set<JWSAlgorithm> jwsAlgorithms = Set.of(JWSAlgorithm.RS256);

    public boolean usePushedAuthorizationRequest() {
        return pushedAuthorizationRequestEndpoint != null && !pushedAuthorizationRequestEndpoint.isEmpty();
    }

    public boolean useSignedAuthorizationResponse() {
        return ResponseMode.QUERY_JWT.equals(responseMode);
    }

    public boolean isClaimsProvider() {
        return false;
    }

    /**
     * A custom id token validator for this OIDC provider (must be set after reading config)
     */
    private IDTokenValidator idTokenValidator;

    /**
     * A validator for signed authorization responses if this OIDC provider requests signed responses (JARM)
     * (must be set after reading config)
     */
    private JARMValidator jarmValidator;
}