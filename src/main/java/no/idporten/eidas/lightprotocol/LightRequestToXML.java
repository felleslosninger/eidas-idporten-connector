package no.idporten.eidas.lightprotocol;


import eu.eidas.auth.commons.light.ILightRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.helpers.DefaultValidationEventHandler;
import no.idporten.eidas.lightprotocol.messages.LightRequest;

import java.io.StringWriter;

public class LightRequestToXML {
    public static String toXml(ILightRequest lightRequest) throws JAXBException {
        if (lightRequest == null) {
            throw new IllegalArgumentException("The lightRequest object cannot be null.");
        }
        JAXBContext context = JAXBContext.newInstance(LightRequest.class);

        Marshaller marshaller = context.createMarshaller();
        marshaller.setEventHandler(new DefaultValidationEventHandler());
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(lightRequest, writer);
        return writer.toString();
    }

    private LightRequestToXML() {
    }

}
