package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.EducationEntry;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class EducationRepository {

    private final AppDatabase database;
    private final ExecutorService executor;

    public EducationRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<List<EducationEntry>> getByPerson(long personId) {
        return database.educationDao().getByPerson(personId);
    }

    public void insert(EducationEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            long id = database.educationDao().insert(entry);
            entry.id = id;
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void update(EducationEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.educationDao().update(entry);
            database.timelineDao().deleteBySource(SourceEntityType.EDUCATION, entry.id);
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void delete(EducationEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.timelineDao().deleteBySource(SourceEntityType.EDUCATION, entry.id);
            database.educationDao().delete(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void addTimeline(EducationEntry entry) {
        TimelineEvent event = new TimelineEvent();
        event.personId = entry.personId;
        event.eventType = TimelineEventType.EDUCATION;
        event.title = "Education: " + (entry.institutionName != null ? entry.institutionName : "Unknown");
        StringBuilder description = new StringBuilder();
        if (entry.degree != null && !entry.degree.isEmpty()) {
            description.append(entry.degree);
        }
        if (entry.fieldOfStudy != null && !entry.fieldOfStudy.isEmpty()) {
            if (description.length() > 0) {
                description.append(" · ");
            }
            description.append(entry.fieldOfStudy);
        }
        event.description = description.length() > 0 ? description.toString() : null;
        event.eventDate = entry.startDate != null ? entry.startDate : new java.util.Date();
        event.sourceEntityType = SourceEntityType.EDUCATION;
        event.sourceEntityId = entry.id;
        database.timelineDao().insert(event);
    }
}
