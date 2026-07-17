package com.document.immigrantvault.data.repository;

import com.document.immigrantvault.data.backup.CsvBackupSerializer;
import com.document.immigrantvault.data.backup.ExportFormat;
import com.document.immigrantvault.data.backup.ExportImportException;
import com.document.immigrantvault.data.backup.JsonBackupSerializer;
import com.document.immigrantvault.data.backup.VaultBackup;
import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.dao.BackupDao;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ExportImportRepository {

    private final BackupDao backupDao;
    private final ExecutorService executor;
    private final int databaseVersion;

    public ExportImportRepository(AppDatabase database, ExecutorService executor) {
        this.backupDao = database.backupDao();
        this.executor = executor;
        this.databaseVersion = 15;
    }

    public Future<byte[]> exportAsync(ExportFormat format) {
        return executor.submit(() -> {
            VaultBackup backup = backupDao.exportAll();
            backup.databaseVersion = databaseVersion;
            if (format == ExportFormat.JSON) {
                return JsonBackupSerializer.toBytes(backup);
            }
            return CsvBackupSerializer.toBytes(backup);
        });
    }

    public Future<Void> importAsync(byte[] data, String mimeType) {
        return executor.submit(() -> {
            VaultBackup backup = parseBackup(data, mimeType);
            backupDao.replaceAll(backup);
            return null;
        });
    }

    private VaultBackup parseBackup(byte[] data, String mimeType) throws ExportImportException {
        if (mimeType != null && (mimeType.contains("json") || mimeType.endsWith("/json"))) {
            return JsonBackupSerializer.fromBytes(data);
        }
        if (mimeType != null && (mimeType.contains("zip") || mimeType.contains("csv"))) {
            return CsvBackupSerializer.fromBytes(data);
        }
        return detectFormat(data);
    }

    private VaultBackup detectFormat(byte[] data) throws ExportImportException {
        if (data.length > 0 && data[0] == '{') {
            return JsonBackupSerializer.fromBytes(data);
        }
        if (data.length > 1 && data[0] == 'P' && data[1] == 'K') {
            return CsvBackupSerializer.fromBytes(data);
        }
        throw new ExportImportException("Unsupported backup file format. Use JSON or CSV (ZIP).");
    }
}
