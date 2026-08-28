package org.example.belgianslotclubspring.utils;

/**
 * Barème officiel du Championnat de Belgique des Rallyes Slot.
 * 1er 24 · 2e 22 · 3e 20 · puis −1 par place jusqu’à 1.
 * +1 au vainqueur scratch. Bonus présence : 2 pts / départ, 4 pts au 6e.
 * Sur 6 manches, seuls les 5 meilleurs résultats comptent (le bonus n’est pas jeté).
 */
public final class RallyeChampionshipPoints {

    public static final int RALLIES_IN_SEASON = 6;
    public static final int COUNTED_RESULTS = 5;

    private RallyeChampionshipPoints() {
    }

    public static int forCategoryPlace(int place) {
        if (place <= 0) {
            return 0;
        }
        if (place == 1) {
            return 24;
        }
        if (place == 2) {
            return 22;
        }
        if (place == 3) {
            return 20;
        }
        return Math.max(23 - place, 1);
    }

    public static int withScratchBonus(int categoryPoints, boolean overallWinner) {
        return categoryPoints + (overallWinner ? 1 : 0);
    }

    /** 2 points par départ ; le 6e départ vaut 4 points au lieu de 2. */
    public static int presenceBonus(int starts) {
        if (starts <= 0) {
            return 0;
        }
        int sixth = starts >= 6 ? 2 : 0;
        return 2 * starts + sixth;
    }

    /**
     * Somme des résultats de course, en jetant le plus mauvais si au moins 6 manches
     * ont été organisées. {@code racePoints} peut contenir des 0 (absent).
     */
    public static int countedRaceTotal(int[] racePoints, int ralliesOrganised) {
        if (racePoints == null || racePoints.length == 0) {
            return 0;
        }
        int sum = 0;
        int worst = Integer.MAX_VALUE;
        int counted = 0;
        for (int pts : racePoints) {
            sum += pts;
            counted++;
            if (pts < worst) {
                worst = pts;
            }
        }
        if (ralliesOrganised >= RALLIES_IN_SEASON && counted > COUNTED_RESULTS && worst != Integer.MAX_VALUE) {
            return sum - worst;
        }
        return sum;
    }

    public static boolean dropsWorst(int ralliesOrganised) {
        return ralliesOrganised >= RALLIES_IN_SEASON;
    }
}
