package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.belgianslotclubspring.models.Club;

/**
 * Catégorie ajoutée par un organisateur (légende + couleur du calendrier).
 */
@Getter
@Setter
@Entity
@Table(
        name = "club_calendar_category",
        uniqueConstraints = @UniqueConstraint(columnNames = {"club_name", "name"})
)
public class ClubCalendarCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_name", nullable = false, length = 32)
    private String clubName;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 7)
    private String color;

    public ClubCalendarCategory() {
    }

    public ClubCalendarCategory(Club club, String name, String color) {
        this.clubName = club.getCode();
        this.name = name;
        this.color = color;
    }
}
