package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "rallye_group_assignment",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rallye_boucle_pilot",
                columnNames = {"rallye_id", "boucle", "pilot_id"}
        )
)
public class RallyeGroupAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rallye_id", nullable = false)
    private Rallye rallye;

    @Column(nullable = false)
    private int boucle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pilot_id", nullable = false)
    private RallyePilot pilot;

    /** Groupe 1-based. */
    @Column(nullable = false)
    private int groupNumber;

    /** Ordre dans le groupe (0-based). */
    @Column(nullable = false)
    private int positionInGroup;

    public RallyeGroupAssignment() {
    }

    public RallyeGroupAssignment(Rallye rallye, int boucle, RallyePilot pilot, int groupNumber, int positionInGroup) {
        this.rallye = rallye;
        this.boucle = boucle;
        this.pilot = pilot;
        this.groupNumber = groupNumber;
        this.positionInGroup = positionInGroup;
    }
}
