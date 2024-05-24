/*
 * Copyright (c) 2021 by European Commission
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

package no.idporten.eidas.connector.exceptions;

import eu.eidas.auth.commons.exceptions.AbstractEIDASException;

/**
 * This exception is thrown when a catchable exception occurred within
 * the SpecificCommunicationDefinition module.
 * <p>
 * Catchable exceptions are to be included in this exception.
 */
public class SpecificConnectorException extends AbstractEIDASException {


    public SpecificConnectorException(final String errorCode, final String errorMessage) {
        super(errorCode, errorMessage);

    }

    public SpecificConnectorException(final String errorCode, final String errorMessage, final String additionalInformation) {
        super(errorCode, errorMessage, additionalInformation);

    }

    public SpecificConnectorException(final String errorCode, final String errorMessage, final Throwable cause) {
        super(errorCode, errorMessage, cause);

    }

    public SpecificConnectorException(final String errorCode, final String errorMessage, final String additionalInformation, final Throwable cause) {
        super(errorCode, errorMessage, additionalInformation, cause);

    }

    public SpecificConnectorException(String message, Throwable cause) {
        super(message, cause.getMessage());

    }


}
