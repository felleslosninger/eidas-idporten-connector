package no.idporten.eidas.lightprotocol.messages;

import eu.eidas.auth.commons.light.IResponseStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.xml.bind.annotation.*;
import lombok.*;

import java.io.Serial;

@XmlRootElement(namespace = "http://cef.eidas.eu/LightResponse")
@XmlType
@XmlAccessorType(XmlAccessType.FIELD)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
public class Status implements IResponseStatus {
    @Serial
    private static final long serialVersionUID = 1L;
    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String statusCode;
    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String statusMessage;
    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private String subStatusCode;
    @XmlElement(namespace = "http://cef.eidas.eu/LightResponse")
    private boolean failure;

    @Nonnull
    @Override
    public String getStatusCode() {
        return statusCode;
    }

    @Nullable
    @Override
    public String getStatusMessage() {
        return statusMessage;
    }

    @Nullable
    @Override
    public String getSubStatusCode() {
        return subStatusCode;
    }

    @Override
    public boolean isFailure() {
        return failure;
    }
}
