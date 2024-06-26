/* 
#   Copyright (c) 2017 European Commission  
#   Licensed under the EUPL, Version 1.2 or – as soon they will be 
#   approved by the European Commission - subsequent versions of the 
#    EUPL (the "Licence"); 
#    You may not use this work except in compliance with the Licence. 
#    You may obtain a copy of the Licence at: 
#    * https://joinup.ec.europa.eu/page/eupl-text-11-12  
#    *
#    Unless required by applicable law or agreed to in writing, software 
#    distributed under the Licence is distributed on an "AS IS" basis, 
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
#    See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package no.idporten.eidas.connector.integration.specificcommunication.caches;

import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import eu.eidas.auth.commons.light.ILightRequest;
import no.idporten.eidas.connector.integration.specificcommunication.service.OIDCRequestStateParams;

import java.io.Serial;
import java.io.Serializable;

/**
 * Holds the light request and the correlated specific request {@link AuthenticationRequest}.
 *
 * @since 2.0
 */
public class CorrelatedRequestHolder implements Serializable {

    @Serial
    private static final long serialVersionUID = 8942548697342198159L;

    private final ILightRequest iLightRequest;

    private final OIDCRequestStateParams authenticationRequest;

    public CorrelatedRequestHolder(ILightRequest iLightRequest, OIDCRequestStateParams authenticationRequest) {
        this.iLightRequest = iLightRequest;
        this.authenticationRequest = authenticationRequest;
    }

    public ILightRequest getiLightRequest() {
        return iLightRequest;
    }

    public OIDCRequestStateParams getAuthenticationRequest() {
        return authenticationRequest;
    }
}
