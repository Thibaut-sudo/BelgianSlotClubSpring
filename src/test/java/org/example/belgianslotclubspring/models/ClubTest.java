package org.example.belgianslotclubspring.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClubTest {

    @Test
    void scoIsRallyOnlyChampionship() {
        Club sco = Club.fromCode("sco").orElseThrow();
        assertEquals("Championnat de Belgique des Rallyes Slot", sco.getDisplayName());
        assertTrue(sco.isRallyOnly());
        assertFalse(Club.SLOT4000.isRallyOnly());
        assertFalse(Club.SRCS.isRallyOnly());
        assertEquals("sco", Club.requireCode("SCO"));
    }
}
