package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.RaceResult;
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

    List<RaceResult> getRaceResultByDate(LocalDate date);

    Map<LocalDate, String> getRaceResultDateByClub(String club);

    List<String> getAllCategories();

    List<String> getRaceDates(String cathegorie);

    List<String> getAllCategoriesClub(String club);

    Map<LocalDate, Map<String, Double>> getChampionshipResults(String category, String club);
}
