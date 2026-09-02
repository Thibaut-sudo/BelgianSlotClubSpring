package org.example.belgianslotclubspring.models;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Clubs supportés par l'application.
 * Chaque club a son propre code persistant (stocké en base) et son libellé d'affichage.
 */
public enum Club {
    SLOT4000("slot4000", "Slot 4000"),
    SRCS("srcs", "SRCS"),
    /** Championnat de Belgique des Rallyes Slot — pas un club piste. */
    SCO("sco", "Championnat de Belgique des Rallyes Slot");

    private final String code;
    private final String displayName;

    Club(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Libellé court pour le calendrier commun (légende et pastilles). */
    public String getCalendarLabel() {
        return this == SCO ? "Rallyes Slot" : displayName;
    }

    public static Optional<Club> fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).split(",")[0].trim();
        return Arrays.stream(values())
                .filter(club -> club.code.equals(normalized))
                .findFirst();
    }

    /**
     * Normalise une valeur club reçue (URL, formulaire).
     * @throws IllegalArgumentException si le club est inconnu
     */
    public static String requireCode(String raw) {
        return fromCode(raw)
                .map(Club::getCode)
                .orElseThrow(() -> new IllegalArgumentException("Club inconnu: " + raw));
    }

    public boolean isSlot4000() {
        return this == SLOT4000;
    }

    public boolean isSrcs() {
        return this == SRCS;
    }

    public boolean isRallyOnly() {
        return this == SCO;
    }
}
