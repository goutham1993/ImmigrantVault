package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.Petition;

import java.util.List;

@Dao
public interface PetitionDao {

    @Insert
    long insert(Petition petition);

    @Update
    void update(Petition petition);

    @Delete
    void delete(Petition petition);

    @Query("SELECT * FROM petitions WHERE personId = :personId ORDER BY filedDate DESC")
    LiveData<List<Petition>> getByPerson(long personId);

    @Query("SELECT * FROM petitions WHERE id = :id")
    Petition getByIdSync(long id);
}
