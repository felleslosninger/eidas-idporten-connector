package no.idporten.eidas.connector.service;

import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.Nonce;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.tx.BinaryLightToken;
import lombok.RequiredArgsConstructor;
import no.idporten.eidas.connector.config.EuConnectorProperties;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.specificcommunication.BinaryLightTokenHelper;
import no.idporten.eidas.connector.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.connector.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.connector.integration.specificcommunication.service.OIDCRequestStateParams;
import no.idporten.eidas.connector.integration.specificcommunication.service.SpecificCommunicationServiceImpl;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.eidas.lightprotocol.messages.RequestedAttribute;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * The main service
 */
@Service
@RequiredArgsConstructor
public class SpecificConnectorService {
    private final EuConnectorProperties euConnectorProperties;

    private final SpecificCommunicationServiceImpl specificCommunicationServiceImpl;
    private final LevelOfAssuranceHelper levelOfAssuranceHelper;
    private final OIDCRequestCache oidcRequestCache;
    private static final String FAMILY_NAME = "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName";
    private static final String FIRST_NAME = "http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName";
    private static final String DATE_OF_BIRTH = "http://eidas.europa.eu/attributes/naturalperson/DateOfBirth";
    private static final String PID = "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier";

    public String getEuConnectorRedirectUri() {
        return euConnectorProperties.getRedirectUri();
    }

    public String createStoreBinaryLightTokenRequestBase64(ILightRequest lightRequest) throws SpecificConnectorException {
        BinaryLightToken binaryLightToken = specificCommunicationServiceImpl.putRequest(lightRequest);
        return BinaryLightTokenHelper.encodeBinaryLightTokenBase64(binaryLightToken);
    }

    public String storeStateParams(ILightRequest lightRequest, PushedAuthorizationRequest pushedAuthorizationRequest) {
        CodeVerifier codeVerifier = new CodeVerifier();

        final CorrelatedRequestHolder correlatedRequestHolder = new CorrelatedRequestHolder(lightRequest,
                new OIDCRequestStateParams(new State(pushedAuthorizationRequest.getState()),
                        new Nonce(pushedAuthorizationRequest.getNonce()),
                        codeVerifier));
        oidcRequestCache.put(lightRequest.getRelayState(), correlatedRequestHolder);
        return lightRequest.getRelayState();

    }

    public CorrelatedRequestHolder getCachedRequest(State state) {
        CorrelatedRequestHolder correlatedRequestHolder = oidcRequestCache.get(state.getValue());
        oidcRequestCache.remove(state.getValue());
        return correlatedRequestHolder;
    }

    public LightRequest buildLightRequest(String citizenCountryCode, PushedAuthorizationRequest pushedAuthorizationRequest) {

        return LightRequest.builder()
                .id(UUID.randomUUID().toString())
                .citizenCountryCode(citizenCountryCode)
                .requestedAttributes(List.of(
                        RequestedAttribute.builder().definition(FAMILY_NAME).build(),
                        RequestedAttribute.builder().definition(FIRST_NAME).build(),
                        RequestedAttribute.builder().definition(DATE_OF_BIRTH).build(),
                        RequestedAttribute.builder().definition(PID).build()
                ))
                //todo støtte mer enn en acr level
                .levelOfAssurance(levelOfAssuranceHelper.idportenAcrToEidasAcr(pushedAuthorizationRequest.getAcrValues().getFirst()))
                .issuer(euConnectorProperties.getIssuer())
                .relayState(UUID.randomUUID().toString())
                .build();

    }


}
