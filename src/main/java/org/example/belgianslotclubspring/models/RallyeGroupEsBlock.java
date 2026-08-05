package org.example.belgianslotclubspring.models;

import java.util.List;

/**
 * Une ES sur la feuille d'un groupe : le groupe y passe à {@code passageOrder}.
 */
public record RallyeGroupEsBlock(
        int esNumber,
        int passageOrder,
        List<RallyeGridPilot> pilots
) {
}
