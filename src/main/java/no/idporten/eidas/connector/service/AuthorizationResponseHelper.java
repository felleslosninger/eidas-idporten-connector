package no.idporten.eidas.connector.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.sdk.oidcserver.OpenIDConnectIntegration;
import no.idporten.sdk.oidcserver.protocol.*;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static no.idporten.eidas.connector.config.EidasClaims.IDPORTEN_EIDAS_CITIZEN_COUNTRY_CODE;

@Service
@RequiredArgsConstructor
public class AuthorizationResponseHelper {
    private final OpenIDConnectIntegration openIDConnectSdk;
    private final LevelOfAssuranceHelper levelOfAssuranceHelper;
    protected static final String EIDAS_AMR = "eidas";
    protected static final String PID_CLAIM = "pid";

    private Authorization generateAuthorizationResponse(String levelOfAssurance, EidasUser eidasUser, String pid) {
        Authorization.AuthorizationBuilder authorizationBuilder = Authorization.builder()
                .sub(eidasUser.eidasIdentifier().getFormattedEidasIdentifier())
                .acr(levelOfAssuranceHelper.eidasAcrToIdportenAcr(levelOfAssurance))
                .amr(EIDAS_AMR);

        if (eidasUser.eidasClaims() != null) {
            eidasUser.eidasClaims().forEach(authorizationBuilder::attribute);
        }
        //if a pid was found
        if (pid != null) {
            authorizationBuilder.attribute(PID_CLAIM, pid);
        }

        authorizationBuilder.attribute(IDPORTEN_EIDAS_CITIZEN_COUNTRY_CODE,
                //already validated claim value
                eidasUser.eidasIdentifier().getSubjectCountryCode());
        return authorizationBuilder.build();
    }

    public String returnAuthorizationCode(HttpServletRequest request, HttpServletResponse response, String levelOfAssurance, PushedAuthorizationRequest parRequest, EidasUser eidasUser, String pid) throws IOException {
        Authorization authorization = generateAuthorizationResponse(levelOfAssurance, eidasUser, pid);
        AuthorizationResponse authorizationResponse = openIDConnectSdk.authorize(parRequest, authorization);
        request.getSession().invalidate();
        sendHttpResponse(openIDConnectSdk.createClientResponse(authorizationResponse), response);
        return null;
    }

    protected void sendHttpResponse(ClientResponse clientResponse, HttpServletResponse response) throws IOException {
        switch (clientResponse) {
            case RedirectedResponse redirectedResponse ->
                    response.sendRedirect(redirectedResponse.toQueryRedirectUri().toString());

            case FormPostResponse formPostResponse -> {
                response.setContentType("text/html;charset=UTF-8");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(formPostResponse.getRedirectForm(true));
            }
            default ->
                    throw new IllegalStateException("Unexpected client response type %s".formatted(clientResponse.getClass().getName()));
        }
    }
}
