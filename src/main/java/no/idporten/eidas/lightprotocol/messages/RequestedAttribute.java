package no.idporten.eidas.lightprotocol.messages;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
@Builder
public class RequestedAttribute implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private Object value;

    @XmlElement(namespace = "http://cef.eidas.eu/LightRequest")
    private String definition;

}
