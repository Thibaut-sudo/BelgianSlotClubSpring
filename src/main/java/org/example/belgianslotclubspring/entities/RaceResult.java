package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.utils.CategoryNames;
import org.example.belgianslotclubspring.utils.RaceDateParse;

import java.time.LocalDate;
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

    /** Code club (slot4000 | srcs). Champ historique — ne pas renommer (compatibilité schéma H2). */
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
        this.categoryName = CategoryNames.canonical(categoryName);
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = CategoryNames.canonical(categoryName);
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void addTrackPerformance(int trackNumber, int laps, double bestTime) {

        this.lapsPerTrack.add(new LapsPerTrack(trackNumber, laps, this.date));
        this.bestTime.add(new BestTime(trackNumber, bestTime, this.date));

    }

    @Override
    public String toString() {
        return "RaceResult{nom='" + nom + "', totalTours=" + totalTours + ", lapsPerTrack=" + lapsPerTrack + ", bestTimes=" + bestTime + "}";
    }

    private static LocalDate convertStringToLocalDate(String dateString) {
        return RaceDateParse.parse(dateString);
    }


    public void setClub(String club) {
        this.ClubName = Club.requireCode(club);
    }
}

