package no.idporten.eidas.connector.integration.freggateway.service;

import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.matching.domain.UserMatchFound;
import no.idporten.eidas.connector.matching.domain.UserMatchNotFound;
import no.idporten.eidas.connector.matching.domain.UserMatchResponse;
import no.idporten.eidas.connector.service.EIDASIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("When calling the MatchigServiceClient")
@SpringBootTest
@AutoConfigureMockRestServiceServer
@ExtendWith(MockitoExtension.class)
class FregGwMatchingServiceClientTest {
    @MockitoSpyBean
    private RestClient.Builder fregGatewayEndpointBuilder;

    @Autowired
    @InjectMocks
    private FregGwMatchingServiceClient matchingServiceClient;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.bindTo(fregGatewayEndpointBuilder).build();
    }

    @Test
    @DisplayName("then match returns value when status OK")
    void testMatchReturnsEmptyOptionalWhenNoContent() {
        mockServer.expect(requestTo(("http://junit/eidas/entydig?utenlandskPersonIdentifikasjon=123&foedselsdato=19900101&landkode=SWE")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "123"))
                .andExpect(header("Client-Id", "eidas-idporten-connector-junit"))
                .andRespond(withSuccess("123456789", MediaType.TEXT_PLAIN));

        UserMatchResponse result = matchingServiceClient.match(new EidasUser(new EIDASIdentifier("SE/NO/123"), "1990-01-01", null), Collections.emptySet());
        mockServer.verify();
        assertInstanceOf(UserMatchFound.class, result);
        assertEquals("123456789", ((UserMatchFound) result).pid());
    }

    @Test
    @DisplayName("then match ignores non-empty requestedScopes and still returns value when status OK")
    void testMatchIgnoresRequestedScopes() {
        mockServer.expect(requestTo(("http://junit/eidas/entydig?utenlandskPersonIdentifikasjon=123&foedselsdato=19900101&landkode=SWE")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "123"))
                .andExpect(header("Client-Id", "eidas-idporten-connector-junit"))
                .andRespond(withSuccess("987654321", MediaType.TEXT_PLAIN));

        UserMatchResponse result = matchingServiceClient.match(new EidasUser(new EIDASIdentifier("SE/NO/123"), "1990-01-01", null), java.util.Set.of("email", "address"));

        mockServer.verify();
        assertInstanceOf(UserMatchFound.class, result);
        assertEquals("987654321", ((UserMatchFound) result).pid());
    }

    @Test
    @DisplayName("then match returns optional emtpy when status NOT_FOUND")
    void testMatchReturnsEmtpyWhenStatusOkButEmpty() {
        mockServer.expect(requestTo(("http://junit/eidas/entydig?utenlandskPersonIdentifikasjon=123&foedselsdato=19900101&landkode=SWE")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "123"))
                .andExpect(header("Client-Id", "eidas-idporten-connector-junit"))
                .andRespond(withResourceNotFound());

        UserMatchResponse result = matchingServiceClient.match(new EidasUser(new EIDASIdentifier("SE/NO/123"), "1990-01-01", null), Collections.emptySet());

        assertInstanceOf(UserMatchNotFound.class, result);
    }


    @DisplayName("then match throws exception when error status")
    @Test
    void testMatchThrowsExceptionWhenStatusNotOkOrNoContent() {
        mockServer.expect(requestTo(("http://junit/eidas/entydig?utenlandskPersonIdentifikasjon=123&foedselsdato=19900101&landkode=SWE")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "123"))
                .andExpect(header("Client-Id", "eidas-idporten-connector-junit"))
                .andRespond(withBadRequest().body("{\"code\":\"FREG-002\",\"message\":\"wrong country code\"}").contentType(MediaType.APPLICATION_JSON));

        assertThrows(HttpClientErrorException.class, () -> matchingServiceClient.match(new EidasUser(new EIDASIdentifier("SE/NO/123"), "1990-01-01", null), Collections.emptySet()));
    }

    @DisplayName("then return empty optional when wrong format error")
    @Test
    void testMatchReturnsEmtpyWhenFormatErrrort() {
        mockServer.expect(requestTo(("http://junit/eidas/entydig?utenlandskPersonIdentifikasjon=123&foedselsdato=19900101&landkode=CHE")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "123"))
                .andExpect(header("Client-Id", "eidas-idporten-connector-junit"))
                .andRespond(withBadRequest().body("{\"code\":\"FREG-001\",\"message\":\"wrong format\"}").contentType(MediaType.APPLICATION_JSON));
        UserMatchResponse result = matchingServiceClient.match(new EidasUser(new EIDASIdentifier("CH/NO/123"), "1990-01-01", null), Collections.emptySet());
        assertInstanceOf(UserMatchNotFound.class, result);

    }
}