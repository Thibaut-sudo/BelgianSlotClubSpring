package org.example.belgianslotclubspring.utils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.example.belgianslotclubspring.models.ExcelFilleResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelReaderTest {

    @TempDir
    Path tmp;

    @Test
    void importsRaceRowsWhenQualiTimesAreMissing() throws Exception {
        Path file = tmp.resolve("BPC 09-06-2026.xls");
        try (Workbook wb = new HSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("BPC");
            header.createCell(6).setCellValue("09-06-2026");

            sheet.createRow(1);
            sheet.createRow(2);

            Row row = sheet.createRow(3);
            row.createCell(1).setCellValue("Thibaut");
            row.createCell(4).setCellValue("Paul");
            row.createCell(5).setCellValue(268.86);
            row.createCell(6).setCellValue(46);
            row.createCell(7).setCellValue(4.994);

            try (var out = java.nio.file.Files.newOutputStream(file)) {
                wb.write(out);
            }
        }

        ExcelFilleResult result = new ExcelReader(new ExcelFilleResult()).readRaceResults(file.toString());

        assertEquals("BPC", result.getCategoryName());
        assertEquals(0, result.getQualifs().size());
        assertEquals(1, result.getRaceResults().size());
        assertEquals("Paul", result.getRaceResults().get(0).getNom());
        assertEquals(268.86, result.getRaceResults().get(0).getTotalTours(), 0.001);
        assertEquals(LocalDate.of(2026, 6, 9), result.getRaceResults().get(0).getDate());
    }

    @Test
    void keepsQualifsWhenTimesArePresent() throws Exception {
        Path file = tmp.resolve("BPC 07-07-2026.xls");
        try (Workbook wb = new HSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("BPC");
            header.createCell(6).setCellValue("07-07-2026");
            sheet.createRow(1);
            sheet.createRow(2);

            Row row = sheet.createRow(3);
            row.createCell(1).setCellValue("Thibaut");
            row.createCell(2).setCellValue(5.013);
            row.createCell(4).setCellValue("Thibaut");
            row.createCell(5).setCellValue(270.84);

            try (var out = java.nio.file.Files.newOutputStream(file)) {
                wb.write(out);
            }
        }

        ExcelFilleResult result = new ExcelReader(new ExcelFilleResult()).readRaceResults(file.toString());

        assertEquals(1, result.getQualifs().size());
        assertEquals("Thibaut", result.getQualifs().get(0).getPilotName());
        assertEquals(1, result.getRaceResults().size());
    }

    @Test
    void skipsPlaceholderRowsWithoutNameOrLaps() throws Exception {
        Path file = tmp.resolve("empty-rows.xls");
        try (Workbook wb = new HSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("BPC");
            header.createCell(6).setCellValue("14-04-2026");
            sheet.createRow(1);
            sheet.createRow(2);

            Row empty = sheet.createRow(3);
            empty.createCell(4).setCellValue(0);
            empty.createCell(5).setCellValue(0);

            try (var out = java.nio.file.Files.newOutputStream(file)) {
                wb.write(out);
            }
        }

        ExcelFilleResult result = new ExcelReader(new ExcelFilleResult()).readRaceResults(file.toString());
        assertTrue(result.getQualifs().isEmpty());
        assertTrue(result.getRaceResults().isEmpty());
    }
}
