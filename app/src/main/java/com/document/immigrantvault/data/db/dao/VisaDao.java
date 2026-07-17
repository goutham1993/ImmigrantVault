package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.VisaEntry;

import java.util.List;

@Dao
public interface VisaDao {

    @Insert
    long insert(VisaEntry entry);

    @Update
    void update(VisaEntry entry);

    @Delete
    void delete(VisaEntry entry);

    @Query("SELECT * FROM visa_entries WHERE personId = :personId ORDER BY startDate DESC")
    LiveData<List<VisaEntry>> getByPerson(long personId);

    @Query("SELECT * FROM visa_entries WHERE id = :id")
    VisaEntry getByIdSync(long id);
}
