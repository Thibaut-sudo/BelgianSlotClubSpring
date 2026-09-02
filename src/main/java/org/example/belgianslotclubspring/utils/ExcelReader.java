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
        qualifs.clear();
        raceResults.clear();

        System.out.println("Fichier sélectionné : " + filePath);

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(fis)) {

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) {
                throw new IllegalArgumentException("Fichier Excel vide (pas d’en-tête).");
            }

            String date = readRaceDate(header.getCell(6), evaluator);
            String categoryName = CategoryNames.canonical(getStringCellValue(header.getCell(0), evaluator));

            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String qualifName = getStringCellValue(row.getCell(1), evaluator);
                String qualiTime = getStringCellValue(row.getCell(2), evaluator);
                String raceNameRaw = getStringCellValue(row.getCell(4), evaluator);
                double totalLaps = getDoubleCellValue(row.getCell(5));

                boolean hasQualif = isUsablePilotName(qualifName) && isUsableQualiTime(qualiTime);
                if (!isUsablePilotName(raceNameRaw) || looksLikeFormula(raceNameRaw)) {
                    raceNameRaw = PilotNames.baseName(qualifName);
                }
                boolean hasRace = isUsablePilotName(raceNameRaw) && totalLaps > 0.0001;

                // Les feuilles club ont parfois les temps de qualif (colonne C) vides
                // alors que le classement course (E–R) est déjà calculé.
                if (!hasQualif && !hasRace) {
                    continue;
                }

                if (hasQualif) {
                    qualifs.add(new Qualif(qualifName, qualiTime, date));
                }

                if (!isUsablePilotName(raceNameRaw)) {
                    continue;
                }

                boolean bis = PilotNames.isBis(qualifName) || PilotNames.isBis(raceNameRaw);
                String raceName = PilotNames.withBisMarker(raceNameRaw, bis);

                RaceResult pilot = new RaceResult(
                        raceName,
                        totalLaps,
                        String.valueOf(date),
                        categoryName
                );

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
            excelFilleResult.setCategoriseName(categoryName);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new IllegalArgumentException("Impossible de lire le fichier Excel : " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Fichier Excel illisible : " + e.getMessage(), e);
        }

        return excelFilleResult;
    }

    private String readRaceDate(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            throw new IllegalArgumentException("Date de course manquante (cellule G1).");
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.NUMERIC) {
            try {
                if (DateUtil.isCellDateFormatted(cell) || DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
            } catch (Exception ignored) {
                // fallback texte / série ci-dessous
            }
            return String.valueOf(cell.getNumericCellValue());
        }
        String text = getStringCellValue(cell, evaluator);
        if (text.isBlank()) {
            throw new IllegalArgumentException("Date de course vide (cellule G1).");
        }
        return text;
    }

    private static boolean looksLikeFormula(String value) {
        return value.contains("!") || value.startsWith("=") || value.contains("$");
    }

    static boolean isUsablePilotName(String name) {
        if (name == null) {
            return false;
        }
        String value = name.trim();
        if (value.isEmpty() || "-".equals(value) || "—".equals(value)) {
            return false;
        }
        if (looksLikeFormula(value)) {
            return false;
        }
        return !value.matches("0+(\\.0+)?");
    }

    static boolean isUsableQualiTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String value = raw.trim().replace(',', '.');
        try {
            Float.parseFloat(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String getStringCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case FORMULA -> {
                try {
                    if (cell.getCachedFormulaResultType() == CellType.STRING) {
                        yield cell.getRichStringCellValue().getString().trim();
                    }
                    if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                        yield String.valueOf(cell.getNumericCellValue());
                    }
                    CellValue evaluated = evaluator.evaluate(cell);
                    if (evaluated == null) {
                        yield "";
                    }
                    yield switch (evaluated.getCellType()) {
                        case STRING -> evaluated.getStringValue() == null ? "" : evaluated.getStringValue().trim();
                        case NUMERIC -> String.valueOf(evaluated.getNumberValue());
                        default -> "";
                    };
                } catch (Exception e) {
                    yield "";
                }
            }
            default -> "";
        };
    }

    private int getIntCellValue(Cell cell) {
        return (int) Math.round(getDoubleCellValue(cell));
    }

    private double getDoubleCellValue(Cell cell) {
        if (cell == null) {
            return 0;
        }
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> cell.getNumericCellValue();
                case FORMULA -> {
                    if (cell.getCachedFormulaResultType() == CellType.NUMERIC) {
                        yield cell.getNumericCellValue();
                    }
                    if (cell.getCachedFormulaResultType() == CellType.STRING) {
                        yield parseLooseDouble(cell.getRichStringCellValue().getString());
                    }
                    yield 0;
                }
                case STRING -> parseLooseDouble(cell.getStringCellValue());
                default -> 0;
            };
        } catch (Exception e) {
            return 0;
        }
    }

    private static double parseLooseDouble(String raw) {
        if (raw == null) {
            return 0;
        }
        String value = raw.trim().replace(',', '.');
        if (value.isEmpty() || "-".equals(value) || "—".equals(value)) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
