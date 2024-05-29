package no.idporten.eidas.lightprotocol.messages;

import eu.eidas.auth.commons.attribute.ImmutableAttributeMap;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.light.IResponseStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.*;
import lombok.*;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;

import java.beans.Transient;
import java.io.Serial;
import java.util.List;
import java.util.Map;

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
    public static final String EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER = "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier";

    public static final Map<String, String> EIDAS_EUROPA_EU_ATTRIBUTES = Map.of(
            EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER, "eidas_identifier",
            "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName", "eidas_lastname",
            "http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName", "eidas_firstname",
            "http://eidas.europa.eu/attributes/naturalperson/DateOfBirth", "eidas_date_of_birth"
    );
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

    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
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

    @Transient
    public String getPid() {

        // Attempt to extract the PID based on predefined format and validation rules
        return getAttributesList().stream()
                .filter(attribute -> EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER.equals(attribute.getDefinition()))
                .map(Attribute::getValue)
                .filter(values -> !values.isEmpty())
                .map(List::getFirst)
                .findFirst()
                .orElseThrow(() -> new SpecificConnectorException("invalid_request", "Personidentifier was missing or has unsupported format in attributes list."));
    }


}

