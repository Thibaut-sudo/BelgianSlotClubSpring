package org.example.belgianslotclubspring.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RallyeTest {

    @Test
    void oneLoopOneStageIsClassementOnly() {
        Rallye rallye = new Rallye("Rallycross de Slins", LocalDate.of(2026, 6, 7), "sco");
        rallye.setBoucleCount(1);
        rallye.setStagesPerBoucle(1);
        assertTrue(rallye.isClassementOnly());

        rallye.setBoucleCount(4);
        assertFalse(rallye.isClassementOnly());
    }
}
