package org.example.belgianslotclubspring.models;

import java.util.List;

/**
 * Feuille imprimable d'un groupe : toutes les ES de la boucle, dans l'ordre de parcours.
 */
public record RallyeGroupSheet(
        int groupNumber,
        List<RallyeGridPilot> pilots,
        List<RallyeGroupEsBlock> stages,
        /** Payload QR compact pour le scan photo. */
        String qrPayload
) {
}
