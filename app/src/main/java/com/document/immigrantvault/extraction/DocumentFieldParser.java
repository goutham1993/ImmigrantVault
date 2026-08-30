package com.document.immigrantvault.extraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.data.db.entity.DocumentType;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort field extraction for non-passport documents. Prefers labeled values and avoids
 * over-guessing when confidence is low.
 */
public final class DocumentFieldParser {

    private static final Pattern DOC_NUMBER_LABEL = Pattern.compile(
            "(?i)(?:document\\s*(?:no|number|#)|passport\\s*(?:no|number|#)|"
                    + "card\\s*(?:no|number|#)|license\\s*(?:no|number|#)|"
                    + "perm(?:anent)?\\s*resident\\s*(?:card)?\\s*(?:no|number|#)?|"
                    + "receipt\\s*(?:no|number|#)|alien\\s*(?:no|number|#)|a[- ]?number)\\s*[:#]?\\s*");

    private static final Pattern DOC_NUMBER_VALUE = Pattern.compile(
            "([A-Z0-9][A-Z0-9\\-]{4,20})");

    private static final Pattern ISSUE_LABEL = Pattern.compile(
            "(?i)(?:date\\s*of\\s*issue|issue(?:d)?\\s*date|issued)\\s*[:#]?\\s*");

    private static final Pattern EXPIRY_LABEL = Pattern.compile(
            "(?i)(?:date\\s*of\\s*expir(?:y|ation)|expir(?:y|ation|es)\\s*date|exp(?:ires)?|valid\\s*until)\\s*[:#]?\\s*");

    private static final Pattern COUNTRY_LABEL = Pattern.compile(
            "(?i)(?:issuing\\s*(?:country|authority|state)|country\\s*of\\s*issue|authority)\\s*[:#]?\\s*");

    private static final Pattern COUNTRY_VALUE = Pattern.compile(
            "([A-Za-z][A-Za-z .'-]{1,40})");

    private static final Pattern FALLBACK_NUMBER = Pattern.compile(
            "\\b([A-Z]{1,3}\\d{6,9}|\\d{8,9}|[A-Z0-9]{9,12})\\b");

    private DocumentFieldParser() {
    }

    @NonNull
    public static DocumentExtraction parse(@NonNull OcrText ocr, @NonNull DocumentType selectedType) {
        // Passport MRZ first when type is passport or MRZ is clearly present.
        if (selectedType == DocumentType.PASSPORT) {
            DocumentExtraction mrz = PassportMrzParser.parse(ocr);
            if (mrz != null) {
                return mrz;
            }
        } else {
            DocumentExtraction mrz = PassportMrzParser.parse(ocr);
            if (mrz != null) {
                // User scanned a passport while another type was selected — still prefer MRZ.
                return mrz;
            }
        }

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.type = selectedType;
        String text = ocr.fullText;

        extraction.documentNumber = OcrDateParser.findNearbyValue(
                text, DOC_NUMBER_LABEL, DOC_NUMBER_VALUE);
        if (extraction.documentNumber == null) {
            extraction.documentNumber = firstFallbackNumber(text);
        }

        extraction.issuingCountry = cleanCountry(
                OcrDateParser.findNearbyValue(text, COUNTRY_LABEL, COUNTRY_VALUE));

        extraction.issueDate = OcrDateParser.findNearbyDate(text, ISSUE_LABEL);
        extraction.expiryDate = OcrDateParser.findNearbyDate(text, EXPIRY_LABEL);

        return extraction;
    }

    @Nullable
    private static String firstFallbackNumber(@NonNull String text) {
        Matcher m = FALLBACK_NUMBER.matcher(text.toUpperCase(Locale.US));
        while (m.find()) {
            String value = m.group(1);
            if (value == null) {
                continue;
            }
            // Skip plain years
            if (value.matches("19\\d{2}|20\\d{2}")) {
                continue;
            }
            return value;
        }
        return null;
    }

    @Nullable
    private static String cleanCountry(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[\\r\\n]+", " ").trim();
        if (cleaned.length() < 2) {
            return null;
        }
        // Cut off if we accidentally ate the next label
        int cut = indexOfLabel(cleaned.toLowerCase(Locale.US));
        if (cut > 0) {
            cleaned = cleaned.substring(0, cut).trim();
        }
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static int indexOfLabel(@NonNull String lower) {
        String[] labels = {"date", "expir", "document", "number", "passport", "sex", "nationality"};
        int best = -1;
        for (String label : labels) {
            int idx = lower.indexOf(label);
            if (idx > 0 && (best < 0 || idx < best)) {
                best = idx;
            }
        }
        return best;
    }
}
