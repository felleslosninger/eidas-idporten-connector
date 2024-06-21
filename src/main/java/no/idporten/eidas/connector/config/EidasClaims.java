package no.idporten.eidas.connector.config;

import java.util.Map;

public class EidasClaims {

    public static final String EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER = "http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier";
    public static final String EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_FAMILY_NAME = "http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName";
    public static final String EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_GIVEN_NAME = "http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName";
    public static final String EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH = "http://eidas.europa.eu/attributes/naturalperson/DateOfBirth";

    public static final String IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM = "eidas_identifier";
    public static final String IDPORTEN_EIDAS_FAMILY_NAME_CLAIM = "eidas_lastname";
    public static final String IDPORTEN_EIDAS_GIVEN_NAME_CLAIM = "eidas_firstname";
    public static final String IDPORTEN_EIDAS_DATE_OF_BIRTH_CLAIM = "eidas_date_of_birth";

    public static final Map<String, String> EIDAS_EUROPA_EU_ATTRIBUTES = Map.of(
            EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_PERSON_IDENTIFIER, IDPORTEN_EIDAS_PERSON_IDENTIFIER_CLAIM,
            EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_FAMILY_NAME, IDPORTEN_EIDAS_FAMILY_NAME_CLAIM,
            EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_GIVEN_NAME, IDPORTEN_EIDAS_GIVEN_NAME_CLAIM,
            EIDAS_EUROPA_EU_ATTRIBUTES_NATURALPERSON_DATE_OF_BIRTH, IDPORTEN_EIDAS_DATE_OF_BIRTH_CLAIM
    );

    private EidasClaims() {
    }
}
