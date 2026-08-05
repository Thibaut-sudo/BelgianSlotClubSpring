package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "rallye_pilot")
public class RallyePilot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rallye_id", nullable = false)
    private Rallye rallye;

    @Column(nullable = false)
    private String name;

    private String car;

    private String category;

    private Integer startNumber;

    /** Set (pas List) pour permettre le JOIN FETCH avec Rallye.pilots sans MultipleBagFetchException. */
    @OneToMany(mappedBy = "pilot", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RallyeStageTime> stageTimes = new LinkedHashSet<>();

    public RallyePilot() {
    }

    public RallyePilot(String name, String car, String category, Integer startNumber) {
        this.name = name;
        this.car = car;
        this.category = category;
        this.startNumber = startNumber;
    }

    public Optional<RallyeStageTime> findTime(int boucle, int stage) {
        return stageTimes.stream()
                .filter(t -> t.getBoucle() == boucle && t.getStage() == stage)
                .findFirst();
    }

    public RallyeStageTime getOrCreateTime(int boucle, int stage) {
        return findTime(boucle, stage).orElseGet(() -> {
            RallyeStageTime t = new RallyeStageTime(this, boucle, stage, null);
            stageTimes.add(t);
            return t;
        });
    }

    /** Temps d'une ES (stage 1–5). Null si non couru. */
    public Double getStageSeconds(int boucle, int stage) {
        return findTime(boucle, stage).map(RallyeStageTime::getTimeSeconds).orElse(null);
    }

    /** Pénalité (stage 0) pour une boucle. */
    public Double getPenaltySeconds(int boucle) {
        return getStageSeconds(boucle, RallyeStageTime.PENALTY_STAGE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RallyePilot other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
