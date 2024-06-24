package no.idporten.eidas.lightprotocol.messages;

import eu.eidas.auth.commons.light.ILevelOfAssurance;
import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.util.StringUtils;

import java.io.Serial;

@Data
@NoArgsConstructor
@XmlType(name = "levelOfAssurance")
@XmlAccessorType(XmlAccessType.FIELD)
public class LevelOfAssurance implements ILevelOfAssurance {
    @Serial
    private static final long serialVersionUID = 1L;
    public static final String NOTIFIED = "notified";
    public static final String NON_NOTIFIED = "nonNotified";
    public static final String NOTIFIED_LOA_PREFIX = "http://eidas.europa.eu/LoA/";
    @XmlAttribute
    private String type = NOTIFIED;
    @XmlValue
    private String value;

    public LevelOfAssurance(@NonNull String value, String type) {
        this.value = value;
        if (StringUtils.hasText(type)) {
            this.type = type;
        } else {
            this.type = extractType(value);
        }
    }

    private String extractType(String value) {
        if (value.startsWith(NOTIFIED_LOA_PREFIX)) {
            return NOTIFIED;
        } else {
            return NON_NOTIFIED;
        }
    }

    public LevelOfAssurance(String value) {
        this.value = value;
        this.type = extractType(value);
    }

}
