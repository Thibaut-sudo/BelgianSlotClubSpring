package org.example.belgianslotclubspring.models;

/** Un pilote dans une case de grille (groupe / manche). */
public record RallyeGridPilot(
        Long id,
        Integer startNumber,
        String name,
        String car,
        /** Rang après la boucle précédente (1 = leader), null si boucle 1 ou non classé. */
        Integer previousRank
) {
    public RallyeGridPilot(Long id, Integer startNumber, String name, String car) {
        this(id, startNumber, name, car, null);
    }
}
