package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.Document;
import com.document.immigrantvault.data.db.entity.LinkedEntityType;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;
import com.document.immigrantvault.util.EnumLabels;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class DocumentRepository {

    private final AppDatabase database;
    private final ExecutorService executor;
    private final ReminderRepository reminderRepository;

    public DocumentRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
        this.reminderRepository = new ReminderRepository(database, executor);
    }

    public LiveData<List<Document>> getByPerson(long personId) {
        return database.documentDao().getByPerson(personId);
    }

    public void insert(Document document, Runnable onComplete) {
        executor.execute(() -> {
            long id = database.documentDao().insert(document);
            document.id = id;
            addTimeline(document);
            reminderRepository.syncDocumentReminders(document);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void update(Document document, Runnable onComplete) {
        executor.execute(() -> {
            database.documentDao().update(document);
            database.timelineDao().deleteBySource(SourceEntityType.DOCUMENT, document.id);
            database.reminderDao().deleteByLinked(LinkedEntityType.DOCUMENT, document.id);
            addTimeline(document);
            reminderRepository.syncDocumentReminders(document);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void delete(Document document, Runnable onComplete) {
        executor.execute(() -> {
            database.timelineDao().deleteBySource(SourceEntityType.DOCUMENT, document.id);
            database.reminderDao().deleteByLinked(LinkedEntityType.DOCUMENT, document.id);
            database.documentDao().delete(document);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void addTimeline(Document document) {
        TimelineEvent added = new TimelineEvent();
        added.personId = document.personId;
        added.eventType = TimelineEventType.DOCUMENT_ADDED;
        added.title = EnumLabels.documentType(document.type) + " added";
        added.description = document.documentNumber;
        added.eventDate = document.issueDate != null ? document.issueDate : new java.util.Date();
        added.sourceEntityType = SourceEntityType.DOCUMENT;
        added.sourceEntityId = document.id;
        database.timelineDao().insert(added);

        if (document.expiryDate != null) {
            TimelineEvent expiry = new TimelineEvent();
            expiry.personId = document.personId;
            expiry.eventType = TimelineEventType.DOCUMENT_EXPIRY;
            expiry.title = EnumLabels.documentType(document.type) + " expires";
            expiry.description = document.documentNumber;
            expiry.eventDate = document.expiryDate;
            expiry.sourceEntityType = SourceEntityType.DOCUMENT;
            expiry.sourceEntityId = document.id;
            database.timelineDao().insert(expiry);
        }
    }
}
