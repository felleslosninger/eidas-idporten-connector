package no.idporten.eidas.connector.integration.nobid.web;

import com.nimbusds.oauth2.sdk.PushedAuthorizationRequest;
import lombok.Data;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProtocolVerifiers;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serial;
import java.io.Serializable;

@Component
@SessionScope
@Data
public class NobidSession implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private OidcProtocolVerifiers oidcProtocolVerifiers;
    private String levelOfAssurance;
    private EidasUser eidasUser;
    private PushedAuthorizationRequest pushedAuthorizationRequest;

}
