package org.example.belgianslotclubspring.models;

import java.util.List;

/** Une manche = une rotation : chaque groupe court une ES différente. */
public record RallyeManche(
        int mancheNumber,
        List<RallyeGroupBlock> groups
) {
}
