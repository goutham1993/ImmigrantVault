package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.W2Entry;

import java.util.List;

@Dao
public interface W2Dao {

    @Insert
    long insert(W2Entry entry);

    @Update
    void update(W2Entry entry);

    @Delete
    void delete(W2Entry entry);

    @Query("SELECT * FROM w2_entries WHERE personId = :personId ORDER BY taxYear DESC, employerName ASC")
    LiveData<List<W2Entry>> getByPerson(long personId);

    @Query("SELECT * FROM w2_entries WHERE id = :id")
    W2Entry getByIdSync(long id);
}
