package no.idporten.eidas.connector.service;

import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.Nonce;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.tx.BinaryLightToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.config.EidasClaims;
import no.idporten.eidas.connector.config.EuConnectorProperties;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.exceptions.ErrorCodes;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.nobid.web.NobidSession;
import no.idporten.eidas.connector.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.connector.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.connector.integration.specificcommunication.service.OIDCRequestStateParams;
import no.idporten.eidas.connector.integration.specificcommunication.service.SpecificCommunicationServiceImpl;
import no.idporten.eidas.connector.logging.MDCFilter;
import no.idporten.eidas.connector.matching.domain.UserMatchNotFound;
import no.idporten.eidas.connector.matching.domain.UserMatchResponse;
import no.idporten.eidas.connector.matching.service.MatchingService;
import no.idporten.eidas.lightprotocol.BinaryLightTokenHelper;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.eidas.lightprotocol.messages.LightResponse;
import no.idporten.eidas.lightprotocol.messages.RequestedAttribute;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static no.idporten.eidas.connector.config.EidasClaims.*;

/**
 * The main service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpecificConnectorService {

    private final EuConnectorProperties euConnectorProperties;
    private final SpecificCommunicationServiceImpl specificCommunicationServiceImpl;
    private final LevelOfAssuranceHelper levelOfAssuranceHelper;
    private final OIDCRequestCache oidcRequestCache;
    private final Optional<MatchingService> matchingServiceClient;
    private final NobidSession nobidSession;

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
                        codeVerifier,
                        MDCFilter.getTraceId()
                ));
        oidcRequestCache.put(lightRequest.getRelayState(), correlatedRequestHolder);
        return lightRequest.getRelayState();
    }

    public CorrelatedRequestHolder getCachedRequest(String relayState) {
        CorrelatedRequestHolder correlatedRequestHolder = oidcRequestCache.get(relayState);
        oidcRequestCache.remove(relayState);
        return correlatedRequestHolder;
    }

    public LightRequest buildLightRequest(String citizenCountryCode, PushedAuthorizationRequest pushedAuthorizationRequest) {

        return LightRequest.builder()
                .id(UUID.randomUUID().toString())
                .citizenCountryCode(citizenCountryCode.toUpperCase())
                .requestedAttributes(List.of(
                        RequestedAttribute.builder().definition(EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_FAMILY_NAME).build(),
                        RequestedAttribute.builder().definition(EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_GIVEN_NAME).build(),
                        RequestedAttribute.builder().definition(EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH).build(),
                        RequestedAttribute.builder().definition(EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER).build()
                ))
                .levelOfAssurance(levelOfAssuranceHelper.idportenAcrListToEidasAcr(pushedAuthorizationRequest.getAcrValues()))
                .issuer(euConnectorProperties.getIssuer())
                .relayState(UUID.randomUUID().toString())
                .spType("public")
                .providerName("Norwegian National Identity Authority")
                .build();
    }

    protected Map<String, String> extractEidasClaims(LightResponse lightResponse) {
        Map<String, String> eidasClaims = new HashMap<>();
        lightResponse.getAttributesList().forEach(attribute -> {
            String definition = attribute.getDefinition();
            if (StringUtils.isNotEmpty(definition) && !CollectionUtils.isEmpty(attribute.getValue())) {
                eidasClaims.put(getAttributeName(definition), attribute.getValue().getFirst());
            }
        });
        if (!EIDASIdentifier.isValid(eidasClaims.get(IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM))) {
            log.error("Invalid or missing personidentifier was missing from foreign IDP {}.", eidasClaims.get(IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM));
            throw new SpecificConnectorException(ErrorCodes.INTERNAL_ERROR.getValue(), "Invalid or missing personidentifier was missing from foreign IDP.");
        }
        return eidasClaims;
    }

    public UserMatchResponse matchUser(LightResponse lightResponse) {
        EidasUser eidasUser = getEidasUser(lightResponse);
        if (matchingServiceClient.isEmpty()) {
            return new UserMatchNotFound(eidasUser, "Matching service disabled");
        }
        nobidSession.setLevelOfAssurance(lightResponse.getLevelOfAssurance());

        return matchingServiceClient.get().match(eidasUser);
    }

    private String getAttributeName(String definition) {
        return EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES.get(definition);
    }

    public EidasUser getEidasUser(LightResponse lightResponse) {
        Map<String, String> eidasClaims = extractEidasClaims(lightResponse);
        return new EidasUser(new EIDASIdentifier(eidasClaims.get(IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM)),
                eidasClaims.get(IDPORTEN_EIDAS_DATE_OF_BIRTH_CLAIM),
                eidasClaims
        );
    }


}
