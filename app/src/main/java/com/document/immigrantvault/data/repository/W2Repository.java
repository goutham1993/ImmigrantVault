package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;
import com.document.immigrantvault.data.db.entity.W2Entry;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class W2Repository {

    private final AppDatabase database;
    private final ExecutorService executor;

    public W2Repository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<List<W2Entry>> getByPerson(long personId) {
        return database.w2Dao().getByPerson(personId);
    }

    public void insert(W2Entry entry, Runnable onComplete) {
        executor.execute(() -> {
            long id = database.w2Dao().insert(entry);
            entry.id = id;
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void update(W2Entry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.w2Dao().update(entry);
            database.timelineDao().deleteBySource(SourceEntityType.W2, entry.id);
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void delete(W2Entry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.timelineDao().deleteBySource(SourceEntityType.W2, entry.id);
            database.w2Dao().delete(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void addTimeline(W2Entry entry) {
        TimelineEvent event = new TimelineEvent();
        event.personId = entry.personId;
        event.eventType = TimelineEventType.W2_ADDED;
        String employer = entry.employerName != null ? entry.employerName : "Unknown";
        event.title = "W-2 " + entry.taxYear + " · " + employer;
        event.description = null;
        event.eventDate = taxYearDate(entry.taxYear);
        event.sourceEntityType = SourceEntityType.W2;
        event.sourceEntityId = entry.id;
        database.timelineDao().insert(event);
    }

    private static Date taxYearDate(int taxYear) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, taxYear);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }
}
