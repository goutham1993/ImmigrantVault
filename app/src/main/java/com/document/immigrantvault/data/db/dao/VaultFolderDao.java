package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.VaultFolder;

import java.util.List;

@Dao
public interface VaultFolderDao {

    @Insert
    long insert(VaultFolder folder);

    @Update
    void update(VaultFolder folder);

    @Delete
    void delete(VaultFolder folder);

    @Query("SELECT * FROM vault_folders WHERE personId = :personId ORDER BY sortOrder ASC, name ASC")
    LiveData<List<VaultFolder>> getByPerson(long personId);

    @Query("SELECT * FROM vault_folders WHERE personId = :personId ORDER BY sortOrder ASC, name ASC")
    List<VaultFolder> getByPersonSync(long personId);

    @Query("SELECT * FROM vault_folders WHERE id = :id")
    VaultFolder getByIdSync(long id);

    @Query("SELECT * FROM vault_folders WHERE id = :id")
    LiveData<VaultFolder> getById(long id);

    @Query("SELECT COUNT(*) FROM vault_folders WHERE personId = :personId")
    int countByPersonSync(long personId);

    @Query("SELECT * FROM vault_folders")
    List<VaultFolder> getAllSync();
}
