package no.idporten.eidas.connector.service;

import eu.eidas.auth.commons.EIDASStatusCode;
import no.idporten.eidas.connector.config.EidasClaims;
import no.idporten.eidas.connector.config.EuConnectorProperties;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.freggateway.service.MatchingServiceClient;
import no.idporten.eidas.connector.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.connector.integration.specificcommunication.service.SpecificCommunicationServiceImpl;
import no.idporten.eidas.lightprotocol.messages.Attribute;
import no.idporten.eidas.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.lightprotocol.messages.LightResponse;
import no.idporten.eidas.lightprotocol.messages.Status;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static no.idporten.eidas.connector.service.SpecificConnectorService.PID_CLAIM;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class SpecificConnectorServiceTest {

    @Mock
    private EuConnectorProperties euConnectorProperties;
    @Mock
    private SpecificCommunicationServiceImpl specificCommunicationServiceImpl;
    @Mock
    private LevelOfAssuranceHelper levelOfAssuranceHelper;
    @Mock
    private OIDCRequestCache oidcRequestCache;


    @Mock
    private MatchingServiceClient matchingServiceClient;

    private SpecificConnectorService specificConnectorService;

    @BeforeEach
    void setup() {
        when(matchingServiceClient.match(new EIDASIdentifier("SE/NO/1234"), "2000-12-1")).thenReturn(Optional.of("123-abc"));
        specificConnectorService = new SpecificConnectorService(euConnectorProperties, specificCommunicationServiceImpl, levelOfAssuranceHelper, oidcRequestCache, Optional.of(matchingServiceClient));
    }

    @Test
    @DisplayName("when pid exists then get authorization with pid and sub claim set to pid")
    void testGetAuthorizationWithPid() {
        LightResponse lightResponse = getLightResponse("relayState", "SE/NO/1234");
        Authorization authorization = specificConnectorService.getAuthorization(lightResponse);
        assertAll("all claims are present", () ->
                        assertTrue(authorization.getAttributes().containsKey(PID_CLAIM)),
                () -> assertEquals(authorization.getAttributes().get(PID_CLAIM), authorization.getSub()),
                () -> assertTrue(authorization.getAttributes().containsKey(EidasClaims.IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM)),
                () -> assertTrue(authorization.getAttributes().containsKey(EidasClaims.IDPORTEN_EIDAS_DATE_OF_BIRTH_CLAIM)),
                () -> assertTrue(authorization.getAttributes().containsKey(EidasClaims.IDPORTEN_EIDAS_FAMILY_NAME_CLAIM)),
                () -> assertTrue(authorization.getAttributes().containsKey(EidasClaims.IDPORTEN_EIDAS_GIVEN_NAME_CLAIM)),
                () -> assertTrue(authorization.getAttributes().containsKey(PID_CLAIM)),
                () -> assertEquals(authorization.getAttributes().get(PID_CLAIM), authorization.getSub())
        );

    }

    @Test
    @DisplayName("when pid doesn't exists then get authorization without pid and sub claim set to eidas identifier")
    void testGetAuthorizationWithoutPid() {
        LightResponse lightResponse = getLightResponse("relayState", "SE/NO/4321");
        Authorization authorization = specificConnectorService.getAuthorization(lightResponse);
        assertAll("pid not set when no match",
                () -> assertFalse(authorization.getAttributes().containsKey(PID_CLAIM)),
                () -> assertEquals("SE/NO/4321", authorization.getSub())
        );
        assertFalse(authorization.getAttributes().containsKey(PID_CLAIM));
        assertEquals("SE/NO/4321", authorization.getSub());
    }

    @Test
    @DisplayName("when  personal identifier is invalid then throw a specificconnector exception")
    void testGetAuthorizationWhenNoEidasSet() {
        LightResponse lightResponse = getLightResponse("relayState", "banan");
        assertThrows(SpecificConnectorException.class, () -> specificConnectorService.getAuthorization(lightResponse));
    }

    private static LightResponse getLightResponse(String relayState, String eidasId) {
        return LightResponse.builder()
                .citizenCountryCode("NO")
                .id("123")
                .issuer("issuer")
                .attributes(List.of(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_FAMILY_NAME, List.of("myFamilyName")),
                        new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_GIVEN_NAME, List.of("myFirstName")),
                        new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH, List.of("2000-12-1")),
                        new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER, List.of(eidasId))))
                .levelOfAssurance(LevelOfAssurance.EIDAS_LOA_LOW)
                .relayState(relayState)
                .inResponseToId("abc")
                .status(Status.builder().statusCode(EIDASStatusCode.SUCCESS_URI.getValue()).failure(false).statusMessage("ok").build())
                .build();
    }
}
