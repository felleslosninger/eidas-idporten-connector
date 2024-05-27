package no.idporten.eidas.lightprotocol.messages;

import eu.eidas.auth.commons.attribute.ImmutableAttributeMap;
import eu.eidas.auth.commons.light.ILevelOfAssurance;
import eu.eidas.auth.commons.light.ILightRequest;
import jakarta.annotation.Nonnull;
import jakarta.xml.bind.annotation.*;
import lombok.*;

import java.io.Serial;
import java.util.List;

@XmlRootElement(name = "lightRequest", namespace = "http://cef.eidas.eu/LightRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "requestedAttributes")
@EqualsAndHashCode
@Builder
@XmlType
public class LightRequest implements ILightRequest {
    @Serial
    private static final long serialVersionUID = 1L;
    private String citizenCountryCode;
    private String id;
    private String issuer;
    private LevelOfAssurance levelOfAssurance;
    private String relayState;
    private String providerName;
    private String spType;
    private String nameIdFormat;
    private String requesterId;
    private String spCountryCode;
    @XmlElementWrapper(name = "requestedAttributes")
    @XmlElement(name = "attribute")
    private List<RequestedAttribute> requestedAttributes;

    @Nonnull
    public ImmutableAttributeMap getRequestedAttributes() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public List<RequestedAttribute> getRequestedAttributesList() {
        return requestedAttributes;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Override
    public String getLevelOfAssurance() {
        return levelOfAssurance.getValue();
    }

    @Override
    public List<ILevelOfAssurance> getLevelsOfAssurance() {
        return List.of(levelOfAssurance);
    }

    @Nonnull
    public String getIssuer() {
        return issuer;
    }


}

