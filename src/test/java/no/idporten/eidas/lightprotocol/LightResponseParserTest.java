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
                <id>_.4HOai4WXza6eMaCvHIMZ4Vw5cVgD-kc97B4fvpGd-E1H7R3Mi01H__h-K74vkO</id>
                <inResponseToId>76bfaf21-e313-4e30-bb7d-01d465529117</inResponseToId>
                <consent>urn:oasis:names:tc:SAML:2.0:consent:unspecified</consent>
                <issuer>http://eidas-connector:8083/ConnectorMetadata</issuer>
                <relayState>295089f0-eea3-4aa0-9da5-a175385a5e6b</relayState>
                <subject>0123456</subject>
                <subjectNameIdFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified</subjectNameIdFormat>
                <levelOfAssurance>http://eidas.europa.eu/LoA/substantial</levelOfAssurance>
                <status>
                    <failure>false</failure>
                    <statusCode>urn:oasis:names:tc:SAML:2.0:status:Success</statusCode>
                    <statusMessage>urn:oasis:names:tc:SAML:2.0:status:Success</statusMessage>
                </status>
                <attributes>
                    <attribute>
                        <definition>http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName</definition>
                        <value>Phil</value>
                    </attribute>
                    <attribute>
                        <definition>http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName</definition>
                        <value>claude</value>
                    </attribute>
                    <attribute>
                        <definition>http://eidas.europa.eu/attributes/naturalperson/DateOfBirth</definition>
                        <value>1965-01-01</value>
                    </attribute>
                    <attribute>
                        <definition>http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier</definition>
                        <value>CA/NO/11111</value>
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
            assertEquals("0123456", lightResponse.getSubject());
            assertEquals("CA/NO/11111", lightResponse.getPid());
        } catch (JAXBException e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }
}
