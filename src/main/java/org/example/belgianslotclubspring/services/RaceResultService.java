package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.models.RaceSummary;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public interface RaceResultService {
    void saveRaceResult(RaceResult raceResult);
    void updateRaceResult(RaceResult raceResult);
    void deleteRaceResult(RaceResult raceResult);
    RaceResult findRaceResultById(int id);
    Map<LocalDate, String> getRaceResultDate();

    /** @deprecated utiliser {@link #getRaceResultByDateAndClub(LocalDate, String)} */
    @Deprecated
    List<RaceResult> getRaceResultByDate(LocalDate date);

    List<RaceResult> getRaceResultByDateAndClub(LocalDate date, String club);

    /** Résultats d'une course (date + club + catégorie normalisée). */
    List<RaceResult> getRaceResultByDateClubAndCategory(LocalDate date, String club, String category);

    /** Liste des courses d'un club (date + catégorie), sans collision. */
    List<RaceSummary> getRaceSummariesByClub(String club);

    /** @deprecated utiliser {@link #getRaceSummariesByClub(String)} */
    @Deprecated
    Map<LocalDate, String> getRaceResultDateByClub(String club);

    List<String> getAllCategories();

    List<String> getRaceDates(String cathegorie, Integer year);

    List<String> getAllCategoriesClub(String club);

    List<String> getAllCategoriesClub(String club, Integer year);

    Map<LocalDate, Map<String, Double>> getChampionshipResults(String category, String club, Integer year);

    List<String> getAllYearsClub(String club);

    List<Integer> getAvailableYears(String club);

    /** Supprime une course (résultats + qualifs si plus rien pour cette date). */
    void deleteRace(LocalDate date, String club, String category);

    /** Invalide les caches de maintenance et des pages championnat après import Excel. */
    void invalidateClubMaintenanceCaches(String club);
}
