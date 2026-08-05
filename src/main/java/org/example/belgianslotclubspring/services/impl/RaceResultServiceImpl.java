package org.example.belgianslotclubspring.services.impl;

import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.RaceSummary;
import org.example.belgianslotclubspring.repo.RaceResultRepo;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RaceResultServiceImpl implements RaceResultService {

    private final RaceResultRepo raceResultRepo;

    public RaceResultServiceImpl(RaceResultRepo raceResultRepo) {
        this.raceResultRepo = raceResultRepo;
    }

    @Override
    public void saveRaceResult(RaceResult raceResult) {
    }

    @Override
    public void updateRaceResult(RaceResult raceResult) {
    }

    @Override
    public void deleteRaceResult(RaceResult raceResult) {
    }

    @Override
    public RaceResult findRaceResultById(int id) {
        return null;
    }

    @Override
    public Map<LocalDate, String> getRaceResultDate() {
        return Map.of();
    }

    @Override
    @Deprecated
    public List<RaceResult> getRaceResultByDate(LocalDate date) {
        throw new UnsupportedOperationException("Utiliser getRaceResultByDateAndClub(date, club)");
    }

    @Override
    public List<RaceResult> getRaceResultByDateAndClub(LocalDate date, String club) {
        String clubCode = Club.requireCode(club);
        return raceResultRepo.getRaceResultByDateAndClub(date, clubCode);
    }

    @Override
    public List<RaceSummary> getRaceSummariesByClub(String club) {
        String clubCode = Club.requireCode(club);
        List<Object[]> results = raceResultRepo.getRaceResultDateByClubName(clubCode);

        return results.stream()
                .map(row -> new RaceSummary((LocalDate) row[0], (String) row[1], clubCode))
                .collect(Collectors.toList());
    }

    @Override
    @Deprecated
    public Map<LocalDate, String> getRaceResultDateByClub(String club) {
        // Conservé pour compatibilité : une seule catégorie par date (dernière vue)
        return getRaceSummariesByClub(club).stream()
                .collect(Collectors.toMap(
                        RaceSummary::date,
                        RaceSummary::category,
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new
                ));
    }

    @Override
    public List<String> getAllCategories() {
        return List.of();
    }

    @Override
    public List<String> getRaceDates(String cathegorie, Integer year) {
        return List.of();
    }

    @Override
    public List<String> getAllCategoriesClub(String club) {
        String clubCode = Club.requireCode(club);
        return raceResultRepo.getAllCategoriesClub(clubCode);
    }

    @Override
    public Map<LocalDate, Map<String, Double>> getChampionshipResults(String category, String club, Integer year) {
        String clubCode = Club.requireCode(club);
        List<Object[]> results = raceResultRepo.getChampionshipResultsRaw(category, clubCode, year);

        Map<LocalDate, Map<String, Double>> championshipResults = new LinkedHashMap<>();

        for (Object[] row : results) {
            LocalDate raceDate = (LocalDate) row[0];
            String pilot = (String) row[1];
            Double totalTours = (Double) row[2];

            championshipResults
                    .computeIfAbsent(raceDate, k -> new LinkedHashMap<>())
                    .put(pilot, totalTours);
        }

        for (Map<String, Double> raceResults : championshipResults.values()) {
            updateRaceResultsWithStar(raceResults);
        }

        return championshipResults;
    }

    @Override
    public List<String> getAllYearsClub(String club) {
        return getAvailableYears(club).stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public List<Integer> getAvailableYears(String club) {
        String clubCode = Club.requireCode(club);
        return raceResultRepo.getAvailableYears(clubCode);
    }

    private static void updateRaceResultsWithStar(Map<String, Double> raceResults) {
        int[] pointsTable = {
                50, 40, 35, 32, 30, 28, 26, 24, 22, 20,
                19, 18, 17, 16, 15, 14, 13, 12, 11, 10,
                9, 8, 7, 6, 5, 4, 3, 2
        };

        List<Map.Entry<String, Double>> normalList = new ArrayList<>();
        List<Map.Entry<String, Double>> starList = new ArrayList<>();

        for (Map.Entry<String, Double> entry : raceResults.entrySet()) {
            if (entry.getKey().contains("*")) {
                starList.add(entry);
            } else {
                normalList.add(entry);
            }
        }

        normalList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        starList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        Map<String, Double> updatedResults = new LinkedHashMap<>();

        for (int i = 0; i < normalList.size(); i++) {
            String pilot = normalList.get(i).getKey();
            int points = (i < pointsTable.length) ? pointsTable[i] : 1;
            updatedResults.put(pilot, (double) points);
        }

        for (int i = 0; i < starList.size(); i++) {
            String pilot = starList.get(i).getKey();
            int points = (i < pointsTable.length) ? pointsTable[i] : 1;
            updatedResults.put(pilot, (double) points);
        }

        raceResults.clear();
        raceResults.putAll(updatedResults);
    }
}
