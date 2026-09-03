package com.document.immigrantvault.extraction;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class W2FieldParserTest {

    @Test
    public void extractsLabeledW2Fields() {
        String text = ""
                + "Form W-2 Wage and Tax Statement 2024\n"
                + "b Employer identification number (EIN) 12-3456789\n"
                + "c Employer's name, address, and ZIP code\n"
                + "Acme Software Inc\n"
                + "1 Wages, tips, other compensation 95000.50\n"
                + "2 Federal income tax withheld 18000.00\n"
                + "3 Social security wages 95000.50\n"
                + "4 Social security tax withheld 5890.03\n"
                + "5 Medicare wages and tips 95000.50\n"
                + "6 Medicare tax withheld 1377.51\n"
                + "12a D 5000.00\n"
                + "15 State CA\n"
                + "16 State wages, tips, etc. 95000.50\n"
                + "17 State income tax 7000.00\n";

        java.util.List<String> lines = Arrays.asList(text.split("\n"));
        W2Extraction result = W2FieldParser.parse(new OcrText(text, lines));

        assertTrue(result.hasAnyField());
        assertEquals(Integer.valueOf(2024), result.taxYear);
        assertEquals("12-3456789", result.ein);
        assertEquals("Acme Software Inc", result.employerName);
        assertEquals(95000.50, result.wages, 0.001);
        assertEquals(18000.00, result.federalIncomeTax, 0.001);
        assertEquals(95000.50, result.socialSecurityWages, 0.001);
        assertEquals(5890.03, result.socialSecurityTax, 0.001);
        assertEquals(95000.50, result.medicareWages, 0.001);
        assertEquals(1377.51, result.medicareTax, 0.001);
        assertEquals("D", result.box12aCode);
        assertEquals(5000.00, result.box12aAmount, 0.001);
        assertEquals("CA", result.state);
        assertNotNull(result.stateWages);
        assertEquals(7000.00, result.stateIncomeTax, 0.001);
    }
}
