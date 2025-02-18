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

/**
 * Classe utilitaire pour la lecture et l'extraction des données d'un fichier Excel.
 * Cette classe utilise Apache POI pour manipuler les fichiers Excel (.xls et .xlsx).
 */
@Component
public class ExcelReader {

    private final List<Qualif> qualifs = new ArrayList<>();
    private final List<RaceResult> raceResults = new ArrayList<>();
    private final ExcelFilleResult excelFilleResult;

    /**
     * Constructeur de la classe ExcelReader.
     *
     * @param excelFilleResult Objet contenant les résultats extraits du fichier Excel.
     */
    public ExcelReader(ExcelFilleResult excelFilleResult) {
        this.excelFilleResult = excelFilleResult;
    }

    /**
     * Lit un fichier Excel et extrait les résultats des courses et qualifications.
     *
     * @param filePath Chemin du fichier Excel à lire.
     * @return Un objet {@link ExcelFilleResult} contenant les données extraites.
     */
    public ExcelFilleResult readRaceResults(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {  // Compatible avec XLS et XLSX

            Sheet sheet = workbook.getSheetAt(0); // Lecture de la première feuille

            // Récupération de la date de la course depuis la cellule concernée
            String date = String.valueOf(sheet.getRow(0).getCell(6));

            // Parcours des lignes du fichier Excel, en commençant après les en-têtes
            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue; // Ignore les lignes vides

                // Extraction des données de qualification
                String qualifName = getStringCellValue(row.getCell(1));
                String qualifTime = getStringCellValue(row.getCell(2));

                // Vérification que les valeurs ne sont pas vides avant d'ajouter
                if (!qualifName.isEmpty() && !qualifTime.isEmpty()) {
                    Qualif qualif = new Qualif(qualifName, qualifTime, date);
                    qualifs.add(qualif);
                }

                // Extraction des données de course
                RaceResult pilot = new RaceResult(
                        row.getCell(4).getStringCellValue(),
                        getDoubleCellValue(row.getCell(5)),
                        String.valueOf(sheet.getRow(0).getCell(6)),
                        sheet.getRow(0).getCell(0).getStringCellValue()
                );

                // Ajout des performances sur chaque piste
                for (int track = 1, col = 6; track <= 6; track++, col += 2) {
                    pilot.addTrackPerformance(track, getIntCellValue(row.getCell(col)), getDoubleCellValue(row.getCell(col + 1)));
                }

                raceResults.add(pilot);
            }

            // Mise à jour de l'objet contenant les résultats
            excelFilleResult.setQualifs(qualifs);
            excelFilleResult.setRaceResults(raceResults);
            excelFilleResult.setCategoriseName(sheet.getRow(0).getCell(0).getStringCellValue());

        } catch (IOException e) {
            System.err.println("Erreur de lecture du fichier Excel : " + e.getMessage());
        }

        return excelFilleResult;
    }

    /**
     * Récupère la valeur d'une cellule sous forme de chaîne de caractères.
     *
     * @param cell La cellule à lire.
     * @return La valeur de la cellule en String.
     */
    private String getStringCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            default -> "";
        };
    }

    /**
     * Récupère la valeur d'une cellule sous forme d'entier.
     *
     * @param cell La cellule à lire.
     * @return La valeur entière de la cellule.
     */
    private int getIntCellValue(Cell cell) {
        if (cell == null) return 0;
        return (int) cell.getNumericCellValue();
    }

    /**
     * Récupère la valeur d'une cellule sous forme de nombre décimal.
     *
     * @param cell La cellule à lire.
     * @return La valeur décimale de la cellule.
     */
    private double getDoubleCellValue(Cell cell) {
        if (cell == null) return 0;
        return cell.getNumericCellValue();
    }
}
