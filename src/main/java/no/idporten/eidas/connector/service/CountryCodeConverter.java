package no.idporten.eidas.connector.service;

import com.ibm.icu.util.ULocale;
import org.apache.commons.lang.StringUtils;

public class CountryCodeConverter {

    //we fake the country code for the demo land to be sweden for now
    public static final String DEMOLAND = "CA";

    public static String getISOAlpha3CountryCode(String countryCode) {
        if (DEMOLAND.equals(countryCode)) {
            return "SWE";
        }
        if (StringUtils.isNotEmpty(countryCode) && countryCode.length() == 2) {
            ULocale locale = new ULocale("und-" + countryCode);
            return locale.getISO3Country();
        }
        throw new IllegalArgumentException("Invalid eidas identifier");
    }
}
