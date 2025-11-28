package no.idporten.eidas.connector.integration.nobid.web;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProtocolVerifiers;
import no.idporten.eidas.connector.integration.nobid.service.NobidMatchingServiceClient;
import no.idporten.eidas.connector.service.AuthorizationResponseHelper;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

import static no.idporten.eidas.connector.web.SessionAttributes.SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NobidCallbackControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthorizationResponseHelper authorizationResponseHelper;

    @Mock
    private NobidSession nobidSession;

    @Mock
    private NobidMatchingServiceClient matchingServiceClient;

    @InjectMocks
    private NobidCallbackController controller;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new LocalAdvice())
                .build();
    }

    @Test
    @DisplayName("callback returns ok and invokes AuthorizationResponseHelper on success")
    void callback_success() throws Exception {
        // Given
        var verifiers = new OidcProtocolVerifiers("nobid", new com.nimbusds.oauth2.sdk.id.State("st"), new Nonce("nn"), new com.nimbusds.oauth2.sdk.pkce.CodeVerifier("a".repeat(43)), Instant.now());
        when(nobidSession.getOidcProtocolVerifiers()).thenReturn(verifiers);
        when(nobidSession.getLevelOfAssurance()).thenReturn("http://eidas.europa.eu/LoA/substantial");
        when(nobidSession.getEidasUser()).thenReturn(new EidasUser(new no.idporten.eidas.connector.service.EIDASIdentifier("NO/NO/ABC"), "1990-01-01", java.util.Map.of()));

        // Build ID Token with matching nonce and a sub/pid
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .claim("nonce", "nn")
                .claim("sub", "12345678901")
                .build();
        SignedJWT idToken = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        idToken.sign(new MACSigner("01234567890123456789012345678901"));
        OIDCTokenResponse tokenResponse = mock(OIDCTokenResponse.class);
        OIDCTokens tokens = mock(OIDCTokens.class);
        when(tokenResponse.getOIDCTokens()).thenReturn(tokens);
        when(tokens.getIDToken()).thenReturn(idToken);
        when(matchingServiceClient.getToken(eq("code123"), eq(verifiers))).thenReturn(tokenResponse);

        // When + Then
        mockMvc.perform(get("/callback/nobid")
                        .param("state", "st")
                        .param("code", "code123")
                        .sessionAttr(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST, mock(PushedAuthorizationRequest.class)))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        verify(authorizationResponseHelper).returnAuthorizationCode(any(), any(), anyString(), any(PushedAuthorizationRequest.class), any(EidasUser.class), eq("12345678901"));
    }

    @Test
    @DisplayName("callback returns 500 on missing verifiers")
    void callback_missing_verifiers() throws Exception {
        when(nobidSession.getOidcProtocolVerifiers()).thenReturn(null);
        mockMvc.perform(get("/callback/nobid").param("state", "st"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("callback returns ok when code is null and does not call token endpoint")
    void callback_code_null() throws Exception {
        // Given
        var verifiers = new OidcProtocolVerifiers("nobid", new com.nimbusds.oauth2.sdk.id.State("st"), new Nonce("nn"), new com.nimbusds.oauth2.sdk.pkce.CodeVerifier("a".repeat(43)), Instant.now());
        when(nobidSession.getOidcProtocolVerifiers()).thenReturn(verifiers);
        when(nobidSession.getLevelOfAssurance()).thenReturn("http://eidas.europa.eu/LoA/substantial");
        when(nobidSession.getEidasUser()).thenReturn(new EidasUser(new no.idporten.eidas.connector.service.EIDASIdentifier("NO/NO/ABC"), "1990-01-01", java.util.Map.of()));

        // When + Then
        mockMvc.perform(get("/callback/nobid")
                        .param("state", "st")
                        .sessionAttr(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST, mock(PushedAuthorizationRequest.class)))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        // Then: helper called with pid null and token endpoint not called
        verify(authorizationResponseHelper).returnAuthorizationCode(any(), any(), anyString(), any(PushedAuthorizationRequest.class), any(EidasUser.class), isNull());
        verify(matchingServiceClient, never()).getToken(anyString(), any());
    }

    @ControllerAdvice
    static class LocalAdvice {
        @ExceptionHandler(no.idporten.eidas.connector.exceptions.SpecificConnectorException.class)
        public ResponseEntity<String> handle(no.idporten.eidas.connector.exceptions.SpecificConnectorException ex) {
            return ResponseEntity.status(500).body("error");
        }
    }
}
