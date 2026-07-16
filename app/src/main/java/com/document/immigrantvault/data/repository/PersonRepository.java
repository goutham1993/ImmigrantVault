package com.document.immigrantvault.data.repository;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.data.db.entity.Relationship;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class PersonRepository {

    private final AppDatabase database;
    private final ExecutorService executor;
    private final ReminderRepository reminderRepository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PersonRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
        this.reminderRepository = new ReminderRepository(database, executor);
    }

    public LiveData<List<Person>> getAll() {
        return database.personDao().getAll();
    }

    public LiveData<Person> getById(long id) {
        return database.personDao().getById(id);
    }

    public void insert(Person person, Runnable onComplete) {
        executor.execute(() -> {
            person.sortOrder = database.personDao().count();
            long id = database.personDao().insert(person);
            person.id = id;
            addTimelineForPerson(person);
            reminderRepository.syncVisaReminders(person);
            completeOnMain(onComplete);
        });
    }

    public void update(Person person, Runnable onComplete) {
        executor.execute(() -> {
            database.personDao().update(person);
            addTimelineForPerson(person);
            reminderRepository.syncVisaReminders(person);
            completeOnMain(onComplete);
        });
    }

    public void delete(Person person, Runnable onComplete) {
        executor.execute(() -> {
            database.personDao().delete(person);
            completeOnMain(onComplete);
        });
    }

    private void completeOnMain(Runnable onComplete) {
        if (onComplete != null) {
            mainHandler.post(onComplete);
        }
    }

    public void ensureSelfExists() {
        executor.execute(() -> {
            if (database.personDao().countByRelationship(Relationship.SELF) == 0) {
                Person self = new Person("Me", Relationship.SELF);
                self.sortOrder = 0;
                database.personDao().insert(self);
            }
        });
    }

    public int getCountSync() {
        return database.personDao().count();
    }

    private void addTimelineForPerson(Person person) {
        if (person.visaStartDate != null) {
            TimelineEvent event = new TimelineEvent();
            event.personId = person.id;
            event.eventType = TimelineEventType.VISA_START;
            event.title = "Visa started";
            event.description = person.currentVisaType != null ? person.currentVisaType : "Visa";
            event.eventDate = person.visaStartDate;
            event.sourceEntityType = SourceEntityType.PERSON;
            event.sourceEntityId = person.id;
            database.timelineDao().insert(event);
        }
        if (person.visaEndDate != null) {
            TimelineEvent event = new TimelineEvent();
            event.personId = person.id;
            event.eventType = TimelineEventType.VISA_END;
            event.title = "Visa expires";
            event.description = person.currentVisaType != null ? person.currentVisaType : "Visa";
            event.eventDate = person.visaEndDate;
            event.sourceEntityType = SourceEntityType.PERSON;
            event.sourceEntityId = person.id;
            database.timelineDao().insert(event);
        }
    }
}
