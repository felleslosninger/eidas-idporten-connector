package no.idporten.eidas.lightprotocol;

import jakarta.xml.bind.JAXBException;
import no.idporten.eidas.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.eidas.lightprotocol.messages.RequestedAttribute;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LightRequestToXMLTest {

    @Test
    void testSerializeToXML() {
        LightRequest lightRequest = LightRequest.builder()
                .id("123")
                .levelOfAssurance(LevelOfAssurance.fromString(LevelOfAssurance.EIDAS_LOA_LOW))
                .issuer("issuer")
                .requestedAttributes(List.of(new RequestedAttribute("name", "first_name")))
                .citizenCountryCode("NO")
                .relayState("123")
                .requesterId("requester_id")
                .build();
        try {
            String xml = LightRequestToXML.toXml(lightRequest);
            assertNotNull(xml);
        } catch (JAXBException e) {
            fail("Failed to serialize to XML %s : %s: %s", e.getErrorCode(), e.getMessage(), e.getCause());
        }
    }
}
