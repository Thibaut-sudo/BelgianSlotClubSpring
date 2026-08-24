package org.example.belgianslotclubspring.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import org.example.belgianslotclubspring.utils.RaceDateParse;

import java.time.LocalDate;


@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Entity
@Getter
@Setter
public class Qualif {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private float bestTime;

    @Getter
    private String pilotName;
    private LocalDate date;

    /** Code club (slot4000 | srcs) — isole les qualifications par club. */
    @Column(name = "club_name")
    private String clubName;

    public Qualif(String qualifName, String qualiTime) {
        this.bestTime = parseQualiTime(qualiTime);
        this.pilotName = qualifName;
    }

    public Qualif(String qualifName, String qualiTime, String date) {
        this.bestTime = parseQualiTime(qualiTime);
        this.pilotName = qualifName;
        this.date = RaceDateParse.parse(date);
    }

    public Qualif() {
    }

    public void setClub(String club) {
        this.clubName = club;
    }

    private static float parseQualiTime(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Temps de qualification vide.");
        }
        String value = raw.trim().replace(',', '.');
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Temps de qualification illisible (« " + raw + " »).", e);
        }
    }
}
