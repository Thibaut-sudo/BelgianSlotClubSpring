package org.example.belgianslotclubspring.utils;

/**
 * Catégories rallye (WRC EX / 1 / 2 / 3) — texte libre en base, normalisé ici.
 */
public final class RallyeCategory {

    private RallyeCategory() {
    }

    public static String canonical(String raw) {
        String key = CategoryNames.key(raw);
        return switch (key) {
            case "wrcex", "wrce" -> "WRC EX";
            case "wrc1", "wrc" -> "WRC 1";
            case "wrc2" -> "WRC 2";
            case "wrc3" -> "WRC 3";
            default -> raw == null ? "" : raw.trim();
        };
    }

    public static boolean isWrc3(String raw) {
        return "wrc3".equals(CategoryNames.key(canonical(raw)));
    }

    public static boolean same(String a, String b) {
        String ca = canonical(a);
        String cb = canonical(b);
        return !ca.isEmpty() && CategoryNames.same(ca, cb);
    }
}
