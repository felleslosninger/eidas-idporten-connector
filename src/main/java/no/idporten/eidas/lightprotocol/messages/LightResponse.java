package no.idporten.eidas.lightprotocol.messages;

import eu.eidas.auth.commons.attribute.ImmutableAttributeMap;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.light.IResponseStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.*;
import lombok.*;

import java.io.Serial;
import java.util.List;

@XmlRootElement(namespace = "http://cef.eidas.eu/LightResponse")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
@ToString(exclude = "attributes")
@XmlAccessorType(XmlAccessType.FIELD)
public class LightResponse implements ILightResponse {
    @Serial
    private static final long serialVersionUID = 1L;
    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String citizenCountryCode;
    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String id;
    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String issuer;
    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String levelOfAssurance;
    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String relayState;

    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String ipAddress;

    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String inResponseToId;

    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String consent;

    private String subject;

    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String subjectNameIdFormat;

    @XmlElement(name = "status", namespace = "http://cef.eidas.eu/LightResponse")
    private Status status;

    @XmlElementWrapper(name = "attributes", namespace = "http://cef.eidas.eu/LightResponse")
    @XmlElement(name = "attribute", namespace = "http://cef.eidas.eu/LightResponse")
    @Singular
    private List<Attribute> attributes;

    public List<Attribute> getAttributesList() {
        return attributes;
    }

    @Nonnull
    @Override
    public ImmutableAttributeMap getAttributes() {
        return ImmutableAttributeMap.builder().build();
    }

    @Nullable
    @Override
    public String getIPAddress() {
        return ipAddress;
    }

    @Nonnull
    @Override
    public IResponseStatus getStatus() {
        return status;
    }

    public List<LevelOfAssurance> getLevelsOfAssurance() {
        return List.of(new LevelOfAssurance("notified", levelOfAssurance));
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getIssuer() {
        return issuer;
    }


}

