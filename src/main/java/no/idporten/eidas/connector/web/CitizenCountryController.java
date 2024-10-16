package no.idporten.eidas.connector.web;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import no.idporten.eidas.connector.config.EUCountriesProperties;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.eidas.connector.service.SpecificConnectorService;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.sdk.oidcserver.protocol.FormPostResponse;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

import static no.idporten.eidas.connector.web.SessionAttributes.SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST;

@Controller
@RequestMapping("/citizencountry")
@RequiredArgsConstructor
public class CitizenCountryController {

    private static final String CITIZEN_FORM = "citizenForm";
    private static final String ERROR = "error";
    private final AuditService auditService;
    private final SpecificConnectorService specificConnectorService;
    private final EUCountriesProperties euCountriesProperties;

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute(CITIZEN_FORM, new CitizenCountryForm());
        model.addAttribute("countriesIncluded", euCountriesProperties.included());
        model.addAttribute("countriesExcluded", euCountriesProperties.excluded());
        model.addAttribute("isTest", euCountriesProperties.isTest());
        return "selector";
    }

    @PostMapping
    public void submitSelection(HttpServletResponse response,
                                @ModelAttribute(CITIZEN_FORM) CitizenCountryForm form,
                                @SessionAttribute(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST) PushedAuthorizationRequest authorizationRequest,
                                @RequestParam("action") String action) throws IOException {

        if ("next".equals(action)) {
            String selectedCountry = form.getCountryId().toUpperCase();
            auditService.auditCountrySelection(selectedCountry);
            Map<String, String> parameterMap = createLightRequest(selectedCountry, authorizationRequest);

            FormPostResponse formPostResponse = new FormPostResponse(specificConnectorService.getEuConnectorRedirectUri(),
                    parameterMap);
            response.setContentType("text/html;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(formPostResponse.getRedirectForm());

        } else {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("<html><body>Error: Unknown action '" + action + "'</body></html>");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

    }


    record RequestToEUConnector(String lightToken, String relayState) {
    }

    private Map<String, String> createLightRequest(String selectedCountry, PushedAuthorizationRequest pushedAuthorizationRequest) {
        LightRequest lightRequest = specificConnectorService.buildLightRequest(selectedCountry, pushedAuthorizationRequest);
        auditService.auditLightRequest(lightRequest);
        String lightToken = specificConnectorService.createStoreBinaryLightTokenRequestBase64(lightRequest);
        String relayState = specificConnectorService.storeStateParams(lightRequest, pushedAuthorizationRequest);
        return Map.of("token", lightToken, "RelayState", relayState);
    }

}
