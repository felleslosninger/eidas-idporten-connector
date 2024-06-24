package no.idporten.eidas.connector.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eidas.connector.config.AcrProperties;
import no.idporten.eidas.lightprotocol.messages.LevelOfAssurance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Representation of Level of Assurance
 */
@Component
@RequiredArgsConstructor
public class LevelOfAssuranceHelper {

    private final AcrProperties acrProperties;

    /**
     * @param requestedAcr the originally requested acr levels
     * @param returnedAcr  the acr in the returned token
     * @return true if the match is valid, false if the match is invalid
     */

    public boolean hasValidAcrLevel(String returnedAcr, List<String> requestedAcr) {
        if (requestedAcr == null)
            return isValidAcr(returnedAcr);
        return isValidAcr(returnedAcr) && requestedAcr.stream().anyMatch(a ->
                isEqualOrHigherThan(returnedAcr, a)
        );
    }

    private boolean isEqualOrHigherThan(String candiateAcr, String lowerLimit) {
        return acrProperties.getSupportedAcrValues().indexOf(lowerLimit) <= acrProperties.getSupportedAcrValues().indexOf(candiateAcr);
    }


    protected boolean isValidAcr(String returnedAcr) {
        return acrProperties.getSupportedAcrValues().contains(returnedAcr);
    }

    protected List<String> eidasAcrListToIdportenAcrList(List<LevelOfAssurance> acrLevels) {
        return acrLevels.stream()
                .map(a -> eidasAcrToIdportenAcr(a.getValue()))
                .toList();
    }

    public String eidasAcrToIdportenAcr(String eidasAcr) {
        Map<String, String> acrValueMap = acrProperties.getAcrValueMap();
        return acrValueMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(eidasAcr))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("eidas-loa-low");
    }

    public List<LevelOfAssurance> idportenAcrListToEidasAcr(List<String> idportenAcrLevel) {
        return idportenAcrLevel.stream()
                .map(acr -> new LevelOfAssurance(acrProperties.getAcrValueMap().get(acr)))
                .toList();
    }
}