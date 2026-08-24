package org.example.belgianslotclubspring.utils;

import org.apache.poi.ss.usermodel.DateUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

/**
 * Parse les dates de courses Excel (texte FR/EN, ISO, ou numéro de série Excel).
 */
public final class RaceDateParse {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            flexible("d-MMM-uuuu", Locale.FRENCH),
            flexible("d-MMM-uuuu", Locale.ENGLISH),
            flexible("d/M/uuuu", Locale.FRENCH),
            flexible("d-M-uuuu", Locale.FRENCH),
            flexible("d.M.uuuu", Locale.FRENCH),
            flexible("d MMMM uuuu", Locale.FRENCH),
            flexible("d MMM uuuu", Locale.FRENCH),
            flexible("d MMM uuuu", Locale.ENGLISH)
    );

    private RaceDateParse() {
    }

    public static LocalDate parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Date de course vide dans le fichier Excel.");
        }
        String value = raw.trim();
        if (value.startsWith("DATE:")) {
            value = value.substring(5).trim();
        }

        DateTimeParseException last = null;
        for (String candidate : List.of(value, value.replace(".", ""))) {
            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    return LocalDate.parse(candidate, formatter);
                } catch (DateTimeParseException e) {
                    last = e;
                }
            }
        }

        if (value.matches("\\d+(\\.\\d+)?")) {
            double serial = Double.parseDouble(value);
            if (DateUtil.isValidExcelDate(serial) && serial > 20_000 && serial < 80_000) {
                return DateUtil.getLocalDateTime(serial).toLocalDate();
            }
        }

        throw new IllegalArgumentException(
                "Date de course illisible (« " + raw.trim() + " »). "
                        + "Attendu : 06-janv.-2026, 06-Jan-2026 ou 06/01/2026.",
                last
        );
    }

    private static DateTimeFormatter flexible(String pattern, Locale locale) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .appendPattern(pattern)
                .toFormatter(locale)
                .withResolverStyle(ResolverStyle.SMART);
    }
}
