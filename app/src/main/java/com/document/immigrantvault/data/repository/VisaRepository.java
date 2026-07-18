package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.LinkedEntityType;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;
import com.document.immigrantvault.data.db.entity.VisaEntry;
import com.document.immigrantvault.util.EnumLabels;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class VisaRepository {

    private final AppDatabase database;
    private final ExecutorService executor;
    private final ReminderRepository reminderRepository;

    public VisaRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
        this.reminderRepository = new ReminderRepository(database, executor);
    }

    public LiveData<List<VisaEntry>> getByPerson(long personId) {
        return database.visaDao().getByPerson(personId);
    }

    public void insert(VisaEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            long id = database.visaDao().insert(entry);
            entry.id = id;
            addTimeline(entry);
            reminderRepository.syncVisaEntryReminders(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void update(VisaEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.visaDao().update(entry);
            database.timelineDao().deleteBySource(SourceEntityType.VISA, entry.id);
            addTimeline(entry);
            reminderRepository.syncVisaEntryReminders(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void delete(VisaEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.timelineDao().deleteBySource(SourceEntityType.VISA, entry.id);
            database.reminderDao().deleteByLinked(LinkedEntityType.VISA, entry.id);
            database.visaDao().delete(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void addTimeline(VisaEntry entry) {
        String typeLabel = EnumLabels.visaType(entry.type);

        if (entry.startDate != null) {
            TimelineEvent start = new TimelineEvent();
            start.personId = entry.personId;
            start.eventType = TimelineEventType.VISA_START;
            start.title = typeLabel + " starts";
            start.description = entry.visaNumber;
            start.eventDate = entry.startDate;
            start.sourceEntityType = SourceEntityType.VISA;
            start.sourceEntityId = entry.id;
            database.timelineDao().insert(start);
        }

        if (entry.endDate != null) {
            TimelineEvent end = new TimelineEvent();
            end.personId = entry.personId;
            end.eventType = TimelineEventType.VISA_END;
            end.title = typeLabel + " ends";
            end.description = entry.visaNumber;
            end.eventDate = entry.endDate;
            end.sourceEntityType = SourceEntityType.VISA;
            end.sourceEntityId = entry.id;
            database.timelineDao().insert(end);
        }
    }
}
