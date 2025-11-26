package no.idporten.eidas.connector.integration.nobid.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import no.idporten.eidas.connector.integration.nobid.config.NobidProperties;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProtocolVerifiers;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProvider;
import no.idporten.eidas.connector.integration.nobid.web.NobidSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link NobidMatchingServiceClient}.
 */
@ExtendWith(MockitoExtension.class)
class NobidMatchingServiceClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private NobidSession session;

    @Mock
    private OidcProvider provider;

    @Mock
    private NobidProperties nobidProperties;

    @InjectMocks
    private NobidMatchingServiceClient client;

    @BeforeEach
    void setUp() {
        lenient().when(provider.issuer()).thenReturn(URI.create("https://example.com"));
        lenient().when(provider.clientId()).thenReturn("myNobidClient");
        when(provider.tokenEndpoint()).thenReturn(URI.create("https://example.com/token"));
        lenient().when(session.getOidcProtocolVerifiers()).thenReturn(new OidcProtocolVerifiers("nobid", new State("123"), new Nonce("456"), new CodeVerifier(randomPkceValue(53)), Instant.now().minus(Duration.ofMinutes(1))));
        lenient().when(provider.redirectUri()).thenReturn(URI.create("https://client.example.com/cb"));
    }

    @Test
    @DisplayName("getToken returns OIDCTokenResponse on success, and add client_id")
    void getToken_success() throws Exception {

        // Static mock ClientAuthenticationService.createClientAuthentication
        try (MockedStatic<ClientAuthenticationService> mocked = Mockito.mockStatic(ClientAuthenticationService.class)) {
            PrivateKeyJWT mockPrivateKeyJWT = Mockito.mock(PrivateKeyJWT.class);
            when(mockPrivateKeyJWT.getClientID()).thenReturn(new ClientID("myclient"));
            mocked.when(() -> ClientAuthenticationService.createClientAuthentication(any(OidcProvider.class)))
                    .thenReturn(mockPrivateKeyJWT);

            // Stub HTTP call via spy
            NobidMatchingServiceClient spyClient = Mockito.spy(client);
            String idToken = new PlainJWT(new JWTClaimsSet.Builder().claim("sub", "123").build()).serialize();
            // Simulate provider returning only an ID Token without an access_token
            String body = "{" +
                    "\"token_type\":\"Bearer\"," +
                    "\"expires_in\":3600," +
                    "\"access_token\":\"123\"," +
                    "\"id_token\":\"" + idToken + "\"" +
                    "}";
            HTTPResponse ok = new HTTPResponse(200);
            ok.setContentType("application/json");
            ok.setBody(body);
            Mockito.doReturn(ok).when(spyClient).sendHttpRequest(any(HTTPRequest.class));

            // Act
            OIDCTokenResponse response = spyClient.getToken("auth-code-123", session.getOidcProtocolVerifiers());

            // Assert
            // Verify the token request body contains expected parameters
            ArgumentCaptor<HTTPRequest> reqCaptor = ArgumentCaptor.forClass(HTTPRequest.class);
            verify(spyClient).sendHttpRequest(reqCaptor.capture());
            HTTPRequest captured = reqCaptor.getValue();
            assertNotNull(captured);
            String form = captured.getBody();
            assertNotNull(form);
            assertTrue(form.contains("grant_type=authorization_code"));
            assertTrue(form.contains("redirect_uri="));
            assertTrue(form.contains("code="));
            assertTrue(form.contains("code_verifier="));
            // Using client_secret_post should include client_id
            assertTrue(form.contains("client_id=myclient"));

            assertNotNull(response);
            assertNotNull(response.getOIDCTokens());
            assertNotNull(response.getOIDCTokens().getIDToken());
            // Access token may be absent for this provider; only ID Token is required

            // Verify static call occurred
            mocked.verify(() -> ClientAuthenticationService.createClientAuthentication(any(OidcProvider.class)));
        }
    }


    // Generates a PKCE-compliant code_verifier value using unreserved characters, with the requested length.
    private static String randomPkceValue(int length) {
        final char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~".toCharArray();
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet[random.nextInt(alphabet.length)]);
        }
        return sb.toString();
    }

}
