package org.example.belgianslotclubspring.utils;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.ClubCalendar;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Flux iCalendar (RFC 5545) par club, pour Google Agenda / Apple / Outlook.
 */
public final class ClubIcsCalendar {

    public static final ZoneId ZONE = ZoneId.of("Europe/Brussels");
    public static final LocalTime START = LocalTime.of(18, 0);
    public static final LocalTime END = LocalTime.of(22, 0);

    /**
     * À incrémenter quand les titres ou dates changent.
     * Google Agenda ignore souvent un ICS identique (même URL, mêmes UID).
     */
    public static final int FEED_REVISION = 2;
    static final String REVISION_UTC = "20260824T213000Z";

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private ClubIcsCalendar() {
    }

    public static String build(Club club) {
        return build(club, ClubCalendar.eventsFor(club));
    }

    public static String build(Club club, Map<String, String> events) {
        if (club == null) {
            throw new IllegalArgumentException("Club obligatoire");
        }
        Map<String, String> eventMap = events == null ? ClubCalendar.eventsFor(club) : events;
        StringBuilder ics = new StringBuilder(8_192);
        line(ics, "BEGIN:VCALENDAR");
        line(ics, "VERSION:2.0");
        line(ics, "PRODID:-//Belgian Slot Club//Calendrier " + club.getDisplayName() + "//FR");
        line(ics, "CALSCALE:GREGORIAN");
        line(ics, "METHOD:PUBLISH");
        line(ics, "X-WR-CALNAME:" + escape(calendarName(club)));
        line(ics, "X-WR-CALDESC:" + escape(calendarDescription(club)));
        line(ics, "X-WR-TIMEZONE:Europe/Brussels");
        line(ics, "REFRESH-INTERVAL;VALUE=DURATION:PT1H");
        line(ics, "X-PUBLISHED-TTL:PT1H");
        appendVTimezone(ics);

        for (Map.Entry<String, String> entry : eventMap.entrySet()) {
            appendEvent(ics, club, LocalDate.parse(entry.getKey()), entry.getValue());
        }

        line(ics, "END:VCALENDAR");
        return ics.toString();
    }

    public static String fileName(Club club) {
        return "calendrier-" + club.getCode() + ".ics";
    }

    /** URL jamais vue par Google / Cloudflare, pour forcer un nouvel abonnement. */
    public static String publicFeedPath(Club club) {
        return "/calendrier/" + club.getCode() + "/v" + FEED_REVISION + ".ics";
    }

    static String calendarName(Club club) {
        return club.getDisplayName() + " 2026-2027 — Belgian Slot Club";
    }

    private static String calendarDescription(Club club) {
        return "Courses et soirées " + club.getDisplayName() + ". " + eveningHint(club)
                + " https://belgianslotclub.com/prochain-evenement?club=" + club.getCode();
    }

    static String location(Club club) {
        if (club.isSlot4000()) {
            return "Quai de la Boverie 78-87, 4020 Liège";
        }
        return "Rue Champs d'Oiseaux 240, 4101 Jemeppe-sur-Meuse";
    }

    static String eveningHint(Club club) {
        return club.isSlot4000() ? "Vendredi dès 18h." : "Mardi dès 18h.";
    }

    private static void appendEvent(StringBuilder ics, Club club, LocalDate date, String name) {
        ZonedDateTime start = date.atTime(START).atZone(ZONE);
        ZonedDateTime end = date.atTime(END).atZone(ZONE);
        String uid = date + "-" + club.getCode() + "-v" + FEED_REVISION + "@belgianslotclub.com";
        String summary = club.getDisplayName() + " — " + name;
        String description = name + ". " + eveningHint(club)
                + " Détails : https://belgianslotclub.com/prochain-evenement?club=" + club.getCode();

        line(ics, "BEGIN:VEVENT");
        line(ics, "UID:" + uid);
        line(ics, "DTSTAMP:" + REVISION_UTC);
        line(ics, "LAST-MODIFIED:" + REVISION_UTC);
        line(ics, "SEQUENCE:" + FEED_REVISION);
        line(ics, "DTSTART;TZID=Europe/Brussels:" + LOCAL.format(start));
        line(ics, "DTEND;TZID=Europe/Brussels:" + LOCAL.format(end));
        line(ics, "SUMMARY:" + escape(summary));
        line(ics, "DESCRIPTION:" + escape(description));
        line(ics, "LOCATION:" + escape(location(club)));
        line(ics, "URL:https://belgianslotclub.com/prochain-evenement?club=" + club.getCode());
        line(ics, "CATEGORIES:" + escape(name));
        line(ics, "STATUS:CONFIRMED");
        line(ics, "TRANSP:OPAQUE");
        line(ics, "END:VEVENT");
    }

    private static void appendVTimezone(StringBuilder ics) {
        line(ics, "BEGIN:VTIMEZONE");
        line(ics, "TZID:Europe/Brussels");
        line(ics, "X-LIC-LOCATION:Europe/Brussels");
        line(ics, "BEGIN:DAYLIGHT");
        line(ics, "TZOFFSETFROM:+0100");
        line(ics, "TZOFFSETTO:+0200");
        line(ics, "TZNAME:CEST");
        line(ics, "DTSTART:19700329T020000");
        line(ics, "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU");
        line(ics, "END:DAYLIGHT");
        line(ics, "BEGIN:STANDARD");
        line(ics, "TZOFFSETFROM:+0200");
        line(ics, "TZOFFSETTO:+0100");
        line(ics, "TZNAME:CET");
        line(ics, "DTSTART:19701025T030000");
        line(ics, "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU");
        line(ics, "END:STANDARD");
        line(ics, "END:VTIMEZONE");
    }

    static String escape(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        return raw.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    private static void line(StringBuilder ics, String content) {
        int limit = 75;
        int index = 0;
        while (index < content.length()) {
            int room = index == 0 ? limit : limit - 1;
            int end = Math.min(content.length(), index + room);
            if (end < content.length() && Character.isHighSurrogate(content.charAt(end - 1))) {
                end--;
            }
            if (index > 0) {
                ics.append(' ');
            }
            ics.append(content, index, end).append("\r\n");
            index = end;
        }
    }
}
