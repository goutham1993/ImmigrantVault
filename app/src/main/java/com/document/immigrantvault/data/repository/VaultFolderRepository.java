package com.document.immigrantvault.data.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.dao.VaultFileDao;
import com.document.immigrantvault.data.db.entity.VaultFile;
import com.document.immigrantvault.data.db.entity.VaultFolder;
import com.document.immigrantvault.util.VaultFileStorage;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class VaultFolderRepository {

    private final AppDatabase database;
    private final ExecutorService executor;
    private final VaultFileStorage storage;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public VaultFolderRepository(AppDatabase database, ExecutorService executor, VaultFileStorage storage) {
        this.database = database;
        this.executor = executor;
        this.storage = storage;
    }

    public LiveData<List<VaultFolder>> getByPerson(long personId) {
        return database.vaultFolderDao().getByPerson(personId);
    }

    public LiveData<VaultFolder> getById(long folderId) {
        return database.vaultFolderDao().getById(folderId);
    }

    public LiveData<List<VaultFileDao.FolderFileCount>> getFileCounts(long personId) {
        return database.vaultFileDao().countByFolderForPerson(personId);
    }

    public LiveData<List<VaultFileDao.PersonFileCount>> getPersonFileCounts() {
        return database.vaultFileDao().countByPerson();
    }

    /**
     * Creates the starter folder set the first time a person's vault is opened. Idempotent:
     * once the person has any folder at all, including ones they deleted down to a single
     * remaining folder, nothing is inserted.
     */
    public void ensureDefaultFolders(long personId, String[] defaultNames) {
        executor.execute(() -> {
            if (database.vaultFolderDao().countByPersonSync(personId) > 0) {
                return;
            }
            for (int i = 0; i < defaultNames.length; i++) {
                VaultFolder folder = new VaultFolder();
                folder.personId = personId;
                folder.name = defaultNames[i];
                folder.sortOrder = i;
                folder.isSystem = true;
                database.vaultFolderDao().insert(folder);
            }
        });
    }

    public void insert(VaultFolder folder, RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            try {
                long id = database.vaultFolderDao().insert(folder);
                folder.id = id;
                postSuccess(callback, id);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    public void rename(long folderId, String name, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                VaultFolder folder = database.vaultFolderDao().getByIdSync(folderId);
                if (folder == null) {
                    postError(callback, new IllegalStateException("Folder no longer exists."));
                    return;
                }
                folder.name = name;
                database.vaultFolderDao().update(folder);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, e);
            }
        });
    }

    /** Deletes the folder and every file inside it, removing the bytes from disk first. */
    public void delete(VaultFolder folder, RepositoryCallback<Void> callback) {
        executor.execute(() -> {
            try {
                for (VaultFile file : database.vaultFileDao().getByFolderSync(folder.id)) {
                    storage.delete(file.personId, file.storedName);
                }
                database.vaultFolderDao().delete(folder);
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, e);
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
