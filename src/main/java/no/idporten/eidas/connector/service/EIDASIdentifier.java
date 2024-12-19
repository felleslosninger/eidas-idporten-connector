package no.idporten.eidas.connector.service;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Data
public class EIDASIdentifier {
    private static final String EIDAS_USERIDENTIFIER_FORMAT_REGEX = "^[A-Z]{2}/[A-Z]{2}/[\\p{L}0-9-=]+$";
    private static final Pattern PATTERN = Pattern.compile(EIDAS_USERIDENTIFIER_FORMAT_REGEX);
    private String foreignIdentifier;
    private String subjectCountryCode;
    private String spCountryCode;

    /**
     * @param value format CC/CC/ID
     */
    public EIDASIdentifier(String value) {
        if (isValid(value)) {
            this.subjectCountryCode = value.substring(0, 2);
            this.spCountryCode = value.substring(3, 5);
            this.foreignIdentifier = value.substring(6);

        } else {
            throw new IllegalArgumentException("Invalid eIDAS identifier");
        }
    }

    public static boolean isValid(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return PATTERN.matcher(value).matches();
    }
}

