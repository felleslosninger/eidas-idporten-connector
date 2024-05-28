package no.idporten.eidas.connector.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionController {

    public static final String BAD_REQUEST_ERROR = "invalid_request";

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex) {
        if (log.isDebugEnabled()) {
            log.debug("Binding Exception caught, creating error response", ex);
        }
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(getErrorResponseForFieldError(ex.getBindingResult()));
    }

    private ErrorResponse getErrorResponseForFieldError(BindingResult bindingResult) {
        final FieldError fieldError = bindingResult.getFieldError();

        return ErrorResponse.builder()
                .error(fieldError != null ? fieldError.getDefaultMessage() : BAD_REQUEST_ERROR)
                .errorDescription(fieldError != null ? String.format("Invalid %s.", getJsonFieldName(bindingResult)) : "unknown field or cause")
                .build();
    }

    private String getJsonFieldName(BindingResult bindingResult) {
        if (bindingResult.getFieldError() != null) {
            try {
                final String field = bindingResult.getFieldError().getField();
                return bindingResult.getTarget().getClass().getDeclaredField(field).getAnnotation(JsonProperty.class).value();
            } catch (Exception e) {
                return bindingResult.getFieldError().getField();
            }
        }
        return null;
    }

}
