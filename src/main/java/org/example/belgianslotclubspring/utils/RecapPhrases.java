package org.example.belgianslotclubspring.utils;

/**
 * Choisit une tournure parmi plusieurs options de façon déterministe
 * (même données → même texte), pour éviter la répétition d’analyse en analyse.
 */
public final class RecapPhrases {

    private RecapPhrases() {
    }

    public static int seed(Object... parts) {
        int h = 17;
        for (Object p : parts) {
            h = 31 * h + (p == null ? 0 : p.hashCode());
        }
        return h;
    }

    public static String pick(int seed, String... options) {
        if (options == null || options.length == 0) {
            return "";
        }
        return options[Math.floorMod(seed, options.length)];
    }

    /** Concatène un préfixe choisi + le reste (évite les templates trop figés). */
    public static String line(int seed, String body, String... openings) {
        if (openings == null || openings.length == 0) {
            return body;
        }
        return pick(seed, openings) + body;
    }

    /** Choisit un canevas (liste d’ids de « beats ») de façon déterministe. */
    @SafeVarargs
    public static String[] pickTemplate(int seed, String[]... templates) {
        if (templates == null || templates.length == 0) {
            return new String[0];
        }
        return templates[Math.floorMod(seed, templates.length)];
    }
}
