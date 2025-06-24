package no.idporten.eidas.connector.web;

import lombok.RequiredArgsConstructor;
import no.idporten.eidas.connector.config.StaticResourcesProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class StaticResourcesControllerAdvice {

    private final StaticResourcesProperties staticResourcesProperties;

    @ModelAttribute("themeId")
    public String themeId() {
        return staticResourcesProperties.getThemeId();
    }

    @ModelAttribute("staticResourcesBaseUri")
    public String staticResourcesBaseUri() {
        return staticResourcesProperties.getStaticResourcesBaseUri();
    }
}
