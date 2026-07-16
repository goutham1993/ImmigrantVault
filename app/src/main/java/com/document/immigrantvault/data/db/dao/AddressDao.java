package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.AddressEntry;

import java.util.List;

@Dao
public interface AddressDao {

    @Insert
    long insert(AddressEntry entry);

    @Update
    void update(AddressEntry entry);

    @Delete
    void delete(AddressEntry entry);

    @Query("SELECT * FROM address_entries WHERE personId = :personId ORDER BY startDate DESC")
    LiveData<List<AddressEntry>> getByPerson(long personId);

    @Query("SELECT * FROM address_entries WHERE id = :id")
    AddressEntry getByIdSync(long id);
}
