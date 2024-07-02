package no.idporten.eidas.lightprotocol;

import eu.eidas.auth.commons.EIDASStatusCode;
import eu.eidas.auth.commons.EIDASSubStatusCode;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.light.IResponseStatus;
import eu.eidas.auth.commons.protocol.impl.SamlNameIdFormat;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncomingLightResponseValidatorTest {

    @Test
    @DisplayName("Validate null light response throws SpecificConnectorException")
    void testValidateNullLightResponse() {
        Exception exception = assertThrows(SpecificConnectorException.class, () ->
                IncomingLightResponseValidator.validate(null)
        );
        assertEquals("LightResponse is null", exception.getMessage());
    }

    @Test
    @DisplayName("Validate light response with null status returns false")
    void testValidateLightResponseWithNullStatus() {
        ILightResponse lightResponse = mock(ILightResponse.class);
        when(lightResponse.getStatus()).thenReturn(null);
        assertFalse(IncomingLightResponseValidator.validate(lightResponse));
    }

    @Test
    @DisplayName("Validate light response with valid status and name ID format")
    void testValidateValidLightResponse() {
        IResponseStatus status = mock(IResponseStatus.class);
        ILightResponse lightResponse = mock(ILightResponse.class);
        when(lightResponse.getStatus()).thenReturn(status);
        when(status.getStatusCode()).thenReturn(EIDASStatusCode.REQUESTER_URI.getValue());
        when(status.getSubStatusCode()).thenReturn(EIDASSubStatusCode.AUTHN_FAILED_URI.getValue());
        when(lightResponse.getSubjectNameIdFormat()).thenReturn(SamlNameIdFormat.UNSPECIFIED.getNameIdFormat());

        assertTrue(IncomingLightResponseValidator.validate(lightResponse));
    }

    @Test
    @DisplayName("Validate light response with invalid status code throws SpecificConnectorException")
    void testValidateInvalidStatusCode() {
        IResponseStatus status = mock(IResponseStatus.class);
        when(status.getStatusCode()).thenReturn("invalidCode");

        SpecificConnectorException exception = assertThrows(SpecificConnectorException.class, () ->
                IncomingLightResponseValidator.validateStatusCodeValue(status.getStatusCode())
        );
        assertTrue(exception.getMessage().contains("StatusCode : invalidCode is invalid"));
    }

    @Test
    @DisplayName("Validate light response with invalid sub-status code throws SpecificConnectorException")
    void testValidateInvalidSubStatusCode() {
        IResponseStatus status = mock(IResponseStatus.class);
        when(status.getSubStatusCode()).thenReturn("invalidSubCode");

        Exception exception = assertThrows(SpecificConnectorException.class, () ->
                IncomingLightResponseValidator.validateSubStatusCodeValue(status.getSubStatusCode())
        );
        assertTrue(exception.getMessage().contains("SubStatusCode : invalidSubCode is invalid"));
    }

    @Test
    @DisplayName("Validate light response with invalid name ID format throws SpecificConnectorException")
    void testValidateInvalidNameIdFormat() {
        ILightResponse lightResponse = mock(ILightResponse.class);
        when(lightResponse.getSubjectNameIdFormat()).thenReturn("invalidFormat");

        Exception exception = assertThrows(SpecificConnectorException.class, () ->
                IncomingLightResponseValidator.validateNameIDFormat(lightResponse.getSubjectNameIdFormat())
        );
        assertTrue(exception.getMessage().contains("NameID Format : invalidFormat is invalid"));
    }
}
