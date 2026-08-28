package org.example.belgianslotclubspring.utils;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Rapproche « Thibaut » / « Thibaut Lenertz », « Chris B » / « Christophe B », etc.
 */
public final class RallyePilotNames {

    private static final Map<String, String> ALIASES = aliases();

    private RallyePilotNames() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String stripped = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        String aliased = ALIASES.getOrDefault(stripped, stripped);
        return ALIASES.getOrDefault(aliased, aliased);
    }

    public static String displayName(String current, String candidate) {
        String a = current == null ? "" : current.trim();
        String b = candidate == null ? "" : candidate.trim();
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        boolean aFull = a.contains(" ");
        boolean bFull = b.contains(" ");
        if (bFull && !aFull) {
            return b;
        }
        if (aFull && !bFull) {
            return a;
        }
        return b.length() > a.length() ? b : a;
    }

    public static String[] tokens(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return new String[0];
        }
        return normalized.split(" ");
    }

    private static Map<String, String> aliases() {
        Map<String, String> map = new HashMap<>();
        map.put("pat", "patrick");
        map.put("max", "maximilien thonon");
        map.put("matt", "matthieu kempinaire");
        map.put("stef", "stephane rome");
        map.put("stephane", "stephane rome");
        map.put("leo", "leo rome");
        map.put("jonjon", "john geonet");
        map.put("batiste", "batiste geonet");
        map.put("chris l", "christophe l");
        map.put("chris b", "christophe b");
        map.put("bouillet christophe", "christophe b");
        map.put("christophe bouillet", "christophe b");
        map.put("brighenti pierre", "pierre");
        map.put("pierre brighenti", "pierre");
        return Map.copyOf(map);
    }
}
