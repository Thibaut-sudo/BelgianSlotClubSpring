package org.example.belgianslotclubspring.utils;

/**
 * Détection catégorie bis (pilotes marqués d'une étoile dans le fichier chrono).
 */
public final class PilotNames {

    private PilotNames() {
    }

    public static boolean isBis(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String t = name.trim();
        return t.contains("*") || t.contains("∗") || t.contains("＊");
    }

    /** Nom sans marque bis, pour rapprochement quali ↔ course. */
    public static String baseName(String name) {
        if (name == null) {
            return "";
        }
        String t = name.trim()
                .replace("＊", "")
                .replace("∗", "")
                .replace("*", "")
                .trim()
                .replaceAll("\\s+", " ");
        return t;
    }

    /** Conserve le libellé de course, ajoute « * » si le pilote est bis. */
    public static String withBisMarker(String raceName, boolean bis) {
        String base = baseName(raceName);
        if (base.isEmpty()) {
            return raceName == null ? "" : raceName.trim();
        }
        if (!bis) {
            return base;
        }
        return base + " *";
    }
}
