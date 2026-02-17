package no.idporten.eidas.connector.web;

import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for converting Spring's HttpHeaders and MultiValueMap to Map<String, List<String>>
 * for compatibility with no.idporten.sdk.oidcserver protocol classes.
 */
public class HttpHeadersConverter {

    private HttpHeadersConverter() {
        // Utility class
    }

    /**
     * Converts Spring's HttpHeaders to Map<String, List<String>>.
     *
     * @param headers the HttpHeaders to convert
     * @return a Map representation of the headers
     */
    public static Map<String, List<String>> toMap(HttpHeaders headers) {
        if (headers == null) {
            return new HashMap<>();
        }
        Map<String, List<String>> result = new HashMap<>();
        headers.forEach(result::put);
        return result;
    }

}
