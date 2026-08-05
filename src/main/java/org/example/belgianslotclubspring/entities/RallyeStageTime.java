package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "rallye_stage_time",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pilot_id", "boucle", "stage"})
)
public class RallyeStageTime {

    /** Stage 0 = colonne PENO (pénalité) de l'Excel. */
    public static final int PENALTY_STAGE = 0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pilot_id", nullable = false)
    private RallyePilot pilot;

    /** Boucle 1–4. */
    @Column(nullable = false)
    private int boucle;

    /** ES 1–5, ou 0 pour pénalité. */
    @Column(nullable = false)
    private int stage;

    /** Temps en secondes. Null = non saisi. */
    private Double timeSeconds;

    public RallyeStageTime() {
    }

    public RallyeStageTime(RallyePilot pilot, int boucle, int stage, Double timeSeconds) {
        this.pilot = pilot;
        this.boucle = boucle;
        this.stage = stage;
        this.timeSeconds = timeSeconds;
    }

    public boolean isPenalty() {
        return stage == PENALTY_STAGE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RallyeStageTime other)) {
            return false;
        }
        if (id != null && other.id != null) {
            return id.equals(other.id);
        }
        return boucle == other.boucle
                && stage == other.stage
                && pilot != null
                && other.pilot != null
                && pilot.getId() != null
                && pilot.getId().equals(other.pilot.getId());
    }

    @Override
    public int hashCode() {
        // Stable : ne pas basculer sur id après persist (casse le Set stageTimes).
        return getClass().hashCode();
    }
}
