package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.AddressEntry;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class AddressRepository {

    private final AppDatabase database;
    private final ExecutorService executor;

    public AddressRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<List<AddressEntry>> getByPerson(long personId) {
        return database.addressDao().getByPerson(personId);
    }

    public void insert(AddressEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            long id = database.addressDao().insert(entry);
            entry.id = id;
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void update(AddressEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.addressDao().update(entry);
            database.timelineDao().deleteBySource(SourceEntityType.ADDRESS, entry.id);
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void delete(AddressEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.timelineDao().deleteBySource(SourceEntityType.ADDRESS, entry.id);
            database.addressDao().delete(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void addTimeline(AddressEntry entry) {
        TimelineEvent event = new TimelineEvent();
        event.personId = entry.personId;
        event.eventType = TimelineEventType.ADDRESS_CHANGE;
        event.title = entry.isCurrent
                ? "Updated current residential address"
                : "Changed residential address";
        event.description = formatAddressSummary(entry);
        event.eventDate = entry.startDate != null ? entry.startDate : new java.util.Date();
        event.sourceEntityType = SourceEntityType.ADDRESS;
        event.sourceEntityId = entry.id;
        database.timelineDao().insert(event);
    }

    private String formatAddressSummary(AddressEntry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.city != null && !entry.city.isEmpty()) {
            sb.append(entry.city);
        }
        if (entry.state != null && !entry.state.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.state);
        }
        if (sb.length() > 0) {
            return sb.toString();
        }
        if (entry.line1 != null && !entry.line1.isEmpty()) {
            return entry.line1;
        }
        return null;
    }
}
