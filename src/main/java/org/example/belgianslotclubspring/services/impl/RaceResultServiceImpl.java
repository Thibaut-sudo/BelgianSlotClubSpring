package org.example.belgianslotclubspring.services.impl;

import org.example.belgianslotclubspring.entities.RaceResult;
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

    private RaceResultRepo raceResultRepo;

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
    public List<RaceResult> getRaceResultByDate(LocalDate date) {
        return raceResultRepo.getRaceResultByDate(date);

    }


    @Override
    public Map<LocalDate, String> getRaceResultDateByClub(String club) {
        List<Object[]> results = raceResultRepo.getRaceResultDateByClubName(club);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> (LocalDate) row[0], // Date
                        row -> (String) row[1] // ClubCathegorie
                ));
    }


    @Override
    public List<String> getAllCategories() {
        return List.of();
    }

    @Override
    public List<String> getRaceDates(String cathegorie) {
        return List.of();
    }

    @Override
    public List<String> getAllCategoriesClub(String club) {

        return raceResultRepo.getAllCategoriesClub(club);
    }

    @Override
    public Map<LocalDate, Map<String, Double>> getChampionshipResults(String category) {
        List<Object[]> results = raceResultRepo.getChampionshipResultsRaw(category);

        Map<LocalDate, Map<String, Double>> championshipResults = new LinkedHashMap<>();

        for (Object[] row : results) {
            LocalDate raceDate = (LocalDate) row[0];
            String pilot = (String) row[1];
            Double totalTours = (Double) row[2];

            // Ajout des résultats dans la map
            championshipResults
                    .computeIfAbsent(raceDate, k -> new LinkedHashMap<>())
                    .put(pilot, totalTours);
        }

        // Mettre à jour les résultats en attribuant les points
        for (Map<String, Double> raceResults : championshipResults.values()) {
            updateRaceResults(raceResults);
        }

        return championshipResults;
    }



    private static void updateRaceResults(Map<String, Double> raceResults) {
        int[] pointsTable = {
                50, 40, 35, 32, 30, 28, 26, 24, 22, 20,
                19, 18, 17, 16, 15, 14, 13, 12, 11, 10,
                9, 8, 7, 6, 5, 4, 3, 2
        };

        // Trier les pilotes par nombre de tours effectués (ordre décroissant)
        List<Map.Entry<String, Double>> sortedList = new ArrayList<>(raceResults.entrySet());
        sortedList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Map temporaire pour stocker les résultats mis à jour
        Map<String, Double> updatedResults = new LinkedHashMap<>();

        // Attribution des points
        for (int i = 0; i < sortedList.size(); i++) {
            String pilot = sortedList.get(i).getKey();
            int points = (i < pointsTable.length) ? pointsTable[i] : 1; // Si au-delà de la 28ᵉ place → 1 point
            updatedResults.put(pilot, (double) points);
        }

        // Mettre à jour la Map d'origine
        raceResults.clear();
        raceResults.putAll(updatedResults);


    }
}