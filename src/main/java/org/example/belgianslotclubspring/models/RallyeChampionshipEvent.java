package org.example.belgianslotclubspring.models;

import java.time.LocalDate;

public record RallyeChampionshipEvent(
        long id,
        String name,
        String shortLabel,
        LocalDate date
) {
}
