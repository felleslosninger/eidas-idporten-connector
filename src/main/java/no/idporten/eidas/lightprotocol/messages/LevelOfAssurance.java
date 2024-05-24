package no.idporten.eidas.lightprotocol.messages;

import eu.eidas.auth.commons.light.ILevelOfAssurance;
import jakarta.xml.bind.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlType(name = "levelOfAssurance")
@XmlAccessorType(XmlAccessType.FIELD)
public class LevelOfAssurance implements ILevelOfAssurance {
    @Serial
    private static final long serialVersionUID = 1L;
    @XmlAttribute
    private String type = "notified";
    @XmlValue
    private String value;

    public static LevelOfAssurance fromString(String assuranceLevelURI) {
        LevelOfAssurance loa = new LevelOfAssurance();
        loa.setValue(assuranceLevelURI);

        // Determine the type based on the URI
        if (assuranceLevelURI != null) {
            switch (assuranceLevelURI) {
                case ILevelOfAssurance.EIDAS_LOA_LOW -> loa.setType("low");
                case ILevelOfAssurance.EIDAS_LOA_SUBSTANTIAL -> loa.setType("substantial");
                case ILevelOfAssurance.EIDAS_LOA_HIGH -> loa.setType("high");
                default -> loa.setType("low");
            }
        }

        return loa;
    }
}
