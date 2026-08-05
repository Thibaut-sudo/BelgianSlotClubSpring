package org.example.belgianslotclubspring.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


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
        this.bestTime = Float.parseFloat(qualiTime);
        this.pilotName = qualifName;
    }

    public Qualif(String qualifName, String qualiTime, String date) {
        this.bestTime = Float.parseFloat(qualiTime);
        this.pilotName = qualifName;
        this.date = convertStringToLocalDate(date);
    }

    public Qualif() {
    }

    public void setClub(String club) {
        this.clubName = club;
    }

    private static LocalDate convertStringToLocalDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.FRENCH);
        return LocalDate.parse(dateString, formatter);
    }
}
