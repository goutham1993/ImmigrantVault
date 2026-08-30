package com.document.immigrantvault.extraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Label-based extraction for Form W-2. OCR layout varies a lot, so this prefers clearly labeled
 * box values and leaves uncertain fields empty for manual correction.
 */
public final class W2FieldParser {

    private static final Pattern AMOUNT = Pattern.compile(
            "\\$?\\s*(\\d{1,3}(?:,\\d{3})+(?:\\.\\d{2})?|\\d+\\.\\d{2}|\\d+)");

    private static final Pattern EIN = Pattern.compile(
            "\\b(\\d{2}-\\d{7})\\b");

    private static final Pattern YEAR = Pattern.compile(
            "\\b(20[0-2]\\d)\\b");

    private static final Pattern EMPLOYER_LABEL = Pattern.compile(
            "(?i)(?:c\\s*)?employer'?s?\\s*name"
                    + "(?:\\s*,\\s*address(?:\\s*,?\\s*and\\s*ZIP\\s*code)?)?\\s*[:#]?\\s*");

    private static final Pattern EMPLOYER_VALUE = Pattern.compile(
            "([A-Za-z0-9][A-Za-z0-9 .,'&/-]{1,60})");

    private static final Pattern EIN_LABEL = Pattern.compile(
            "(?i)(?:employer(?:'s)?\\s*(?:identification|id)?\\s*(?:number|no|#)|EIN|b\\s*employer)\\s*[:#]?\\s*");

    private static final Pattern YEAR_LABEL = Pattern.compile(
            "(?i)(?:tax\\s*year|for\\s*(?:calendar\\s*)?year|form\\s*w-?2)\\s*[:#]?\\s*");

    private static final Pattern BOX1 = Pattern.compile(
            "(?i)(?:(?:box\\s*)?1\\s*[,:]?\\s*)?(?:wages,?\\s*tips,?\\s*(?:other\\s*)?compensation)\\s*[:#]?\\s*");

    private static final Pattern BOX2 = Pattern.compile(
            "(?i)(?:(?:box\\s*)?2\\s*[,:]?\\s*)?(?:federal\\s*income\\s*tax\\s*withheld)\\s*[:#]?\\s*");

    private static final Pattern BOX3 = Pattern.compile(
            "(?i)(?:(?:box\\s*)?3\\s*[,:]?\\s*)?(?:social\\s*security\\s*wages)\\s*[:#]?\\s*");

    private static final Pattern BOX4 = Pattern.compile(
            "(?i)(?:(?:box\\s*)?4\\s*[,:]?\\s*)?(?:social\\s*security\\s*tax\\s*withheld)\\s*[:#]?\\s*");

    private static final Pattern BOX5 = Pattern.compile(
            "(?i)(?:(?:box\\s*)?5\\s*[,:]?\\s*)?(?:medicare\\s*wages(?:\\s*and\\s*tips)?)\\s*[:#]?\\s*");

    private static final Pattern BOX6 = Pattern.compile(
            "(?i)(?:(?:box\\s*)?6\\s*[,:]?\\s*)?(?:medicare\\s*tax\\s*withheld)\\s*[:#]?\\s*");

    private static final Pattern BOX14 = Pattern.compile(
            "(?i)(?:(?:box\\s*)?14\\s*[,:]?\\s*)?(?:other)\\s*[:#]?\\s*");

    private static final Pattern BOX15 = Pattern.compile(
            "(?i)(?:box\\s*)?15\\s*[,:]?\\s*(?:state)?\\s*[:#]?\\s*");

    private static final Pattern BOX16 = Pattern.compile(
            "(?i)(?:(?:box\\s*)?16\\s*[,:]?\\s*)?(?:state\\s*wages,?\\s*tips,?\\s*etc\\.?)\\s*[:#]?\\s*");

    private static final Pattern BOX17 = Pattern.compile(
            "(?i)(?:(?:box\\s*)?17\\s*[,:]?\\s*)?(?:state\\s*income\\s*tax)\\s*[:#]?\\s*");

    private static final Pattern STATE_CODE = Pattern.compile(
            "\\b([A-Z]{2})\\b");

    private static final Pattern BOX12_ENTRY = Pattern.compile(
            "(?i)(?:(?:box\\s*)?12\\s*([abcd])|\\b12([abcd])\\b)\\s*[:#]?\\s*"
                    + "([A-Z]{1,2})?\\s*"
                    + "\\$?\\s*(\\d{1,3}(?:,\\d{3})+(?:\\.\\d{2})?|\\d+\\.\\d{2}|\\d+)");

    private static final Pattern BOX14_VALUE = Pattern.compile(
            "([A-Za-z0-9][A-Za-z0-9 .,$%-]{0,80})");

    private W2FieldParser() {
    }

    @NonNull
    public static W2Extraction parse(@NonNull OcrText ocr) {
        W2Extraction extraction = new W2Extraction();
        String text = ocr.fullText;

        extraction.taxYear = parseYear(text);
        extraction.ein = firstGroup(EIN.matcher(text));
        if (extraction.ein == null) {
            extraction.ein = OcrDateParser.findNearbyValue(text, EIN_LABEL, EIN);
        }

        String employer = OcrDateParser.findNearbyValue(text, EMPLOYER_LABEL, EMPLOYER_VALUE);
        extraction.employerName = cleanEmployer(employer);
        if (extraction.employerName == null) {
            extraction.employerName = employerFromLines(ocr);
        }

        extraction.wages = findAmount(text, BOX1);
        extraction.federalIncomeTax = findAmount(text, BOX2);
        extraction.socialSecurityWages = findAmount(text, BOX3);
        extraction.socialSecurityTax = findAmount(text, BOX4);
        extraction.medicareWages = findAmount(text, BOX5);
        extraction.medicareTax = findAmount(text, BOX6);

        parseBox12(text, extraction);

        String box14 = OcrDateParser.findNearbyValue(text, BOX14, BOX14_VALUE);
        extraction.box14 = cleanLine(box14);

        String state = OcrDateParser.findNearbyValue(text, BOX15, STATE_CODE);
        extraction.state = state != null ? state.toUpperCase(Locale.US) : null;
        extraction.stateWages = findAmount(text, BOX16);
        extraction.stateIncomeTax = findAmount(text, BOX17);

        return extraction;
    }

    @Nullable
    private static Integer parseYear(@NonNull String text) {
        String labeled = OcrDateParser.findNearbyValue(text, YEAR_LABEL, YEAR);
        if (labeled != null) {
            try {
                return Integer.parseInt(labeled);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        // Prefer a year that appears near "W-2" / "Wage"
        Matcher near = Pattern.compile("(?i)(?:w-?2|wage).{0,40}?\\b(20[0-2]\\d)\\b").matcher(text);
        if (near.find()) {
            return Integer.parseInt(near.group(1));
        }
        return null;
    }

    @Nullable
    private static Double findAmount(@NonNull String text, @NonNull Pattern label) {
        String raw = OcrDateParser.findNearbyValue(text, label, AMOUNT);
        return parseMoney(raw);
    }

    private static void parseBox12(@NonNull String text, @NonNull W2Extraction extraction) {
        Matcher m = BOX12_ENTRY.matcher(text);
        while (m.find()) {
            String slot = m.group(1) != null ? m.group(1) : m.group(2);
            if (slot == null) {
                continue;
            }
            String code = m.group(3);
            Double amount = parseMoney(m.group(4));
            char key = Character.toLowerCase(slot.charAt(0));
            switch (key) {
                case 'a':
                    if (extraction.box12aCode == null && code != null) {
                        extraction.box12aCode = code.toUpperCase(Locale.US);
                    }
                    if (extraction.box12aAmount == null) {
                        extraction.box12aAmount = amount;
                    }
                    break;
                case 'b':
                    if (extraction.box12bCode == null && code != null) {
                        extraction.box12bCode = code.toUpperCase(Locale.US);
                    }
                    if (extraction.box12bAmount == null) {
                        extraction.box12bAmount = amount;
                    }
                    break;
                case 'c':
                    if (extraction.box12cCode == null && code != null) {
                        extraction.box12cCode = code.toUpperCase(Locale.US);
                    }
                    if (extraction.box12cAmount == null) {
                        extraction.box12cAmount = amount;
                    }
                    break;
                case 'd':
                    if (extraction.box12dCode == null && code != null) {
                        extraction.box12dCode = code.toUpperCase(Locale.US);
                    }
                    if (extraction.box12dAmount == null) {
                        extraction.box12dAmount = amount;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Nullable
    private static Double parseMoney(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(raw.replace(",", "").replace("$", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static String firstGroup(@NonNull Matcher matcher) {
        return matcher.find() ? matcher.group(1) : null;
    }

    @Nullable
    private static String employerFromLines(@NonNull OcrText ocr) {
        for (int i = 0; i < ocr.lines.size(); i++) {
            String line = ocr.lines.get(i);
            if (line == null) {
                continue;
            }
            if (!EMPLOYER_LABEL.matcher(line).find()
                    && !line.toLowerCase(Locale.US).contains("employer's name")
                    && !line.toLowerCase(Locale.US).contains("employers name")) {
                continue;
            }
            for (int j = i + 1; j < Math.min(ocr.lines.size(), i + 4); j++) {
                String candidate = cleanEmployer(ocr.lines.get(j));
                if (candidate != null && !looksLikeFormNoise(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static boolean looksLikeFormNoise(@NonNull String value) {
        String lower = value.toLowerCase(Locale.US);
        return lower.startsWith("address")
                || lower.startsWith("zip")
                || lower.contains("identification")
                || lower.matches("\\d{2}-\\d{7}")
                || lower.matches("box\\s*\\d.*");
    }

    @Nullable
    private static String cleanEmployer(@Nullable String value) {
        String cleaned = cleanLine(value);
        if (cleaned == null || looksLikeFormNoise(cleaned)) {
            return null;
        }
        String lower = cleaned.toLowerCase(Locale.US);
        for (String stop : new String[]{
                "employer", "address", "ein", "control", "employee", "box", "wages", "federal"
        }) {
            int idx = lower.indexOf(stop);
            if (idx > 0) {
                cleaned = cleaned.substring(0, idx).trim();
                break;
            }
        }
        return cleaned.isEmpty() || looksLikeFormNoise(cleaned) ? null : cleaned;
    }

    @Nullable
    private static String cleanLine(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replaceAll("[\\r\\n]+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
