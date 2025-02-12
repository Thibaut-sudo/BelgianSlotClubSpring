package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Getter
@Entity
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter
    private Long id;

    private String nom;
    private double totalTours;
    @Setter
    private LocalDate date;
    private String categoryName;

    private String ClubName;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "race_result_id")
    private List<LapsPerTrack> lapsPerTrack = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "race_result_id")
    private List<BestTime> bestTime = new ArrayList<>();

    public RaceResult() {
    }

    public RaceResult(String stringCellValue, double doubleCellValue, String date, String categoryName) {
        this.nom = stringCellValue;
        this.totalTours = doubleCellValue;
        this.date = convertStringToLocalDate(date);
        this.categoryName = categoryName;
    }

    public void addTrackPerformance(int trackNumber, int laps, double bestTime) {
        System.out.println(this.date);
        this.lapsPerTrack.add(new LapsPerTrack(trackNumber, laps, this.date));
        this.bestTime.add(new BestTime(trackNumber, bestTime, this.date));

    }

    @Override
    public String toString() {
        return "RaceResult{" +
                "nom='" + nom + '\'' +
                ", totalTours=" + totalTours +
                ", lapsPerTrack=" + lapsPerTrack +
                ", bestTimes=" + bestTime +
                '}';
    }

    private static LocalDate convertStringToLocalDate(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.FRENCH);
        return LocalDate.parse(dateString, formatter);
    }


    public void setClub(String club) {
        this.ClubName = club;
    }
}

