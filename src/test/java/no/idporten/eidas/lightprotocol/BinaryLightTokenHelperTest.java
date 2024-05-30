
package no.idporten.eidas.lightprotocol;

import eu.eidas.auth.commons.light.impl.LightToken;
import eu.eidas.auth.commons.tx.BinaryLightToken;
import jakarta.servlet.http.HttpServletRequest;
import org.joda.time.DateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BinaryLightTokenHelperTest {

    @Test
    @DisplayName("Get the BinaryLightToken id from a BinaryLightToken Base64 encoded string")
    void getBinaryLightTokenId() {
        String token = "c3BlY2lmaWNDb21tdW5pY2F0aW9uRGVmaW5pdGlvbkNvbm5lY3RvclJlc3BvbnNlfGE4NjQ0NThjLTEzYjgtNGE1YS04ZmFmLWVmOTAyNzJhMTdiNXwyMDI0LTA1LTI5IDA3OjQxOjA2IDgyN3xEYlovWWI0WU5tc0RWN3hic3JPT2xTMTRaSFA3Ukh5YlpmNXpncTdpODZ3PQ==";
        String lightTokenId = BinaryLightTokenHelper.getBinaryLightTokenId(token, "mySecretConnectorResponse", "SHA-256");
        assertEquals("a864458c-13b8-4a5a-8faf-ef90272a17b5", lightTokenId);
    }

    @Test
    void getBinaryToken() {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getAttribute("token")).thenReturn("theToken");
        String token = BinaryLightTokenHelper.getBinaryToken(httpServletRequest, "token");
        assertEquals("theToken", token);
    }

    @Test
    void encodeBinaryLightTokenBase64() {
        LightToken lightToken = LightToken.builder().id("113df59f-3113-4a98-a839-72a4b07af79b").issuer("specificCommunicationDefinitionConnectorserviceRequest").createdOn(DateTime.now())
                .build();
        String tokenBytes = "c3BlY2lmaWNDb21tdW5pY2F0aW9uRGVmaW5pdGlvbkNvbm5lY3RvcnNlcnZpY2VSZXF1ZXN0fDExM2RmNTlmLTMxMTMtNGE5OC1hODM5LTcyYTRiMDdhZjc5YnwyMDI0LTA1LTI5IDA5OjQwOjM0IDY1M3xCWXM0Yk9zcnF3bCtQT2NlUUJHMms5eDNzZWZTVEhOKzFQbTMzamNCT2ZjPQ==";
        BinaryLightToken binaryLightToken = new BinaryLightToken(lightToken, tokenBytes.getBytes());
        String s = BinaryLightTokenHelper.encodeBinaryLightTokenBase64(binaryLightToken);
        assertNotNull(s);
        assertEquals("YzNCbFkybG1hV05EYjIxdGRXNXBZMkYwYVc5dVJHVm1hVzVwZEdsdmJrTnZibTVsWTNSdmNuTmxjblpwWTJWU1pYRjFaWE4wZkRFeE0yUm1OVGxtTFRNeE1UTXROR0U1T0MxaE9ETTVMVGN5WVRSaU1EZGhaamM1WW53eU1ESTBMVEExTFRJNUlEQTVPalF3T2pNMElEWTFNM3hDV1hNMFlrOXpjbkYzYkN0UVQyTmxVVUpITW1zNWVETnpaV1pUVkVoT0t6RlFiVE16YW1OQ1QyWmpQUT09", s);
    }

    @Test
    void createBinaryLightToken() {
        String issuerName = "specificCommunicationDefinitionConnectorserviceRequest";
        String secret = "mySecretConnectorRequest";
        String algorithm = "SHA-256";

        BinaryLightToken binaryLightToken = BinaryLightTokenHelper.createBinaryLightToken(issuerName, secret, algorithm);
        assertAll("binaryLightToken validation",
                () -> assertNotNull(binaryLightToken, "binaryLightToken should not be null"),
                () -> assertNotNull(binaryLightToken.getToken(), "Token object within binaryLightToken should not be null"),
                () -> assertNotNull(binaryLightToken.getToken().getId(), "Token ID should not be null"),
                () -> assertEquals("specificCommunicationDefinitionConnectorserviceRequest", binaryLightToken.getToken().getIssuer(),
                        "Token issuer should match the expected value"),
                () -> assertNotNull(binaryLightToken.getTokenBytes(), "Token bytes should not be null")
        );
    }


}
