package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.ClubCalendarEvent;
import org.example.belgianslotclubspring.models.Club;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClubCalendarServiceTest {

    @Test
    void mergeOverlaysOfficialTuesdayWithCustomEvent() {
        Map<String, String> official = Map.of(
                "2026-09-01", "Scaleauto",
                "2026-09-08", "GT24 / Proto 24"
        );
        ClubCalendarEvent extra = new ClubCalendarEvent(Club.SRCS, LocalDate.of(2026, 9, 5), "BEL-LMS");
        ClubCalendarEvent overlay = new ClubCalendarEvent(Club.SRCS, LocalDate.of(2026, 9, 1), "Soirée Fun");

        Map<String, String> merged = ClubCalendarService.merge(official, List.of(extra, overlay));

        assertEquals("Soirée Fun", merged.get("2026-09-01"));
        assertEquals("GT24 / Proto 24", merged.get("2026-09-08"));
        assertEquals("BEL-LMS", merged.get("2026-09-05"));
        assertEquals(3, merged.size());
    }

    @Test
    void cleanNameTrimsAndRejectsBlank() {
        assertEquals("BEL-LMS Chimay", ClubCalendarService.cleanName("  BEL-LMS   Chimay "));
        assertThrows(IllegalArgumentException.class, () -> ClubCalendarService.cleanName("   "));
    }

    @Test
    void requireDateRejectsOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> ClubCalendarService.requireDate(LocalDate.of(2019, 1, 1)));
        ClubCalendarService.requireDate(LocalDate.of(2026, 9, 1));
    }

    @Test
    void officialSrcsNamesAreRecognized() {
        assertTrue(ClubCalendarService.isOfficialName(Club.SRCS, "Scaleauto"));
        assertTrue(ClubCalendarService.isOfficialName(Club.SRCS, "GT24 / Proto 24"));
        assertFalse(ClubCalendarService.isOfficialName(Club.SRCS, "Ninco"));
    }

    @Test
    void cleanColorAcceptsHexAndDefaultsWhenBlank() {
        assertEquals("#be185d", ClubCalendarService.cleanColor("#BE185D", "Ninco"));
        assertEquals(ClubCalendarService.defaultColorFor("Ninco"), ClubCalendarService.cleanColor("", "Ninco"));
        assertThrows(IllegalArgumentException.class, () -> ClubCalendarService.cleanColor("red", "Ninco"));
    }

    @Test
    void firstCategoryNameTakesPartBeforeComma() {
        assertEquals("Ninco", ClubCalendarService.firstCategoryName("Ninco, proto"));
        assertEquals("Scaleauto", ClubCalendarService.firstCategoryName("Scaleauto"));
    }
}
