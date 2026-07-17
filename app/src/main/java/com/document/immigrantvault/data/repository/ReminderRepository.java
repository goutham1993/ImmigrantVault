package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.Document;
import com.document.immigrantvault.data.db.entity.LinkedEntityType;
import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.data.db.entity.Petition;
import com.document.immigrantvault.data.db.entity.PetitionStatus;
import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.data.db.entity.ReminderKind;
import com.document.immigrantvault.data.db.entity.VisaEntry;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.EnumLabels;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class ReminderRepository {

    private static final int[] DEFAULT_LEAD_DAYS = {30, 14, 7};

    private final AppDatabase database;
    private final ExecutorService executor;

    public ReminderRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<List<Reminder>> getAllEnabled() {
        return database.reminderDao().getAllEnabled();
    }

    public void update(Reminder reminder) {
        executor.execute(() -> database.reminderDao().update(reminder));
    }

    public List<Reminder> getDueRemindersSync() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 30);
        return database.reminderDao().getDueRemindersSync(cal.getTime());
    }

    public void syncDocumentReminders(Document document) {
        if (document.expiryDate == null) {
            return;
        }
        database.reminderDao().deleteByLinked(LinkedEntityType.DOCUMENT, document.id);
        for (int leadDays : DEFAULT_LEAD_DAYS) {
            Reminder reminder = new Reminder();
            reminder.linkedType = LinkedEntityType.DOCUMENT;
            reminder.linkedId = document.id;
            reminder.personId = document.personId;
            reminder.reminderKind = ReminderKind.DOC_EXPIRY;
            reminder.triggerDate = DateUtils.addDays(document.expiryDate, -leadDays);
            reminder.leadDays = leadDays;
            reminder.title = EnumLabels.documentType(document.type) + " expiring soon";
            reminder.body = document.documentNumber + " expires on " + DateUtils.formatDate(document.expiryDate);
            database.reminderDao().insert(reminder);
        }
    }

    public void syncVisaReminders(Person person) {
        if (person.visaEndDate == null) {
            return;
        }
        database.reminderDao().deleteByLinked(LinkedEntityType.PERSON, person.id);
        for (int leadDays : DEFAULT_LEAD_DAYS) {
            Reminder reminder = new Reminder();
            reminder.linkedType = LinkedEntityType.PERSON;
            reminder.linkedId = person.id;
            reminder.personId = person.id;
            reminder.reminderKind = ReminderKind.VISA_EXPIRY;
            reminder.triggerDate = DateUtils.addDays(person.visaEndDate, -leadDays);
            reminder.leadDays = leadDays;
            reminder.title = "Visa expiring soon";
            reminder.body = (person.currentVisaType != null ? person.currentVisaType : "Visa")
                    + " for " + person.name + " expires on " + DateUtils.formatDate(person.visaEndDate);
            database.reminderDao().insert(reminder);
        }
    }

    public void syncVisaEntryReminders(VisaEntry entry) {
        database.reminderDao().deleteByLinked(LinkedEntityType.VISA, entry.id);
        if (entry.endDate == null) {
            return;
        }
        String typeLabel = EnumLabels.visaType(entry.type);
        for (int leadDays : DEFAULT_LEAD_DAYS) {
            Reminder reminder = new Reminder();
            reminder.linkedType = LinkedEntityType.VISA;
            reminder.linkedId = entry.id;
            reminder.personId = entry.personId;
            reminder.reminderKind = ReminderKind.VISA_EXPIRY;
            reminder.triggerDate = DateUtils.addDays(entry.endDate, -leadDays);
            reminder.leadDays = leadDays;
            reminder.title = typeLabel + " expiring soon";
            reminder.body = (entry.visaNumber != null && !entry.visaNumber.isEmpty()
                    ? entry.visaNumber + " · "
                    : "")
                    + typeLabel + " expires on " + DateUtils.formatDate(entry.endDate);
            database.reminderDao().insert(reminder);
        }
    }

    public void syncPetitionReminders(Petition petition) {
        database.reminderDao().deleteByLinked(LinkedEntityType.PETITION, petition.id);
        if (petition.status != PetitionStatus.PENDING) {
            return;
        }
        Date baseDate = petition.lastCheckedDate != null ? petition.lastCheckedDate : petition.filedDate;
        if (baseDate == null) {
            baseDate = new Date();
        }
        Reminder reminder = new Reminder();
        reminder.linkedType = LinkedEntityType.PETITION;
        reminder.linkedId = petition.id;
        reminder.personId = petition.personId;
        reminder.reminderKind = ReminderKind.PETITION_CHECK;
        reminder.triggerDate = DateUtils.addDays(baseDate, petition.checkIntervalDays);
        reminder.leadDays = 0;
        reminder.title = "Check petition status";
        reminder.body = EnumLabels.petitionType(petition.type) + " (" + petition.receiptNumber + ")";
        database.reminderDao().insert(reminder);
    }
}
