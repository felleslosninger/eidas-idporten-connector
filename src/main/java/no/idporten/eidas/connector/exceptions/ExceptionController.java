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

import java.lang.reflect.Field;
import java.util.Optional;

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
        return Optional.ofNullable(bindingResult.getFieldError())
                .map(fieldError -> Optional.ofNullable(bindingResult.getTarget())
                        .map(target -> {
                            try {
                                Field field = target.getClass().getDeclaredField(fieldError.getField());
                                return Optional.ofNullable(field.getAnnotation(JsonProperty.class))
                                        .map(JsonProperty::value)
                                        .orElse(fieldError.getField());
                            } catch (NoSuchFieldException | SecurityException e) {
                                return fieldError.getField();
                            }
                        })
                        .orElse(fieldError.getField()))
                .orElse(null);
    }


}
