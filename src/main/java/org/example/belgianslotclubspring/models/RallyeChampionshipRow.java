package org.example.belgianslotclubspring.models;

import java.util.List;

/**
 * Une ligne du championnat rallye.
 * {@code racePoints} suit l’ordre des épreuves de la table ; null = absent.
 */
public record RallyeChampionshipRow(
        int position,
        String name,
        String category,
        boolean titleEligible,
        List<Integer> racePoints,
        Integer droppedPoints,
        int countedRaceTotal,
        int presenceBonus,
        int starts,
        int total
) {
}
