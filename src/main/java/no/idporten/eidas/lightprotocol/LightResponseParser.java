package no.idporten.eidas.lightprotocol;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.helpers.DefaultValidationEventHandler;
import no.idporten.eidas.lightprotocol.messages.LightResponse;

import java.io.StringReader;

public class LightResponseParser {
    public static LightResponse parseXml(String xml) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(LightResponse.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());
        return (LightResponse) unmarshaller.unmarshal(new StringReader(xml));
    }

    private LightResponseParser() {
    }
}
