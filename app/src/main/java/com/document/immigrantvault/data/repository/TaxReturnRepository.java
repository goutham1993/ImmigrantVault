package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TaxReturnEntry;
import com.document.immigrantvault.data.db.entity.TaxReturnOutcome;
import com.document.immigrantvault.data.db.entity.TaxReturnType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class TaxReturnRepository {

    private final AppDatabase database;
    private final ExecutorService executor;

    public TaxReturnRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<List<TaxReturnEntry>> getByPerson(long personId) {
        return database.taxReturnDao().getByPerson(personId);
    }

    public void insert(TaxReturnEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            long id = database.taxReturnDao().insert(entry);
            entry.id = id;
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void update(TaxReturnEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.taxReturnDao().update(entry);
            database.timelineDao().deleteBySource(SourceEntityType.TAX_RETURN, entry.id);
            addTimeline(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void delete(TaxReturnEntry entry, Runnable onComplete) {
        executor.execute(() -> {
            database.timelineDao().deleteBySource(SourceEntityType.TAX_RETURN, entry.id);
            database.taxReturnDao().delete(entry);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void addTimeline(TaxReturnEntry entry) {
        TimelineEvent event = new TimelineEvent();
        event.personId = entry.personId;
        event.eventType = TimelineEventType.TAX_RETURN_ADDED;
        event.title = timelineTitle(entry);
        event.description = null;
        event.eventDate = entry.filedDate != null ? entry.filedDate : taxYearDate(entry.taxYear);
        event.sourceEntityType = SourceEntityType.TAX_RETURN;
        event.sourceEntityId = entry.id;
        database.timelineDao().insert(event);
    }

    private static String timelineTitle(TaxReturnEntry entry) {
        String typeLabel = entry.returnType == TaxReturnType.STATE
                ? "State" + (entry.state != null ? " · " + entry.state : "")
                : "Federal";
        String outcomeLabel = entry.outcome == TaxReturnOutcome.AMOUNT_OWED ? "owed" : "refund";
        return "Tax return " + entry.taxYear + " · " + typeLabel + " · " + outcomeLabel;
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
