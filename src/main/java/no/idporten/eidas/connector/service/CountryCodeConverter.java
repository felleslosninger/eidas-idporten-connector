package no.idporten.eidas.connector.service;

import com.ibm.icu.util.ULocale;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CountryCodeConverter {

    private final Optional<Map<String, String>> demoCountryCodeMap;

    public String getISOAlpha3CountryCode(String countryCode) {
        if (demoCountryCodeMap.isPresent() && demoCountryCodeMap.get().containsKey(countryCode)) {
            return demoCountryCodeMap.get().get(countryCode);
        }

        if (StringUtils.isNotEmpty(countryCode) && countryCode.length() == 2) {
            ULocale locale = new ULocale("und-" + countryCode);
            return locale.getISO3Country();
        }
        throw new IllegalArgumentException("Invalid eidas identifier");
    }
}
