package org.example.belgianslotclubspring.services.impl;

import org.example.belgianslotclubspring.entities.Rallye;
import org.example.belgianslotclubspring.entities.RallyePilot;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RallyeChampionshipServiceImplTest {

    @Test
    void ignoresUnfinishedTestAndEmptyRallies() {
        Rallye test = new Rallye("test", LocalDate.of(2026, 8, 27), "sco");
        test.setFinished(true);
        test.addPilot(new RallyePilot("X", null, "WRC 1", 1));
        assertFalse(RallyeChampionshipServiceImpl.countsForChampionship(test, 2026));

        Rallye empty = new Rallye("Rallye de la Vallée du Geer", LocalDate.of(2026, 3, 31), "sco");
        empty.setFinished(true);
        assertFalse(RallyeChampionshipServiceImpl.countsForChampionship(empty, 2026));

        Rallye ok = new Rallye("Rallye de la Vallée du Geer", LocalDate.of(2026, 3, 31), "sco");
        ok.setFinished(true);
        ok.addPilot(new RallyePilot("Thibaut", null, "WRC EX", 1));
        assertTrue(RallyeChampionshipServiceImpl.countsForChampionship(ok, 2026));
        assertFalse(RallyeChampionshipServiceImpl.countsForChampionship(ok, 2025));
        ok.setFinished(false);
        assertFalse(RallyeChampionshipServiceImpl.countsForChampionship(ok, 2026));
    }
}
