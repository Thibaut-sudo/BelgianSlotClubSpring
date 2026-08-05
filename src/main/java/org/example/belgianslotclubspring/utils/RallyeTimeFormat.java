package org.example.belgianslotclubspring.utils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse / format des temps rallye.
 * Accepte : {@code 12.345}, {@code 1:23.45}, {@code 1:02:03.4}, virgule ou point.
 */
public final class RallyeTimeFormat {

    private static final Pattern MM_SS = Pattern.compile(
            "^(\\d+):([0-5]?\\d)(?:[.,](\\d{1,3}))?$"
    );
    private static final Pattern HH_MM_SS = Pattern.compile(
            "^(\\d+):([0-5]?\\d):([0-5]?\\d)(?:[.,](\\d{1,3}))?$"
    );

    private RallyeTimeFormat() {
    }

    public static Double parse(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().replace(',', '.');
        if (value.isEmpty() || "-".equals(value)) {
            return null;
        }

        Matcher hh = HH_MM_SS.matcher(value);
        if (hh.matches()) {
            double hours = Double.parseDouble(hh.group(1));
            double minutes = Double.parseDouble(hh.group(2));
            double seconds = Double.parseDouble(hh.group(3));
            double frac = parseFraction(hh.group(4));
            return hours * 3600 + minutes * 60 + seconds + frac;
        }

        Matcher mm = MM_SS.matcher(value);
        if (mm.matches()) {
            double minutes = Double.parseDouble(mm.group(1));
            double seconds = Double.parseDouble(mm.group(2));
            double frac = parseFraction(mm.group(3));
            return minutes * 60 + seconds + frac;
        }

        return Double.parseDouble(value);
    }

    public static String format(Double seconds) {
        if (seconds == null) {
            return "";
        }
        if (seconds < 0) {
            return format(-seconds);
        }

        long totalMillis = Math.round(seconds * 1000);
        long abs = Math.abs(totalMillis);
        long h = abs / 3_600_000;
        long m = (abs % 3_600_000) / 60_000;
        long s = (abs % 60_000) / 1000;
        long ms = abs % 1000;

        String body;
        if (h > 0) {
            body = String.format(Locale.US, "%d:%02d:%02d.%03d", h, m, s, ms);
        } else if (m > 0) {
            body = String.format(Locale.US, "%d:%02d.%03d", m, s, ms);
        } else {
            body = String.format(Locale.US, "%d.%03d", s, ms);
        }
        return totalMillis < 0 ? "-" + body : body;
    }

    public static String formatGap(Double gapSeconds) {
        if (gapSeconds == null || gapSeconds == 0) {
            return "—";
        }
        return "+" + format(gapSeconds);
    }

    private static double parseFraction(String digits) {
        if (digits == null || digits.isEmpty()) {
            return 0;
        }
        // "3" → 0.3, "34" → 0.34, "345" → 0.345
        String padded = (digits + "000").substring(0, 3);
        return Integer.parseInt(padded) / 1000.0;
    }
}
