package no.idporten.eidas.connector.integration.freggateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.matching.domain.UserMatchFound;
import no.idporten.eidas.connector.matching.domain.UserMatchNotFound;
import no.idporten.eidas.connector.matching.domain.UserMatchResponse;
import no.idporten.eidas.connector.matching.service.MatchingService;
import no.idporten.eidas.connector.service.CountryCodeConverter;
import no.idporten.eidas.connector.service.EIDASIdentifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
public class FregGwMatchingServiceClient implements MatchingService {

    private final RestClient.Builder fregGatewayEndpointBuilder;
    private final CountryCodeConverter countryCodeConverter;

    @Override
    public UserMatchResponse match(EidasUser eidasUser, Set<String> requestedScopes) {
        if (!CollectionUtils.isEmpty(requestedScopes)) {
            log.warn("Optional scopes not supported for freg-gw matching service. Ignoring scopes: {}", requestedScopes);
        }
        return match(eidasUser);
    }

    private UserMatchResponse match(EidasUser eidasUser) {
        Optional<String> matchingUserId = fregGatewayEndpointBuilder.build().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/eidas/entydig")
                        .queryParam("utenlandskPersonIdentifikasjon", eidasUser.eidasIdentifier().getForeignIdentifier())
                        .queryParam("foedselsdato", eidasUser.birthdate().replace("-", ""))
                        .queryParam("landkode", getCountryCode(eidasUser.eidasIdentifier()))
                        .build())
                .exchange((request, response) -> handleResponse(response));
        if (matchingUserId.isPresent()) return new UserMatchFound(eidasUser, matchingUserId.get());
        else return new UserMatchNotFound(eidasUser, "No match found for user");
    }


    private String getCountryCode(EIDASIdentifier eidasIdentifier) {
        return countryCodeConverter.getISOAlpha3CountryCode(eidasIdentifier.getSubjectCountryCode());
    }

    private Optional<String> handleResponse(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
        HttpStatusCode statusCode = response.getStatusCode();
        Optional<String> responseBody = getResponseString(response);

        if (statusCode.isSameCodeAs(HttpStatus.NO_CONTENT) || statusCode.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return Optional.empty();
        }

        if (statusCode.isSameCodeAs(HttpStatus.OK)) {
            return responseBody;
        }

        if (statusCode.isSameCodeAs(HttpStatus.BAD_REQUEST)) {
            return handleBadRequest(responseBody);
        }

        throw new HttpClientErrorException(statusCode, responseBody.orElse("Internal error"));
    }

    private Optional<String> handleBadRequest(Optional<String> responseBody) {
        String message = responseBody.orElse("N/A");
        log.warn("Bad request from freg-gw with message: {}", message);

        boolean isKnownError = responseBody
                .map(msg -> msg.contains("FREG-001") ||
                        msg.contains("UtenlandskPersonidentifikasjon er ikke på gyldig format"))
                .orElse(false);

        if (isKnownError) {
            return Optional.empty();
        }

        throw new HttpClientErrorException(
                HttpStatus.BAD_REQUEST,
                "Invalid request %s".formatted(message)
        );
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
