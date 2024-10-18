package no.idporten.eidas.connector.integration.freggateway.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eidas.connector.service.CountryCodeConverter;
import no.idporten.eidas.connector.service.EIDASIdentifier;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@RequiredArgsConstructor
public class MatchingServiceClient {

    private final RestClient.Builder fregGatewayEndpointBuilder;
    private final CountryCodeConverter countryCodeConverter;

    public Optional<String> match(EIDASIdentifier eidasIdentifier, String birthdate) {
        String isoAlpha3CountryCode = countryCodeConverter.getISOAlpha3CountryCode(eidasIdentifier.getSubjectCountryCode());
        return fregGatewayEndpointBuilder.build().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/eidas/entydig")
                        .queryParam("utenlandskPersonIdentifikasjon", eidasIdentifier.getForeignIdentifier())
                        .queryParam("foedselsdato", birthdate.replace("-", ""))
                        .queryParam("landkode", isoAlpha3CountryCode)
                        .build())
                .exchange((request, response) -> {
                    if (HttpStatus.NO_CONTENT == response.getStatusCode() || HttpStatus.NOT_FOUND == response.getStatusCode()) {
                        return Optional.empty();
                    } else if (HttpStatus.OK == response.getStatusCode()) {
                        return getResponseString(response);
                    } else if (HttpStatus.BAD_REQUEST == response.getStatusCode()) {
                        Optional<String> message = getResponseString(response);
                        return message
                                .filter(msg -> msg.contains("FREG-001") || msg.contains("UtenlandskPersonidentifikasjon er ikke på gyldig format"))
                                .map(msg -> Optional.<String>empty())
                                .orElseThrow(() -> new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid request %s".formatted(message.orElse("N/A"))));
                    } else {
                        throw new HttpClientErrorException(response.getStatusCode(), getResponseString(response).orElse("Internal error"));
                    }
                });
    }

    private static Optional<String> getResponseString(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
        String responseBody = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        if (StringUtils.hasText(responseBody)) {
            return Optional.of(responseBody);
        } else {
            return Optional.empty();
        }
    }
}
