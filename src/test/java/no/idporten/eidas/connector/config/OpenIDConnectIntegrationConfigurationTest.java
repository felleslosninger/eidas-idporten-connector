package no.idporten.eidas.connector.config;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import no.digdir.oidc.redis.service.RedisOpenIDConnectCache;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenIDConnectIntegrationConfigurationTest {

    @Mock
    private RedisOpenIDConnectCache cache;
    @Mock
    private AuditService auditService;
    @Mock
    private OidcServerProperties properties;
    @InjectMocks
    private OpenIDConnectIntegrationConfiguration config;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getIssuer()).thenReturn(URI.create("http://localhost:8080"));
        lenient().when(properties.isRequirePkce()).thenReturn(true);
        lenient().when(properties.getParLifetimeSeconds()).thenReturn(3600);
    }

    @Test
    void testOpenIDConnectSdkConfiguration() throws Exception {
        when(properties.getKeystoreType()).thenReturn(null); // To trigger generateServerKeys

        OpenIDConnectSdkConfiguration result = config.openIDConnectSdkConfiguration(cache, auditService, properties);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals("eidas", result.getInternalId()),
                () -> assertEquals("http://localhost:8080", result.getIssuer().toString()),
                () -> assertEquals("http://localhost:8080/authorize", result.getAuthorizationEndpoint().toString()),
                () -> assertEquals(1, result.getUiLocales().size()),
                () -> assertEquals("en", result.getUiLocales().getFirst()),
                () -> assertTrue(result.isRequirePkce()),
                () -> assertEquals(3600, result.getAuthorizationRequestLifetimeSeconds()),
                () -> assertNotNull(result.getJwk()))
        ;

    }

    @Test
    void testGenerateServerKeys() throws Exception {
        RSAKey rsaKey = config.generateServerKeys();
        assertNotNull(rsaKey);
        assertEquals(KeyUse.SIGNATURE, rsaKey.getKeyUse());
        assertNotNull(rsaKey.getKeyID());
    }


}
