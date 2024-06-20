package no.idporten.eidas.connector.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When validating an EIDAS identifier")
class EIDASIdentifierTest {

    @Test
    @DisplayName("then AA/BB/1234567 should return true")
    void whenValidFormat_thenIsValidReturnsTrue() {
        assertTrue(EIDASIdentifier.isValid("AA/BB/1234567"));
        assertTrue(EIDASIdentifier.isValid("XY/ZZ/vivaldi-æøå-987654321"));
    }

    @Test
    @DisplayName("then AA/BB/1234567 should return a valid EIDASIdentifier object")
    void whenValidFormat_thenCreateObject() {
        EIDASIdentifier eidasIdentifier = new EIDASIdentifier("AA/BB/1234567");
        assertAll("All parameters set correctly",
                () -> assertEquals("AA", eidasIdentifier.getSubjectCountryCode()),
                () -> assertEquals("BB", eidasIdentifier.getSpCountryCode()),
                () -> assertEquals("1234567", eidasIdentifier.getForeignIdentifier())
        );
    }

    @Test
    @DisplayName("then various invalid formats should return false")
    void whenInvalidFormat_thenIsValidReturnsFalse() {
        assertFalse(EIDASIdentifier.isValid("A/BB/1234567"));  // Invalid first segment
        assertFalse(EIDASIdentifier.isValid("AA/B/1234567"));  // Invalid second segment
        assertFalse(EIDASIdentifier.isValid("BB/"));           // Missing first segment
        assertFalse(EIDASIdentifier.isValid("BB/1234567"));    // Missing second segment
        assertFalse(EIDASIdentifier.isValid("AA/BB/"));        // Missing third segment
        assertFalse(EIDASIdentifier.isValid("AA/BB/123 4567"));// Invalid characters
        assertFalse(EIDASIdentifier.isValid("hei hå nå er det jul igjen"));// just invalid
    }

    @Test
    @DisplayName("then null value should return false")
    void whenNull_thenIsValidReturnsFalse() {
        assertFalse(EIDASIdentifier.isValid(null));
    }
}
