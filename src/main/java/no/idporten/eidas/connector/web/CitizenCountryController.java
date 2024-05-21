package no.idporten.eidas.connector.web;

import eu.eidas.auth.commons.Country;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.logging.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@RequestMapping("/citizencountry")
@RequiredArgsConstructor
public class CitizenCountryController {

    private final AuditService auditService;

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("citizenForm", new CitizenCountryForm());
        return "selector";
    }

    @PostMapping
    public ModelAndView submitSelection(@ModelAttribute("citizenForm") CitizenCountryForm form,
                                        Model model,
                                        @RequestParam("action") String action) {
        ModelAndView modelAndView = new ModelAndView();

        if ("ok".equals(action)) {
            // Logic for save action
            modelAndView.setViewName("confirmation");
            modelAndView.addObject("selectedCountry", form.getCountryId());
            form.setCountryId(form.getCountryId()) ;
            modelAndView.addObject("citizenForm", form);
        } else if ("cancel".equals(action)) {
            modelAndView.setViewName("redirect:/some-other-page");
        } else {
            modelAndView.setViewName("error");
            modelAndView.addObject("error", "Unknown action: %s".formatted(action));
        }
        return modelAndView;
    }

    @PostMapping("/confirm")
    public ModelAndView confirmation(@ModelAttribute("citizenForm") CitizenCountryForm form,

                                     @RequestParam("action") String action) {
        ModelAndView modelAndView = new ModelAndView();

        if ("confirm".equals(action)) {
            String selectedCountry = form.getCountryId();
            auditService.auditCountrySelection(selectedCountry);
            //todo add light request and send token to connector.https://digdir.atlassian.net/browse/ID-4291
            if ("is".equalsIgnoreCase(selectedCountry)) {
                modelAndView.setViewName("redirect:https://www.visiticeland.com");
            } else if ("ca".equalsIgnoreCase(selectedCountry)) {
                modelAndView.setViewName("redirect:https://www.turing.ac.uk/research/research-projects/demoland");
            } else {
                modelAndView.setViewName("error");
                modelAndView.addObject("error", "Unsupported country: %s".formatted(selectedCountry));
            }
        } else if ("back".equals(action)) {
            modelAndView.addObject("citizenForm", new CitizenCountryForm());
            modelAndView.setViewName("selector");
        } else {
            modelAndView.setViewName("error");
            modelAndView.addObject("error", "Unknown action: %s".formatted(action));
        }
        return modelAndView;
    }
}
