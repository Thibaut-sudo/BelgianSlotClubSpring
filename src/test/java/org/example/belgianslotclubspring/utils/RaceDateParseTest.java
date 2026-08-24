package org.example.belgianslotclubspring.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RaceDateParseTest {

    @Test
    void parsesCommonExcelDateFormatsIncluding2026() {
        LocalDate expected = LocalDate.of(2026, 1, 6);
        assertEquals(expected, RaceDateParse.parse("2026-01-06"));
        assertEquals(expected, RaceDateParse.parse("06-Jan-2026"));
        assertEquals(expected, RaceDateParse.parse("6-Jan-2026"));
        assertEquals(expected, RaceDateParse.parse("06/01/2026"));
        assertEquals(expected, RaceDateParse.parse("6/1/2026"));
        assertEquals(expected, RaceDateParse.parse("06-janv.-2026"));
        assertEquals(expected, RaceDateParse.parse("6 janv. 2026"));
        assertEquals(LocalDate.of(2025, 1, 10), RaceDateParse.parse("10-janv.-2025"));
        assertEquals(LocalDate.of(2026, 9, 4), RaceDateParse.parse("04-Sep-2026"));
    }
}
