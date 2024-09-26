package no.idporten.eidas.connector.service;

import lombok.RequiredArgsConstructor;
import no.idporten.eidas.connector.config.AcrProperties;
import no.idporten.eidas.lightprotocol.messages.LevelOfAssurance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                .orElseThrow(() -> new IllegalArgumentException("No mapping found for eidas acr %s".formatted(eidasAcr)));
    }

    public LevelOfAssurance idportenAcrToEidasAcr(String idportenAcrLevel) {
        return new LevelOfAssurance(Optional.of(acrProperties.getAcrValueMap()
                        .get(idportenAcrLevel))
                .orElseThrow(() -> new IllegalArgumentException("No mapping found for idporten acr %s".formatted(idportenAcrLevel))));
    }

    public List<LevelOfAssurance> idportenAcrListToEidasAcr(List<String> idportenAcrLevel) {
        return idportenAcrLevel.stream()
                .map(this::idportenAcrToEidasAcr)
                .toList();
    }

    public LevelOfAssurance getLowestSupportedAcrValueInEidasFormat(List<String> idportenAcrLevel) {
        if (idportenAcrLevel.isEmpty()) {
            return new LevelOfAssurance(acrProperties.getSupportedAcrValues().getFirst());
        }
        //must come in order
        for (String idportenAcr : idportenAcrLevel) {
            LevelOfAssurance eidasAcr = idportenAcrToEidasAcr(idportenAcr);
            if (acrProperties.getSupportedAcrValues().contains(eidasAcr.getValue())) {
                return eidasAcr;
            }
        }
        throw new IllegalArgumentException("Acr levels %s are supported".formatted(idportenAcrLevel));
    }


}