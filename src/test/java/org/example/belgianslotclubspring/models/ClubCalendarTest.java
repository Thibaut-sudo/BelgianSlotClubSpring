package org.example.belgianslotclubspring.models;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClubCalendarTest {

    @Test
    void srcsTuesdaySeasonStartsFirstWeekOfSeptember() {
        Map<String, String> events = ClubCalendar.eventsFor(Club.SRCS);
        assertEquals("Scaleauto", events.get("2026-09-01"));
        assertEquals("GT24 / Proto 24", events.get("2026-09-08"));
        assertEquals("BPC", events.get("2026-09-15"));
        assertEquals("Revoslot", events.get("2026-09-22"));
        assertEquals("BRM", events.get("2026-09-29"));
        assertEquals("Scaleauto", events.get("2026-10-06"));
        assertEquals(DayOfWeek.TUESDAY, LocalDate.parse("2026-09-01").getDayOfWeek());
    }

    @Test
    void srcsSkipsChristmasWeeksAndResumesInJanuary() {
        Map<String, String> events = ClubCalendar.eventsFor(Club.SRCS);
        assertNull(events.get("2026-12-22"));
        assertNull(events.get("2026-12-29"));
        assertEquals("Scaleauto", events.get("2026-12-15"));
        assertEquals("GT24 / Proto 24", events.get("2027-01-05"));
        assertEquals("GT24 / Proto 24", events.get("2027-06-29"));
    }

    @Test
    void srcsTuesdayCategoriesAreEvenlySpread() {
        Map<String, Integer> counts = new HashMap<>();
        ClubCalendar.eventsFor(Club.SRCS).forEach((date, name) -> {
            if (date.compareTo("2026-09-01") >= 0) {
                counts.merge(name, 1, Integer::sum);
            }
        });
        assertEquals(5, counts.size());
        int min = counts.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        assertTrue(max - min <= 1, "Répartition inégale: " + counts);
        assertEquals(9, counts.get("GT24 / Proto 24"));
        assertEquals(9, counts.get("Scaleauto"));
        assertEquals(8, counts.get("BPC"));
        assertEquals(8, counts.get("Revoslot"));
        assertEquals(8, counts.get("BRM"));
    }

    @Test
    void slot4000Autumn2026MatchesHobby2000Proposal() {
        Map<String, String> events = ClubCalendar.eventsFor(Club.SLOT4000);
        assertEquals("PROTO32", events.get("2026-09-04"));
        assertEquals("GT24", events.get("2026-09-11"));
        assertEquals("GR5", events.get("2026-09-18"));
        assertEquals("PROTO24", events.get("2026-09-25"));
        assertEquals("SLOT.IT", events.get("2026-10-02"));
        assertEquals("TCR ALL", events.get("2026-10-09"));
        assertEquals("PROTO32", events.get("2026-10-16"));
        assertEquals("GT24", events.get("2026-10-23"));
        assertEquals("GT32", events.get("2026-10-30"));
        assertEquals("PROTO24", events.get("2026-11-06"));
        assertEquals("GR5", events.get("2026-11-13"));
        assertEquals("TCR ALL", events.get("2026-11-20"));
        assertEquals("SLOT.IT", events.get("2026-11-27"));
        assertEquals("GT24", events.get("2026-12-04"));
        assertEquals("PROTO32", events.get("2026-12-11"));
        assertEquals("Soirée Fun", events.get("2026-12-18"));
        assertEquals(DayOfWeek.FRIDAY, LocalDate.parse("2026-09-04").getDayOfWeek());
    }
}
