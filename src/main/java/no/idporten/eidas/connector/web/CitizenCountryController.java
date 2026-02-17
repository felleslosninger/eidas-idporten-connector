package no.idporten.eidas.connector.web;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import no.idporten.eidas.connector.config.EUCountriesProperties;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.eidas.connector.service.SpecificConnectorService;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.sdk.oidcserver.protocol.FormPostResponse;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
    private static final String NEXT_ACTION = "next";
    private static final String CANCEL_ACTION = "cancel";
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
    public String submitSelection(HttpServletResponse response,
                                  @ModelAttribute(CITIZEN_FORM) CitizenCountryForm form,
                                  BindingResult bindingResult,
                                  @SessionAttribute(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST) PushedAuthorizationRequest authorizationRequest,
                                  @RequestParam("action") String action
    ) throws IOException {

        switch (action) {
            case NEXT_ACTION:
                if (StringUtils.isEmpty(form.getCountryId())) {
                    bindingResult.rejectValue("countryId", "error.countryId", "Please select a country");
                    return "selector";
                }
                handleSubmit(response, form, authorizationRequest);
                break;
            case CANCEL_ACTION:
                throw new SpecificConnectorException("access_denied", "User cancelled");
            default:
                handleUnknownAction(response, action);
                break;
        }
        return null;
    }

    private static void handleUnknownAction(HttpServletResponse response, String action) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("<html><body>Error: Unknown action '" + action + "'</body></html>");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    private void handleSubmit(HttpServletResponse response, CitizenCountryForm form, PushedAuthorizationRequest authorizationRequest) throws IOException {
        String selectedCountry = form.getCountryId().toUpperCase();
        auditService.auditCountrySelection(selectedCountry);
        Map<String, String> parameterMap = createLightRequest(selectedCountry, authorizationRequest);

        FormPostResponse formPostResponse = new FormPostResponse(specificConnectorService.getEuConnectorRedirectUri(),
                parameterMap);
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(formPostResponse.getRedirectForm(true));
    }

    private Map<String, String> createLightRequest(String selectedCountry, PushedAuthorizationRequest pushedAuthorizationRequest) {
        LightRequest lightRequest = specificConnectorService.buildLightRequest(selectedCountry, pushedAuthorizationRequest);
        auditService.auditLightRequest(lightRequest);
        String lightToken = specificConnectorService.createStoreBinaryLightTokenRequestBase64(lightRequest);
        String relayState = specificConnectorService.storeStateParams(lightRequest, pushedAuthorizationRequest);
        return Map.of("token", lightToken, "RelayState", relayState);
    }

}
