package org.example.belgianslotclubspring.models;

import java.time.LocalDate;

/**
 * Résumé d'une course pour un club donné (date + catégorie).
 * Évite les collisions quand deux catégories ont lieu le même jour.
 */
public record RaceSummary(LocalDate date, String category, String club) {
}
