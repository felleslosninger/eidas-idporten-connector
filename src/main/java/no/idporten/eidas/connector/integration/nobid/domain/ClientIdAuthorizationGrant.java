package no.idporten.eidas.connector.integration.nobid.domain;

import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.id.ClientID;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps an existing AuthorizationGrant and adds an explicit client_id
 * parameter to the token request. Since nobid demands it
 */
public final class ClientIdAuthorizationGrant extends AuthorizationGrant {

    private final AuthorizationGrant delegate;
    private final ClientID clientId;

    public ClientIdAuthorizationGrant(AuthorizationGrant delegate, ClientID clientId) {
        super(delegate.getType());
        this.delegate = delegate;
        this.clientId = clientId;
    }

    @Override
    public Map<String, List<String>> toParameters() {
        // Preserve order by using LinkedHashMap
        Map<String, List<String>> params = new LinkedHashMap<>(delegate.toParameters());
        if (clientId != null) {
            params.put("client_id", Collections.singletonList(clientId.getValue()));
        }
        return params;
    }
}
