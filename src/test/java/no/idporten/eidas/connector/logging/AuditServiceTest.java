package no.idporten.eidas.connector.logging;

import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.logging.audit.AuditEntry;
import no.idporten.logging.audit.AuditLogger;
import no.idporten.sdk.oidcserver.protocol.AuditData;
import no.idporten.sdk.oidcserver.protocol.AuthorizationRequest;
import no.idporten.sdk.oidcserver.protocol.AuthorizationResponse;
import no.idporten.sdk.oidcserver.protocol.TokenRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogger mockAuditLogger;

    @InjectMocks
    private AuditService auditService;

    @Captor
    private ArgumentCaptor<AuditEntry> auditEntryCaptor;

    @Test
    @DisplayName("Audit OIDC Authorization Request")
    void testAuditAuthorizationRequest() {
        AuthorizationRequest request = mock(AuthorizationRequest.class);
        when(request.getAuditData()).thenReturn(mock(AuditData.class));
        auditService.auditAuthorizationRequest(request);
        verify(mockAuditLogger).log(any());
    }

    @Test
    @DisplayName("Audit OIDC Authorization Response")
    void testAuditAuthorizationResponse() {
        // Assuming AuthorizationResponse has a similar getAuditData() method
        AuthorizationResponse response = mock(AuthorizationResponse.class);
        when(response.getAuditData()).thenReturn(mock(AuditData.class));
        auditService.auditAuthorizationResponse(response);
        verify(mockAuditLogger).log(any());
    }

    @Test
    @DisplayName("Audit OIDC Token Request")
    void testAuditTokenRequest() {
        TokenRequest request = mock(TokenRequest.class);
        when(request.getAuditData()).thenReturn(mock(AuditData.class));
        auditService.auditTokenRequest(request);
        verify(mockAuditLogger).log(any());
    }

    @Test
    @DisplayName("Audit EIDAS Light Request")
    void testAuditLightRequest() {
        AuditData auditData = mock(AuditData.class);
        when(auditData.getAttributes()).thenReturn(Map.of("key", "value"));
        LightRequest lightRequest = mock(LightRequest.class);
        when(lightRequest.getAuditData()).thenReturn(auditData);

        auditService.auditLightRequest(lightRequest);

        verify(mockAuditLogger).log(auditEntryCaptor.capture());
        AuditEntry loggedEntry = auditEntryCaptor.getValue();

        assertNotNull(loggedEntry);
        assertEquals(AuditService.AuditIdPattern.EIDAS_LIGHT_REQUEST.auditIdentifier().auditId(),
                loggedEntry.getAuditId().auditId());
        assertTrue(loggedEntry.getAttributes().containsKey("light_request"));
        assertEquals(auditData.getAttributes(), loggedEntry.getAttributes().get("light_request"));
    }
}
