package org.example.belgianslotclubspring.models;

import java.util.List;

/** Un groupe sur une manche : liste de pilotes + ES à courir. */
public record RallyeGroupBlock(
        int groupNumber,
        int esNumber,
        List<RallyeGridPilot> pilots
) {
}
