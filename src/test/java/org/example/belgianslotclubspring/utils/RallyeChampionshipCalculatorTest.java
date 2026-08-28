package org.example.belgianslotclubspring.utils;

import org.example.belgianslotclubspring.models.RallyeChampionshipRow;
import org.example.belgianslotclubspring.models.RallyeChampionshipTable;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RallyeChampionshipCalculatorTest {

    @Test
    void scaleMatchesOfficialTable() {
        assertEquals(24, RallyeChampionshipPoints.forCategoryPlace(1));
        assertEquals(22, RallyeChampionshipPoints.forCategoryPlace(2));
        assertEquals(20, RallyeChampionshipPoints.forCategoryPlace(3));
        assertEquals(19, RallyeChampionshipPoints.forCategoryPlace(4));
        assertEquals(17, RallyeChampionshipPoints.forCategoryPlace(6));
        assertEquals(14, RallyeChampionshipPoints.forCategoryPlace(9));
        assertEquals(13, RallyeChampionshipPoints.forCategoryPlace(10));
        assertEquals(1, RallyeChampionshipPoints.forCategoryPlace(22));
        assertEquals(1, RallyeChampionshipPoints.forCategoryPlace(23));
        assertEquals(25, RallyeChampionshipPoints.withScratchBonus(24, true));
        assertEquals(6, RallyeChampionshipPoints.presenceBonus(3));
        assertEquals(14, RallyeChampionshipPoints.presenceBonus(6));
        assertEquals(70, RallyeChampionshipPoints.countedRaceTotal(new int[]{25, 25, 20}, 3));
        assertEquals(104, RallyeChampionshipPoints.countedRaceTotal(new int[]{25, 25, 20, 18, 16, 1}, 6));
    }

    @Test
    void aliasesMergeShortAndFullNames() {
        assertEquals("thibaut lenertz", RallyePilotNames.normalize("Thibaut Lenertz"));
        assertEquals("thibaut", RallyePilotNames.normalize("Thibaut"));
        assertEquals("stephane rome", RallyePilotNames.normalize("Stef"));
        assertEquals("stephane rome", RallyePilotNames.normalize("Stéphane"));
        assertEquals("christophe b", RallyePilotNames.normalize("Bouillet Christophe"));
        assertEquals("pierre", RallyePilotNames.normalize("Brighenti Pierre"));
        assertEquals("WRC EX", RallyeCategory.canonical("WRCEX"));
        assertTrue(RallyeCategory.isWrc3("WRC3"));
        assertFalse(RallyeCategory.isWrc3("WRC EX"));
    }

    @Test
    void threeRalliesMatchPublishedTotals() {
        List<RallyeChampionshipCalculator.EventInput> events = List.of(
                new RallyeChampionshipCalculator.EventInput(1, "Rallye de la Vallée du Geer", LocalDate.of(2026, 3, 31), List.of(
                        p("Thibaut Lenertz", "WRC EX", 1),
                        p("Csaba Borsodi", "WRC 3", 10),
                        p("Lode Schrey", "WRC 3", 18),
                        p("Louise Valkenborgh", "WRC 3", 19)
                )),
                new RallyeChampionshipCalculator.EventInput(2, "Rallye Slotracefriends Diepenbeek", LocalDate.of(2026, 5, 3), List.of(
                        p("Thibaut", "WRC EX", 1),
                        p("Louise", "WRC 3", 16)
                )),
                new RallyeChampionshipCalculator.EventInput(3, "Rallycross de Slins", LocalDate.of(2026, 6, 7), List.of(
                        p("Patrick", "WRC EX", 1),
                        p("Maximilien Thonon", "WRC EX", 2),
                        p("Thibaut Lenertz", "WRC EX", 3),
                        p("Jordy Vabockrijck", "WRC 3", 21),
                        p("Louise Valkenborgh", "WRC 3", 23)
                ))
        );

        RallyeChampionshipTable table = RallyeChampionshipCalculator.compile(2026, events, null);
        RallyeChampionshipRow thibaut = row(table, "Thibaut");
        RallyeChampionshipRow louise = row(table, "Louise");

        assertEquals(25, thibaut.racePoints().get(0));
        assertEquals(25, thibaut.racePoints().get(1));
        assertEquals(20, thibaut.racePoints().get(2));
        assertEquals(6, thibaut.presenceBonus());
        assertEquals(76, thibaut.total());
        assertTrue(thibaut.titleEligible());

        assertEquals(1, thibaut.position());
        assertEquals(thibaut, table.rows().getFirst());
        assertEquals(76, table.leaderPoints());
        assertTrue(table.rows().get(0).total() >= table.rows().get(1).total());

        assertEquals(20, louise.racePoints().get(0));
        assertEquals(24, louise.racePoints().get(1));
        assertEquals(22, louise.racePoints().get(2));
        assertEquals(72, louise.total());
        assertEquals(2, louise.position());
        assertFalse(louise.titleEligible());
        assertEquals(thibaut, table.titleLeader());
        assertFalse(table.dropsWorst());
    }

    private static RallyeChampionshipCalculator.PilotInput p(String name, String cat, int overall) {
        return new RallyeChampionshipCalculator.PilotInput(name, cat, overall, true);
    }

    private static RallyeChampionshipRow row(RallyeChampionshipTable table, String prefix) {
        return table.rows().stream()
                .filter(r -> r.name().toLowerCase().contains(prefix.toLowerCase()))
                .findFirst()
                .orElseThrow();
    }
}
