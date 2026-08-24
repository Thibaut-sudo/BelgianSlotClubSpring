package org.example.belgianslotclubspring.services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ForumServiceTest {

    @Test
    void slugNormalizesAccentsAndSpaces() {
        assertEquals("reglement", ForumService.slug("Règlement"));
        assertEquals("eclairage-led", ForumService.slug("Éclairage LED"));
    }

    @Test
    void cleanLineRejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> ForumService.cleanLine("   ", 40, "un nom"));
        assertEquals("Thibaut", ForumService.cleanLine("  Thibaut  ", 40, "un nom"));
    }

    @Test
    void cleanBodyKeepsNewlines() {
        assertEquals("Ligne 1\nLigne 2", ForumService.cleanBody("Ligne 1\nLigne 2\n", 4000, "une question"));
    }
}
