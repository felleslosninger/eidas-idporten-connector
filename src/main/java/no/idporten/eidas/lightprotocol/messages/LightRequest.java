package no.idporten.eidas.lightprotocol.messages;

import eu.eidas.auth.commons.attribute.ImmutableAttributeMap;
import eu.eidas.auth.commons.light.ILevelOfAssurance;
import eu.eidas.auth.commons.light.ILightRequest;
import jakarta.annotation.Nonnull;
import jakarta.xml.bind.annotation.*;
import lombok.*;
import no.idporten.sdk.oidcserver.protocol.AuditData;
import no.idporten.sdk.oidcserver.protocol.AuditDataProvider;

import java.io.Serial;
import java.util.ArrayList;
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
public class LightRequest implements ILightRequest, AuditDataProvider {
    @Serial
    private static final long serialVersionUID = 1L;
    private String citizenCountryCode;
    private String id;
    private String issuer;
    private List<LevelOfAssurance> levelOfAssurance;
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
        return levelOfAssurance.getFirst().getValue();
    }

    @Override
    public List<ILevelOfAssurance> getLevelsOfAssurance() {
        return new ArrayList<>(levelOfAssurance);
    }

    @Nonnull
    public String getIssuer() {
        return issuer;
    }


    @Override
    public AuditData getAuditData() {
        return AuditData.builder()
                .attribute("id", id)
                .attribute("relay_state", relayState)
                .attribute("citizen_country_code", citizenCountryCode)
                .attribute("level_of_assurance", levelOfAssurance)
                .attribute("sp_country_code", spCountryCode)
                .attribute("requested_attributes", requestedAttributes)
                .build();
    }
}

