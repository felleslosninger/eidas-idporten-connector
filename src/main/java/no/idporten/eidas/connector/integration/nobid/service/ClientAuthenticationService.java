package no.idporten.eidas.connector.integration.nobid.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.*;
import com.nimbusds.oauth2.sdk.id.ClientID;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProvider;
import org.springframework.stereotype.Service;

@Service
public class ClientAuthenticationService {

    /**
     * Create client authentication towards OIDC provider.
     *
     * @param oidcProvider oidc provider config
     * @return client authentication using configured client authentication method
     */
    public static ClientAuthentication createClientAuthentication(OidcProvider oidcProvider) throws JOSEException {
        if (ClientAuthenticationMethod.CLIENT_SECRET_JWT.equals(oidcProvider.clientAuthenticationMethod())) {
            return new ClientSecretJWT(new ClientID(oidcProvider.clientId()), oidcProvider.issuer(), JWSAlgorithm.HS256, new Secret(oidcProvider.clientSecret()));
        }
        if (ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(oidcProvider.clientAuthenticationMethod())) {
            return new ClientSecretPost(new ClientID(oidcProvider.clientId()), new Secret(oidcProvider.clientSecret()));
        }
        return new ClientSecretBasic(new ClientID(oidcProvider.clientId()), new Secret(oidcProvider.clientSecret()));
    }

}
