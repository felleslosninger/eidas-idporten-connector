package no.idporten.eidas.connector.web;

import lombok.RequiredArgsConstructor;
import no.idporten.eidas.connector.config.EUCountriesProperties;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.eidas.connector.service.SpecificConnectorService;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

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
        return "selector";
    }

    @PostMapping
    public ModelAndView submitSelection(@ModelAttribute(CITIZEN_FORM) CitizenCountryForm form,
                                        @SessionAttribute(SESSION_ATTRIBUTE_AUTHORIZATION_REQUEST) PushedAuthorizationRequest authorizationRequest,
                                        @RequestParam("action") String action) {
        ModelAndView modelAndView = new ModelAndView();

        if ("next".equals(action)) {
            String selectedCountry = form.getCountryId().toUpperCase();
            auditService.auditCountrySelection(selectedCountry);
            modelAndView.setViewName(createLightRequest(selectedCountry, authorizationRequest));
        } else {
            modelAndView.setViewName(ERROR);
            modelAndView.addObject(ERROR, "Unknown action: %s".formatted(action));
        }
        return modelAndView;
    }


    private String createLightRequest(String selectedCountry, PushedAuthorizationRequest pushedAuthorizationRequest) {
        LightRequest lightRequest = specificConnectorService.buildLightRequest(selectedCountry, pushedAuthorizationRequest);
        auditService.auditLightRequest(lightRequest);
        String lightToken = specificConnectorService.createStoreBinaryLightTokenRequestBase64(lightRequest);
        specificConnectorService.storeStateParams(lightRequest, pushedAuthorizationRequest);
        return "redirect:%s?token=%s".formatted(specificConnectorService.getEuConnectorRedirectUri(), lightToken);
    }

    @ModelAttribute("isTest")
    public boolean includeTestCountries() {
        return euCountriesProperties.isTest();
    }

    @ModelAttribute("countriesIncluded")
    public String included() {
        if (CollectionUtils.isEmpty(euCountriesProperties.getIncluded())) {
            return "";
        }
        return String.join(",", euCountriesProperties.getIncluded());
    }

    @ModelAttribute("countriesExcluded")
    public String excluded() {
        if (CollectionUtils.isEmpty(euCountriesProperties.getExcluded())) {
            return "";
        }
        return String.join(",", euCountriesProperties.getExcluded());
    }
}
