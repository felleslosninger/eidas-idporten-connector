package no.idporten.eidas.connector.web;

import eu.eidas.auth.commons.Country;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@RequestMapping("/citizencountry")
public class CitizenCountryController {

    @GetMapping
    public String showForm(Model model) {
        model.addAttribute("citizenForm", new CitizenCountryForm());
        return "selector";
    }

    @PostMapping
    public ModelAndView submitForm(@ModelAttribute("citizenForm") CitizenCountryForm form) {
        ModelAndView modelAndView = new ModelAndView("confirmation");
        modelAndView.addObject("selectedCountry", form.getCountryId());
        return modelAndView;
    }
}
