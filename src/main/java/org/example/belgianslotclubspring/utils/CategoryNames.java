package org.example.belgianslotclubspring.utils;

import java.util.Locale;
import java.util.Map;

/**
 * Normalise les noms de catégories courses (ex. « Slot it » / « Slot-it » → SLOT.IT).
 */
public final class CategoryNames {

    private static final Map<String, String> CANONICAL = Map.ofEntries(
            Map.entry("slotit", "SLOT.IT"),
            Map.entry("groupe5", "Groupe 5"),
            Map.entry("gr5", "Groupe 5"),
            Map.entry("gt32", "GT32"),
            Map.entry("gt24", "GT24"),
            Map.entry("proto24", "PROTO24"),
            Map.entry("proto32", "PROTO32"),
            Map.entry("tcrscale", "TCR-SCALE"),
            Map.entry("tcrall", "TCR ALL"),
            Map.entry("scaleauto", "SCALEAUTO"),
            Map.entry("bpc", "BPC"),
            Map.entry("1000kms", "1000KMS"),
            Map.entry("1000km", "1000KMS")
    );

    private CategoryNames() {
    }

    /** Clé de comparaison : lettres/chiffres uniquement, minuscules. */
    public static String key(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = Character.toLowerCase(raw.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Libellé canonique pour affichage / stockage. */
    public static String canonical(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String k = key(trimmed);
        if (k.isEmpty()) {
            return trimmed;
        }
        String known = CANONICAL.get(k);
        if (known != null) {
            return known;
        }
        return titleWords(trimmed.replaceAll("[\\s._\\-]+", " ").trim());
    }

    public static boolean same(String a, String b) {
        String ka = key(a);
        String kb = key(b);
        return !ka.isEmpty() && ka.equals(kb);
    }

    private static String titleWords(String cleaned) {
        if (cleaned.isEmpty()) {
            return cleaned;
        }
        String[] parts = cleaned.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.toString();
    }
}
