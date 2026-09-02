package org.example.belgianslotclubspring.utils;

import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.ClubCalendar;
import org.example.belgianslotclubspring.models.GlobalCalendarEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Flux iCalendar (RFC 5545) par club, pour Google Agenda / Apple / Outlook.
 */
public final class ClubIcsCalendar {

    public static final ZoneId ZONE = ZoneId.of("Europe/Brussels");
    public static final LocalTime START = LocalTime.of(18, 0);
    public static final LocalTime END = LocalTime.of(22, 0);

    /**
     * À incrémenter quand on change l’URL publique (nouveaux abonnements).
     * Les UID gardent ce numéro pour que Google reconnaisse les mêmes événements.
     */
    public static final int FEED_REVISION = 3;

    /** Point de départ des SEQUENCE, supérieur aux anciennes valeurs figées (3). */
    static final Instant SEQUENCE_EPOCH = Instant.parse("2026-08-28T20:52:00Z");
    static final String ALL_CALENDAR_NAME = "Belgian Slot Club — Tous les clubs";
    private static final String ALL_CALENDAR_DESC =
            "Slot 4000, SRCS et Rallyes Slot. https://belgianslotclub.com/calendrier";

    private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);

    private ClubIcsCalendar() {
    }

    public static String build(Club club) {
        return build(club, ClubCalendar.eventsFor(club), Instant.now());
    }

    public static String build(Club club, Map<String, String> events) {
        return build(club, events, Instant.now());
    }

    static String build(Club club, Map<String, String> events, Instant generatedAt) {
        if (club == null) {
            throw new IllegalArgumentException("Club obligatoire");
        }
        Map<String, String> eventMap = events == null ? ClubCalendar.eventsFor(club) : events;
        Instant stamp = generatedAt == null ? Instant.now() : generatedAt;
        String utcStamp = UTC.format(stamp);
        int sequence = sequence(stamp);
        StringBuilder ics = new StringBuilder(8_192);
        line(ics, "BEGIN:VCALENDAR");
        line(ics, "VERSION:2.0");
        line(ics, "PRODID:-//Belgian Slot Club//Calendrier " + club.getDisplayName() + "//FR");
        line(ics, "CALSCALE:GREGORIAN");
        line(ics, "METHOD:PUBLISH");
        line(ics, "X-WR-CALNAME:" + escape(calendarName(club)));
        line(ics, "X-WR-CALDESC:" + escape(calendarDescription(club)));
        line(ics, "X-WR-TIMEZONE:Europe/Brussels");
        line(ics, "REFRESH-INTERVAL;VALUE=DURATION:PT15M");
        line(ics, "X-PUBLISHED-TTL:PT15M");
        appendVTimezone(ics);

        for (Map.Entry<String, String> entry : eventMap.entrySet()) {
            appendEvent(ics, club, LocalDate.parse(entry.getKey()), entry.getValue(), utcStamp, sequence, false);
        }

        line(ics, "END:VCALENDAR");
        return ics.toString();
    }

    public static String buildAll(Map<String, List<GlobalCalendarEvent>> eventsByDate) {
        return buildAll(eventsByDate, Instant.now());
    }

    static String buildAll(Map<String, List<GlobalCalendarEvent>> eventsByDate, Instant generatedAt) {
        Instant stamp = generatedAt == null ? Instant.now() : generatedAt;
        String utcStamp = UTC.format(stamp);
        int sequence = sequence(stamp);
        StringBuilder ics = new StringBuilder(12_288);
        line(ics, "BEGIN:VCALENDAR");
        line(ics, "VERSION:2.0");
        line(ics, "PRODID:-//Belgian Slot Club//Calendrier tous clubs//FR");
        line(ics, "CALSCALE:GREGORIAN");
        line(ics, "METHOD:PUBLISH");
        line(ics, "X-WR-CALNAME:" + escape(ALL_CALENDAR_NAME));
        line(ics, "X-WR-CALDESC:" + escape(ALL_CALENDAR_DESC));
        line(ics, "X-WR-TIMEZONE:Europe/Brussels");
        line(ics, "REFRESH-INTERVAL;VALUE=DURATION:PT15M");
        line(ics, "X-PUBLISHED-TTL:PT15M");
        appendVTimezone(ics);

        if (eventsByDate != null) {
            for (Map.Entry<String, List<GlobalCalendarEvent>> day : eventsByDate.entrySet()) {
                if (day.getKey() == null || day.getValue() == null) {
                    continue;
                }
                LocalDate date = LocalDate.parse(day.getKey());
                for (GlobalCalendarEvent event : day.getValue()) {
                    if (event == null || event.clubCode() == null) {
                        continue;
                    }
                    Club club = Club.fromCode(event.clubCode()).orElse(null);
                    if (club == null) {
                        continue;
                    }
                    String name = event.name() == null ? "" : event.name();
                    appendEvent(ics, club, date, name, utcStamp, sequence, true);
                }
            }
        }

        line(ics, "END:VCALENDAR");
        return ics.toString();
    }

    public static String fileName(Club club) {
        return "calendrier-" + club.getCode() + ".ics";
    }

    public static String fileNameAll() {
        return "calendrier-belgian-slot-club.ics";
    }

    /** URL jamais vue par Google / Cloudflare, pour forcer un nouvel abonnement. */
    public static String publicFeedPath(Club club) {
        return "/calendrier/" + club.getCode() + "/v" + FEED_REVISION + ".ics";
    }

    public static String publicFeedPathAll() {
        return "/calendrier/all/v" + FEED_REVISION + ".ics";
    }

    /** Empreinte du contenu (sans horodatage), pour ETag HTTP. */
    public static String contentEtag(Club club, Map<String, String> events) {
        String code = club == null ? "" : club.getCode();
        return Integer.toHexString(Objects.hash(code, events));
    }

    public static String contentEtagAll(Map<String, List<GlobalCalendarEvent>> eventsByDate) {
        return Integer.toHexString(Objects.hash("all", eventsByDate));
    }

    static int sequence(Instant generatedAt) {
        Instant stamp = generatedAt == null ? Instant.now() : generatedAt;
        long minutes = Duration.between(SEQUENCE_EPOCH, stamp).toMinutes();
        long value = FEED_REVISION + Math.max(0, minutes);
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    static String calendarName(Club club) {
        return club.getDisplayName() + " 2026-2027 — Belgian Slot Club";
    }

    private static String calendarDescription(Club club) {
        if (club.isRallyOnly()) {
            return "Championnat de Belgique des Rallyes Slot. " + eveningHint(club)
                    + " https://belgianslotclub.com/rallye?club=" + club.getCode();
        }
        return "Courses et soirées " + club.getDisplayName() + ". " + eveningHint(club)
                + " https://belgianslotclub.com/prochain-evenement?club=" + club.getCode();
    }

    static String location(Club club) {
        if (club.isSrcs()) {
            return "Rue Champs d'Oiseaux 240, 4101 Jemeppe-sur-Meuse";
        }
        return "Quai de la Boverie 78-87, 4020 Liège";
    }

    static String eveningHint(Club club) {
        if (club.isRallyOnly()) {
            return "Épreuves rallye slot.";
        }
        return club.isSlot4000() ? "Vendredi dès 18h." : "Mardi dès 18h.";
    }

    private static LocalTime startTime(Club club) {
        return club.isRallyOnly() ? LocalTime.of(9, 0) : START;
    }

    private static LocalTime endTime(Club club) {
        return club.isRallyOnly() ? LocalTime.of(18, 0) : END;
    }

    private static void appendEvent(StringBuilder ics, Club club, LocalDate date, String name,
                                    String utcStamp, int sequence, boolean useCalendarLabel) {
        ZonedDateTime start = date.atTime(startTime(club)).atZone(ZONE);
        ZonedDateTime end = date.atTime(endTime(club)).atZone(ZONE);
        String uid = date + "-" + club.getCode() + "-v" + FEED_REVISION + "@belgianslotclub.com";
        String clubLabel = useCalendarLabel ? club.getCalendarLabel() : club.getDisplayName();
        String summary = clubLabel + " — " + name;
        String description = name + ". " + eveningHint(club)
                + " Détails : https://belgianslotclub.com/prochain-evenement?club=" + club.getCode();

        line(ics, "BEGIN:VEVENT");
        line(ics, "UID:" + uid);
        line(ics, "DTSTAMP:" + utcStamp);
        line(ics, "LAST-MODIFIED:" + utcStamp);
        line(ics, "SEQUENCE:" + sequence);
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
