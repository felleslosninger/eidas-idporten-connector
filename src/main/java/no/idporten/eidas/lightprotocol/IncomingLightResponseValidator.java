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

    private IncomingLightResponseValidator() {
    }


    public static boolean validate(ILightResponse lightResponse) {
        if (null == lightResponse) {
            throw new SpecificConnectorException("invalid_request", "LightResponse is null");
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
        if (null == responseStatus) {
            throw new SpecificConnectorException("invalid_request", "ResponseStatus cannot be null");
        }
        validateStatusCodeValue(responseStatus.getStatusCode());
        validateSubStatusCodeValue(responseStatus.getSubStatusCode());
    }

    private static void validateStatusCodeValue(String statusCode) throws SpecificConnectorException {
        if (null != statusCode) {
            Arrays.stream(EIDASStatusCode.values())
                    .map(EIDASStatusCode::getValue)
                    .filter(Predicate.isEqual(statusCode))
                    .findAny()
                    .orElseThrow(() -> new SpecificConnectorException("invalid_request", "StatusCode : " + statusCode + " is invalid"));
        }
    }

    private static void validateSubStatusCodeValue(String subStatusCode) throws SpecificConnectorException {
        if (null != subStatusCode) {
            Arrays.stream(EIDASSubStatusCode.values())
                    .map(EIDASSubStatusCode::getValue)
                    .filter(Predicate.isEqual(subStatusCode))
                    .findAny()
                    .orElseThrow(() -> new SpecificConnectorException("invalid_request", "SubStatusCode : " + subStatusCode + " is invalid"));
        }
    }

    private static void validateNameIDFormat(String nameIDFormat) throws SpecificConnectorException {
        if (null != nameIDFormat) {
            Arrays.stream(SamlNameIdFormat.values())
                    .map(SamlNameIdFormat::getNameIdFormat)
                    .filter(Predicate.isEqual(nameIDFormat))
                    .findAny()
                    .orElseThrow(() -> new SpecificConnectorException("invalid_request", "NameID Format : " + nameIDFormat + " is invalid"));
        }
    }

}
