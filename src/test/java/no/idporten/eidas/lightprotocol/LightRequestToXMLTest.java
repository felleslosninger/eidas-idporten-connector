package no.idporten.eidas.lightprotocol;

import jakarta.xml.bind.JAXBException;
import no.idporten.eidas.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.eidas.lightprotocol.messages.RequestedAttribute;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.*;

class LightRequestToXMLTest {

    @Test
    void testSerializeToXML() {
        LightRequest lightRequest = LightRequest.builder()
                .id("123")
                .levelOfAssurance(List.of(new LevelOfAssurance(LevelOfAssurance.EIDAS_LOA_LOW), new LevelOfAssurance(LevelOfAssurance.EIDAS_LOA_HIGH)))
                .issuer("issuer")
                .requestedAttributes(List.of(new RequestedAttribute("name", "first_name")))
                .citizenCountryCode("NO")
                .relayState("123")
                .requesterId("requester_id")
                .build();
        try {
            String xml = LightRequestToXML.toXml(lightRequest);
            assertAll("Check xml",
                    () -> assertNotNull(xml),
                    () -> assertFalse(xml.contains("ns2:"), "should not include any namespace prefix (ns2:)"),
                    () -> assertTrue(xml.contains("levelOfAssurance"), "should include element levelOfAssurance"),
                    () -> assertTrue(xml.contains("123"), "should include id"),
                    () -> assertTrue(xml.contains("issuer"), "should include issuer"),
                    () -> assertTrue(xml.contains("NO"), "should include citizenCountryCode"),
                    () -> assertTrue(xml.contains("123"), "should include relayState"),
                    () -> assertTrue(xml.contains("requester_id"), "should include requesterId"),
                    () -> assertTrue(xml.contains("first_name"), "should include requested attribute first_name"),
                    () -> assertTrue(xml.contains("name"), "should include requested attribute name"),
                    () -> assertTrue(xml.contains(LevelOfAssurance.EIDAS_LOA_LOW), "should include level of assurance LOW"),
                    () -> assertTrue(xml.contains(LevelOfAssurance.EIDAS_LOA_HIGH), "should include level of assurance HIGH"),
                    () -> assertTrue(xml.contains("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified"), "should include unspecified as nameidformat")
            );


        } catch (JAXBException e) {
            fail("Failed to serialize to XML %s : %s: %s", e.getErrorCode(), e.getMessage(), e.getCause());
        }
    }
}
