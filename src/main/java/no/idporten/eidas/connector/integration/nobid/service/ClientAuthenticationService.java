package no.idporten.eidas.connector.integration.nobid.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.JWTID;
import no.idporten.eidas.connector.integration.nobid.config.KeyStoreProperties;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProvider;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreDirectAccess;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ClientAuthenticationService {

    /**
     * Create client authentication towards OIDC provider.
     *
     * @param oidcProvider oidc provider config
     * @return client authentication using configured client authentication method
     */
    public static ClientAuthentication createClientAuthentication(OidcProvider oidcProvider) throws JOSEException {
        if (ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(oidcProvider.clientAuthenticationMethod())) {
            Instant now = Instant.now();
            JWTAuthenticationClaimsSet claims = new JWTAuthenticationClaimsSet(
                    new Issuer(oidcProvider.clientId()),
                    new ClientID(oidcProvider.clientId()),                       // iss + sub
                    List.of(new Audience(oidcProvider.issuer())),
                    Date.from(now.plusSeconds(60)),
                    null,
                    Date.from(now),
                    new JWTID(UUID.randomUUID().toString())
            );

            JWSAlgorithm alg = oidcProvider.jwsAlgorithms().stream().findFirst().orElse(JWSAlgorithm.RS256);
            KeyProvider keyProvider = oidcProvider.keyProvider();
            return new PrivateKeyJWT(
                    claims,
                    JWSAlgorithm.RS256,
                    keyProvider.privateKey(),
                    oidcProvider.kid(),
                    null // no special JCA provider
            );
        }
        if (ClientAuthenticationMethod.CLIENT_SECRET_JWT.equals(oidcProvider.clientAuthenticationMethod())) {
            return new ClientSecretJWT(new ClientID(oidcProvider.clientId()), oidcProvider.issuer(), JWSAlgorithm.HS256, new Secret(oidcProvider.clientSecret()));
        }
        if (ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(oidcProvider.clientAuthenticationMethod())) {
            return new ClientSecretPost(new ClientID(oidcProvider.clientId()), new Secret(oidcProvider.clientSecret()));
        }
        return new ClientSecretBasic(new ClientID(oidcProvider.clientId()), new Secret(oidcProvider.clientSecret()));
    }

}
