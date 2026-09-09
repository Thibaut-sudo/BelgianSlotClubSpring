package org.example.belgianslotclubspring.models;

import org.example.belgianslotclubspring.models.ClubRaceStats.NamedCount;
import org.example.belgianslotclubspring.models.ClubRaceStats.RecordHolder;

import java.time.LocalDate;
import java.util.List;

/**
 * Statistiques d'un pilote dans un club (toutes saisons).
 */
public record PilotClubStats(
        String name,
        int starts,
        int wins,
        int podiums,
        int poles,
        String avgPosition,
        String winRate,
        RecordHolder fastestLap,
        RecordHolder fastestQuali,
        List<NamedCount> byCategory,
        List<Appearance> races
) {
    public record Appearance(
            LocalDate date,
            String category,
            int position,
            int fieldSize,
            String totalTours,
            String bestLap
    ) {
    }

    public static PilotClubStats empty(String name) {
        return new PilotClubStats(
                name == null ? "" : name,
                0, 0, 0, 0, "—", "—",
                null, null,
                List.of(), List.of()
        );
    }
}
