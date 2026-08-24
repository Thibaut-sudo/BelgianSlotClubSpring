package org.example.belgianslotclubspring.services.impl;

import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.ExcelFilleResult;
import org.example.belgianslotclubspring.repo.QualifRepo;
import org.example.belgianslotclubspring.repo.RaceResultRepo;
import org.example.belgianslotclubspring.services.RaceResultService;
import org.example.belgianslotclubspring.services.SaveExcelFilleService;
import org.example.belgianslotclubspring.utils.ExcelReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SaveExcelFilleServiceImpl implements SaveExcelFilleService {

    private final ExcelReader excelReader;
    private final QualifRepo qualifRepo;
    private final RaceResultRepo raceResultRepo;
    private final RaceResultService raceResultService;

    public SaveExcelFilleServiceImpl(ExcelReader excelReader, QualifRepo qualifRepo, RaceResultRepo raceResultRepo,
                                     RaceResultService raceResultService) {
        this.excelReader = excelReader;
        this.qualifRepo = qualifRepo;
        this.raceResultRepo = raceResultRepo;
        this.raceResultService = raceResultService;
    }

    @Override
    @Transactional
    public void saveExcelFile(String path, String club) {
        String clubCode = Club.requireCode(club);

        try {
            ExcelFilleResult excelFilleResult = excelReader.readRaceResults(path);

            if (excelFilleResult.getQualifs() != null && !excelFilleResult.getQualifs().isEmpty()) {
                saveQualifList(excelFilleResult.getQualifs(), clubCode);
            }

            if (excelFilleResult.getRaceResults() != null && !excelFilleResult.getRaceResults().isEmpty()) {
                saveRaceResultList(excelFilleResult.getRaceResults(), clubCode);
            } else {
                System.out.println("Aucun résultat de course à sauvegarder");
            }

            raceResultService.invalidateClubMaintenanceCaches(clubCode);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du fichier excel: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du traitement du fichier Excel", e);
        }
    }

    private void saveRaceResultList(List<RaceResult> raceResults, String clubCode) {
        for (RaceResult raceResult : raceResults) {
            raceResult.setClub(clubCode);
        }

        List<RaceResult> savedResults = raceResultRepo.saveAll(raceResults);
        raceResultRepo.flush();

        System.out.println(savedResults.size() + " résultats de course persistés pour le club: " + clubCode);
    }

    private void saveQualifList(List<Qualif> qualifs, String clubCode) {
        for (Qualif qualif : qualifs) {
            qualif.setClub(clubCode);
        }

        List<Qualif> savedQualifs = qualifRepo.saveAll(qualifs);
        qualifRepo.flush();

        System.out.println(savedQualifs.size() + " qualifications persistées pour le club: " + clubCode);
    }
}
