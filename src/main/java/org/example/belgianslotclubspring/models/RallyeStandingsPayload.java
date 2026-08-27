package org.example.belgianslotclubspring.models;

import java.util.List;
import java.util.Objects;

/**
 * Classement rallye pour l’affichage live (TV / public).
 */
public record RallyeStandingsPayload(
        long fingerprint,
        String rallyeName,
        boolean finished,
        int afterStages,
        int totalStages,
        List<Integer> checkpoints,
        List<String> categories,
        String category,
        List<RallyeStandingRow> rows
) {
    public static long fingerprintOf(List<RallyeStandingRow> rows) {
        long h = 1L;
        for (RallyeStandingRow row : rows) {
            h = 31L * h + Objects.hash(
                    row.pilotId(),
                    row.position(),
                    row.totalSeconds(),
                    row.stagesCompleted(),
                    row.category(),
                    row.name()
            );
        }
        return h;
    }
}
