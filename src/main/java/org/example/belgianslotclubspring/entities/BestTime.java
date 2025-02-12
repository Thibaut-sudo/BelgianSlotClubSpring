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
public class BestTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int trackNumber;
    private double bestLapTime;

    private LocalDate raceDate; // Ajout de la référence à la date

    public BestTime(int trackNumber, double bestLapTime, LocalDate raceDate) {
        this.trackNumber = trackNumber;
        this.bestLapTime = bestLapTime;
        this.raceDate = raceDate;
    }
    public BestTime() {
    }

    @Override
    public String toString() {
        return "BestTime{" +
                "trackNumber=" + trackNumber +
                ", bestLapTime=" + bestLapTime +
                ", raceDate=" + raceDate +
                '}';
    }
}
