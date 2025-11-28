package no.idporten.eidas.connector.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import no.idporten.eidas.connector.config.EidasClaims;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.net.URI;
import java.util.Map;

import static no.idporten.eidas.connector.config.EidasClaims.IDPORTEN_EIDAS_CITIZEN_COUNTRY_CODE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationResponseHelperTest {

    @Mock
    private OpenIDConnectIntegration openIDConnectSdk;
    @Mock
    private LevelOfAssuranceHelper levelOfAssuranceHelper;

    @InjectMocks
    private AuthorizationResponseHelper helper;


    private static EidasUser userWithClaims(String eidasId) {
        return new EidasUser(new EIDASIdentifier(eidasId), "2000-12-01",
                Map.of(
                        EidasClaims.IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM, eidasId,
                        EidasClaims.IDPORTEN_EIDAS_GIVEN_NAME_CLAIM, "Given",
                        EidasClaims.IDPORTEN_EIDAS_FAMILY_NAME_CLAIM, "Family",
                        EidasClaims.IDPORTEN_EIDAS_DATE_OF_BIRTH_CLAIM, "2000-12-01"
                ));
    }

    @Test
    @DisplayName("when pid exists then get authorization with pid and sub claim set to eidas identifier")
    void testReturnAuthorizationCode_withPid() throws Exception {
        // Given
        when(levelOfAssuranceHelper.eidasAcrToIdportenAcr("high")).thenReturn("idporten-high");
        AuthorizationResponse authorizationResponse = mock(AuthorizationResponse.class);
        when(openIDConnectSdk.authorize(any(), any())).thenReturn(authorizationResponse);
        when(openIDConnectSdk.createClientResponse(authorizationResponse)).thenReturn(mock(RedirectedResponse.class));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        HttpServletResponse response = mock(HttpServletResponse.class);

        PushedAuthorizationRequest par = mock(PushedAuthorizationRequest.class);
        EidasUser eidasUser = userWithClaims("SE/NO/ABC123");

        ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        // Use spy to bypass Whenual HTTP writing in helper
        AuthorizationResponseHelper spyHelper = spy(helper);
        doNothing().when(spyHelper).sendHttpResponse(any(), any());

        // When
        spyHelper.returnAuthorizationCode(request, response, "high", par, eidasUser, "pid-xyz");

        // Then
        verify(openIDConnectSdk).authorize(eq(par), authCaptor.capture());
        Authorization auth = authCaptor.getValue();
        assertEquals("SE/NO/ABC123", auth.getSub());
        assertEquals("idporten-high", auth.getAcr());
        assertTrue(auth.getAmr().contains("eidas"));
        assertEquals("SE", auth.getAttributes().get(IDPORTEN_EIDAS_CITIZEN_COUNTRY_CODE));
        assertEquals("pid-xyz", auth.getAttributes().get("pid"));
        // contains pass-through EIDAS claims
        assertEquals("Given", auth.getAttributes().get(EidasClaims.IDPORTEN_EIDAS_GIVEN_NAME_CLAIM));
        assertEquals("Family", auth.getAttributes().get(EidasClaims.IDPORTEN_EIDAS_FAMILY_NAME_CLAIM));

        verify(session).invalidate();
        verify(openIDConnectSdk).createClientResponse(authorizationResponse);
    }

    @Test
    @DisplayName("when pid doesn't exists then get authorization without pid and sub claim set to eidas identifier")
    void testReturnAuthorizationCode_withoutPid() throws Exception {
        when(levelOfAssuranceHelper.eidasAcrToIdportenAcr("low")).thenReturn("idporten-low");
        when(openIDConnectSdk.authorize(any(), any())).thenReturn(mock(AuthorizationResponse.class));
        when(openIDConnectSdk.createClientResponse(any())).thenReturn(mock(FormPostResponse.class));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession()).thenReturn(session);
        HttpServletResponse response = mock(HttpServletResponse.class);

        PushedAuthorizationRequest par = mock(PushedAuthorizationRequest.class);
        EidasUser eidasUser = userWithClaims("DE/NO/987");

        ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        AuthorizationResponseHelper spyHelper = spy(helper);
        doNothing().when(spyHelper).sendHttpResponse(any(), any());

        spyHelper.returnAuthorizationCode(request, response, "low", par, eidasUser, null);

        verify(openIDConnectSdk).authorize(eq(par), authCaptor.capture());
        Authorization auth = authCaptor.getValue();
        assertEquals("DE/NO/987", auth.getSub());
        assertEquals("idporten-low", auth.getAcr());
        assertEquals("DE", auth.getAttributes().get(IDPORTEN_EIDAS_CITIZEN_COUNTRY_CODE));
        assertFalse(auth.getAttributes().containsKey("pid"));
        verify(session).invalidate();
    }


    @Test
    @DisplayName("sendHttpResponse redirects for RedirectedResponse")
    void testSendHttpResponse_redirect() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        RedirectedResponse redirected = mock(RedirectedResponse.class);
        when(redirected.toQueryRedirectUri()).thenReturn(URI.create("https://example.org/cb?code=abc"));

        helper.sendHttpResponse(redirected, response);

        verify(response).sendRedirect("https://example.org/cb?code=abc");
    }

    @Test
    @DisplayName("sendHttpResponse writes form for FormPostResponse")
    void testSendHttpResponse_formPost() throws Exception {
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);

        FormPostResponse form = mock(FormPostResponse.class);
        when(form.getRedirectForm(true)).thenReturn("<form>post</form>");

        helper.sendHttpResponse(form, response);

        verify(response).setContentType("text/html;charset=UTF-8");
        verify(response).setCharacterEncoding("UTF-8");
        verify(writer).write("<form>post</form>");
    }
}
