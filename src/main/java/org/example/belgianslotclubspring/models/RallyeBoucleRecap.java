package org.example.belgianslotclubspring.models;

import java.util.List;

/** Compte-rendu d'une boucle (en cours ou terminée uniquement). */
public record RallyeBoucleRecap(
        int boucle,
        boolean hasData,
        boolean finished,
        int stagesWithTimes,
        String leaderName,
        String leaderTime,
        List<RallyeScratchTally> scratchLeaders,
        List<RallyeStageHighlight> stages,
        List<String> headlines
) {
    public String statusLabel() {
        return finished ? "Terminée" : "En cours";
    }
}
