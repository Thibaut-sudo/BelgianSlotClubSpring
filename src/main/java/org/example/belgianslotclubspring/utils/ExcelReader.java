package org.example.belgianslotclubspring.utils;


import org.apache.poi.ss.usermodel.*;
import org.example.belgianslotclubspring.entities.Qualif;
import org.example.belgianslotclubspring.entities.RaceResult;
import org.example.belgianslotclubspring.models.ExcelFilleResult;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelReader {

    private final List<Qualif> qualifs = new ArrayList<>();
    private final List<RaceResult> raceResults = new ArrayList<>();

    private final ExcelFilleResult excelFilleResult;

    public ExcelReader(ExcelFilleResult excelFilleResult) {
        this.excelFilleResult = excelFilleResult;
    }

    /**
     * Lit un fichier Excel et extrait les résultats de course.
     *
     * @param filePath Chemin du fichier Excel
     * @return Liste des résultats de course
     */
    public ExcelFilleResult readRaceResults(String filePath) {


        System.out.println("Fichier sélectionné : " + filePath);

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {  // Supporte XLS et XLSX

            Sheet sheet = workbook.getSheetAt(0); // Prend la première feuille


            String date = String.valueOf(sheet.getRow(0).getCell(6));


            for (int i = 3; i <= sheet.getLastRowNum(); i++) { // Ignore la première ligne (en-têtes)
                Row row = sheet.getRow(i);
                if (row == null) continue; // Ignore les lignes vides


                String QualifName = getStringCellValue(row.getCell(1));
                String QualiTime = getStringCellValue(row.getCell(2));


                if (QualifName.isEmpty() || QualiTime.isEmpty()) continue;

                Qualif qualif = new Qualif(QualifName, QualiTime, date);
                qualifs.add(qualif);


                RaceResult pilot = new RaceResult(row.getCell(4).getStringCellValue(), getDoubleCellValue(row.getCell(5)),String.valueOf(sheet.getRow(0).getCell(6)),sheet.getRow(0).getCell(0).getStringCellValue());



                pilot.addTrackPerformance(1, getIntCellValue(row.getCell(6)), getDoubleCellValue(row.getCell(7)));
                pilot.addTrackPerformance(2, getIntCellValue(row.getCell(8)), getDoubleCellValue(row.getCell(9)));
                pilot.addTrackPerformance(3, getIntCellValue(row.getCell(10)), getDoubleCellValue(row.getCell(11)));
                pilot.addTrackPerformance(4, getIntCellValue(row.getCell(12)), getDoubleCellValue(row.getCell(13)));
                pilot.addTrackPerformance(5, getIntCellValue(row.getCell(14)), getDoubleCellValue(row.getCell(15)));
                pilot.addTrackPerformance(6, getIntCellValue(row.getCell(16)), getDoubleCellValue(row.getCell(17)));

                raceResults.add(pilot);


            }
            excelFilleResult.setQualifs(qualifs);
            excelFilleResult.setRaceResults(raceResults);

            excelFilleResult.setCategoriseName(sheet.getRow(0).getCell(0).getStringCellValue());


        } catch (IOException e) {
            System.err.println("Erreur de lecture du fichier Excel : " + e.getMessage());
        }

        return excelFilleResult;
    }


    private String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            default -> "";
        };
    }

    private int getIntCellValue(Cell cell) {
        if (cell == null) return 0;
        return (int) cell.getNumericCellValue();
    }

    private double getDoubleCellValue(Cell cell) {
        if (cell == null) return 0;
        return cell.getNumericCellValue();
    }


}




