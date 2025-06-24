package no.idporten.eidas.connector.web;

import no.idporten.eidas.connector.config.EUCountriesProperties;
import no.idporten.eidas.connector.config.StaticResourcesProperties;
import no.idporten.eidas.connector.config.WebSecurityConfig;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.eidas.connector.service.SpecificConnectorService;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.AuthorizationResponse;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.RedirectedResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static no.idporten.eidas.connector.web.SessionAttributes.SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CitizenCountryController.class)
@DisplayName("When calling the CitizenCountryController")
@Import(WebSecurityConfig.class)
class CitizenCountryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StaticResourcesProperties staticResourcesProperties;

    @MockitoBean
    private OpenIDConnectIntegration openIDConnectSdk;
    @MockitoBean
    private AuditService auditService;
    @MockitoBean
    private SpecificConnectorService specificConnectorService;
    @MockitoBean
    private EUCountriesProperties euCountriesProperties;

    @Test
    @DisplayName("the a lightrequest shall be auditlogged")
    void testAuditLogging() throws Exception {
        PushedAuthorizationRequest authorizationRequest = mock(PushedAuthorizationRequest.class);
        when(specificConnectorService.createStoreBinaryLightTokenRequestBase64(any())).thenReturn("lighttoken");
        when(specificConnectorService.getEuConnectorRedirectUri()).thenReturn("http//junit");
        when(specificConnectorService.buildLightRequest(any(), any())).thenReturn(mock(LightRequest.class));
        when(specificConnectorService.storeStateParams(any(), any())).thenReturn("123");
        mockMvc.perform(post("/citizencountry")
                        .sessionAttr(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST, authorizationRequest)
                        .formField("action", "next")
                        .formField("countryId", "CA"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(containsString("Click to redirect")));


        verify(auditService, times(1)).auditLightRequest(any(LightRequest.class));
    }

    @Test
    @DisplayName("if the countryid is empty, return error")
    void testValidation() throws Exception {
        PushedAuthorizationRequest authorizationRequest = mock(PushedAuthorizationRequest.class);

        mockMvc.perform(post("/citizencountry")
                        .sessionAttr(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST, authorizationRequest)
                        .formField("action", "next"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(containsString("Please select a country")));


        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("if the user cancels, return with access_denied")
    void testCancel() throws Exception {
        PushedAuthorizationRequest authorizationRequest = mock(PushedAuthorizationRequest.class);
        when(openIDConnectSdk.errorResponse(any(), any(), any())).thenReturn(mock(AuthorizationResponse.class));
        RedirectedResponse redirectedResponse = mock(RedirectedResponse.class);
        when(openIDConnectSdk.createClientResponse(any())).thenReturn(redirectedResponse);
        when(redirectedResponse.toQueryRedirectUri()).thenReturn(URI.create("http://backToIdporten"));
        mockMvc.perform(post("/citizencountry")
                        .sessionAttr(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST, authorizationRequest)
                        .formField("action", "cancel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://backToIdporten"));


        verifyNoInteractions(auditService);
    }


    @Test
    @DisplayName("when countries included, include the countries")
    void testCountryIncluded() throws Exception {
        when(euCountriesProperties.included()).thenReturn("is,dk");
        mockMvc.perform(get("/citizencountry"))
                .andExpect(status().isOk())
                .andExpect(view().name("selector"))
                .andExpect(model().attributeExists("countriesIncluded"))
                .andExpect(model().attribute("countriesIncluded", "is,dk"))
        ;
    }

    @Test
    @DisplayName("when excluded and test countries configured, include the config")
    void testCountryConfig() throws Exception {
        when(euCountriesProperties.included()).thenReturn("is,dk");
        when(euCountriesProperties.excluded()).thenReturn("se");
        when(euCountriesProperties.isTest()).thenReturn(true);
        mockMvc.perform(get("/citizencountry"))
                .andExpect(status().isOk())
                .andExpect(view().name("selector"))
                .andExpect(model().attributeExists("countriesIncluded"))
                .andExpect(model().attribute("countriesIncluded", "is,dk"))
                .andExpect(model().attribute("countriesExcluded", "se"))
                .andExpect(model().attribute("isTest", true))
        ;

    }

}
