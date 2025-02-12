package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class LapsPerTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int trackNumber;
    private int laps;

    private LocalDate raceDate; // Ajout de la référence à la date

    public LapsPerTrack(int trackNumber, int laps, LocalDate raceDate) {
        this.trackNumber = trackNumber;
        this.laps = laps;
        this.raceDate = raceDate;
    }
    public LapsPerTrack() {

    }

    @Override
    public String toString() {
        return "LapsPerTrack{" +
                "trackNumber=" + trackNumber +
                ", laps=" + laps +
                ", raceDate=" + raceDate +
                '}';
    }
}
