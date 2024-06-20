package no.idporten.eidas.connector.integration.freggateway.service;

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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@DisplayName("When calling the MatchigServiceClient")
@SpringBootTest
@AutoConfigureMockRestServiceServer
@ExtendWith(MockitoExtension.class)
class MatchingServiceClientTest {
    @SpyBean
    private RestClient.Builder fregGatewayEndpointBuilder;

    @Autowired
    @InjectMocks
    private MatchingServiceClient matchingServiceClient;

    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setUp() {
        mockServer = MockRestServiceServer.bindTo(fregGatewayEndpointBuilder).build();
    }

    @Test
    @DisplayName("then match returns value when status OK")
    void testMatchReturnsEmptyOptionalWhenNoContent() {
        mockServer.expect(requestTo(("http://localhost:8080/eidas/entydig?utenlandskPersonIdentifikasjon=123&foedselsdato=19900101&landkode=SWE")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "123"))
                .andExpect(header("Client-Id", "eidas-idporten-connector-junit"))
                .andRespond(withSuccess("123456789", MediaType.TEXT_PLAIN));

        Optional<String> result = matchingServiceClient.match(new EIDASIdentifier("SE/NO/123"), "1990-01-01");
        mockServer.verify();
        assertEquals(Optional.of("123456789"), result);
    }

    @Test
    @DisplayName("then match returns optional emtpy when status NOT_FOUND")
    void testMatchReturnsEmtpyWhenStatusOkButEmpty() {
        mockServer.expect(requestTo(("http://localhost:8080/eidas/entydig?utenlandskPersonIdentifikasjon=123&foedselsdato=19900101&landkode=SWE")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "123"))
                .andExpect(header("Client-Id", "eidas-idporten-connector-junit"))
                .andRespond(withResourceNotFound());

        Optional<String> result = matchingServiceClient.match(new EIDASIdentifier("SE/NO/123"), "1990-01-01");

        assertTrue(result.isEmpty());
    }


    @DisplayName("then match throws exception when error status")
    @Test
    void testMatchThrowsExceptionWhenStatusNotOkOrNoContent() {
        mockServer.expect(requestTo(("http://localhost:8080/eidas/entydig?utenlandskPersonIdentifikasjon=123&foedselsdato=19900101&landkode=SWE")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "123"))
                .andExpect(header("Client-Id", "eidas-idporten-connector-junit"))
                .andRespond(withBadRequest());

        assertThrows(HttpClientErrorException.class, () -> matchingServiceClient.match(new EIDASIdentifier("SE/NO/123"), "1990-01-01"));
    }
}