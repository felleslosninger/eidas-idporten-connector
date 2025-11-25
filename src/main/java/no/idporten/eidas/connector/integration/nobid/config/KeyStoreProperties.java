package no.idporten.eidas.connector.integration.nobid.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;

@Validated
public record KeyStoreProperties(@NotNull
                                 String location,
                                 @NotNull
                                 String type,
                                 @NotNull
                                 String password,
                                 @NotNull
                                 String keyAlias,
                                 @NotNull
                                 String keyPassword,
                                 @NotNull
                                 String kid
                                 ) implements Serializable {

}
