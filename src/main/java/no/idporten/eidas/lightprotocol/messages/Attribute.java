package no.idporten.eidas.lightprotocol.messages;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
public class Attribute implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @XmlElement(name = "definition")
    private String definition;
    @XmlElement(name = "value")
    private List<String> value;
}
