package no.idporten.eidas.connector.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Converting ISO Alpha 2 country code to ISO Alpha 3 country code")
class CountryCodeConverterTest {

    CountryCodeConverter countryCodeConverter = new CountryCodeConverter();

    @ParameterizedTest(name = "{index} => iso2={0}, expectedAlpha3={1}")
    @MethodSource("countryCodeProvider")
    void testCountryCodeConversion(String eidasIdentifier, String expectedAlpha3) {
        assertEquals(expectedAlpha3, countryCodeConverter.getISOAlpha3CountryCode(eidasIdentifier));
    }

    private static Stream<Arguments> countryCodeProvider() {
        return Stream.of(
                Arguments.of("SE", "SWE"),  // Swedish
                Arguments.of("IS", "ISL"),  // Icelandic
                Arguments.of("PL", "POL")   // Polish
        );
    }

}
