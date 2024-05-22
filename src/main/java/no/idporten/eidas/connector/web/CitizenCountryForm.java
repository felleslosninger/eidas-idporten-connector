package no.idporten.eidas.connector.web;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@AllArgsConstructor
@NoArgsConstructor
public class CitizenCountryForm {

    @NotEmpty(message = "Select citizen country")
    private String countryId ;

}
