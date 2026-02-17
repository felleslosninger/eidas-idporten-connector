package no.idporten.eidas.connector.service;

import no.idporten.eidas.connector.config.EidasClaims;
import no.idporten.eidas.connector.config.EuConnectorProperties;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.freggateway.service.FregGwMatchingServiceClient;
import no.idporten.eidas.connector.integration.nobid.web.NobidSession;
import no.idporten.eidas.connector.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.connector.integration.specificcommunication.caches.OIDCRequestCache;
import no.idporten.eidas.connector.integration.specificcommunication.service.OIDCRequestStateParams;
import no.idporten.eidas.connector.integration.specificcommunication.service.SpecificCommunicationServiceImpl;
import no.idporten.eidas.connector.matching.domain.UserMatchFound;
import no.idporten.eidas.connector.matching.domain.UserMatchNotFound;
import no.idporten.eidas.lightprotocol.messages.*;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        lenient().when(euConnectorProperties.getIssuer()).thenReturn("issuerId");
        EidasUser eidasUser = new EidasUser(new EIDASIdentifier("SE/NO/1234"), "2000-12-01", null);
        lenient().when(matchingServiceClient.match(any(), eq(Collections.emptySet()))).thenReturn(new UserMatchFound(eidasUser, "123-abc"));
        specificConnectorService = new SpecificConnectorService(
                euConnectorProperties,
                specificCommunicationServiceImpl,
                levelOfAssuranceHelper,
                oidcRequestCache,
                Optional.of(matchingServiceClient),
                nobidSession
        );
    }

    @Test
    @DisplayName("getEuConnectorRedirectUri returns property value")
    void getEuConnectorRedirectUri_returnsConfigured() {
        when(euConnectorProperties.getRedirectUri()).thenReturn("https://example.org/cb");

        String uri = specificConnectorService.getEuConnectorRedirectUri();

        assertEquals("https://example.org/cb", uri);
    }

    @Test
    @DisplayName("buildLightRequest sets issuer, uppercases country, LoA and 4 requested attributes")
    void buildLightRequest_populatesFields() {
        // Given
        when(euConnectorProperties.getIssuer()).thenReturn("issuerId");
        PushedAuthorizationRequest par = mock(PushedAuthorizationRequest.class);
        when(par.getAcrValues()).thenReturn(List.of("idporten-high"));
        when(levelOfAssuranceHelper.idportenAcrListToEidasAcr(List.of("idporten-high")))
                .thenReturn(List.of(new LevelOfAssurance("eidas-high")));

        // When
        LightRequest lr = specificConnectorService.buildLightRequest("se", par);

        // Then
        assertEquals("SE", lr.getCitizenCountryCode());
        assertEquals("issuerId", lr.getIssuer());
        assertEquals("eidas-high", lr.getLevelOfAssurance());
        assertNotNull(lr.getRelayState());
        assertNotNull(lr.getId());
        assertEquals(4, lr.getRequestedAttributesList().size());
        List<String> defs = lr.getRequestedAttributesList().stream().map(RequestedAttribute::getDefinition).toList();
        assertTrue(defs.containsAll(List.of(
                EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_FAMILY_NAME,
                EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_GIVEN_NAME,
                EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH,
                EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER
        )));
    }

    @Test
    @DisplayName("getEidasUser extracts values and validates person identifier")
    void getEidasUser_extraction_success() {
        LightResponse response = LightResponse.builder()
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER, List.of("SE/NO/ABC123")))
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_GIVEN_NAME, List.of("Given")))
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_FAMILY_NAME, List.of("Family")))
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH, List.of("2000-12-01")))
                .build();

        EidasUser user = specificConnectorService.getEidasUser(response);

        assertEquals("SE/NO/ABC123", user.eidasIdentifier().getFormattedEidasIdentifier());
        assertEquals("2000-12-01", user.birthdate());
        Map<String, String> claims = user.eidasClaims();
        assertNotNull(claims);
        assertEquals("Given", claims.get(EidasClaims.IDPORTEN_EIDAS_GIVEN_NAME_CLAIM));
        assertEquals("Family", claims.get(EidasClaims.IDPORTEN_EIDAS_FAMILY_NAME_CLAIM));
    }

    @Test
    @DisplayName("getEidasUser throws when identifier invalid or missing")
    void getEidasUser_invalidIdentifier_throws() {
        LightResponse response = LightResponse.builder()
                // invalid person identifier (missing country codes)
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER, List.of("invalid")))
                .build();

        assertThrows(SpecificConnectorException.class, () -> specificConnectorService.getEidasUser(response));
    }

    @Test
    @DisplayName("getEidasUser builds domain object from LightResponse")
    void getEidasUser_mapsClaims() {
        LightResponse response = LightResponse.builder()
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER, List.of("SE/NO/USER1")))
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH, List.of("1999-01-02")))
                .build();

        EidasUser user = specificConnectorService.getEidasUser(response);

        assertEquals("SE/NO/USER1", user.eidasIdentifier().getFormattedEidasIdentifier());
        assertEquals("1999-01-02", user.birthdate());
        assertNotNull(user.eidasClaims());
    }

    @Test
    @DisplayName("matchUser returns not found when matching disabled")
    void matchUser_disabled_returnsNotFound() {
        SpecificConnectorService serviceWithNoMatching = new SpecificConnectorService(
                euConnectorProperties,
                specificCommunicationServiceImpl,
                levelOfAssuranceHelper,
                oidcRequestCache,
                Optional.empty(),
                nobidSession
        );

        LightResponse response = LightResponse.builder()
                .levelOfAssurance("eidas-high")
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER, List.of("SE/NO/AAA")))
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH, List.of("2000-12-01")))
                .build();

        assertInstanceOf(UserMatchNotFound.class, serviceWithNoMatching.matchUser(response, Collections.emptySet()));
    }

    @Test
    @DisplayName("matchUser delegates to matching service and sets NobidSession LoA")
    void matchUser_enabled_delegatesAndSetsLoA() {
        LightResponse response = LightResponse.builder()
                .levelOfAssurance("eidas-high")
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER, List.of("SE/NO/BBB")))
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH, List.of("2000-12-01")))
                .build();

        specificConnectorService.matchUser(response, Collections.emptySet());

        verify(nobidSession).setLevelOfAssurance("eidas-high");
        verify(matchingServiceClient).match(any(EidasUser.class), eq(Collections.emptySet()));
    }

    @Test
    @DisplayName("matchUser forwards non-empty requestedScopes to matching service")
    void matchUser_forwards_requested_scopes() {
        // Given
        EidasUser eidasUser = new EidasUser(new EIDASIdentifier("SE/NO/BBB"), "2000-12-01", null);
        when(matchingServiceClient.match(any(), eq(Set.of("nobid:sandbox")))).thenReturn(new UserMatchFound(eidasUser, "match-id"));

        LightResponse response = LightResponse.builder()
                .levelOfAssurance("eidas-high")
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER, List.of("SE/NO/BBB")))
                .attribute(new Attribute(EidasClaims.EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH, List.of("2000-12-01")))
                .build();

        // When
        specificConnectorService.matchUser(response, Set.of("nobid:sandbox"));

        // Then
        verify(matchingServiceClient).match(any(EidasUser.class), eq(Set.of("nobid:sandbox")));
    }

    @Test
    @DisplayName("storeStateParams stores in cache and returns relayState; getCachedRequest retrieves and removes")
    void storeAndGetCachedRequest_works() {
        LightRequest request = LightRequest.builder()
                .id("req1")
                .issuer("issuerId")
                .relayState("relay-123")
                .build();
        PushedAuthorizationRequest par = mock(PushedAuthorizationRequest.class);
        when(par.getState()).thenReturn("state-xyz");
        when(par.getNonce()).thenReturn("nonce-xyz");

        // When storing
        String relay = specificConnectorService.storeStateParams(request, par);
        assertEquals("relay-123", relay);

        ArgumentCaptor<CorrelatedRequestHolder> holderCaptor = ArgumentCaptor.forClass(CorrelatedRequestHolder.class);
        verify(oidcRequestCache).put(eq("relay-123"), holderCaptor.capture());
        CorrelatedRequestHolder stored = holderCaptor.getValue();
        assertEquals("req1", stored.getiLightRequest().getId());
        OIDCRequestStateParams params = stored.getAuthenticationRequest();
        assertEquals("state-xyz", params.getState().getValue());
        assertEquals("nonce-xyz", params.getNonce().getValue());
        assertNotNull(params.getCodeVerifier());

        // When getting
        when(oidcRequestCache.get("relay-123")).thenReturn(stored);
        CorrelatedRequestHolder retrieved = specificConnectorService.getCachedRequest("relay-123");
        assertSame(stored, retrieved);
        verify(oidcRequestCache).remove("relay-123");
    }
}
