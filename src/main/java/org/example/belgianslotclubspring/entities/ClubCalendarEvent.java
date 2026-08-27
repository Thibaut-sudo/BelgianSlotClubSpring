package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.belgianslotclubspring.models.Club;

import java.time.LocalDate;

/**
 * Événement ajouté par un organisateur (superpose le calendrier officiel du club).
 */
@Getter
@Setter
@Entity
@Table(
        name = "club_calendar_event",
        uniqueConstraints = @UniqueConstraint(columnNames = {"club_name", "event_date"})
)
public class ClubCalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_name", nullable = false, length = 32)
    private String clubName;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false, length = 80)
    private String name;

    public ClubCalendarEvent() {
    }

    public ClubCalendarEvent(Club club, LocalDate eventDate, String name) {
        this.clubName = club.getCode();
        this.eventDate = eventDate;
        this.name = name;
    }
}
