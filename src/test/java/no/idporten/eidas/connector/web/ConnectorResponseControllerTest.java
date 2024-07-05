package no.idporten.eidas.connector.web;

import com.nimbusds.oauth2.sdk.AuthorizationResponse;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.Nonce;
import eu.eidas.auth.commons.EIDASStatusCode;
import eu.eidas.auth.commons.EidasParameterKeys;
import eu.eidas.auth.commons.light.ILightResponse;
import jakarta.servlet.http.HttpServletRequest;
import no.idporten.eidas.connector.integration.specificcommunication.caches.CorrelatedRequestHolder;
import no.idporten.eidas.connector.integration.specificcommunication.config.EidasCacheProperties;
import no.idporten.eidas.connector.integration.specificcommunication.service.OIDCRequestStateParams;
import no.idporten.eidas.connector.integration.specificcommunication.service.SpecificCommunicationService;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.eidas.connector.service.SpecificConnectorService;
import no.idporten.eidas.lightprotocol.BinaryLightTokenHelper;
import no.idporten.eidas.lightprotocol.IncomingLightResponseValidator;
import no.idporten.eidas.lightprotocol.messages.*;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.Authorization;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.RedirectedResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;

import static no.idporten.eidas.connector.web.SessionAttributes.SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@WebMvcTest(ConnectorResponseController.class)
@DisplayName("When calling the ProxyServiceRequestController")
class ConnectorResponseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpecificCommunicationService specificCommunicationService;
    @MockBean
    private OpenIDConnectIntegration openIDConnectSdk;
    @MockBean
    private AuditService auditService;
    @MockBean
    private SpecificConnectorService specificConnectorService;
    @MockBean
    private EidasCacheProperties eidasCacheProperties;

    private final static String lightTokenId = "mockedLightTokenId";

    private static MockedStatic<BinaryLightTokenHelper> binaryLightTokenHelperMock;
    private static MockedStatic<IncomingLightResponseValidator> incomingLightResponseValidatorMock;


    @BeforeEach
    void setupEach() {
        // Mock the static methods using try-with-resources
        binaryLightTokenHelperMock = Mockito.mockStatic(BinaryLightTokenHelper.class);
        incomingLightResponseValidatorMock = Mockito.mockStatic(IncomingLightResponseValidator.class);

        String tokenBase64 = "mockedTokenBase64";
        ILightResponse mockLightResponse = mock(ILightResponse.class);
        // Mock static method behaviors
        binaryLightTokenHelperMock.when(() -> BinaryLightTokenHelper.getBinaryToken(any(HttpServletRequest.class), eq(EidasParameterKeys.TOKEN.toString()))).thenReturn(tokenBase64);
        binaryLightTokenHelperMock.when(() -> BinaryLightTokenHelper.getBinaryLightTokenId(eq(tokenBase64), any(), any())).thenReturn(lightTokenId);
        incomingLightResponseValidatorMock.when(() -> IncomingLightResponseValidator.validate(any(ILightResponse.class))).thenReturn(true);

        State state = new State("123q");
        when(mockLightResponse.getRelayState()).thenReturn("relayState");
        when(mockLightResponse.getStatus()).thenReturn(new Status("200", "OK", null, false));
        AuthorizationResponse authorizationResponse = mock(AuthorizationResponse.class);
        when(authorizationResponse.getState()).thenReturn(state);
    }

    @Test
    @DisplayName("then if there is a valid response return redirect to authorization endpoint")
    void testValidLightRequest() throws Exception {

        LightResponse lightResponse = getLightResponse("relayState");
        when(specificConnectorService.getCachedRequest(any(String.class))).thenReturn(new CorrelatedRequestHolder(LightRequest.builder().id("abc").relayState("relayState").build(),
                new OIDCRequestStateParams(new State("123"),
                        new Nonce("123"),
                        null,
                        "mockedTraceId"
                )));
        when(specificCommunicationService.getAndRemoveResponse(any(String.class), any())).thenReturn(lightResponse);
        RedirectedResponse clientResponse = mock(RedirectedResponse.class);
        when(clientResponse.toQueryRedirectUri()).thenReturn(new URI("http//junit?client_id=123&request_uri=http://redirect-url.com"));
        PushedAuthorizationRequest authorizationRequest = mock(PushedAuthorizationRequest.class);
        when(specificConnectorService.getAuthorization(lightResponse)).thenReturn(mock(Authorization.class));
        when(openIDConnectSdk.authorize(eq(authorizationRequest), any(Authorization.class))).thenReturn(mock(no.idporten.sdk.oidcserver.protocol.AuthorizationResponse.class));
        when(openIDConnectSdk.createClientResponse(any(no.idporten.sdk.oidcserver.protocol.AuthorizationResponse.class))).thenReturn(clientResponse);
        mockMvc.perform(post("/ConnectorResponse")
                        .sessionAttr(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST, authorizationRequest)
                )
                .andExpect(redirectedUrl("http//junit?client_id=123&request_uri=http://redirect-url.com"));
    }


    @Test
    @DisplayName("then if there is a valid lightrequest, but wrong relay state, return error")
    void testValidLightRequestButWrongState() throws Exception {

        LightResponse lightResponse = getLightResponse("otherRelayState");
        when(specificConnectorService.getCachedRequest(any(String.class))).thenReturn(null);
        when(specificCommunicationService.getAndRemoveResponse(any(String.class), any())).thenReturn(lightResponse);
        RedirectedResponse clientResponse = mock(RedirectedResponse.class);
        when(clientResponse.toQueryRedirectUri()).thenReturn(new URI("http//junit?token=hei"));
        PushedAuthorizationRequest authorizationRequest = mock(PushedAuthorizationRequest.class);
        when(openIDConnectSdk.errorResponse(eq(authorizationRequest), anyString(), anyString())).thenReturn(mock(no.idporten.sdk.oidcserver.protocol.AuthorizationResponse.class));
        when(openIDConnectSdk.createClientResponse(any(no.idporten.sdk.oidcserver.protocol.AuthorizationResponse.class))).thenReturn(clientResponse);
        mockMvc.perform(post("/ConnectorResponse")
                        .sessionAttr(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST, authorizationRequest)
                )
                .andExpect(redirectedUrl("http//junit?token=hei"));
    }

    private static LightResponse getLightResponse(String relayState) {
        return LightResponse.builder()
                .citizenCountryCode("NO")
                .id("123")
                .issuer("issuer")
                .attributes(List.of(new Attribute("http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName", List.of("familyName")),
                        new Attribute("http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName", List.of("firstName")),
                        new Attribute("http://eidas.europa.eu/attributes/naturalperson/DateOfBirth", List.of("dateOfBirth")),
                        new Attribute("http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier", List.of("pid"))))
                .levelOfAssurance(LevelOfAssurance.EIDAS_LOA_LOW)
                .relayState(relayState)
                .inResponseToId("abc")
                .status(Status.builder().statusCode(EIDASStatusCode.SUCCESS_URI.getValue()).failure(false).statusMessage("ok").build())
                .build();
    }

    @AfterEach
    void tearDown() {
        verify(auditService).auditLightResponse(any(LightResponse.class), any());
        // Ensure that the static mocks are closed after each test
        binaryLightTokenHelperMock.close();
        incomingLightResponseValidatorMock.close();
    }

}
