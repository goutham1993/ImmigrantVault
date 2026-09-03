package com.document.immigrantvault.extraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Label-based extraction for CBP/USCIS I-94 travel history printouts. */
public final class I94FieldParser {

    private static final Pattern I94_LABEL = Pattern.compile(
            "(?i)(?:admission\\s*(?:\\(i-?94\\))?\\s*record\\s*(?:number|no|#)?|"
                    + "i-?94\\s*(?:number|no|#|record)?|"
                    + "admission\\s*(?:number|no|#))\\s*[:#]?\\s*");

    private static final Pattern I94_VALUE = Pattern.compile(
            "([A-Z0-9]{8,14})");

    private static final Pattern DOC_LABEL = Pattern.compile(
            "(?i)(?:document\\s*(?:number|no|#)|passport\\s*(?:number|no|#))\\s*[:#]?\\s*");

    private static final Pattern DOC_VALUE = Pattern.compile(
            "([A-Z0-9][A-Z0-9\\-]{4,20})");

    private static final Pattern CITIZENSHIP_LABEL = Pattern.compile(
            "(?i)(?:country\\s*of\\s*citizenship|citizenship|nationality)\\s*[:#]?\\s*");

    private static final Pattern TEXT_VALUE = Pattern.compile(
            "([A-Za-z][A-Za-z .'-]{1,40})");

    private static final Pattern ARRIVAL_LABEL = Pattern.compile(
            "(?i)(?:date\\s*of\\s*(?:entry|arrival)|arrival\\s*date|most\\s*recent\\s*date\\s*of\\s*entry)\\s*[:#]?\\s*");

    private static final Pattern ADMIT_LABEL = Pattern.compile(
            "(?i)(?:admit\\s*until\\s*date|admitted\\s*until|date\\s*admit(?:ted)?\\s*until)\\s*[:#]?\\s*");

    private static final Pattern PORT_LABEL = Pattern.compile(
            "(?i)(?:port\\s*of\\s*entry(?:\\s*location)?|location)\\s*[:#]?\\s*");

    private static final Pattern CLASS_LABEL = Pattern.compile(
            "(?i)(?:class\\s*of\\s*admission|admission\\s*class)\\s*[:#]?\\s*");

    private static final Pattern CLASS_VALUE = Pattern.compile(
            "([A-Z]{1,2}[- ]?\\d{0,2}[A-Z]?|WB|WT|VWP)");

    private static final Pattern FALLBACK_I94 = Pattern.compile(
            "\\b([0-9]{9,11}[A-Z]?|[A-Z0-9]{10,12})\\b");

    private I94FieldParser() {
    }

    @NonNull
    public static I94Extraction parse(@NonNull OcrText ocr) {
        I94Extraction extraction = new I94Extraction();
        String text = ocr.fullText;

        extraction.i94Number = OcrDateParser.findNearbyValue(text, I94_LABEL, I94_VALUE);
        if (extraction.i94Number == null) {
            extraction.i94Number = fallbackI94(text);
        }

        extraction.documentNumber = OcrDateParser.findNearbyValue(text, DOC_LABEL, DOC_VALUE);
        extraction.countryOfCitizenship = cleanText(
                OcrDateParser.findNearbyValue(text, CITIZENSHIP_LABEL, TEXT_VALUE));
        extraction.arrivalDate = OcrDateParser.findNearbyDate(text, ARRIVAL_LABEL);
        extraction.admitUntilDate = OcrDateParser.findNearbyDate(text, ADMIT_LABEL);
        extraction.portOfEntry = cleanText(
                OcrDateParser.findNearbyValue(text, PORT_LABEL, TEXT_VALUE));
        extraction.classOfAdmission = normalizeClass(
                OcrDateParser.findNearbyValue(text, CLASS_LABEL, CLASS_VALUE));

        // D/S admit until often appears as text
        if (extraction.admitUntilDate == null && text.matches("(?is).*admit\\s*until.*\\bD/?S\\b.*")) {
            // Leave date null; class may already capture status — no date field for D/S.
        }

        return extraction;
    }

    @Nullable
    private static String fallbackI94(@NonNull String text) {
        Matcher m = FALLBACK_I94.matcher(text.toUpperCase(Locale.US));
        while (m.find()) {
            String value = m.group(1);
            if (value == null) {
                continue;
            }
            if (value.matches("19\\d{2}|20\\d{2}")) {
                continue;
            }
            // Prefer values that look like admission numbers (digits + optional letter)
            if (value.matches("\\d{9,11}[A-Z]?")) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    private static String cleanText(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[\\r\\n]+", " ").trim();
        String lower = cleaned.toLowerCase(Locale.US);
        for (String stop : new String[]{"date", "class", "port", "document", "admit", "arrival"}) {
            int idx = lower.indexOf(stop);
            if (idx > 0) {
                cleaned = cleaned.substring(0, idx).trim();
                break;
            }
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    @Nullable
    private static String normalizeClass(@Nullable String value) {
        if (value == null) {
            return null;
        }
        return value.replace(" ", "").toUpperCase(Locale.US);
    }
}
