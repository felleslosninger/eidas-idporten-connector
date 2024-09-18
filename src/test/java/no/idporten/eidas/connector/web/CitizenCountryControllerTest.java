package no.idporten.eidas.connector.web;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

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

    @Test
    @DisplayName("the a lightrequest shall be auditlogged")
    void testAuditLogging() throws Exception {
        PushedAuthorizationRequest authorizationRequest = mock(PushedAuthorizationRequest.class);
        when(specificConnectorService.createStoreBinaryLightTokenRequestBase64(any())).thenReturn("lighttoken");
        when(specificConnectorService.getEuConnectorRedirectUri()).thenReturn("http//junit");

        mockMvc.perform(post("/citizencountry")
                        .sessionAttr(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST, authorizationRequest)
                        .formField("action", "ok")
                        .formField("countryId", "CA"))
                .andExpect(redirectedUrl("http//junit?token=lighttoken"));

        verify(auditService, times(1)).auditLightRequest(any());
    }

}
