package org.example.belgianslotclubspring.models;

import java.util.List;

/** Compte-rendu final du rallye. */
public record RallyeFinalRecap(
        boolean hasData,
        String championName,
        String championTime,
        List<String> podiumLines,
        List<RallyeScratchTally> scratchLeaders,
        List<String> headlines
) {
}
