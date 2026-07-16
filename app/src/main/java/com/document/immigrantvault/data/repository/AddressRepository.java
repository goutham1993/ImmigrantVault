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
        event.title = entry.isCurrent ? "Current address" : "Address change";
        event.description = formatAddress(entry);
        event.eventDate = entry.startDate != null ? entry.startDate : new java.util.Date();
        event.sourceEntityType = SourceEntityType.ADDRESS;
        event.sourceEntityId = entry.id;
        database.timelineDao().insert(event);
    }

    private String formatAddress(AddressEntry entry) {
        StringBuilder sb = new StringBuilder();
        if (entry.isApartment()) {
            if (entry.apartmentNumber != null && !entry.apartmentNumber.isEmpty()) {
                sb.append("Apt ").append(entry.apartmentNumber);
            }
            if (entry.apartmentName != null && !entry.apartmentName.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(entry.apartmentName);
            }
            if (sb.length() > 0) sb.append(" · ");
        }
        if (entry.line1 != null) sb.append(entry.line1);
        if (entry.city != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.city);
        }
        if (entry.state != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.state);
        }
        return sb.toString();
    }
}
