package org.example.belgianslotclubspring.models;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Calendriers officiels des clubs (courses et soirées).
 */
public final class ClubCalendar {

    /** Rotation des soirées club SRCS (mardi). */
    static final String[] SRCS_TUESDAY_ROTATION = {
            "Scaleauto",
            "GT24 / Proto 24",
            "BPC",
            "Revoslot",
            "BRM"
    };

    private ClubCalendar() {
    }

    public static Map<String, String> eventsFor(Club club) {
        Map<String, String> events = new TreeMap<>();
        if (club == null) {
            return events;
        }
        if (club.isSlot4000()) {
            events.putAll(SLOT4000);
        } else if (club.isSrcs()) {
            events.putAll(SRCS);
        } else if (club.isRallyOnly()) {
            events.putAll(SCO);
        }
        return events;
    }

    private static final Map<String, String> SLOT4000;
    private static final Map<String, String> SRCS;
    private static final Map<String, String> SCO;

    static {
        Map<String, String> slot4000 = new TreeMap<>();
        // 2025
        slot4000.put("2025-01-10", "GT32");
        slot4000.put("2025-01-24", "GR5");
        slot4000.put("2025-01-31", "PROTO24");
        slot4000.put("2025-02-07", "PROTO32, GT32");
        slot4000.put("2025-02-14", "TCR-SCALE, PROTO24");
        slot4000.put("2025-02-21", "SLOT.IT, SCALEAUTO");
        slot4000.put("2025-02-22", "BPC");
        slot4000.put("2025-02-23", "BPC");
        slot4000.put("2025-02-28", "GT24");
        slot4000.put("2025-03-07", "GT32");
        slot4000.put("2025-03-21", "GR5");
        slot4000.put("2025-03-27", "1000KMS");
        slot4000.put("2025-03-28", "1000KMS");
        slot4000.put("2025-03-29", "1000KMS");
        slot4000.put("2025-04-04", "PROTO32");
        slot4000.put("2025-04-11", "GT24");
        slot4000.put("2025-04-18", "SLOT.IT");
        slot4000.put("2025-04-25", "PROTO24");
        slot4000.put("2025-05-02", "GT32");
        slot4000.put("2025-05-09", "TCR-SCALE");
        slot4000.put("2025-05-16", "GR5");
        slot4000.put("2025-05-30", "PROTO32");
        slot4000.put("2025-06-06", "PROTO24");
        slot4000.put("2025-06-13", "SLOT.IT");
        slot4000.put("2025-06-20", "TCR-SCALE");
        slot4000.put("2025-06-27", "GT32");
        slot4000.put("2025-09-05", "GR5");
        slot4000.put("2025-09-12", "PROTO24");
        slot4000.put("2025-09-19", "PROTO32");
        slot4000.put("2025-09-26", "GT24");
        slot4000.put("2025-10-03", "SLOT.IT");
        slot4000.put("2025-10-10", "TCR-SCALE");
        slot4000.put("2025-10-17", "GT32");
        slot4000.put("2025-10-24", "PROTO24");
        slot4000.put("2025-10-31", "GR5");
        slot4000.put("2025-11-07", "GT24");
        slot4000.put("2025-11-14", "PROTO24");
        slot4000.put("2025-11-21", "TCR-SCALE");
        slot4000.put("2025-11-27", "GT24, BEL LMS");
        slot4000.put("2025-12-05", "PROTO24");
        slot4000.put("2025-12-12", "SLOT.IT");
        slot4000.put("2025-12-19", "GT24");
        slot4000.put("2025-12-26", "PROTO32");
        // 2026 — calendrier officiel Slot 4000
        slot4000.put("2026-01-06", "PROTO32");
        slot4000.put("2026-01-13", "GT24");
        slot4000.put("2026-01-16", "GT24");
        slot4000.put("2026-01-23", "GT32");
        slot4000.put("2026-01-30", "PROTO24");
        slot4000.put("2026-02-06", "PROTO32");
        slot4000.put("2026-02-20", "GR5");
        slot4000.put("2026-02-27", "PROTO24");
        slot4000.put("2026-03-06", "SLOT.IT");
        slot4000.put("2026-03-13", "GT24");
        slot4000.put("2026-03-20", "GT32");
        slot4000.put("2026-03-24", "Soirée VAB");
        slot4000.put("2026-03-27", "PROTO32");
        slot4000.put("2026-04-03", "GR5");
        slot4000.put("2026-04-10", "PROTO24");
        slot4000.put("2026-04-17", "SLOT.IT");
        slot4000.put("2026-04-24", "GT24");
        slot4000.put("2026-05-01", "GT32");
        slot4000.put("2026-05-08", "PROTO24");
        slot4000.put("2026-05-15", "PROTO32");
        slot4000.put("2026-05-22", "GT24");
        slot4000.put("2026-05-29", "GR5");
        slot4000.put("2026-06-05", "PROTO24");
        slot4000.put("2026-06-12", "SLOT.IT");
        slot4000.put("2026-06-19", "GT24");
        slot4000.put("2026-06-26", "GT32");
        slot4000.put("2026-07-03", "PROTO24");
        // 2026 sept–déc — proposition Hobby 2000 (vendredis)
        slot4000.put("2026-09-04", "PROTO32");
        slot4000.put("2026-09-11", "GT24");
        slot4000.put("2026-09-18", "GR5");
        slot4000.put("2026-09-25", "PROTO24");
        slot4000.put("2026-10-02", "SLOT.IT");
        slot4000.put("2026-10-09", "TCR ALL");
        slot4000.put("2026-10-16", "PROTO32");
        slot4000.put("2026-10-23", "GT24");
        slot4000.put("2026-10-30", "GT32");
        slot4000.put("2026-11-06", "PROTO24");
        slot4000.put("2026-11-13", "GR5");
        slot4000.put("2026-11-20", "TCR ALL");
        slot4000.put("2026-11-27", "SLOT.IT");
        slot4000.put("2026-12-04", "GT24");
        slot4000.put("2026-12-11", "PROTO32");
        slot4000.put("2026-12-18", "Soirée Fun");
        SLOT4000 = Collections.unmodifiableMap(slot4000);

        Map<String, String> srcs = new TreeMap<>();
        srcs.put("2025-01-04", "Revoslot");
        srcs.put("2025-01-11", "BEL-LMS S.R.C.S");
        srcs.put("2025-01-18", "BPC D.S.C.A");
        srcs.put("2025-01-21", "GT24");
        srcs.put("2025-01-25", "GT 24");
        srcs.put("2025-01-28", "BRM");
        srcs.put("2025-02-08", "BEL-LMS Chimay");
        srcs.put("2025-02-15", "Revo S4all");
        srcs.put("2025-02-16", "BPC Chimay");
        srcs.put("2025-02-18", "Scaleauto");
        srcs.put("2025-02-23", "BPC Slot 4000");
        srcs.put("2025-02-25", "BRM");
        srcs.put("2025-03-04", "BPC");
        srcs.put("2025-03-11", "proto 24");
        srcs.put("2025-03-18", "Scaleauto");
        srcs.put("2025-03-22", "Scaleauto");
        srcs.put("2025-03-29", "BPC");
        srcs.put("2025-04-01", "Revoslot");
        srcs.put("2025-04-08", "GT 24");
        srcs.put("2025-04-12", "BEL-LMS fastlane");
        srcs.put("2025-04-15", "Proto 24");
        srcs.put("2025-04-25", "Proto 24");
        srcs.put("2025-05-06", "BRM");
        srcs.put("2025-05-11", "BPC Dulmen");
        srcs.put("2025-05-13", "Revoslot");
        srcs.put("2025-05-20", "GT 24");
        srcs.put("2025-05-24", "BEL-LMS D.S.C.A");
        srcs.put("2025-05-27", "Proto 24");
        srcs.put("2025-05-29", "BPC Stolberg");
        srcs.put("2025-06-03", "Revoslot");
        srcs.put("2025-06-07", "Revo Diepenbeek");
        srcs.put("2025-06-10", "BRM");
        srcs.put("2025-06-17", "Deph'one F1");
        srcs.put("2025-06-29", "BPC Stolberg");
        srcs.put("2025-09-01", "BPC");
        srcs.put("2025-09-07", "BPC S.R.C.S");
        srcs.put("2025-09-09", "Deph'one F1");
        srcs.put("2025-09-16", "Revoslot");
        srcs.put("2025-09-20", "Revo S.R.C.S");
        srcs.put("2025-09-23", "BRM");
        srcs.put("2025-09-25", "Chall. E.Pirotte");
        srcs.put("2025-09-28", "BPC");
        srcs.put("2025-09-30", "GT 24");
        srcs.put("2025-10-02", "BPC M.R.T.U");
        srcs.put("2025-10-04", "BRM");
        srcs.put("2025-10-07", "Proto 24");
        srcs.put("2025-10-08", "Revo Eindhoven");
        srcs.put("2025-10-11", "Revoslot");
        srcs.put("2025-10-14", "GT 24");
        srcs.put("2025-10-18", "Deph'one F1");
        srcs.put("2025-10-25", "Chall. E.Pirotte");
        srcs.put("2025-10-26", "Chall. E. Pirotte");
        srcs.put("2025-10-28", "BPC");
        srcs.put("2025-11-01", "BPC M.R.T.U");
        srcs.put("2025-11-08", "Revo Eindhoven");
        srcs.put("2025-11-11", "Revoslot");
        srcs.put("2025-11-13", "Revo DSCA");
        srcs.put("2025-11-16", "BRM");
        srcs.put("2025-11-18", "Deph'one F1");
        srcs.put("2025-11-25", "Scaleauto");
        srcs.put("2025-11-26", "Scaleauto S.R.C.S");
        srcs.put("2025-12-02", "BPC");
        srcs.put("2025-12-09", "Revoslot");
        srcs.put("2025-12-13", "Revo DSCA");
        srcs.put("2025-12-16", "BRM");
        srcs.put("2025-12-20", "Revo S.R.C.S");
        srcs.put("2025-12-23", "BRM");
        srcs.put("2025-12-27", "BRM");
        srcs.put("2025-12-30", "GT 24");
        // 2026–2027 — soirées club du mardi (reprise 1er sept. 2026)
        addSrcsTuesdayClubSeason(srcs);
        SRCS = Collections.unmodifiableMap(srcs);

        Map<String, String> sco = new TreeMap<>();
        sco.put("2026-10-25", "Rallye de la Basse Meuse");
        SCO = Collections.unmodifiableMap(sco);
    }

    /**
     * Tous les mardis de septembre à fin juin, hors congés de Noël.
     * Ordre : Scaleauto → GT24/Proto → BPC → Revoslot → BRM.
     */
    private static void addSrcsTuesdayClubSeason(Map<String, String> srcs) {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2027, 6, 29);
        if (start.getDayOfWeek() != DayOfWeek.TUESDAY) {
            throw new IllegalStateException("La reprise SRCS doit tomber un mardi : " + start);
        }
        int index = 0;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusWeeks(1)) {
            if (isSrcsChristmasBreak(date)) {
                continue;
            }
            srcs.put(date.toString(), SRCS_TUESDAY_ROTATION[index % SRCS_TUESDAY_ROTATION.length]);
            index++;
        }
    }

    /** Semaine de Noël et semaine du Nouvel An (mardis 22 et 29 décembre 2026). */
    private static boolean isSrcsChristmasBreak(LocalDate date) {
        return date.getMonthValue() == 12 && date.getDayOfMonth() >= 21;
    }
}
