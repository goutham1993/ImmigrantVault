package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;

import java.util.List;

@Dao
public interface TimelineDao {

    @Insert
    long insert(TimelineEvent event);

    @Delete
    void delete(TimelineEvent event);

    @Query("SELECT * FROM timeline_events WHERE personId = :personId ORDER BY eventDate DESC")
    LiveData<List<TimelineEvent>> getByPerson(long personId);

    @Query("DELETE FROM timeline_events WHERE sourceEntityType = :sourceType AND sourceEntityId = :sourceId")
    void deleteBySource(SourceEntityType sourceType, long sourceId);
}
