package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.TaxReturnEntry;

import java.util.List;

@Dao
public interface TaxReturnDao {

    @Insert
    long insert(TaxReturnEntry entry);

    @Update
    void update(TaxReturnEntry entry);

    @Delete
    void delete(TaxReturnEntry entry);

    @Query("SELECT * FROM tax_return_entries WHERE personId = :personId "
            + "ORDER BY taxYear DESC, returnType ASC, state ASC")
    LiveData<List<TaxReturnEntry>> getByPerson(long personId);

    @Query("SELECT * FROM tax_return_entries WHERE id = :id")
    TaxReturnEntry getByIdSync(long id);
}
