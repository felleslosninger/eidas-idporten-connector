package no.idporten.eidas.connector.config;

import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import no.digdir.oidc.redis.service.RedisOpenIDConnectCache;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegrationBase;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(OidcServerProperties.class)
@Setter
@Slf4j
@Validated
public class OpenIDConnectIntegrationConfiguration {

    @Bean
    public OpenIDConnectIntegration openIDConnectSdk(OpenIDConnectSdkConfiguration openIDConnectSdkConfiguration)  {
        return new OpenIDConnectIntegrationBase(openIDConnectSdkConfiguration);
    }
    @Bean
    public OpenIDConnectSdkConfiguration openIDConnectSdkConfiguration(RedisOpenIDConnectCache cache, AuditService auditService, OidcServerProperties oidcServerProperties) throws Exception {
        RSAKey jwk = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .generate();
        OpenIDConnectSdkConfiguration.OpenIDConnectSdkConfigurationBuilder builder = OpenIDConnectSdkConfiguration.builder()
                .internalId("eidas")
                .issuer(oidcServerProperties.getIssuer())
                .requirePkce(oidcServerProperties.isRequirePkce())
                .pushedAuthorizationRequestEndpoint(UriComponentsBuilder.fromUri(oidcServerProperties.getIssuer()).path("/par").build().toUri())
                .authorizationEndpoint(UriComponentsBuilder.fromUri(oidcServerProperties.getIssuer()).path("/authorize").build().toUri())
                .requirePushedAuthorizationRequests(false)
                .authorizationResponseIssParameterSupported(oidcServerProperties.isAuthorizationResponseIssParameterSupported())
                .tokenEndpoint(UriComponentsBuilder.fromUri(oidcServerProperties.getIssuer()).path("/token").build().toUri())
                .jwksUri(UriComponentsBuilder.fromUri(oidcServerProperties.getIssuer()).path("/jwks").build().toUri())
                .authorizationRequestLifetimeSeconds(oidcServerProperties.getParLifetimeSeconds())
                .authorizationLifetimeSeconds(oidcServerProperties.getAuthorizationLifetimeSeconds())
                .accessTokenLifetimeSeconds(oidcServerProperties.getAccessTokenLifetimeSeconds())
                .clients(oidcServerProperties.getClients())
                .cache(cache)
                .auditLogger(auditService)
                .jwk(jwk)
                .uiLocale("en")
                .responseModes(oidcServerProperties.getResponseModesSupported())
                .acrValues(oidcServerProperties.getAcrValues())
                .userinfoEndpoint(UriComponentsBuilder.fromUri(oidcServerProperties.getIssuer()).path("/userinfo").build().toUri())
                .scopesSupported(oidcServerProperties.getScopesSupported());

        if (oidcServerProperties.getKeystoreType() == null) {
            builder.jwk(generateServerKeys());
        } else {
            builder.keystore(loadServerKeystore(oidcServerProperties), oidcServerProperties.getKeystoreKeyAlias(),oidcServerProperties.getKeystoreKeyPassword());
        }
        return builder.build();
    }

    public RSAKey generateServerKeys() throws Exception {
        RSAKey rsaKey = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .generate();
        log.info("Generated server keys for signing av tokens.");
        return rsaKey;
    }

    public KeyStore loadServerKeystore(OidcServerProperties osp) {
        try (InputStream is = new FileInputStream(new DefaultResourceLoader().getResource(osp.getKeystoreLocation()).getFile())) {
            KeyStore keyStore = KeyStore.getInstance(osp.getKeystoreType());
            keyStore.load(is, osp.getKeystorePassword().toCharArray());
            log.info("Loaded server keystore from {}", osp.getKeystoreLocation());
            return keyStore;
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
