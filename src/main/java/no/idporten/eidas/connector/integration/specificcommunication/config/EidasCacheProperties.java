package no.idporten.eidas.connector.integration.specificcommunication.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Data
@ConfigurationProperties(prefix = "eidas.cache")
@Validated
public class EidasCacheProperties {

    public static final String LIGHT_REQUEST = "specificNodeConnectorRequestCache";
    public static final String LIGHT_RESPONSE = "nodeSpecificConnectorResponseCache";
    public static final String IDP_REQUEST = "correlation-map";
    private static final String PREFIX_TEMPLATE = "%s:%s";

    private long lightRequestLifetimeSeconds = 120;
    private long oidcRequestStateLifetimeSeconds = 700;
    private String requestSecret;
    private String responseSecret;
    private String algorithm = "SHA256";
    private String responseIssuerName = "specificCommunicationDefinitionConnectorResponse";
    private String requestIssuerName = "specificCommunicationDefinitionConnectorRequest";

    public String getLightRequestPrefix(String id) {
        return PREFIX_TEMPLATE.formatted(LIGHT_REQUEST, id);
    }

    public String getLightResponsePrefix(String id) {
        return PREFIX_TEMPLATE.formatted(LIGHT_RESPONSE, id);
    }


}
