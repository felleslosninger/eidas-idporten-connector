package no.idporten.eidas.connector.integration.nobid.service;

import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.matching.domain.UserMatchRedirect;
import no.idporten.eidas.connector.matching.domain.UserMatchResponse;
import no.idporten.eidas.connector.service.EIDASIdentifier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("When calling nobid")
@SpringBootTest
@ActiveProfiles("local-dev-nobid")
@Disabled("Comment out to run a direct test against nobid")
class NobidMatchingServiceClientIT {

    @Autowired
    private NobidMatchingServiceClient matchingServiceClient;

    @Test
    @DisplayName("then match returns a redirect uri when match is initiated correctly")
    void testMatchReturnsValueIfOk() {

        UserMatchResponse result = matchingServiceClient.match(new EidasUser(new EIDASIdentifier("SE/NO/1634736525341-3"), "2000-01-01", null));
        assertInstanceOf(UserMatchRedirect.class, result);
        assertNotNull(((UserMatchRedirect) result).redirectUrl());
        System.out.println(((UserMatchRedirect) result).redirectUrl());//put this in the url below and open in browser
    }

    //go to
    //http://localhost:7070/authorize?client_id=democlient1&request_uri=urn:ietf:params:oauth:request_uri:ZnCmzpQZvdKrPYiysP0BqA.rucdH7p-atQK-0tYn8L8sA

}