package org.example.belgianslotclubspring.models;

import java.util.List;

/**
 * Grille complète d'une boucle.
 * {@code groupSheets} = une feuille imprimable par groupe (toutes les ES de la boucle).
 * {@code manualGroups} = composition ajustée manuellement (drag & drop).
 */
public record RallyeBoucleGrid(
        int boucle,
        int groupCount,
        List<List<RallyeGridPilot>> baseGroups,
        List<RallyeGroupSheet> groupSheets,
        boolean seededFromResults,
        Integer sourceBoucle,
        int pilotsRanked,
        boolean manualGroups
) {
    public RallyeBoucleGrid(
            int boucle,
            int groupCount,
            List<List<RallyeGridPilot>> baseGroups,
            List<RallyeGroupSheet> groupSheets,
            boolean seededFromResults,
            Integer sourceBoucle,
            int pilotsRanked
    ) {
        this(boucle, groupCount, baseGroups, groupSheets, seededFromResults, sourceBoucle, pilotsRanked, false);
    }
}
