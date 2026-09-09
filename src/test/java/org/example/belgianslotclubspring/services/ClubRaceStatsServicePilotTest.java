package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.models.PilotClubStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClubRaceStatsServicePilotTest {

    @Test
    void emptyWhenPilotNeverRaced() {
        RaceResult other = race("Ada", 100, "2026-09-01", "GT24");
        PilotClubStats stats = ClubRaceStatsService.aggregatePilot("Max", List.of(other), List.of());
        assertEquals("Max", stats.name());
        assertEquals(0, stats.starts());
        assertTrue(stats.races().isEmpty());
    }

    @Test
    void matchesBisMarkerAndCountsWinPodiumAverage() {
        RaceResult first = race("Max *", 120.5, "2026-09-08", "GT24");
        first.addTrackPerformance(1, 40, 8.210);
        RaceResult second = race("Léo", 118.0, "2026-09-08", "GT24");
        RaceResult third = race("Ada", 110.0, "2026-09-08", "GT24");

        RaceResult later = race("Max", 90.0, "2026-09-15", "GT24");
        later.addTrackPerformance(1, 30, 8.050);
        RaceResult laterWinner = race("Ada", 99.0, "2026-09-15", "GT24");
        RaceResult laterThird = race("Léo", 80.0, "2026-09-15", "GT24");
        RaceResult laterFourth = race("Sam", 70.0, "2026-09-15", "GT24");

        Qualif pole = new Qualif("Max *", "8.100", "2026-09-08");
        Qualif slower = new Qualif("Ada", "8.400", "2026-09-08");

        PilotClubStats stats = ClubRaceStatsService.aggregatePilot(
                "Max *",
                List.of(first, second, third, later, laterWinner, laterThird, laterFourth),
                List.of(pole, slower)
        );

        assertEquals("Max", stats.name());
        assertEquals(2, stats.starts());
        assertEquals(1, stats.wins());
        assertEquals(2, stats.podiums());
        assertEquals(1, stats.poles());
        assertEquals("1,5", stats.avgPosition());
        assertEquals("50 %", stats.winRate());
        assertEquals("8.050", stats.fastestLap().time());
        assertEquals("8.100", stats.fastestQuali().time());
        assertEquals(1, stats.byCategory().size());
        assertEquals("GT24", stats.byCategory().getFirst().label());
        assertEquals(2, stats.races().size());
        assertEquals("2026-09-15", stats.races().getFirst().date().toString());
        assertEquals(2, stats.races().getFirst().position());
        assertEquals(4, stats.races().getFirst().fieldSize());
        assertEquals(1, stats.races().get(1).position());
    }

    @Test
    void tiedFirstCountsAsWin() {
        RaceResult a = race("Max", 100, "2026-01-06", "SLOT.IT");
        RaceResult b = race("Ada", 100, "2026-01-06", "SLOT.IT");
        RaceResult c = race("Léo", 80, "2026-01-06", "SLOT.IT");

        PilotClubStats stats = ClubRaceStatsService.aggregatePilot("Max", List.of(a, b, c), List.of());
        assertEquals(1, stats.starts());
        assertEquals(1, stats.wins());
        assertEquals(1, stats.races().getFirst().position());
        assertNull(stats.fastestLap());
    }

    private static RaceResult race(String name, double tours, String date, String category) {
        return new RaceResult(name, tours, date, category);
    }
}
