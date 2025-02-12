package org.example.belgianslotclubspring.services.impl;

import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.repo.RaceResultRepo;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
        return List.of();
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
        return Map.of();
    }
}
