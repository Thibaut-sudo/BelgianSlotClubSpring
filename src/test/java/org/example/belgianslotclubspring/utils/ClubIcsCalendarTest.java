package org.example.belgianslotclubspring.utils;

import org.example.belgianslotclubspring.models.Club;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClubIcsCalendarTest {

    @Test
    void srcsFeedContainsTuesdayClubNights() {
        String ics = ClubIcsCalendar.build(Club.SRCS);
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.contains("END:VCALENDAR\r\n"));
        assertTrue(ics.contains("X-WR-CALNAME:SRCS"));
        assertTrue(ics.contains("UID:2026-09-01-srcs-v2@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:SRCS — Scaleauto"));
        assertTrue(ics.contains("UID:2026-09-08-srcs-v2@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:SRCS — GT24 / Proto 24"));
        assertTrue(ics.contains("SEQUENCE:2"));
        assertTrue(ics.contains("LAST-MODIFIED:20260824T213000Z"));
        assertTrue(ics.contains("DTSTART;TZID=Europe/Brussels:20260901T180000"));
        assertTrue(ics.contains("DTEND;TZID=Europe/Brussels:20260901T220000"));
        assertTrue(ics.contains("LOCATION:Rue Champs d'Oiseaux 240\\, 4101 Jemeppe-sur-Meuse"));
        assertEquals("/calendrier/srcs/v2.ics", ClubIcsCalendar.publicFeedPath(Club.SRCS));
        assertFalse(ics.contains("UID:2026-12-22-srcs@"));
    }

    @Test
    void slot4000FeedEscapesCommasAndKeepsHobby2000Autumn() {
        String ics = ClubIcsCalendar.build(Club.SLOT4000);
        assertTrue(ics.contains("X-WR-CALNAME:Slot 4000"));
        assertTrue(ics.contains("UID:2026-09-25-slot4000-v2@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:Slot 4000 — PROTO24"));
        assertTrue(ics.contains("UID:2026-10-23-slot4000-v2@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:Slot 4000 — GT24"));
        assertTrue(ics.contains("CATEGORIES:GT24\\, BEL LMS"));
        assertTrue(ics.contains("LOCATION:Quai de la Boverie 78-87\\, 4020 Liège"));
    }

    @Test
    void customEventsAreIncludedWhenProvided() {
        String ics = ClubIcsCalendar.build(Club.SRCS, java.util.Map.of(
                "2026-09-05", "BEL-LMS Chimay"
        ));
        assertTrue(ics.contains("UID:2026-09-05-srcs-v2@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:SRCS — BEL-LMS Chimay"));
    }
}
