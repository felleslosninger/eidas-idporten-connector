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
import no.idporten.eidas.connector.exceptions.ErrorCodes;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.freggateway.service.MatchingServiceClient;
import no.idporten.eidas.connector.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.connector.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.connector.integration.specificcommunication.service.OIDCRequestStateParams;
import no.idporten.eidas.connector.integration.specificcommunication.service.SpecificCommunicationServiceImpl;
import no.idporten.eidas.connector.logging.MDCFilter;
import no.idporten.eidas.lightprotocol.BinaryLightTokenHelper;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.eidas.lightprotocol.messages.LightResponse;
import no.idporten.eidas.lightprotocol.messages.RequestedAttribute;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

import static no.idporten.eidas.connector.config.EidasClaims.*;

/**
 * The main service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpecificConnectorService {

    protected static final String EIDAS_AMR = "eidas";
    protected static final String PID_CLAIM = "pid";

    private final EuConnectorProperties euConnectorProperties;
    private final SpecificCommunicationServiceImpl specificCommunicationServiceImpl;
    private final LevelOfAssuranceHelper levelOfAssuranceHelper;
    private final OIDCRequestCache oidcRequestCache;
    private final Optional<MatchingServiceClient> matchingServiceClient;

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

    protected Optional<String> matchUser(Map<String, String> eidasClaims) {
        if (matchingServiceClient.isEmpty()) {
            return Optional.empty();
        }

        try {
            return matchingServiceClient.get().match(new EIDASIdentifier(eidasClaims.get(IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM)),
                    eidasClaims.get(IDPORTEN_EIDAS_DATE_OF_BIRTH_CLAIM));
        } catch (HttpClientErrorException e) {
            if (400 == e.getStatusCode().value() && e.getMessage().contains("FREG-001")) {
                log.error("Error while matching user. {}", e.getMessage());
                return Optional.empty();
            }
            throw e;
        }
    }

    private String getAttributeName(String definition) {
        return EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES.get(definition);
    }

    public Authorization getAuthorization(LightResponse lightResponse) {
        Map<String, String> eidasClaims = extractEidasClaims(lightResponse);
        Authorization.AuthorizationBuilder authorizationBuilder = Authorization.builder()
                .acr(levelOfAssuranceHelper.eidasAcrToIdportenAcr(lightResponse.getLevelOfAssurance()))
                .amr(EIDAS_AMR);

        eidasClaims.forEach(authorizationBuilder::attribute);
        Optional<String> userMatch = matchUser(eidasClaims);
        userMatch.ifPresentOrElse(s -> authorizationBuilder.attribute(PID_CLAIM, s).sub(s),
                () -> authorizationBuilder.sub(eidasClaims.get(IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM)));

        authorizationBuilder.attribute(IDPORTEN_EIDAS_CITIZEN_COUNTRY_CODE,
                //already validated claim value
                eidasClaims.get(IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM).substring(0, 2));
        return authorizationBuilder.build();
    }
}
