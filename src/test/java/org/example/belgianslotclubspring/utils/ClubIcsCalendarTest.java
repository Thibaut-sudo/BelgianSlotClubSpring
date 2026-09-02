package org.example.belgianslotclubspring.utils;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.GlobalCalendarEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClubIcsCalendarTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-09-03T00:00:00Z");

    @Test
    void srcsFeedContainsTuesdayClubNights() {
        String ics = ClubIcsCalendar.build(Club.SRCS, null, GENERATED_AT);
        int sequence = ClubIcsCalendar.sequence(GENERATED_AT);
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.contains("END:VCALENDAR\r\n"));
        assertTrue(ics.contains("X-WR-CALNAME:SRCS"));
        assertTrue(ics.contains("UID:2026-09-01-srcs-v" + ClubIcsCalendar.FEED_REVISION + "@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:SRCS — Scaleauto"));
        assertTrue(ics.contains("UID:2026-09-08-srcs-v" + ClubIcsCalendar.FEED_REVISION + "@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:SRCS — GT24 / Proto 24"));
        assertTrue(ics.contains("SEQUENCE:" + sequence));
        assertTrue(ics.contains("LAST-MODIFIED:20260903T000000Z"));
        assertTrue(ics.contains("DTSTAMP:20260903T000000Z"));
        assertTrue(ics.contains("REFRESH-INTERVAL;VALUE=DURATION:PT15M"));
        assertTrue(ics.contains("DTSTART;TZID=Europe/Brussels:20260901T180000"));
        assertTrue(ics.contains("DTEND;TZID=Europe/Brussels:20260901T220000"));
        assertTrue(ics.contains("LOCATION:Rue Champs d'Oiseaux 240\\, 4101 Jemeppe-sur-Meuse"));
        assertEquals("/calendrier/srcs/v" + ClubIcsCalendar.FEED_REVISION + ".ics", ClubIcsCalendar.publicFeedPath(Club.SRCS));
        assertFalse(ics.contains("UID:2026-12-22-srcs@"));
        assertTrue(sequence > ClubIcsCalendar.FEED_REVISION);
    }

    @Test
    void sequenceIncreasesWhenGooglePollsLater() {
        int earlier = ClubIcsCalendar.sequence(GENERATED_AT);
        int later = ClubIcsCalendar.sequence(GENERATED_AT.plusSeconds(3600));
        assertTrue(later > earlier);
    }

    @Test
    void contentEtagChangesWhenAnEventNameChanges() {
        Map<String, String> before = Map.of("2026-09-05", "Scaleauto");
        Map<String, String> after = Map.of("2026-09-05", "BEL-LMS");
        assertEquals(
                ClubIcsCalendar.contentEtag(Club.SRCS, before),
                ClubIcsCalendar.contentEtag(Club.SRCS, Map.of("2026-09-05", "Scaleauto"))
        );
        assertFalse(ClubIcsCalendar.contentEtag(Club.SRCS, before)
                .equals(ClubIcsCalendar.contentEtag(Club.SRCS, after)));
    }

    @Test
    void slot4000FeedEscapesCommasAndKeepsHobby2000Autumn() {
        String ics = ClubIcsCalendar.build(Club.SLOT4000);
        assertTrue(ics.contains("X-WR-CALNAME:Slot 4000"));
        assertTrue(ics.contains("UID:2026-09-25-slot4000-v" + ClubIcsCalendar.FEED_REVISION + "@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:Slot 4000 — PROTO24"));
        assertTrue(ics.contains("UID:2026-10-23-slot4000-v" + ClubIcsCalendar.FEED_REVISION + "@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:Slot 4000 — GT24"));
        assertTrue(ics.contains("CATEGORIES:GT24\\, BEL LMS"));
        assertTrue(ics.contains("LOCATION:Quai de la Boverie 78-87\\, 4020 Liège"));
    }

    @Test
    void customEventsAreIncludedWhenProvided() {
        String ics = ClubIcsCalendar.build(Club.SRCS, Map.of(
                "2026-09-05", "BEL-LMS Chimay"
        ), GENERATED_AT);
        assertTrue(ics.contains("UID:2026-09-05-srcs-v" + ClubIcsCalendar.FEED_REVISION + "@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:SRCS — BEL-LMS Chimay"));
        assertTrue(ics.contains("LAST-MODIFIED:20260903T000000Z"));
    }

    @Test
    void combinedFeedIncludesEveryClubOnTheSameDay() {
        Map<String, List<GlobalCalendarEvent>> events = Map.of(
                "2026-09-05", List.of(
                        new GlobalCalendarEvent("slot4000", "Slot 4000", "GT32"),
                        new GlobalCalendarEvent("srcs", "SRCS", "Scaleauto"),
                        new GlobalCalendarEvent("sco", "Rallyes Slot", "Rallye de la Basse Meuse")
                )
        );
        String ics = ClubIcsCalendar.buildAll(events, GENERATED_AT);
        assertTrue(ics.contains("X-WR-CALNAME:Belgian Slot Club — Tous les clubs"));
        assertTrue(ics.contains("UID:2026-09-05-slot4000-v" + ClubIcsCalendar.FEED_REVISION + "@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:Slot 4000 — GT32"));
        assertTrue(ics.contains("UID:2026-09-05-srcs-v" + ClubIcsCalendar.FEED_REVISION + "@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:SRCS — Scaleauto"));
        assertTrue(ics.contains("UID:2026-09-05-sco-v" + ClubIcsCalendar.FEED_REVISION + "@belgianslotclub.com"));
        assertTrue(ics.contains("SUMMARY:Rallyes Slot — Rallye de la Basse Meuse"));
        assertTrue(ics.contains("DTSTART;TZID=Europe/Brussels:20260905T090000"));
        assertEquals("/calendrier/all/v" + ClubIcsCalendar.FEED_REVISION + ".ics", ClubIcsCalendar.publicFeedPathAll());
        assertEquals("calendrier-belgian-slot-club.ics", ClubIcsCalendar.fileNameAll());
    }

    @Test
    void contentEtagAllChangesWhenAnEventIsAdded() {
        var before = Map.of("2026-09-05", List.of(
                new GlobalCalendarEvent("srcs", "SRCS", "Scaleauto")
        ));
        var after = Map.of("2026-09-05", List.of(
                new GlobalCalendarEvent("srcs", "SRCS", "Scaleauto"),
                new GlobalCalendarEvent("slot4000", "Slot 4000", "GT32")
        ));
        assertFalse(ClubIcsCalendar.contentEtagAll(before).equals(ClubIcsCalendar.contentEtagAll(after)));
    }

    @Test
    void scoFeedUsesHobby2000AndDaytimeHours() {
        String ics = ClubIcsCalendar.build(Club.SCO);
        assertTrue(ics.contains("X-WR-CALNAME:Championnat de Belgique des Rallyes Slot"));
        assertTrue(ics.contains("SUMMARY:Championnat de Belgique des Rallyes Slot — Rallye de la Basse Meuse"));
        assertTrue(ics.contains("LOCATION:Quai de la Boverie 78-87\\, 4020 Liège"));
        assertTrue(ics.contains("DTSTART;TZID=Europe/Brussels:20261025T090000"));
        assertTrue(ics.contains("DTEND;TZID=Europe/Brussels:20261025T180000"));
        assertEquals("/calendrier/sco/v" + ClubIcsCalendar.FEED_REVISION + ".ics", ClubIcsCalendar.publicFeedPath(Club.SCO));
    }
}
