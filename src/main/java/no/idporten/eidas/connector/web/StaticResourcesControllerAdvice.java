package no.idporten.eidas.connector.web;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class StaticResourcesControllerAdvice {

    private static final String LATEST_VERSION = "latest";
    private static final String DEFAULT_THEME = "idporten";

    @Value("${idporten.static.design.version:latest}")
    private String designVersion;

    @Value("${idporten.static.design.themeId:idporten}")
    private String themeId;

    @ModelAttribute("themeId")
    public String themeId() {
        return StringUtils.isNotBlank(themeId)? themeId : DEFAULT_THEME;
    }

    @ModelAttribute("designVersion")
    public String designVersion() {
        return StringUtils.isNotBlank(designVersion)? designVersion : LATEST_VERSION;
    }

    @ModelAttribute("staticResourcesBaseUri")
    public String staticResourcesBaseUri() {
        return "https://static.idporten.no/" + designVersion();
    }
}
