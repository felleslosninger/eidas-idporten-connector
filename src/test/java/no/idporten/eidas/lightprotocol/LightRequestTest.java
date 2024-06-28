package no.idporten.eidas.lightprotocol;


import no.idporten.eidas.lightprotocol.messages.LevelOfAssurance;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.eidas.lightprotocol.messages.RequestedAttribute;
import no.idporten.sdk.oidcserver.protocol.AuditData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LightRequestTest {

    @Test
    void getAuditDataWhenEmtpyThenDoesNotFailAndLoggesEmptyRequest() {
        AuditData auditData = new LightRequest().getAuditData();
        assertNotNull(auditData);
        assertNotNull(auditData.getAttributes());
        assertEquals(0, auditData.getAttributes().size());

    }

    @Test
    void getAuditDataWhenRequestHasAll6AttributesThenLoggesAllAttributes() {
        AuditData auditData = createLightRequest().getAuditData();
        assertNotNull(auditData);
        assertNotNull(auditData.getAttributes());
        assertEquals(6, auditData.getAttributes().size());
        assertInstanceOf(String.class, auditData.getAttributes().get("attributes"));
        assertInstanceOf(String.class, auditData.getAttributes().get("level_of_assurance"));
    }

    private static LightRequest createLightRequest() {
        return LightRequest.builder()
                .id("id")
                .requestedAttributes(List.of(new RequestedAttribute("value", "definition")))
                .relayState("state")
                .citizenCountryCode("CA")
                .spCountryCode("CB")
                .levelOfAssurance(List.of(new LevelOfAssurance(LevelOfAssurance.EIDAS_LOA_HIGH)))
                .build();
    }


}
