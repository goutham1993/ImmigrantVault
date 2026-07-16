package com.document.immigrantvault.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.document.immigrantvault.data.db.entity.LinkedEntityType;
import com.document.immigrantvault.data.db.entity.Reminder;

import java.util.Date;
import java.util.List;

@Dao
public interface ReminderDao {

    @Insert
    long insert(Reminder reminder);

    @Update
    void update(Reminder reminder);

    @Delete
    void delete(Reminder reminder);

    @Query("SELECT * FROM reminders WHERE enabled = 1 ORDER BY triggerDate ASC")
    LiveData<List<Reminder>> getAllEnabled();

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND triggerDate <= :maxDate ORDER BY triggerDate ASC")
    List<Reminder> getDueRemindersSync(Date maxDate);

    @Query("DELETE FROM reminders WHERE linkedType = :linkedType AND linkedId = :linkedId")
    void deleteByLinked(LinkedEntityType linkedType, long linkedId);

    @Query("SELECT * FROM reminders WHERE id = :id")
    Reminder getByIdSync(long id);
}
