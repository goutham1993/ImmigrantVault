package com.document.immigrantvault;

import com.document.immigrantvault.data.db.entity.VaultFile;
import com.document.immigrantvault.ui.files.FileFormat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileFormatTest {

    @Test
    public void downloadFileName_addsTypeExtension() {
        VaultFile file = new VaultFile();
        file.displayName = "Passport scan";
        file.mimeType = "image/jpeg";
        assertEquals("Passport scan.jpg", FileFormat.downloadFileName(file));
    }

    @Test
    public void downloadFileName_doesNotDuplicateExtension() {
        VaultFile file = new VaultFile();
        file.displayName = "Return.pdf";
        file.mimeType = "application/pdf";
        assertEquals("Return.pdf", FileFormat.downloadFileName(file));
    }

    @Test
    public void downloadFileName_sanitizesPathCharacters() {
        VaultFile file = new VaultFile();
        file.displayName = "I-94 / 2024";
        file.mimeType = "application/pdf";
        assertEquals("I-94 _ 2024.pdf", FileFormat.downloadFileName(file));
    }
}
