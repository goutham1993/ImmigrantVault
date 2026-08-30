package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.VaultFile;

import java.util.List;

@Dao
public interface VaultFileDao {

    @Insert
    long insert(VaultFile file);

    @Update
    void update(VaultFile file);

    @Delete
    void delete(VaultFile file);

    @Query("SELECT * FROM vault_files WHERE folderId = :folderId ORDER BY createdAt DESC")
    LiveData<List<VaultFile>> getByFolder(long folderId);

    @Query("SELECT * FROM vault_files WHERE folderId = :folderId ORDER BY createdAt DESC")
    List<VaultFile> getByFolderSync(long folderId);

    @Query("SELECT * FROM vault_files WHERE personId = :personId ORDER BY createdAt DESC")
    List<VaultFile> getByPersonSync(long personId);

    @Query("SELECT * FROM vault_files WHERE id = :id")
    VaultFile getByIdSync(long id);

    @Query("SELECT * FROM vault_files WHERE id = :id")
    LiveData<VaultFile> getById(long id);

    @Query("SELECT COUNT(*) FROM vault_files WHERE personId = :personId")
    LiveData<Integer> countByPerson(long personId);

    @Query("SELECT folderId, COUNT(*) AS fileCount FROM vault_files WHERE personId = :personId GROUP BY folderId")
    LiveData<List<FolderFileCount>> countByFolderForPerson(long personId);

    @Query("SELECT personId AS ownerId, COUNT(*) AS fileCount FROM vault_files GROUP BY personId")
    LiveData<List<PersonFileCount>> countByPerson();

    @Query("SELECT * FROM vault_files")
    List<VaultFile> getAllSync();

    class FolderFileCount {
        public long folderId;
        public int fileCount;
    }

    class PersonFileCount {
        public long ownerId;
        public int fileCount;
    }
}
