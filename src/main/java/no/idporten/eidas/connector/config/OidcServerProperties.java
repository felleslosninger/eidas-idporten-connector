package no.idporten.eidas.connector.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import no.idporten.sdk.oidcserver.client.ClientMetadata;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.util.List;

@Data
@Slf4j
@Validated
@ConfigurationProperties(prefix = "eidas.oidc-server")
public class OidcServerProperties {
    @NotNull
    private URI issuer;
    private boolean requirePkce;
    @NotEmpty
    private List<ClientMetadata> clients;
    @Min(1)
    private int parLifetimeSeconds = 60;
    @Min(1)
    private int authorizationLifetimeSeconds = 60;
    @Min(1)
    private int accessTokenLifetimeSeconds = 60;
    @NotEmpty
    private List<String> responseModesSupported = List.of("query");
    @NotEmpty
    private List<String> acrValues;
    private boolean authorizationResponseIssParameterSupported = true;
    @NotEmpty
    private List<String> scopesSupported;
    private String keystoreType;
    private String keystoreLocation;
    private String keystorePassword;
    private String keystoreKeyAlias;
    private String keystoreKeyPassword;
}
