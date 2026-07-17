package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.UsefulLink;

import java.util.List;

@Dao
public interface UsefulLinkDao {

    @Insert
    long insert(UsefulLink link);

    @Update
    void update(UsefulLink link);

    @Delete
    void delete(UsefulLink link);

    @Query("SELECT * FROM useful_links WHERE personId = :personId ORDER BY title COLLATE NOCASE ASC")
    LiveData<List<UsefulLink>> getByPerson(long personId);

    @Query("SELECT * FROM useful_links WHERE id = :id")
    UsefulLink getByIdSync(long id);
}
