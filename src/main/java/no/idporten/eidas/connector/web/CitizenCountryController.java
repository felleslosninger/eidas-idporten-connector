package no.idporten.eidas.connector.web;

import lombok.RequiredArgsConstructor;
import no.idporten.eidas.connector.logging.AuditService;
import no.idporten.eidas.connector.service.SpecificConnectorService;
import no.idporten.eidas.lightprotocol.messages.LightRequest;
import no.idporten.sdk.oidcserver.protocol.PushedAuthorizationRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

        if ("ok".equals(action)) {
            String selectedCountry = form.getCountryId().toUpperCase();
            auditService.auditCountrySelection(selectedCountry);
            modelAndView.setViewName(createLightRequest(selectedCountry, authorizationRequest));

        } else if ("cancel".equals(action)) {
            modelAndView.setViewName("redirect:/some-other-page");
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
}
