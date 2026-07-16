package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.TimelineEvent;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class TimelineRepository {

    private final AppDatabase database;
    private final ExecutorService executor;

    public TimelineRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<List<TimelineEvent>> getByPerson(long personId) {
        return database.timelineDao().getByPerson(personId);
    }
}
