package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.LinkedEntityType;
import com.document.immigrantvault.data.db.entity.Petition;
import com.document.immigrantvault.data.db.entity.PetitionStatus;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;
import com.document.immigrantvault.util.EnumLabels;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class PetitionRepository {

    private final AppDatabase database;
    private final ExecutorService executor;
    private final ReminderRepository reminderRepository;

    public PetitionRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
        this.reminderRepository = new ReminderRepository(database, executor);
    }

    public LiveData<List<Petition>> getByPerson(long personId) {
        return database.petitionDao().getByPerson(personId);
    }

    public void insert(Petition petition, Runnable onComplete) {
        executor.execute(() -> {
            long id = database.petitionDao().insert(petition);
            petition.id = id;
            addTimeline(petition);
            reminderRepository.syncPetitionReminders(petition);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void update(Petition petition, Runnable onComplete) {
        executor.execute(() -> {
            database.petitionDao().update(petition);
            database.timelineDao().deleteBySource(SourceEntityType.PETITION, petition.id);
            database.reminderDao().deleteByLinked(LinkedEntityType.PETITION, petition.id);
            addTimeline(petition);
            reminderRepository.syncPetitionReminders(petition);
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
            if (petition.status == PetitionStatus.PENDING) {
                reminderRepository.syncPetitionReminders(petition);
            }
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
            filed.title = EnumLabels.petitionType(petition.type) + " filed";
            filed.description = petition.receiptNumber;
            filed.eventDate = petition.filedDate;
            filed.sourceEntityType = SourceEntityType.PETITION;
            filed.sourceEntityId = petition.id;
            database.timelineDao().insert(filed);
        }

        TimelineEvent status = new TimelineEvent();
        status.personId = petition.personId;
        status.eventType = TimelineEventType.PETITION_STATUS;
        status.title = EnumLabels.petitionType(petition.type) + " — " + EnumLabels.petitionStatus(petition.status);
        status.description = petition.receiptNumber;
        status.eventDate = petition.lastCheckedDate != null ? petition.lastCheckedDate : new java.util.Date();
        status.sourceEntityType = SourceEntityType.PETITION;
        status.sourceEntityId = petition.id;
        database.timelineDao().insert(status);
    }
}
