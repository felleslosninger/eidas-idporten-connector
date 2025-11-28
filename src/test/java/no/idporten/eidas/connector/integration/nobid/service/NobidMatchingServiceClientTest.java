package no.idporten.eidas.connector.integration.nobid.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.integration.nobid.config.NobidProperties;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProtocolVerifiers;
import no.idporten.eidas.connector.integration.nobid.domain.OidcProvider;
import no.idporten.eidas.connector.integration.nobid.web.NobidSession;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.eidas.connector.matching.domain.UserMatchError;
import no.idporten.eidas.connector.matching.domain.UserMatchRedirect;
import no.idporten.eidas.connector.service.CountryCodeConverter;
import no.idporten.eidas.connector.service.EIDASIdentifier;
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

import static org.junit.jupiter.api.Assertions.*;
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

    @Mock
    private AuditService auditService;

    @Mock
    private CountryCodeConverter countryCodeConverter;

    @InjectMocks
    private NobidMatchingServiceClient client;

    @BeforeEach
    void setUp() {
        lenient().when(provider.issuer()).thenReturn(URI.create("https://example.com"));
        lenient().when(provider.clientId()).thenReturn("myNobidClient");
        lenient().when(provider.tokenEndpoint()).thenReturn(URI.create("https://example.com/token"));
        lenient().when(session.getOidcProtocolVerifiers()).thenReturn(new OidcProtocolVerifiers("nobid", new State("123"), new Nonce("456"), new CodeVerifier(randomPkceValue(53)), Instant.now().minus(Duration.ofMinutes(1))));
        lenient().when(provider.redirectUri()).thenReturn(URI.create("https://client.example.com/cb"));
    }

    @Test
    @DisplayName("sendPushedAuthorizationRequest returns UserMatchRedirect on PAR success")
    void par_success_returns_redirect() throws Exception {

        // Static mock for client auth
        try (MockedStatic<ClientAuthenticationService> mocked = Mockito.mockStatic(ClientAuthenticationService.class)) {
            PrivateKeyJWT mockPrivateKeyJWT = Mockito.mock(PrivateKeyJWT.class);
            mocked.when(() -> ClientAuthenticationService.createClientAuthentication(any(OidcProvider.class)))
                    .thenReturn(mockPrivateKeyJWT);

            // Provider endpoints
            lenient().when(provider.pushedAuthorizationRequestEndpoint()).thenReturn(URI.create("https://example.com/par"));
            lenient().when(provider.authorizationEndpoint()).thenReturn(URI.create("https://example.com/authorize"));
            lenient().when(provider.issuer()).thenReturn(URI.create("https://example.com"));

            // Spy client to intercept HTTP call
            NobidMatchingServiceClient spyClient = Mockito.spy(client);

            // Build a minimal AuthenticationRequest
            AuthenticationRequest authReq = new AuthenticationRequest.Builder(
                    new ResponseType(ResponseType.Value.CODE),
                    new Scope("openid"),
                    new ClientID("myclient"),
                    URI.create("https://client.example.com/cb")
            ).state(new State("abc"))
                    .build();

            // PAR success (201) with request_uri
            HTTPResponse httpResponse = new HTTPResponse(201);
            httpResponse.setContentType("application/json");
            httpResponse.setBody("{\n  \"request_uri\":\"urn:ietf:params:oauth:request_uri:ABC\",\n  \"expires_in\":90\n}");
            Mockito.doReturn(httpResponse).when(spyClient).sendHttpRequest(any(HTTPRequest.class));

            // When
            var result = spyClient.sendPushedAuthorizationRequest(authReq);

            // Then
            assertInstanceOf(UserMatchRedirect.class, result);
            String url = ((no.idporten.eidas.connector.matching.domain.UserMatchRedirect) result).redirectUrl();
            assertTrue(url.startsWith("https://example.com/authorize?request_uri="));
            assertTrue(url.contains("client_id=myNobidClient"));
        }
    }

    @Test
    @DisplayName("sendPushedAuthorizationRequest throws on PAR error response")
    void par_error_throws() throws Exception {
        try (MockedStatic<ClientAuthenticationService> mocked = Mockito.mockStatic(ClientAuthenticationService.class)) {
            PrivateKeyJWT mockPrivateKeyJWT = Mockito.mock(PrivateKeyJWT.class);
            mocked.when(() -> ClientAuthenticationService.createClientAuthentication(any(OidcProvider.class)))
                    .thenReturn(mockPrivateKeyJWT);

            lenient().when(provider.pushedAuthorizationRequestEndpoint()).thenReturn(URI.create("https://example.com/par"));

            NobidMatchingServiceClient spyClient = Mockito.spy(client);

            AuthenticationRequest authReq = new AuthenticationRequest.Builder(
                    new ResponseType(ResponseType.Value.CODE),
                    new Scope("openid"),
                    new ClientID("myclient"),
                    URI.create("https://client.example.com/cb")
            ).state(new State("abc"))
                    .build();

            // 400 error JSON per RFC pushed auth error
            HTTPResponse httpResponse = new HTTPResponse(400);
            httpResponse.setContentType("application/json");
            httpResponse.setBody("{\"error\":\"invalid_request\",\"error_description\":\"bad\"}");
            Mockito.doReturn(httpResponse).when(spyClient).sendHttpRequest(any(HTTPRequest.class));

            try {
                spyClient.sendPushedAuthorizationRequest(authReq);
            } catch (SpecificConnectorException e) {
                assertTrue(e.getMessage().contains("Integration with nobidClaimsProvider failed"));
                return;
            }
            throw new AssertionError("Expected SpecificConnectorException");
        }
    }

    @Test
    @DisplayName("match returns UserMatchError when internal exception occurs")
    void match_returns_error_on_exception() {
        NobidMatchingServiceClient spyClient = Mockito.spy(client);
        // Avoid objectMapper usage by stubbing authentication request creation
        AuthenticationRequest dummy = new AuthenticationRequest.Builder(
                new ResponseType(ResponseType.Value.CODE), new Scope("openid"), new ClientID("c"), URI.create("https://cb"))
                .state(new State("s")).build();
        doReturn(dummy).when(spyClient).createNobidAuthenticationRequest(any(), any());
        // Force exception during PAR call
        try {
            doThrow(new RuntimeException("boom")).when(spyClient).sendPushedAuthorizationRequest(any(AuthenticationRequest.class));
        } catch (Exception ignored) {
        }

        var resp = spyClient.match(new EidasUser(new EIDASIdentifier("NO/NO/123"), "2000-01-01", java.util.Map.of()));
        assertInstanceOf(UserMatchError.class, resp);
    }

    @Test
    @DisplayName("copyWithSubjectCountry changes subject country in identifier")
    void copyWithSubjectCountry_changes_country() {
        EidasUser u = new EidasUser(new EIDASIdentifier("SE/NO/ABC"), "1990-01-01", java.util.Map.of());
        EidasUser copy = NobidMatchingServiceClient.copyWithSubjectCountry(u, "DE");
        org.junit.jupiter.api.Assertions.assertEquals("DE/NO/ABC", copy.eidasIdentifier().getFormattedEidasIdentifier());
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
            // Simulate a provider returning only an ID Token without an access_token
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

            // when
            OIDCTokenResponse response = spyClient.getToken("auth-code-123", session.getOidcProtocolVerifiers());

            // then
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
            // Access token may be absent for this provider; only ID Token is required, although it will fail in nimbus without one

            // Verify static call occurred
            mocked.verify(() -> ClientAuthenticationService.createClientAuthentication(any(OidcProvider.class)));
        }
    }


    // Generates a PKCE-compliant code_verifier value using unreserved charWheners, with the requested length.
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
