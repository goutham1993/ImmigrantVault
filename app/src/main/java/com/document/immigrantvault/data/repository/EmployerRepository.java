package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.EmployerEntry;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class EmployerRepository {

    private final AppDatabase database;
    private final ExecutorService executor;

    public EmployerRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<List<EmployerEntry>> getByPerson(long personId) {
        return database.employerDao().getByPerson(personId);
    }

    public void insert(EmployerEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            long id = database.employerDao().insert(entry);
            entry.id = id;
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void update(EmployerEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.employerDao().update(entry);
            database.timelineDao().deleteBySource(SourceEntityType.EMPLOYER, entry.id);
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void delete(EmployerEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.timelineDao().deleteBySource(SourceEntityType.EMPLOYER, entry.id);
            database.employerDao().delete(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void addTimeline(EmployerEntry entry) {
        TimelineEvent event = new TimelineEvent();
        event.personId = entry.personId;
        event.eventType = TimelineEventType.EMPLOYER_CHANGE;
        event.title = "Employer: " + (entry.employerName != null ? entry.employerName : "Unknown");
        event.description = entry.jobTitle;
        event.eventDate = entry.startDate != null ? entry.startDate : new java.util.Date();
        event.sourceEntityType = SourceEntityType.EMPLOYER;
        event.sourceEntityId = entry.id;
        database.timelineDao().insert(event);
    }
}
