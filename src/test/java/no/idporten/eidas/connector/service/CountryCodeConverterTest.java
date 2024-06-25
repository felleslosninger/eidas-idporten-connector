package no.idporten.eidas.connector.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Converting ISO Alpha 2 country code to ISO Alpha 3 country code")
class CountryCodeConverterTest {

    private static Stream<Arguments> countryCodeValueProvider() {
        return Stream.of(
                Arguments.of("SE", "SWE"),  // Swedish
                Arguments.of("IS", "ISL"),  // Icelandic
                Arguments.of("PL", "POL")   // Polish
        );
    }

    @ParameterizedTest(name = "{index} => iso2={0}, expectedAlpha3={1}")
    @MethodSource("countryCodeValueProvider")
    void testCountryCodeConversion(String eidasIdentifier, String expectedAlpha3) {
        CountryCodeConverter countryCodeConverter = new CountryCodeConverter(Optional.empty());
        assertEquals(expectedAlpha3, countryCodeConverter.getISOAlpha3CountryCode(eidasIdentifier));
    }

    private static Stream<Arguments> demoCountryCodeValueProvider() {
        return Stream.of(
                Arguments.of("CA", "SWE"),  // mapped
                Arguments.of("CB", "")  // not mapped
        );
    }

    @ParameterizedTest(name = "{index} => iso2={0}, expectedAlpha3={1}")
    @MethodSource("demoCountryCodeValueProvider")
    void testDemoCountryCodeConversion(String eidasIdentifier, String expectedAlpha3) {
        CountryCodeConverter countryCodeConverter = new CountryCodeConverter(Optional.of(Map.of("CA", "SWE")));
        assertEquals(expectedAlpha3, countryCodeConverter.getISOAlpha3CountryCode(eidasIdentifier));
    }

}
