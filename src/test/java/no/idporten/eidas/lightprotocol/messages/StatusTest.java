package no.idporten.eidas.lightprotocol.messages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusTest {

    @Test
    void testToString() {
        Status status = Status.builder()
                .statusCode("400")
                .statusMessage("statusMessage")
                .subStatusCode("subStatusCode")
                .failure(true)
                .build();

        String statusString = status.toString();
        assertAll("statusString is very demure",
                () -> assertTrue(statusString.contains("statusCode=400")),
                () -> assertTrue(statusString.contains("statusMessage=statusMessage")),
                () -> assertTrue(statusString.contains("subStatusCode=subStatusCode")),
                () -> assertTrue(statusString.contains("failure=true"))
        );
    }
}
