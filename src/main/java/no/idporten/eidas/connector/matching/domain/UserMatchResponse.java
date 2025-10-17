package no.idporten.eidas.connector.matching.domain;

public sealed interface UserMatchResponse permits UserMatchFound, UserMatchNotFound, UserMatchRedirect, UserMatchError {
}
