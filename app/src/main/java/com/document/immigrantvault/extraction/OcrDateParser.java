package com.document.immigrantvault.extraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared date parsing helpers for OCR label extractors. */
final class OcrDateParser {

    private static final Pattern[] DATE_PATTERNS = {
            Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})\\b"),
            Pattern.compile("\\b(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})\\b"),
            Pattern.compile(
                    "\\b((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*)[.]?\\s+(\\d{1,2}),?\\s+(\\d{4})\\b",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile(
                    "\\b(\\d{1,2})\\s+((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*)[.]?\\s+(\\d{4})\\b",
                    Pattern.CASE_INSENSITIVE)
    };

    private static final String[] PARSE_FORMATS = {
            "MM/dd/yyyy", "M/d/yyyy", "MM-dd-yyyy", "M-d-yyyy",
            "dd/MM/yyyy", "d/M/yyyy",
            "yyyy-MM-dd", "yyyy/MM/dd",
            "MMM d yyyy", "MMM d, yyyy", "MMMM d yyyy", "MMMM d, yyyy",
            "d MMM yyyy", "d MMMM yyyy"
    };

    private OcrDateParser() {
    }

    @Nullable
    static Date parseFirst(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        for (String format : PARSE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                sdf.setLenient(false);
                return sdf.parse(trimmed);
            } catch (ParseException ignored) {
                // try next
            }
        }
        Matcher m = DATE_PATTERNS[0].matcher(trimmed);
        if (m.find()) {
            return parseNumeric(m.group(1), m.group(2), m.group(3), false);
        }
        m = DATE_PATTERNS[1].matcher(trimmed);
        if (m.find()) {
            return parseNumeric(m.group(2), m.group(3), m.group(1), true);
        }
        return null;
    }

    @Nullable
    static Date findNearbyDate(@NonNull String text, @NonNull Pattern labelPattern) {
        Matcher label = labelPattern.matcher(text);
        if (!label.find()) {
            return null;
        }
        int from = label.end();
        int to = Math.min(text.length(), from + 80);
        String window = text.substring(from, to);
        for (Pattern datePattern : DATE_PATTERNS) {
            Matcher dm = datePattern.matcher(window);
            if (dm.find()) {
                Date parsed = parseFirst(dm.group());
                if (parsed != null) {
                    return parsed;
                }
            }
        }
        return null;
    }

    @Nullable
    static String findNearbyValue(@NonNull String text, @NonNull Pattern labelPattern,
                                  @NonNull Pattern valuePattern) {
        Matcher label = labelPattern.matcher(text);
        if (!label.find()) {
            return null;
        }
        int from = label.end();
        int to = Math.min(text.length(), from + 120);
        String window = text.substring(from, to);
        Matcher vm = valuePattern.matcher(window);
        if (vm.find()) {
            return vm.group(1) != null ? vm.group(1).trim() : vm.group().trim();
        }
        return null;
    }

    @Nullable
    private static Date parseNumeric(String a, String b, String yearRaw, boolean yearFirst) {
        try {
            int year = Integer.parseInt(yearRaw);
            if (year < 100) {
                year += year >= 70 ? 1900 : 2000;
            }
            int month;
            int day;
            if (yearFirst) {
                month = Integer.parseInt(a);
                day = Integer.parseInt(b);
            } else {
                // Prefer US MM/DD when ambiguous
                month = Integer.parseInt(a);
                day = Integer.parseInt(b);
                if (month > 12 && day <= 12) {
                    int tmp = month;
                    month = day;
                    day = tmp;
                }
            }
            if (month < 1 || month > 12 || day < 1 || day > 31) {
                return null;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-M-d", Locale.US);
            sdf.setLenient(false);
            return sdf.parse(year + "-" + month + "-" + day);
        } catch (Exception e) {
            return null;
        }
    }
}
