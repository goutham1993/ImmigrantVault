package com.document.immigrantvault.extraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.data.db.entity.DocumentType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses ICAO TD3 passport MRZ lines from OCR text. Tolerates common OCR substitutions
 * (O/0, I/1) on the second line when validating check digits fails for the raw string.
 */
public final class PassportMrzParser {

    private static final Pattern MRZ_LINE = Pattern.compile("[A-Z0-9<]{30,44}");

    private PassportMrzParser() {
    }

    @Nullable
    public static DocumentExtraction parse(@NonNull OcrText ocr) {
        List<String> candidates = collectCandidates(ocr);
        for (int i = 0; i < candidates.size() - 1; i++) {
            String line1 = padOrTrim(normalizeMrz(candidates.get(i)), 44);
            String line2 = padOrTrim(normalizeMrz(candidates.get(i + 1)), 44);
            if (!line1.startsWith("P")) {
                continue;
            }
            DocumentExtraction extracted = decode(line1, line2);
            if (extracted != null) {
                return extracted;
            }
            // Retry line2 with OCR digit/letter fixes if check digits failed.
            String fixed2 = fixCommonOcr(line2);
            if (!fixed2.equals(line2)) {
                extracted = decode(line1, fixed2);
                if (extracted != null) {
                    return extracted;
                }
            }
        }
        return null;
    }

    @NonNull
    private static List<String> collectCandidates(@NonNull OcrText ocr) {
        List<String> out = new ArrayList<>();
        for (String line : ocr.lines) {
            String cleaned = line.replace(" ", "").toUpperCase(Locale.US);
            Matcher m = MRZ_LINE.matcher(cleaned);
            while (m.find()) {
                out.add(m.group());
            }
        }
        if (out.isEmpty()) {
            String compact = ocr.fullText.replaceAll("\\s+", "").toUpperCase(Locale.US);
            Matcher m = MRZ_LINE.matcher(compact);
            while (m.find()) {
                out.add(m.group());
            }
        }
        return out;
    }

    @Nullable
    private static DocumentExtraction decode(@NonNull String line1, @NonNull String line2) {
        String passportNumber = line2.substring(0, 9).replace("<", "");
        char numberCheck = line2.charAt(9);
        if (!passportNumber.isEmpty() && !checkDigitValid(line2.substring(0, 9), numberCheck)) {
            return null;
        }

        String nationality = line2.substring(10, 13).replace("<", "");
        String dobRaw = line2.substring(13, 19);
        char dobCheck = line2.charAt(19);
        if (!checkDigitValid(dobRaw, dobCheck)) {
            return null;
        }
        Date dob = parseYyMmDd(dobRaw, true);

        String expiryRaw = line2.substring(21, 27);
        char expiryCheck = line2.charAt(27);
        if (!checkDigitValid(expiryRaw, expiryCheck)) {
            return null;
        }
        Date expiry = parseYyMmDd(expiryRaw, false);

        String issuingCountry = line1.substring(2, 5).replace("<", "");

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.type = DocumentType.PASSPORT;
        extraction.documentNumber = passportNumber.isEmpty() ? null : passportNumber;
        extraction.issuingCountry = issuingCountry.isEmpty() ? null : issuingCountry;
        extraction.nationality = nationality.isEmpty() ? null : nationality;
        extraction.expiryDate = expiry;
        // Issue date is not in TD3 MRZ; leave null.
        // DOB is not a Document field; ignored.
        if (dob == null && expiry == null && extraction.documentNumber == null) {
            return null;
        }
        return extraction;
    }

    private static boolean checkDigitValid(@NonNull String data, char checkChar) {
        if (checkChar == '<') {
            return true;
        }
        int expected = mrzCheckDigit(data);
        int actual = charValue(checkChar);
        return actual >= 0 && expected == actual;
    }

    private static int mrzCheckDigit(@NonNull String data) {
        int[] weights = {7, 3, 1};
        int sum = 0;
        for (int i = 0; i < data.length(); i++) {
            sum += charValue(data.charAt(i)) * weights[i % 3];
        }
        return sum % 10;
    }

    private static int charValue(char c) {
        if (c == '<') {
            return 0;
        }
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'Z') {
            return c - 'A' + 10;
        }
        return -1;
    }

    @Nullable
    private static Date parseYyMmDd(@NonNull String raw, boolean birthDate) {
        if (!raw.matches("\\d{6}")) {
            return null;
        }
        int yy = Integer.parseInt(raw.substring(0, 2));
        int mm = Integer.parseInt(raw.substring(2, 4));
        int dd = Integer.parseInt(raw.substring(4, 6));
        if (mm < 1 || mm > 12 || dd < 1 || dd > 31) {
            return null;
        }
        int currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100;
        int century;
        if (birthDate) {
            // Birth dates: years more than ~10 ahead of current two-digit year are 1900s.
            century = yy > currentYear + 10 ? 1900 : 2000;
        } else {
            // Expiry dates are typically in the near future (2000s).
            century = 2000;
            if (yy > currentYear + 50) {
                century = 1900;
            }
        }
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, century + yy);
        cal.set(Calendar.MONTH, mm - 1);
        cal.set(Calendar.DAY_OF_MONTH, dd);
        return cal.getTime();
    }

    @NonNull
    private static String normalizeMrz(@NonNull String line) {
        return line.replace('«', '<')
                .replace('‹', '<')
                .replace('>', '<')
                .replace(" ", "")
                .toUpperCase(Locale.US);
    }

    @NonNull
    private static String padOrTrim(@NonNull String value, int length) {
        if (value.length() == length) {
            return value;
        }
        if (value.length() > length) {
            return value.substring(0, length);
        }
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < length) {
            sb.append('<');
        }
        return sb.toString();
    }

    @NonNull
    private static String fixCommonOcr(@NonNull String line2) {
        // In numeric MRZ fields, letters that look like digits are common OCR errors.
        char[] chars = line2.toCharArray();
        // passport number area may mix letters and digits — only fix pure digit zones
        fixZone(chars, 13, 19); // DOB
        fixZone(chars, 21, 27); // expiry
        // check digits
        fixDigitChar(chars, 9);
        fixDigitChar(chars, 19);
        fixDigitChar(chars, 27);
        return new String(chars);
    }

    private static void fixZone(char[] chars, int start, int end) {
        for (int i = start; i < end && i < chars.length; i++) {
            fixDigitChar(chars, i);
        }
    }

    private static void fixDigitChar(char[] chars, int index) {
        if (index < 0 || index >= chars.length) {
            return;
        }
        switch (chars[index]) {
            case 'O':
            case 'Q':
            case 'D':
                chars[index] = '0';
                break;
            case 'I':
            case 'L':
                chars[index] = '1';
                break;
            case 'Z':
                chars[index] = '2';
                break;
            case 'S':
                chars[index] = '5';
                break;
            case 'B':
                chars[index] = '8';
                break;
            case 'G':
                chars[index] = '6';
                break;
            default:
                break;
        }
    }
}
