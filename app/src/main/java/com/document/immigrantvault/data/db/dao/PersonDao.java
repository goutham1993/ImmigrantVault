package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.data.db.entity.Relationship;

import java.util.List;

@Dao
public interface PersonDao {

    @Insert
    long insert(Person person);

    @Update
    void update(Person person);

    @Delete
    void delete(Person person);

    @Query("SELECT * FROM persons ORDER BY sortOrder ASC, id ASC")
    LiveData<List<Person>> getAll();

    @Query("SELECT * FROM persons ORDER BY sortOrder ASC, id ASC")
    List<Person> getAllSync();

    @Query("SELECT * FROM persons WHERE id = :id")
    LiveData<Person> getById(long id);

    @Query("SELECT * FROM persons WHERE id = :id")
    Person getByIdSync(long id);

    @Query("SELECT COUNT(*) FROM persons")
    int count();

    @Query("SELECT COUNT(*) FROM persons WHERE relationship = :relationship")
    int countByRelationship(Relationship relationship);
}
