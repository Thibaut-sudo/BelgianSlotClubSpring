package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.belgianslotclubspring.models.Club;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "rallye")
public class Rallye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate date;

    /** Code club (slot4000 | srcs). */
    @Column(name = "club_name", nullable = false)
    private String clubName;

    /** Nombre de boucles = passages complets du rallye (défaut 4). */
    private int boucleCount = 4;

    /** Nombre d'ES = épreuves spéciales distinctes (défaut 5). Indépendant des boucles. */
    private int stagesPerBoucle = 5;

    /** Une fois terminé, plus aucune modification (pilotes, temps, grilles). */
    @Column(nullable = false)
    private boolean finished = false;

    /**
     * Set (pas List) : avec JOIN FETCH des stageTimes, une List dupliquait chaque pilote
     * une fois par temps saisi.
     */
    @OneToMany(mappedBy = "rallye", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startNumber ASC, id ASC")
    private Set<RallyePilot> pilots = new LinkedHashSet<>();

    public Rallye() {
    }

    public Rallye(String name, LocalDate date, String club) {
        this.name = name;
        this.date = date;
        this.clubName = Club.requireCode(club);
    }

    public void addPilot(RallyePilot pilot) {
        pilots.add(pilot);
        pilot.setRallye(this);
    }

    public int totalStages() {
        return boucleCount * stagesPerBoucle;
    }

    /**
     * Rallye-cross / résultat d’archive : une seule place par pilote, sans saisie ES.
     */
    public boolean isClassementOnly() {
        return boucleCount == 1 && stagesPerBoucle == 1;
    }
}
