package org.example.belgianslotclubspring.models;

/**
 * Une ligne de classement rallye (après N spéciales).
 */
public record RallyeStandingRow(
        int position,
        Long pilotId,
        String name,
        String car,
        String category,
        Double totalSeconds,
        Double gapToLeader,
        String totalFormatted,
        String gapFormatted,
        int stagesCompleted,
        int stagesExpected
) {
}
