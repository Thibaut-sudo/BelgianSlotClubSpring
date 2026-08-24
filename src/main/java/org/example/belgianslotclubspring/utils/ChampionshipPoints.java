package org.example.belgianslotclubspring.utils;

/**
 * Barème officiel Slot 4000 : points selon la position à l'arrivée.
 * <pre>
 * 1→50, 2→40, 3→35, 4→32, 5→30, 6→28 … 10→20,
 * 11→19 … 28→2, 29 et plus → 1
 * </pre>
 */
public final class ChampionshipPoints {

    private static final int[] BY_POSITION = {
            50, 40, 35, 32, 30, 28, 26, 24, 22, 20,
            19, 18, 17, 16, 15, 14, 13, 12, 11, 10,
            9, 8, 7, 6, 5, 4, 3, 2
    };

    private ChampionshipPoints() {
    }

    /**
     * @param zeroBasedIndex 0 = 1<sup>re</sup> place, 1 = 2<sup>e</sup>, …
     */
    public static int forRankIndex(int zeroBasedIndex) {
        if (zeroBasedIndex < 0) {
            return 0;
        }
        if (zeroBasedIndex < BY_POSITION.length) {
            return BY_POSITION[zeroBasedIndex];
        }
        return 1;
    }

    /** Position 1-based (1 = vainqueur). */
    public static int forPosition(int position) {
        return forRankIndex(position - 1);
    }
}
