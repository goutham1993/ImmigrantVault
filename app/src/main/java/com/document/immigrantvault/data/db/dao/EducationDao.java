package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.EducationEntry;

import java.util.List;

@Dao
public interface EducationDao {

    @Insert
    long insert(EducationEntry entry);

    @Update
    void update(EducationEntry entry);

    @Delete
    void delete(EducationEntry entry);

    @Query("SELECT * FROM education_entries WHERE personId = :personId ORDER BY startDate DESC")
    LiveData<List<EducationEntry>> getByPerson(long personId);

    @Query("SELECT * FROM education_entries WHERE id = :id")
    EducationEntry getByIdSync(long id);
}
