package org.example.belgianslotclubspring.services.impl;

import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.models.ExcelFilleResult;
import org.example.belgianslotclubspring.repo.QualifRepo;
import org.example.belgianslotclubspring.repo.RaceResultRepo;
import org.example.belgianslotclubspring.services.SaveExcelFilleService;
import org.example.belgianslotclubspring.utils.ExcelReader;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SaveExcelFilleServiceImpl implements SaveExcelFilleService {

    private final ExcelReader excelReader;
    private final QualifRepo qualifRepo;
    private final RaceResultRepo raceResultRepo;

    public SaveExcelFilleServiceImpl(ExcelReader excelReader, QualifRepo qualifRepo, RaceResultRepo raceResultRepo) {
        this.excelReader = excelReader;
        this.qualifRepo = qualifRepo;
        this.raceResultRepo = raceResultRepo;
    }


    @Override
    public void saveExcelFile(String path, String club) {

        try{ExcelFilleResult excelFilleResult = excelReader.readRaceResults(path);
            saveQualifList(excelFilleResult.getQualifs());
            saveRaceResultList(excelFilleResult.getRaceResults(),club);
        }catch (Exception e)
        {
            System.out.println("Erreur lors de la lecture du fichier excel " + e.getMessage());
        }





    }

    private void saveRaceResultList(List<RaceResult> raceResults, String club) {
        for (RaceResult raceResult : raceResults) {
            raceResult.setClub(club);
        }
        raceResultRepo.saveAll(raceResults);
    }

    private void saveQualifList(List<Qualif> qualifs) {
        qualifRepo.saveAll(qualifs);
    }
}
