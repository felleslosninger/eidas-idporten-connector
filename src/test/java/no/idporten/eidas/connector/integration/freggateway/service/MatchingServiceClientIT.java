package no.idporten.eidas.connector.integration.freggateway.service;

import no.idporten.eidas.connector.service.EIDASIdentifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("When calling freg")
@SpringBootTest
@ActiveProfiles("local-dev")
@Disabled("Comment out to run a direct test against the freg-gw")
class MatchingServiceClientIT {

    @Autowired
    private MatchingServiceClient matchingServiceClient;

    @Test
    @DisplayName("then match returns value when status OK")
    void testMatchReturnsEmptyOptionalWhenNoContent() {

        Optional<String> result = matchingServiceClient.match(new EIDASIdentifier("SE/NO/1634736525341-3"), "2000-01-01");
        assertEquals(Optional.of("41810060822"), result);
    }

}