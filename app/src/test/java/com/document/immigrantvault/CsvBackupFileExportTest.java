package com.document.immigrantvault;

import com.document.immigrantvault.data.backup.BackupPayload;
import com.document.immigrantvault.data.backup.CsvBackupSerializer;
import com.document.immigrantvault.data.backup.VaultBackup;
import com.document.immigrantvault.data.db.entity.VaultFile;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CsvBackupFileExportTest {

    @Test
    public void csvZipIncludesNamedDocumentBytes() throws Exception {
        VaultFile file = new VaultFile();
        file.id = 11;
        file.folderId = 4;
        file.personId = 7;
        file.displayName = "Passport scan";
        file.storedName = "abc-123.jpg";
        file.mimeType = "image/jpeg";

        VaultBackup backup = new VaultBackup();
        backup.vaultFiles.add(file);

        byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};
        BackupPayload payload = new BackupPayload(backup);
        payload.files.put(BackupPayload.key(7, "abc-123.jpg"), jpeg);

        byte[] zipBytes = CsvBackupSerializer.toBytes(payload);

        Set<String> names = zipEntryNames(zipBytes);
        assertTrue(names.contains("saved_documents/7/Passport scan__abc-123.jpg"));
        assertTrue(names.contains("vault_files.csv"));

        BackupPayload restored = CsvBackupSerializer.fromBytes(zipBytes);
        assertEquals(1, restored.backup.vaultFiles.size());
        assertEquals("Passport scan", restored.backup.vaultFiles.get(0).displayName);
        assertArrayEquals(jpeg, restored.files.get("7/abc-123.jpg"));
    }

    @Test
    public void stillReadsLegacyFilesPrefix() throws Exception {
        VaultBackup backup = new VaultBackup();
        BackupPayload payload = new BackupPayload(backup);
        payload.files.put("3/old.jpg", new byte[]{1, 2, 3});

        byte[] zipBytes = CsvBackupSerializer.toBytes(payload);
        BackupPayload restored = CsvBackupSerializer.fromBytes(zipBytes);
        assertArrayEquals(new byte[]{1, 2, 3}, restored.files.get("3/old.jpg"));
    }

    private static Set<String> zipEntryNames(byte[] zipBytes) throws Exception {
        Set<String> names = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
                zip.closeEntry();
            }
        }
        return names;
    }
}
