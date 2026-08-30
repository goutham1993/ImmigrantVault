package com.document.immigrantvault.data.repository;

import com.document.immigrantvault.data.backup.BackupPayload;
import com.document.immigrantvault.data.backup.CsvBackupSerializer;
import com.document.immigrantvault.data.backup.ExportFormat;
import com.document.immigrantvault.data.backup.ExportImportException;
import com.document.immigrantvault.data.backup.JsonBackupSerializer;
import com.document.immigrantvault.data.backup.VaultBackup;
import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.dao.BackupDao;
import com.document.immigrantvault.data.db.entity.VaultFile;
import com.document.immigrantvault.util.VaultFileStorage;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ExportImportRepository {

    private final BackupDao backupDao;
    private final ExecutorService executor;
    private final VaultFileStorage vaultFileStorage;
    private final int databaseVersion;

    public ExportImportRepository(AppDatabase database, ExecutorService executor,
                                  VaultFileStorage vaultFileStorage) {
        this.backupDao = database.backupDao();
        this.executor = executor;
        this.vaultFileStorage = vaultFileStorage;
        this.databaseVersion = AppDatabase.VERSION;
    }

    public Future<byte[]> exportAsync(ExportFormat format) {
        return executor.submit(() -> {
            VaultBackup backup = backupDao.exportAll();
            backup.databaseVersion = databaseVersion;
            if (format == ExportFormat.JSON) {
                return JsonBackupSerializer.toBytes(backup);
            }
            BackupPayload payload = new BackupPayload(backup);
            collectBinaries(payload);
            return CsvBackupSerializer.toBytes(payload);
        });
    }

    public Future<Void> importAsync(byte[] data, String mimeType) {
        return executor.submit(() -> {
            BackupPayload payload = parseBackup(data, mimeType);
            backupDao.replaceAll(payload.backup);
            // The database is now authoritative, so start from a clean file tree either way.
            vaultFileStorage.deleteAll();
            restoreBinaries(payload);
            return null;
        });
    }

    public Future<Void> clearAllAsync() {
        return executor.submit(() -> {
            backupDao.clearAll();
            vaultFileStorage.deleteAll();
            return null;
        });
    }

    /** Reads each saved document off disk. Rows whose bytes are missing are skipped. */
    private void collectBinaries(BackupPayload payload) {
        if (payload.backup.vaultFiles != null) {
            for (VaultFile file : payload.backup.vaultFiles) {
                addBinary(payload, file.personId, file.storedName);
            }
        }
        // Pick up anything still on disk if a row was missed or the stored name drifted.
        File root = vaultFileStorage.getRoot();
        File[] personDirs = root.listFiles();
        if (personDirs == null) {
            return;
        }
        for (File personDir : personDirs) {
            if (!personDir.isDirectory()) {
                continue;
            }
            long personId;
            try {
                personId = Long.parseLong(personDir.getName());
            } catch (NumberFormatException ignored) {
                continue;
            }
            File[] stored = personDir.listFiles();
            if (stored == null) {
                continue;
            }
            for (File onDisk : stored) {
                if (onDisk.isFile()) {
                    addBinary(payload, personId, onDisk.getName());
                }
            }
        }
    }

    private void addBinary(BackupPayload payload, long personId, String storedName) {
        String key = BackupPayload.key(personId, storedName);
        if (storedName == null || payload.files.containsKey(key)
                || !vaultFileStorage.exists(personId, storedName)) {
            return;
        }
        try {
            payload.files.put(key, vaultFileStorage.read(personId, storedName));
        } catch (Exception ignored) {
            // An unreadable file should not abort the rest of the export.
        }
    }

    private void restoreBinaries(BackupPayload payload) {
        for (Map.Entry<String, byte[]> entry : payload.files.entrySet()) {
            String key = entry.getKey();
            int slash = key.indexOf('/');
            if (slash <= 0 || slash == key.length() - 1) {
                continue;
            }
            try {
                long personId = Long.parseLong(key.substring(0, slash));
                vaultFileStorage.write(entry.getValue(), personId, key.substring(slash + 1));
            } catch (Exception ignored) {
                // Skip malformed or unwritable entries; the metadata row still imports.
            }
        }
    }

    private BackupPayload parseBackup(byte[] data, String mimeType) throws ExportImportException {
        if (mimeType != null && (mimeType.contains("json") || mimeType.endsWith("/json"))) {
            return new BackupPayload(JsonBackupSerializer.fromBytes(data));
        }
        if (mimeType != null && (mimeType.contains("zip") || mimeType.contains("csv"))) {
            return CsvBackupSerializer.fromBytes(data);
        }
        return detectFormat(data);
    }

    private BackupPayload detectFormat(byte[] data) throws ExportImportException {
        if (data.length > 0 && data[0] == '{') {
            return new BackupPayload(JsonBackupSerializer.fromBytes(data));
        }
        if (data.length > 1 && data[0] == 'P' && data[1] == 'K') {
            return CsvBackupSerializer.fromBytes(data);
        }
        throw new ExportImportException("Unsupported backup file format. Use JSON or CSV (ZIP).");
    }
}
