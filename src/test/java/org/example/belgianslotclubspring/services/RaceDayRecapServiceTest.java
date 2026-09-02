package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.models.RaceDayRecap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaceDayRecapServiceTest {

    private final RaceDayRecapService service = new RaceDayRecapService();

    @Test
    void raceRecapHighlightsPodiumThrillerScratchesAndPole() {
        List<Qualif> quals = List.of(
                new Qualif("Paul", "6.850"),
                new Qualif("Thibaut", "6.900"),
                new Qualif("Gr Fred", "6.940"),
                new Qualif("Marc", "7.010"),
                new Qualif("Luc", "7.040"),
                new Qualif("Jean", "7.080"),
                new Qualif("Olivier", "7.120"),
                new Qualif("El Velasquez", "7.150"),
                new Qualif("Pierre", "7.200"),
                new Qualif("André", "7.280"),
                new Qualif("Pascal", "7.400")
        );

        RaceDayRecap recap = service.build(quals, sampleRace());
        String race = String.join("\n", recap.raceHeadlines());
        String quali = String.join("\n", recap.qualiHeadlines());

        assertTrue(recap.hasRace());
        assertTrue(recap.raceHeadlines().size() >= 5, race);
        assertTrue(recap.raceHeadlines().size() <= 12, race);
        assertTrue(race.contains("Paul"), race);
        assertTrue(race.contains("203.2"), race);
        assertTrue(race.contains("Thibaut") && race.contains("Gr Fred"), race);
        assertTrue(race.contains("0.2"), "Le sprint P2/P3 (0.2 tour) doit apparaître.\n" + race);
        assertTrue(race.contains("El Velasquez") || race.contains("6.722"), race);
        assertTrue(race.contains("Marc"), "La 4e place doit être citée.\n" + race);
        assertTrue(race.contains("Pascal"), "La queue du classement doit être citée.\n" + race);
        assertTrue(race.contains("piste"), race);
        assertTrue(race.contains("pole") || race.contains("Pole") || race.contains("grille")
                || race.contains("Parti"), race);
        assertFalse(race.contains("Mission accomplie"), race);
        assertFalse(race.contains("glane 1 piste"), race);
        assertFalse(race.contains("Impressionnant."), race);
        assertFalse(race.contains("autoritaire"), race);
        assertFalse(race.contains("relégué"), race);
        assertFalse(race.contains("nouvelles bagarres"), race);
        assertFalse(race.contains("désormais figé"), race);

        assertTrue(quali.contains("Paul"), quali);
        assertTrue(quali.contains("6.850") || quali.toLowerCase().contains("pole"), quali);
    }

    @Test
    void raceRecapMentionsComebackWhenPoleSitterLoses() {
        List<Qualif> quals = List.of(
                new Qualif("Thibaut", "6.800"),
                new Qualif("Gr Fred", "6.850"),
                new Qualif("Marc", "6.900"),
                new Qualif("Paul", "6.980")
        );
        RaceDayRecap recap = service.build(quals, sampleRace().subList(0, 4));
        String race = String.join("\n", recap.raceHeadlines());
        assertTrue(
                race.contains("Remontée") || race.contains("qualifs") || race.contains("grille")
                        || race.contains("s’élançait") || race.contains("remonte"),
                race
        );
        assertTrue(race.contains("Paul"), race);
    }

    @Test
    void tiedScratchesAreToldTogetherWithoutRepeatingTheFastestLap() {
        List<Qualif> quals = List.of(
                new Qualif("Bruno", "10.500"),
                new Qualif("Max *", "10.540"),
                new Qualif("Stany", "10.580")
        );
        List<RaceResult> results = List.of(
                pilot("Bruno", 132.0, new double[]{10.521, 10.600, 10.540, 10.610, 10.550, 10.620},
                        new int[]{22, 22, 22, 22, 22, 22}),
                pilot("Max *", 130.0, new double[]{10.700, 10.680, 10.690, 10.700, 10.710, 10.720},
                        new int[]{22, 21, 22, 21, 22, 22}),
                pilot("Stany", 129.5, new double[]{10.650, 10.465, 10.640, 10.500, 10.660, 10.510},
                        new int[]{21, 21, 21, 22, 21, 22}),
                pilot("Sebastien", 111.5, new double[]{11.200, 11.100, 11.050, 11.150, 11.180, 11.220},
                        new int[]{19, 18, 19, 18, 19, 18})
        );

        RaceDayRecap recap = service.build(quals, results);
        String race = String.join("\n", recap.raceHeadlines());

        assertTrue(race.contains("Bruno") && race.contains("Stany"), race);
        assertTrue(race.contains("132"), race);
        assertFalse(race.contains("132.0"), race);
        assertTrue(
                race.contains("nul") || race.contains("se partagent") || race.contains("partout")
                        || race.contains("coupent"),
                "Le 3–3 Bruno/Stany doit être raconté.\n" + race
        );
        assertTrue(race.contains("10.465"), race);
        Matcher times = Pattern.compile("10\\.465").matcher(race);
        int hits = 0;
        while (times.find()) {
            hits++;
        }
        assertTrue(hits == 1, "Le 10.465 ne doit être dit qu’une fois.\n" + race);
        assertFalse(race.contains("autoritaire"), race);
        assertFalse(race.contains("Merci aux"), race);
        assertFalse(race.contains("Dernier classement"), race);
    }

    @Test
    void emptyInputsYieldEmptyRecap() {
        RaceDayRecap recap = service.build(List.of(), List.of());
        assertFalse(recap.hasAnything());
        assertTrue(recap.raceHeadlines().isEmpty());
        assertTrue(recap.qualiHeadlines().isEmpty());
    }

    private static List<RaceResult> sampleRace() {
        return List.of(
                pilot("Paul", 203.2, new double[]{7.036, 6.898, 7.010, 6.913, 6.850, 7.040},
                        new int[]{34, 34, 33, 34, 34, 34}),
                pilot("Thibaut", 201.8, new double[]{7.080, 6.950, 7.020, 6.980, 6.900, 6.992},
                        new int[]{34, 33, 33, 34, 34, 34}),
                pilot("Gr Fred", 201.6, new double[]{7.100, 6.970, 6.922, 7.000, 6.910, 7.050},
                        new int[]{34, 33, 34, 33, 34, 34}),
                pilot("Marc", 200.1, new double[]{7.150, 7.020, 7.050, 7.040, 6.960, 7.080},
                        new int[]{33, 33, 33, 33, 34, 34}),
                pilot("El Velasquez", 199.4, new double[]{7.200, 7.050, 7.080, 7.060, 6.722, 7.100},
                        new int[]{33, 33, 33, 33, 32, 33}),
                pilot("Luc", 198.2, new double[]{7.220, 7.080, 7.100, 7.090, 6.980, 7.120},
                        new int[]{33, 33, 33, 33, 33, 33}),
                pilot("Jean", 196.5, new double[]{7.280, 7.120, 7.140, 7.130, 7.020, 7.160},
                        new int[]{33, 32, 33, 33, 33, 33}),
                pilot("Olivier", 194.0, new double[]{7.350, 7.180, 7.200, 7.190, 7.080, 7.220},
                        new int[]{32, 32, 32, 33, 33, 32}),
                pilot("Pierre", 191.2, new double[]{7.400, 7.250, 7.280, 7.260, 7.150, 7.300},
                        new int[]{32, 32, 32, 32, 32, 31}),
                pilot("André", 186.0, new double[]{7.500, 7.400, 7.350, 7.380, 7.280, 7.420},
                        new int[]{31, 31, 31, 31, 31, 31}),
                pilot("Pascal", 178.1, new double[]{7.806, 7.803, 7.484, 7.771, 7.675, 7.771},
                        new int[]{30, 29, 30, 30, 29, 30})
        );
    }

    private static RaceResult pilot(String name, double tours, double[] bests, int[] laps) {
        RaceResult r = new RaceResult(name, tours, "2026-03-15", "GT24");
        for (int i = 0; i < 6; i++) {
            r.addTrackPerformance(i + 1, laps[i], bests[i]);
        }
        return r;
    }
}
