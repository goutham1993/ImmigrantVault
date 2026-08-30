package com.document.immigrantvault.extraction;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PassportMrzParserTest {

    @Test
    public void parsesValidTd3Sample() {
        // Synthetic TD3 with valid check digits for number/DOB/expiry fields.
        String line1 = "P<UTOLAST<<FIRST<<<<<<<<<<<<<<<<<<<<<<<<<<<";
        String line2 = buildLine2("L898902C3", "UTO", "740812", "M", "120415", "ZE184226B");

        OcrText ocr = new OcrText(line1 + "\n" + line2, Arrays.asList(line1, line2));
        DocumentExtraction result = PassportMrzParser.parse(ocr);

        assertNotNull(result);
        assertEquals("L898902C3", result.documentNumber);
        assertEquals("UTO", result.issuingCountry);
        assertEquals("UTO", result.nationality);
        assertNotNull(result.expiryDate);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result.expiryDate);
        assertEquals(2012, cal.get(Calendar.YEAR));
        assertEquals(Calendar.APRIL, cal.get(Calendar.MONTH));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
        assertTrue(result.hasAnyField());
    }

    private static String buildLine2(String number, String nationality, String dob,
                                     String sex, String expiry, String personal) {
        String numField = pad(number, 9);
        char numCheck = (char) ('0' + checkDigit(numField));
        char dobCheck = (char) ('0' + checkDigit(dob));
        char expCheck = (char) ('0' + checkDigit(expiry));
        String personalField = pad(personal, 14);
        char personalCheck = (char) ('0' + checkDigit(personalField));
        String composite = numField + numCheck + dob + dobCheck + expiry + expCheck
                + personalField + personalCheck;
        char compositeCheck = (char) ('0' + checkDigit(composite));
        return numField + numCheck + nationality + dob + dobCheck + sex + expiry + expCheck
                + personalField + personalCheck + compositeCheck;
    }

    private static String pad(String value, int len) {
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < len) {
            sb.append('<');
        }
        if (sb.length() > len) {
            return sb.substring(0, len);
        }
        return sb.toString();
    }

    private static int checkDigit(String data) {
        int[] weights = {7, 3, 1};
        int sum = 0;
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            int v;
            if (c == '<') {
                v = 0;
            } else if (c >= '0' && c <= '9') {
                v = c - '0';
            } else {
                v = c - 'A' + 10;
            }
            sum += v * weights[i % 3];
        }
        return sum % 10;
    }
}
