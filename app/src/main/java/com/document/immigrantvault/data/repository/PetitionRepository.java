package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.LinkedEntityType;
import com.document.immigrantvault.data.db.entity.Petition;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;
import com.document.immigrantvault.util.EnumLabels;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class PetitionRepository {

    private final AppDatabase database;
    private final ExecutorService executor;

    public PetitionRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<List<Petition>> getByPerson(long personId) {
        return database.petitionDao().getByPerson(personId);
    }

    public void insert(Petition petition, Runnable onComplete) {
        executor.execute(() -> {
            long id = database.petitionDao().insert(petition);
            petition.id = id;
            addTimeline(petition);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void update(Petition petition, Runnable onComplete) {
        executor.execute(() -> {
            database.petitionDao().update(petition);
            database.timelineDao().deleteBySource(SourceEntityType.PETITION, petition.id);
            addTimeline(petition);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void delete(Petition petition, Runnable onComplete) {
        executor.execute(() -> {
            database.timelineDao().deleteBySource(SourceEntityType.PETITION, petition.id);
            database.reminderDao().deleteByLinked(LinkedEntityType.PETITION, petition.id);
            database.petitionDao().delete(petition);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void markChecked(Petition petition, Runnable onComplete) {
        executor.execute(() -> {
            petition.lastCheckedDate = new java.util.Date();
            database.petitionDao().update(petition);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void addTimeline(Petition petition) {
        if (petition.filedDate != null) {
            TimelineEvent filed = new TimelineEvent();
            filed.personId = petition.personId;
            filed.eventType = TimelineEventType.PETITION_FILED;
            filed.title = EnumLabels.petitionType(petition.type) + " petition filed";
            filed.description = formatReceiptDescription(petition);
            filed.eventDate = petition.filedDate;
            filed.sourceEntityType = SourceEntityType.PETITION;
            filed.sourceEntityId = petition.id;
            database.timelineDao().insert(filed);
        }

        if (petition.priorityDate != null) {
            TimelineEvent priority = new TimelineEvent();
            priority.personId = petition.personId;
            priority.eventType = TimelineEventType.PRIORITY_DATE;
            String title = "Priority date";
            if (petition.preferenceCategory != null) {
                title += " · " + EnumLabels.preferenceCategory(petition.preferenceCategory);
            }
            priority.title = title;
            StringBuilder description = new StringBuilder();
            if (petition.receiptNumber != null) {
                description.append(petition.receiptNumber);
            }
            if (petition.countryOfChargeability != null && !petition.countryOfChargeability.isEmpty()) {
                if (description.length() > 0) {
                    description.append(" · ");
                }
                description.append(petition.countryOfChargeability);
            }
            priority.description = description.length() > 0 ? description.toString() : null;
            priority.eventDate = petition.priorityDate;
            priority.sourceEntityType = SourceEntityType.PETITION;
            priority.sourceEntityId = petition.id;
            database.timelineDao().insert(priority);
        }

        if (petition.interviewDate != null) {
            TimelineEvent interview = new TimelineEvent();
            interview.personId = petition.personId;
            interview.eventType = TimelineEventType.PETITION_INTERVIEW;
            interview.title = EnumLabels.petitionType(petition.type) + " interview";
            interview.description = formatReceiptDescription(petition);
            interview.eventDate = petition.interviewDate;
            interview.sourceEntityType = SourceEntityType.PETITION;
            interview.sourceEntityId = petition.id;
            database.timelineDao().insert(interview);
        }

        if (petition.oathDate != null) {
            TimelineEvent oath = new TimelineEvent();
            oath.personId = petition.personId;
            oath.eventType = TimelineEventType.PETITION_OATH;
            oath.title = EnumLabels.petitionType(petition.type) + " oath ceremony";
            oath.description = formatReceiptDescription(petition);
            oath.eventDate = petition.oathDate;
            oath.sourceEntityType = SourceEntityType.PETITION;
            oath.sourceEntityId = petition.id;
            database.timelineDao().insert(oath);
        }

        TimelineEvent status = new TimelineEvent();
        status.personId = petition.personId;
        status.eventType = TimelineEventType.PETITION_STATUS;
        status.title = formatStatusTitle(petition);
        status.description = formatReceiptDescription(petition);
        status.eventDate = petition.lastCheckedDate != null
                ? petition.lastCheckedDate
                : new java.util.Date();
        status.sourceEntityType = SourceEntityType.PETITION;
        status.sourceEntityId = petition.id;
        database.timelineDao().insert(status);
    }

    private static String formatStatusTitle(Petition petition) {
        String type = EnumLabels.petitionType(petition.type);
        if (petition.status == null) {
            return type + " petition status updated";
        }
        switch (petition.status) {
            case APPROVED:
                return type + " petition approved";
            case DENIED:
                return type + " petition denied";
            case RFE:
                return type + " petition — RFE received";
            case PENDING:
                return type + " petition pending";
            default:
                return type + " — " + EnumLabels.petitionStatus(petition.status);
        }
    }

    private static String formatReceiptDescription(Petition petition) {
        if (petition.receiptNumber == null || petition.receiptNumber.isEmpty()) {
            return "USCIS";
        }
        return "USCIS · Receipt " + petition.receiptNumber;
    }
}
