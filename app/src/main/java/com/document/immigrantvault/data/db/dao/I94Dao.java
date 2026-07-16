package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.I94Entry;

@Dao
public interface I94Dao {

    @Insert
    long insert(I94Entry entry);

    @Update
    void update(I94Entry entry);

    @Query("SELECT * FROM i94_entries WHERE personId = :personId LIMIT 1")
    LiveData<I94Entry> getByPerson(long personId);

    @Query("SELECT * FROM i94_entries WHERE personId = :personId LIMIT 1")
    I94Entry getByPersonSync(long personId);
}
