/*
 * Copyright (c) 2020 by European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/page/eupl-text-11-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.idporten.eidas.lightprotocol;

import eu.eidas.auth.commons.EIDASStatusCode;
import eu.eidas.auth.commons.EIDASSubStatusCode;
import eu.eidas.auth.commons.light.ILightResponse;
import eu.eidas.auth.commons.light.IResponseStatus;
import eu.eidas.auth.commons.protocol.impl.SamlNameIdFormat;
import no.idporten.eidas.connector.exceptions.SpecificConnectorException;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Validates the Light Response incoming from Specific modules.
 *
 * @since 2.3
 */
public class IncomingLightResponseValidator {

    public static final String INVALID_REQUEST = "invalid_request";

    private IncomingLightResponseValidator() {
    }


    public static boolean validate(ILightResponse lightResponse) {
        if (lightResponse == null) {
            throw new SpecificConnectorException(INVALID_REQUEST, "LightResponse is null");
        }
        try {
            validateResponseStatus(lightResponse.getStatus());
            validateNameIDFormat(lightResponse.getSubjectNameIdFormat());

        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Validate the response status.
     *
     * @param responseStatus the response status
     * @throws SpecificConnectorException if the response status is not valid.
     */
    private static void validateResponseStatus(IResponseStatus responseStatus) throws SpecificConnectorException {
        if (responseStatus == null) {
            throw new SpecificConnectorException(INVALID_REQUEST, "ResponseStatus cannot be null");
        }
        validateStatusCodeValue(responseStatus.getStatusCode());
        validateSubStatusCodeValue(responseStatus.getSubStatusCode());
    }

    protected static void validateStatusCodeValue(String statusCode) throws SpecificConnectorException {
        if (statusCode == null) {
            return;
        }
        boolean isValid = Arrays.stream(EIDASStatusCode.values())
                .map(EIDASStatusCode::getValue)
                .anyMatch(Predicate.isEqual(statusCode));
        if (!isValid) {
            throw new SpecificConnectorException(INVALID_REQUEST, "StatusCode : %s is invalid".formatted(statusCode));
        }

    }

    protected static void validateSubStatusCodeValue(String subStatusCode) throws SpecificConnectorException {
        if (subStatusCode == null) {
            return;
        }
        boolean isValid = Arrays.stream(EIDASSubStatusCode.values())
                .map(EIDASSubStatusCode::getValue)
                .anyMatch(Predicate.isEqual(subStatusCode));
        if (!isValid) {
            throw new SpecificConnectorException(INVALID_REQUEST, "SubStatusCode : %s is invalid".formatted(subStatusCode));
        }
    }


    protected static void validateNameIDFormat(String nameIDFormat) throws SpecificConnectorException {
        if (nameIDFormat == null) {
            return;
        }
        boolean isValid = Arrays.stream(SamlNameIdFormat.values())
                .map(SamlNameIdFormat::getNameIdFormat)
                .anyMatch(Predicate.isEqual(nameIDFormat));
        if (!isValid) {
            throw new SpecificConnectorException(INVALID_REQUEST, "NameID Format : %s is invalid".formatted(nameIDFormat));
        }
    }
}
