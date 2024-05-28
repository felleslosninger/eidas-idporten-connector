package no.idporten.eidas.connector.service;

import eu.eidas.auth.commons.light.ILevelOfAssurance;
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

    protected List<String> eidasAcrListToIdportenAcrList(List<ILevelOfAssurance> acrLevels) {
        Map<String, String> acrValueMap = acrProperties.getAcrValueMap();
        return acrLevels.stream()
                .map(a -> {
                    String eidasAcr = a.getValue();
                    return acrValueMap.entrySet().stream()
                            .filter(entry -> entry.getValue().equals(eidasAcr))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse("idporten-loa-low");
                })
                .toList();
    }

    public LevelOfAssurance idportenAcrToEidasAcr(String idportenAcrLevel) {
        return LevelOfAssurance.fromString(acrProperties.getAcrValueMap().get(idportenAcrLevel));
    }

    public List<LevelOfAssurance> idportenAcrListToEidasAcr(List<String> idportenAcrLevel) {
        return idportenAcrLevel.stream()
                .map(acr -> LevelOfAssurance.fromString(acrProperties.getAcrValueMap().get(acr)))
                .toList();
    }
}