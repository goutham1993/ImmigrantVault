package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.Document;

import java.util.List;

@Dao
public interface DocumentDao {

    @Insert
    long insert(Document document);

    @Update
    void update(Document document);

    @Delete
    void delete(Document document);

    @Query("SELECT * FROM documents WHERE personId = :personId ORDER BY expiryDate ASC")
    LiveData<List<Document>> getByPerson(long personId);

    @Query("SELECT * FROM documents WHERE id = :id")
    Document getByIdSync(long id);
}
