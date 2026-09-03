package com.document.immigrantvault.extraction;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class I94FieldParserTest {

    @Test
    public void extractsLabeledI94Fields() {
        String text = ""
                + "Admission (I-94) Record Number: 123456789A\n"
                + "Document Number: AB1234567\n"
                + "Country of Citizenship: India\n"
                + "Date of Entry: 03/15/2024\n"
                + "Admit Until Date: 03/14/2027\n"
                + "Port of Entry: JFK\n"
                + "Class of Admission: H-1B\n";

        I94Extraction result = I94FieldParser.parse(new OcrText(text, Collections.emptyList()));

        assertTrue(result.hasAnyField());
        assertEquals("123456789A", result.i94Number);
        assertEquals("AB1234567", result.documentNumber);
        assertEquals("India", result.countryOfCitizenship);
        assertNotNull(result.arrivalDate);
        assertNotNull(result.admitUntilDate);
        assertEquals("JFK", result.portOfEntry);
        assertEquals("H-1B", result.classOfAdmission);
    }
}
