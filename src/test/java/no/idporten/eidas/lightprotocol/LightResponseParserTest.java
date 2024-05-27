package no.idporten.eidas.lightprotocol;

import jakarta.xml.bind.JAXBException;
import no.idporten.eidas.lightprotocol.messages.LightResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.*;

class LightResponseParserTest {
    private final String xmlData = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <lightResponse xmlns="http://cef.eidas.eu/LightResponse">
                <citizenCountryCode>CA</citizenCountryCode>
                <issuer>issuer</issuer>
                <levelOfAssurance>http://eidas.europa.eu/LoA/low</levelOfAssurance>
                <relayState>123</relayState>
                <ipAddress>123.12.12.12</ipAddress>
                <inResponseToId>123</inResponseToId>
                <consent>consent</consent>
                <subjectNameIdFormat>format</subjectNameIdFormat>
                <status>
                    <statusCode>200</statusCode>
                    <statusMessage>ok</statusMessage>
                    <failure>false</failure>
                </status>
                <attributes>
                    <attribute>
                        <definition>http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName</definition>
                        <value>bob</value>
                    </attribute>
                </attributes>
            </lightResponse>
            """;

    @Test
    void testParseXml() {

        try {
            LightResponse lightResponse = LightResponseParser.parseXml(xmlData);
            assertNotNull(lightResponse);
            assertDoesNotThrow(lightResponse::toString);
            assertEquals("CA", lightResponse.getCitizenCountryCode());
            lightResponse.getAttributesList().forEach(a -> {
                assertNotNull(a.getValue());
                assertEquals("bob", a.getValue().getFirst());
            });
        } catch (JAXBException e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }
}
