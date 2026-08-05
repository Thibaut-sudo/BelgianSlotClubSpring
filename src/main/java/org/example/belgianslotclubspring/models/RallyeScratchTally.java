package org.example.belgianslotclubspring.models;

import java.util.List;
import java.util.stream.Collectors;

/** Nombre de scratchs (ES gagnées) pour un pilote. */
public record RallyeScratchTally(
        String pilotName,
        int count,
        List<Integer> stageNumbers
) {
    public String stagesLabel() {
        if (stageNumbers == null || stageNumbers.isEmpty()) {
            return "";
        }
        return stageNumbers.stream()
                .map(n -> "ES " + n)
                .collect(Collectors.joining(", "));
    }
}
