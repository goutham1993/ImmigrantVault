package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.EmployerEntry;

import java.util.List;

@Dao
public interface EmployerDao {

    @Insert
    long insert(EmployerEntry entry);

    @Update
    void update(EmployerEntry entry);

    @Delete
    void delete(EmployerEntry entry);

    @Query("SELECT * FROM employer_entries WHERE personId = :personId ORDER BY startDate DESC")
    LiveData<List<EmployerEntry>> getByPerson(long personId);

    @Query("SELECT * FROM employer_entries WHERE id = :id")
    EmployerEntry getByIdSync(long id);
}
