package org.example.belgianslotclubspring.models;

import java.util.List;

/** Tous les compte-rendus d'un rallye (boucles démarrées + final si terminé). */
public record RallyeRecaps(
        List<RallyeBoucleRecap> boucles,
        RallyeFinalRecap finale
) {
    public boolean hasAnything() {
        return (boucles != null && !boucles.isEmpty())
                || (finale != null && finale.hasData());
    }
}
