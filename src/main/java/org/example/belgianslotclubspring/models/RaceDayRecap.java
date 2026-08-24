package org.example.belgianslotclubspring.models;

import java.util.List;

/** Compte-rendu narratif d'une journée de course (qualifs + finale). */
public record RaceDayRecap(
        boolean hasQuali,
        boolean hasRace,
        List<String> qualiHeadlines,
        List<String> raceHeadlines,
        List<String> podiumLines,
        List<RaceTrackScratch> trackScratches,
        List<String> scratchLeaders
) {
    public boolean hasAnything() {
        return hasQuali || hasRace;
    }
}
