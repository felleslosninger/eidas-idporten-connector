package no.idporten.eidas.connector.service;

import no.idporten.eidas.connector.config.EuConnectorProperties;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.integration.freggateway.service.FregGwMatchingServiceClient;
import no.idporten.eidas.connector.integration.nobid.web.NobidSession;
import no.idporten.eidas.connector.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.connector.integration.specificcommunication.service.SpecificCommunicationServiceImpl;
import no.idporten.eidas.connector.matching.domain.UserMatchFound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

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
    private NobidSession nobidSession;

    @Mock
    private FregGwMatchingServiceClient matchingServiceClient;

    private SpecificConnectorService specificConnectorService;

    @BeforeEach
    void setup() {
        when(euConnectorProperties.getIssuer()).thenReturn("issuerId");
        EidasUser eidasUser = new EidasUser(new EIDASIdentifier("SE/NO/1234"), "2000-12-1", null);
        when(matchingServiceClient.match(eidasUser)).thenReturn(new UserMatchFound(eidasUser, "123-abc"));
        specificConnectorService = new SpecificConnectorService(euConnectorProperties, specificCommunicationServiceImpl, levelOfAssuranceHelper, oidcRequestCache, Optional.of(matchingServiceClient), nobidSession);
    }

  /*  @Test
    @DisplayName("when pid exists then get authorization with pid and sub claim still set to eidas identifier")
    void testGetAuthorizationWithPid() {
        LightResponse lightResponse = getLightResponse("relayState", "SE/NO/1234");
        Authorization authorization = specificConnectorService.getAuthorization(lightResponse);
        assertAll("all claims are present", () ->
                        assertTrue(authorization.getAttributes().containsKey(PID_CLAIM)),
                () -> assertTrue(authorization.getAttributes().containsKey(EidasClaims.IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM)),
                () -> assertTrue(authorization.getAttributes().containsKey(EidasClaims.IDPORTEN_EIDAS_DATE_OF_BIRTH_CLAIM)),
                () -> assertTrue(authorization.getAttributes().containsKey(EidasClaims.IDPORTEN_EIDAS_FAMILY_NAME_CLAIM)),
                () -> assertTrue(authorization.getAttributes().containsKey(EidasClaims.IDPORTEN_EIDAS_GIVEN_NAME_CLAIM)),
                () -> assertTrue(authorization.getAttributes().containsKey(IDPORTEN_EIDAS_CITIZEN_COUNTRY_CODE)),
                () -> assertEquals("SE", authorization.getAttributes().get(IDPORTEN_EIDAS_CITIZEN_COUNTRY_CODE)),
                () -> assertTrue(authorization.getAttributes().containsKey(PID_CLAIM)),
                () -> assertEquals(authorization.getAttributes().get(IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM), authorization.getSub())
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
                        new Attribute(EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_GIVEN_NAME, List.of("myFirstName")),
                        new Attribute(EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH, List.of("2000-12-1")),
                        new Attribute(EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER, List.of(eidasId))))
                .levelOfAssurance(LevelOfAssurance.EIDAS_LOA_LOW)
                .relayState(relayState)
                .inResponseToId("abc")
                .status(Status.builder().statusCode(EIDASStatusCode.SUCCESS_URI.getValue()).failure(false).statusMessage("ok").build())
                .build();
    }

    @Test
    @DisplayName("Build LightRequest with valid parameters")
    void testBuildLightRequest() {
        when(levelOfAssuranceHelper.idportenAcrListToEidasAcr(any())).thenReturn(List.of(new LevelOfAssurance(LevelOfAssurance.EIDAS_LOA_HIGH)));
        String citizenCountryCode = "fr";
        PushedAuthorizationRequest pushedAuthorizationRequest = mock(PushedAuthorizationRequest.class);
        when(pushedAuthorizationRequest.getAcrValues()).thenReturn(List.of(LevelOfAssurance.EIDAS_LOA_HIGH));

        LightRequest result = specificConnectorService.buildLightRequest(citizenCountryCode, pushedAuthorizationRequest);

        assertNotNull(result);
        assertEquals("FR", result.getCitizenCountryCode());
        assertNotNull(result.getId());
        assertNotNull(result.getRelayState());
        assertEquals(4, result.getRequestedAttributesList().size());
        assertEquals(LevelOfAssurance.EIDAS_LOA_HIGH, result.getLevelOfAssurance());
        assertEquals("issuerId", result.getIssuer());
        assertEquals("public", result.getSpType());
        assertEquals("Norwegian National Identity Authority", result.getProviderName());
        assertTrue(result.getRequestedAttributesList().stream().anyMatch(attr -> attr.getDefinition().equals(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_FAMILY_NAME)));
        assertTrue(result.getRequestedAttributesList().stream().anyMatch(attr -> attr.getDefinition().equals(EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_GIVEN_NAME)));
        assertTrue(result.getRequestedAttributesList().stream().anyMatch(attr -> attr.getDefinition().equals(EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH)));
        assertTrue(result.getRequestedAttributesList().stream().anyMatch(attr -> attr.getDefinition().equals(EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER)));
    }*/

}
