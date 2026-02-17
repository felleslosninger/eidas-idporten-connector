package no.idporten.eidas.lightprotocol;

import eu.eidas.auth.commons.EidasParameterKeys;
import eu.eidas.auth.commons.light.ILightRequest;
import eu.eidas.auth.commons.light.impl.LightToken;
import eu.eidas.auth.commons.tx.BinaryLightToken;
import jakarta.servlet.http.HttpServletRequest;
import no.idporten.eidas.connector.integration.specificcommunication.config.EidasCacheProperties;
import no.idporten.eidas.connector.integration.specificcommunication.service.LightRedisCache;
import no.idporten.eidas.connector.integration.specificcommunication.service.SpecificCommunicationServiceImpl;
import no.idporten.eidas.lightprotocol.messages.LightResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpecificCommunicationServiceImplTest {

    @Mock
    private LightRedisCache<String, String> lightRedisCache;
    @Mock
    private EidasCacheProperties eidasCacheProperties;
    @Mock
    private ILightRequest lightRequest;

    @InjectMocks
    private SpecificCommunicationServiceImpl service;

    private static MockedStatic<LightRequestToXML> lightRequestToXMLMock;
    private static MockedStatic<LightResponseParser> lightResponseParserMock;

    private static MockedStatic<BinaryLightTokenHelper> binaryLightTokenHelperMock;

    private final static String lightTokenId = "mockedLightTokenId";

    @BeforeEach
    void setUp() {
        lightRequestToXMLMock = Mockito.mockStatic(LightRequestToXML.class);
        lightResponseParserMock = Mockito.mockStatic(LightResponseParser.class);
        binaryLightTokenHelperMock = Mockito.mockStatic(BinaryLightTokenHelper.class);
        String tokenBase64 = "mockedTokenBase64";
        BinaryLightToken binaryLightToken = mock(BinaryLightToken.class);
        lenient().when(binaryLightToken.getToken()).thenReturn(mock(LightToken.class));
        lenient().when(binaryLightToken.getToken().getId()).thenReturn(lightTokenId);
        binaryLightTokenHelperMock.when(() -> BinaryLightTokenHelper.getBinaryToken(any(HttpServletRequest.class), eq(EidasParameterKeys.TOKEN.toString()))).thenReturn(tokenBase64);
        binaryLightTokenHelperMock.when(() -> BinaryLightTokenHelper.getBinaryLightTokenId(eq(tokenBase64), any(), any())).thenReturn(lightTokenId);
        binaryLightTokenHelperMock.when(() -> BinaryLightTokenHelper.createBinaryLightToken(any(), any(), any())).thenReturn(binaryLightToken);

        lightResponseParserMock.when(() -> LightResponseParser.parseXml(anyString())).thenReturn(mock(LightResponse.class));
    }

    @Test
    @DisplayName("Test put LightRequest")
    void testPutRequest() throws Exception {
        lightRequestToXMLMock.when(() -> LightRequestToXML.toXml(any(ILightRequest.class))).thenReturn("<xml>someXml</xml>");
        service.putRequest(lightRequest);
        verify(lightRedisCache).set(any(), eq("<xml>someXml</xml>"), any());
    }

    @Test
    @DisplayName("Test getAndRemove LightResponse")
    void testGetAndRemoveResponse() throws Exception {
        String cacheKey = "response_prefix_token";
        when(eidasCacheProperties.getLightResponsePrefix("token")).thenReturn(cacheKey);
        when(lightRedisCache.get(cacheKey)).thenReturn("<xml>responseXml</xml>");
        service.getAndRemoveResponse("token", mock(Collection.class));
        verify(lightRedisCache).get(cacheKey);
    }

    @AfterEach
    void tearDown() {
        // Ensure that the static mocks are closed after each test
        lightRequestToXMLMock.close();
        binaryLightTokenHelperMock.close();
        lightResponseParserMock.close();
    }
}
