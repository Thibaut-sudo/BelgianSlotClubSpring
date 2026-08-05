package org.example.belgianslotclubspring.models;

/** Scratch / plus lent pour une ES d'une boucle. */
public record RallyeStageHighlight(
        int esNumber,
        String scratchPilotName,
        String scratchTime,
        String worstPilotName,
        String worstTime,
        String worstGapToScratch
) {
}
