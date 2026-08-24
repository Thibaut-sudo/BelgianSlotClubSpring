package org.example.belgianslotclubspring.models;

import java.time.LocalDate;
import java.util.List;

/**
 * Statistiques agrégées des courses d'un club (toutes années ou une saison).
 */
public record ClubRaceStats(
        int raceCount,
        int uniquePilots,
        int categoryCount,
        int starterCount,
        String avgFieldSize,
        RecordHolder fastestLap,
        RecordHolder fastestQuali,
        List<NamedCount> racesByCategory,
        List<NamedCount> racesByYear,
        List<PilotCount> mostPresent,
        List<PilotCount> mostWins,
        List<RaceCrowd> biggestFields
) {
    public record NamedCount(String label, int count, int pct) {
    }

    public record PilotCount(String name, int count) {
    }

    public record RaceCrowd(LocalDate date, String category, int pilots) {
    }

    public record RecordHolder(String name, String category, LocalDate date, String time) {
    }

    public static ClubRaceStats empty() {
        return new ClubRaceStats(
                0, 0, 0, 0, "—",
                null, null,
                List.of(), List.of(), List.of(), List.of(), List.of()
        );
    }
}
