package no.idporten.eidas.connector.service;

import no.idporten.eidas.connector.config.AcrProperties;
import no.idporten.eidas.lightprotocol.messages.LevelOfAssurance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@DisplayName("When checking level of assurance")
@ExtendWith(MockitoExtension.class)
class LevelOfAssuranceHelperTest {

    @Mock
    private AcrProperties acrPropertiesMock;
    @InjectMocks
    private LevelOfAssuranceHelper levelOfAssuranceHelper;

    @BeforeEach
    void setup() {
        lenient().when(acrPropertiesMock.getSupportedAcrValues()).thenReturn(List.of("http://eidas.europa.eu/LoA/low",
                "http://eidas.europa.eu/LoA/substantial",
                "http://eidas.europa.eu/LoA/high"));
        lenient().when(acrPropertiesMock.getAcrValueMap()).thenReturn(Map.of("eidas-loa-low", "http://eidas.europa.eu/LoA/low",
                "eidas-loa-substantial", "http://eidas.europa.eu/LoA/substantial",
                "eidas-loa-high", "http://eidas.europa.eu/LoA/high"));
    }

    @Test
    @DisplayName("then substantial must be allowed if requested value was substantial and high")
    void testValidAcrsubstantialWhensubstantialAndhighRequested() {
        assertTrue(levelOfAssuranceHelper.hasValidAcrLevel("http://eidas.europa.eu/LoA/substantial", List.of("http://eidas.europa.eu/LoA/high", "http://eidas.europa.eu/LoA/substantial")));
    }

    @Test
    @DisplayName("then substantial must be allowed if requested value was substantial")
    void testValidAcrsubstantialWhenMinimum3Requested() {
        assertTrue(levelOfAssuranceHelper.isValidAcr("http://eidas.europa.eu/LoA/substantial"));
    }

    @Test
    @DisplayName("then all levels must be allowed if acr was not requested")
    void testValidAcrsubstantialAndHighWhenNoLevelRequested() {
        assertTrue(levelOfAssuranceHelper.isValidAcr("http://eidas.europa.eu/LoA/low"));
        assertTrue(levelOfAssuranceHelper.isValidAcr("http://eidas.europa.eu/LoA/substantial"));
        assertTrue(levelOfAssuranceHelper.isValidAcr("http://eidas.europa.eu/LoA/high"));
    }

    @Test
    @DisplayName("then high must be allowed if requested value was substantial ")
    void testValidAcrLevel() {
        assertTrue(levelOfAssuranceHelper.hasValidAcrLevel("http://eidas.europa.eu/LoA/high", List.of("http://eidas.europa.eu/LoA/substantial")));
    }

    @Test
    @DisplayName("then substantial must not be allowed if requested value was high")
    void testInvalidAcrLevel() {
        assertFalse(levelOfAssuranceHelper.hasValidAcrLevel("http://eidas.europa.eu/LoA/substantial", List.of("http://eidas.europa.eu/LoA/high")));
    }

    @Test
    @DisplayName("then high must be allowed if minimum requested value was high")
    void testValidAcrhighWhenMinimumHighRequested() {
        assertTrue(levelOfAssuranceHelper.isValidAcr("http://eidas.europa.eu/LoA/high"));
    }

    @Test
    @DisplayName("then if the request acr is null, but supported, it is valid")
    void testNullAcrsForEid() {
        assertTrue(levelOfAssuranceHelper.hasValidAcrLevel("http://eidas.europa.eu/LoA/low", null));
    }

    @Test
    void testAcrValueFromEidasToIdportenMapping() {
        List<String> idportenAcr = levelOfAssuranceHelper.eidasAcrListToIdportenAcrList(List.of(
                new LevelOfAssurance(LevelOfAssurance.EIDAS_LOA_LOW),
                new LevelOfAssurance(LevelOfAssurance.EIDAS_LOA_SUBSTANTIAL),
                new LevelOfAssurance(LevelOfAssurance.EIDAS_LOA_HIGH)));
        assertEquals(3, idportenAcr.size());
        assertEquals(List.of("eidas-loa-low", "eidas-loa-substantial", "eidas-loa-high"), idportenAcr);
    }

    @Test
    void testAcrValueFromIdportenToEidasMapping() {
        List<LevelOfAssurance> eidasAcr = levelOfAssuranceHelper.idportenAcrListToEidasAcr(List.of("eidas-loa-high", "eidas-loa-substantial", "eidas-loa-low"));
        assertNotNull(eidasAcr);
        assertEquals(LevelOfAssurance.EIDAS_LOA_HIGH, eidasAcr.getFirst().getValue());
        assertEquals(LevelOfAssurance.EIDAS_LOA_SUBSTANTIAL, eidasAcr.get(1).getValue());
        assertEquals(LevelOfAssurance.EIDAS_LOA_LOW, eidasAcr.getLast().getValue());
    }

}


