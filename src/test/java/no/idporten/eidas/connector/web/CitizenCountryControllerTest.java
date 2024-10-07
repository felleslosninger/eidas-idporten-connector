package no.idporten.eidas.connector.web;

import no.idporten.eidas.connector.config.EUCountriesProperties;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.eidas.connector.service.SpecificConnectorService;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static no.idporten.eidas.connector.web.SessionAttributes.SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CitizenCountryController.class)
@DisplayName("When calling the CitizenCountryController")
class CitizenCountryControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private OpenIDConnectIntegration openIDConnectSdk;
    @MockBean
    private AuditService auditService;
    @MockBean
    private SpecificConnectorService specificConnectorService;
    @MockBean
    private EUCountriesProperties euCountriesProperties;

    @Test
    @DisplayName("the a lightrequest shall be auditlogged")
    void testAuditLogging() throws Exception {
        PushedAuthorizationRequest authorizationRequest = mock(PushedAuthorizationRequest.class);
        when(specificConnectorService.createStoreBinaryLightTokenRequestBase64(any())).thenReturn("lighttoken");
        when(specificConnectorService.getEuConnectorRedirectUri()).thenReturn("http//junit");

        mockMvc.perform(post("/citizencountry")
                        .sessionAttr(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST, authorizationRequest)
                        .formField("action", "next")
                        .formField("countryId", "CA"))
                .andExpect(redirectedUrl("http//junit?token=lighttoken"));

        verify(auditService, times(1)).auditLightRequest(any());
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
