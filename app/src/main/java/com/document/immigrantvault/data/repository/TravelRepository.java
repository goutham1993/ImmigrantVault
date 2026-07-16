package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;
import com.document.immigrantvault.data.db.entity.TravelEntry;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class TravelRepository {

    private final AppDatabase database;
    private final ExecutorService executor;

    public TravelRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<List<TravelEntry>> getByPerson(long personId) {
        return database.travelDao().getByPerson(personId);
    }

    public void insert(TravelEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            long id = database.travelDao().insert(entry);
            entry.id = id;
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void update(TravelEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.travelDao().update(entry);
            database.timelineDao().deleteBySource(SourceEntityType.TRAVEL, entry.id);
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void delete(TravelEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.timelineDao().deleteBySource(SourceEntityType.TRAVEL, entry.id);
            database.travelDao().delete(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void addTimeline(TravelEntry entry) {
        if (entry.arrivalDate != null) {
            TimelineEvent arrival = new TimelineEvent();
            arrival.personId = entry.personId;
            arrival.eventType = TimelineEventType.TRAVEL_ENTRY;
            arrival.title = "Arrived in US";
            arrival.description = formatTravelDescription(entry);
            arrival.eventDate = entry.arrivalDate;
            arrival.sourceEntityType = SourceEntityType.TRAVEL;
            arrival.sourceEntityId = entry.id;
            database.timelineDao().insert(arrival);
        }
        if (entry.departureDate != null) {
            TimelineEvent departure = new TimelineEvent();
            departure.personId = entry.personId;
            departure.eventType = TimelineEventType.TRAVEL_EXIT;
            departure.title = "Departed US";
            departure.description = formatTravelDescription(entry);
            departure.eventDate = entry.departureDate;
            departure.sourceEntityType = SourceEntityType.TRAVEL;
            departure.sourceEntityId = entry.id;
            database.timelineDao().insert(departure);
        }
    }

    private String formatTravelDescription(TravelEntry entry) {
        String departureCity = entry.departureCity != null ? entry.departureCity : "";
        String arrivalCity = entry.arrivalCity != null ? entry.arrivalCity : "";
        if (!departureCity.isEmpty() || !arrivalCity.isEmpty()) {
            return (departureCity.isEmpty() ? "—" : departureCity)
                    + " → "
                    + (arrivalCity.isEmpty() ? "—" : arrivalCity);
        }
        return entry.portOfEntry;
    }
}
