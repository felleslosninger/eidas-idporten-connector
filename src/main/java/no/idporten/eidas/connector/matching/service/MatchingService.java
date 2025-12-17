package no.idporten.eidas.connector.matching.service;

import no.idporten.eidas.connector.domain.EidasUser;
import no.idporten.eidas.connector.matching.domain.UserMatchResponse;

import java.util.Set;

/**
 * Interface for matching services.
 * To support configuration of different matching services.
 *
 */
@FunctionalInterface
public interface MatchingService {
    /**
     * Attempts to match an EIDAS identifier to a national identifier using a birthdate hint.
     *
     * @param eidasUser the user returned from EIDAS
     * @param requestedScopes  scopes requested by the client. Must be whitelisted in optional scopes config
     * @return UserMatchResponse containing national identifier if match found, otherwise empty
     */
    UserMatchResponse match(EidasUser eidasUser, Set<String> requestedScopes);


}