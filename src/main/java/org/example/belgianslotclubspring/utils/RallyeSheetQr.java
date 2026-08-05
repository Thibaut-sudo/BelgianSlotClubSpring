package org.example.belgianslotclubspring.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Payload QR des feuilles groupe — compact et versionné pour le scan photo.
 * Format : {@code BSC1|{rallyeId}|{boucle}|{group}|{pilotIds}|{esNumbers}}
 */
public final class RallyeSheetQr {

    public static final String PREFIX = "BSC1";

    private RallyeSheetQr() {
    }

    public static String encode(long rallyeId, int boucle, int groupNumber, List<Long> pilotIds, List<Integer> esNumbers) {
        String pilots = joinLongs(pilotIds);
        String stages = joinInts(esNumbers);
        return PREFIX + "|" + rallyeId + "|" + boucle + "|" + groupNumber + "|" + pilots + "|" + stages;
    }

    public static Payload decode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("QR vide");
        }
        String value = raw.trim();
        String[] parts = value.split("\\|");
        if (parts.length != 6 || !PREFIX.equals(parts[0])) {
            throw new IllegalArgumentException("QR non reconnu (attendu " + PREFIX + ")");
        }
        try {
            long rallyeId = Long.parseLong(parts[1]);
            int boucle = Integer.parseInt(parts[2]);
            int group = Integer.parseInt(parts[3]);
            List<Long> pilots = parseLongs(parts[4]);
            List<Integer> stages = parseInts(parts[5]);
            if (group < 1 || boucle < 1 || pilots.isEmpty() || stages.isEmpty()) {
                throw new IllegalArgumentException("QR incomplet");
            }
            return new Payload(rallyeId, boucle, group, pilots, stages);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("QR invalide", ex);
        }
    }

    /** Ancre imprimée sous chaque case — lisible OCR, sans ambiguïté. */
    public static String cellAnchor(long pilotId, int esNumber) {
        return String.format(Locale.US, "p%de%d", pilotId, esNumber);
    }

    public record Payload(
            long rallyeId,
            int boucle,
            int groupNumber,
            List<Long> pilotIds,
            List<Integer> esNumbers
    ) {
    }

    private static String joinLongs(List<Long> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    private static String joinInts(List<Integer> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    private static List<Long> parseLongs(String raw) {
        List<Long> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String p : raw.split(",")) {
            if (!p.isBlank()) {
                out.add(Long.parseLong(p.trim()));
            }
        }
        return out;
    }

    private static List<Integer> parseInts(String raw) {
        List<Integer> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String p : raw.split(",")) {
            if (!p.isBlank()) {
                out.add(Integer.parseInt(p.trim()));
            }
        }
        return out;
    }
}
