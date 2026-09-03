package com.document.immigrantvault.data.repository;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.FileSource;
import com.document.immigrantvault.data.db.entity.VaultFile;
import com.document.immigrantvault.util.VaultFileStorage;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class VaultFileRepository {

    private final AppDatabase database;
    private final ExecutorService executor;
    private final VaultFileStorage storage;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public VaultFileRepository(AppDatabase database, ExecutorService executor, VaultFileStorage storage) {
        this.database = database;
        this.executor = executor;
        this.storage = storage;
    }

    public VaultFileStorage getStorage() {
        return storage;
    }

    public LiveData<List<VaultFile>> getByFolder(long folderId) {
        return database.vaultFileDao().getByFolder(folderId);
    }

    public LiveData<VaultFile> getById(long fileId) {
        return database.vaultFileDao().getById(fileId);
    }

    /**
     * Copies the bytes behind {@code sourceUri} into the vault and records the metadata row.
     * If the row insert fails the copied bytes are removed so no orphan is left behind.
     */
    public void importFile(Uri sourceUri, long personId, long folderId, String displayName,
                           String mimeType, int pageCount, FileSource source,
                           RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            String storedName = null;
            try {
                String resolvedMime = mimeType != null ? mimeType : storage.resolveMimeType(sourceUri);
                storedName = storage.importFrom(sourceUri, personId, resolvedMime);

                VaultFile file = new VaultFile();
                file.personId = personId;
                file.folderId = folderId;
                file.displayName = displayName;
                file.storedName = storedName;
                file.mimeType = resolvedMime;
                file.sizeBytes = storage.sizeOf(personId, storedName);
                file.pageCount = Math.max(1, pageCount);
                file.source = source;
                file.createdAt = new Date();
                file.updatedAt = file.createdAt;

                long id = database.vaultFileDao().insert(file);
                postSuccess(callback, id);
            } catch (Exception e) {
                if (storedName != null) {
                    storage.delete(personId, storedName);
                }
                postError(callback, e);
            }
        });
    }

    public void rename(long fileId, String displayName, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                VaultFile file = database.vaultFileDao().getByIdSync(fileId);
                if (file == null) {
                    postError(callback, new IllegalStateException("File no longer exists."));
                    return;
                }
                file.displayName = displayName;
                file.updatedAt = new Date();
                database.vaultFileDao().update(file);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    public void move(long fileId, long targetFolderId, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                VaultFile file = database.vaultFileDao().getByIdSync(fileId);
                if (file == null) {
                    postError(callback, new IllegalStateException("File no longer exists."));
                    return;
                }
                file.folderId = targetFolderId;
                file.updatedAt = new Date();
                database.vaultFileDao().update(file);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    /**
     * Duplicates the file bytes and metadata into {@code targetFolderId}. The original row is
     * left untouched. {@code displayName} is used as-is so the UI can add a "(copy)" suffix
     * when duplicating into the same folder.
     */
    public void copyToFolder(long fileId, long targetFolderId, String displayName,
                             RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            String copiedName = null;
            long personId = -1L;
            try {
                VaultFile file = database.vaultFileDao().getByIdSync(fileId);
                if (file == null) {
                    postError(callback, new IllegalStateException("File no longer exists."));
                    return;
                }
                personId = file.personId;
                copiedName = storage.copy(file.personId, file.storedName, file.mimeType);

                VaultFile copy = new VaultFile();
                copy.personId = file.personId;
                copy.folderId = targetFolderId;
                copy.displayName = displayName != null && !displayName.isEmpty()
                        ? displayName : file.displayName;
                copy.storedName = copiedName;
                copy.mimeType = file.mimeType;
                copy.sizeBytes = storage.sizeOf(file.personId, copiedName);
                copy.pageCount = file.pageCount;
                copy.source = file.source;
                copy.createdAt = new Date();
                copy.updatedAt = copy.createdAt;

                long id = database.vaultFileDao().insert(copy);
                postSuccess(callback, id);
            } catch (Exception e) {
                if (copiedName != null && personId >= 0) {
                    storage.delete(personId, copiedName);
                }
                postError(callback, e);
            }
        });
    }

    public void exportTo(long fileId, Uri destination, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                VaultFile file = database.vaultFileDao().getByIdSync(fileId);
                if (file == null) {
                    postError(callback, new IllegalStateException("File no longer exists."));
                    return;
                }
                storage.exportTo(destination, file.personId, file.storedName);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    public void delete(VaultFile file, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                database.vaultFileDao().delete(file);
                storage.delete(file.personId, file.storedName);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    /** Called before a person row is removed, since CASCADE only clears the database. */
    public void deleteFilesForPersonSync(long personId) {
        storage.deletePersonDir(personId);
    }

    /** Drops bytes that no longer have a matching row, e.g. after a crash mid-import. */
    public void sweepOrphans() {
        executor.execute(() -> {
            try {
                Set<String> known = new HashSet<>();
                for (VaultFile file : database.vaultFileDao().getAllSync()) {
                    if (file.storedName != null) {
                        known.add(file.storedName);
                    }
                }
                storage.deleteOrphans(known);
                storage.clearCameraCache();
            } catch (Exception ignored) {
                // A failed sweep only leaves disk space behind; never block startup for it.
            }
        });
    }

    private <T> void postSuccess(RepositoryCallback<T> callback, T value) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(value));
        }
    }

    private <T> void postError(RepositoryCallback<T> callback, Exception error) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(error));
        }
    }
}
