package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.TravelEntry;

import java.util.List;

@Dao
public interface TravelDao {

    @Insert
    long insert(TravelEntry entry);

    @Update
    void update(TravelEntry entry);

    @Delete
    void delete(TravelEntry entry);

    @Query("SELECT * FROM travel_entries WHERE personId = :personId ORDER BY arrivalDate DESC")
    LiveData<List<TravelEntry>> getByPerson(long personId);

    @Query("SELECT * FROM travel_entries WHERE id = :id")
    TravelEntry getByIdSync(long id);
}
