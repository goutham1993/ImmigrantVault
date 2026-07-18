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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public void deleteByPersonId(long personId) {
        database.reminderDao().deleteByPersonId(personId);
    }

    public List<Reminder> getDueRemindersSync() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 30);
        return database.reminderDao().getDueRemindersSync(cal.getTime());
    }

    /**
     * Removes person-level visa reminders that duplicate a visa-history entry
     * with the same end date. Safe to call on app start for existing data.
     */
    public void reconcileOverlappingVisaReminders() {
        executor.execute(() -> {
            List<Person> persons = database.personDao().getAllSync();
            for (Person person : persons) {
                if (person.visaEndDate == null) {
                    continue;
                }
                if (hasVisaEntryWithEndDate(person.id, person.visaEndDate)) {
                    database.reminderDao().deleteByLinked(LinkedEntityType.PERSON, person.id);
                }
            }
        });
    }

    public void syncDocumentReminders(Document document) {
        database.reminderDao().deleteByLinked(LinkedEntityType.DOCUMENT, document.id);
        if (document.expiryDate == null) {
            return;
        }
        String typeLabel = EnumLabels.documentType(document.type);
        String body = document.documentNumber + " expires on " + DateUtils.formatDate(document.expiryDate);
        insertLeadDayReminders(
                LinkedEntityType.DOCUMENT,
                document.id,
                document.personId,
                ReminderKind.DOC_EXPIRY,
                document.expiryDate,
                typeLabel + " expiring soon",
                body
        );
    }

    /**
     * Person overview visa dates create reminders only when no visa-history entry
     * already covers the same end date (avoids duplicate VISA_EXPIRY rows).
     */
    public void syncVisaReminders(Person person) {
        database.reminderDao().deleteByLinked(LinkedEntityType.PERSON, person.id);
        if (person.visaEndDate == null) {
            return;
        }
        if (hasVisaEntryWithEndDate(person.id, person.visaEndDate)) {
            return;
        }
        String typeLabel = person.currentVisaType != null && !person.currentVisaType.isEmpty()
                ? person.currentVisaType
                : "Visa";
        insertLeadDayReminders(
                LinkedEntityType.PERSON,
                person.id,
                person.id,
                ReminderKind.VISA_EXPIRY,
                person.visaEndDate,
                "Visa expiring soon",
                typeLabel + " for " + person.name + " expires on " + DateUtils.formatDate(person.visaEndDate)
        );
    }

    public void syncVisaEntryReminders(VisaEntry entry) {
        database.reminderDao().deleteByLinked(LinkedEntityType.VISA, entry.id);
        if (entry.endDate == null) {
            return;
        }
        String typeLabel = EnumLabels.visaType(entry.type);
        String numberPrefix = entry.visaNumber != null && !entry.visaNumber.isEmpty()
                ? entry.visaNumber + " · "
                : "";
        insertLeadDayReminders(
                LinkedEntityType.VISA,
                entry.id,
                entry.personId,
                ReminderKind.VISA_EXPIRY,
                entry.endDate,
                typeLabel + " expiring soon",
                numberPrefix + typeLabel + " expires on " + DateUtils.formatDate(entry.endDate)
        );

        // Drop overview-level duplicates for the same end date.
        Person person = database.personDao().getByIdSync(entry.personId);
        if (person != null && sameCalendarDay(person.visaEndDate, entry.endDate)) {
            database.reminderDao().deleteByLinked(LinkedEntityType.PERSON, person.id);
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

    /**
     * Collapses 30/14/7 lead-day copies into one row per linked entity for summary UIs.
     * Keeps the soonest upcoming trigger (or the earliest if all are past).
     */
    public static List<Reminder> collapseByLinkedEntity(List<Reminder> reminders) {
        if (reminders == null || reminders.isEmpty()) {
            return new ArrayList<>();
        }
        Date today = startOfDay(new Date());
        Map<String, Reminder> best = new LinkedHashMap<>();
        for (Reminder reminder : reminders) {
            String key = reminder.linkedType + ":" + reminder.linkedId;
            Reminder existing = best.get(key);
            if (existing == null || isBetterSummaryReminder(reminder, existing, today)) {
                best.put(key, reminder);
            }
        }
        return new ArrayList<>(best.values());
    }

    private void insertLeadDayReminders(
            LinkedEntityType linkedType,
            long linkedId,
            long personId,
            ReminderKind kind,
            Date expiryDate,
            String title,
            String body
    ) {
        for (int leadDays : DEFAULT_LEAD_DAYS) {
            Reminder reminder = new Reminder();
            reminder.linkedType = linkedType;
            reminder.linkedId = linkedId;
            reminder.personId = personId;
            reminder.reminderKind = kind;
            reminder.triggerDate = DateUtils.addDays(expiryDate, -leadDays);
            reminder.leadDays = leadDays;
            reminder.title = title;
            reminder.body = body;
            database.reminderDao().insert(reminder);
        }
    }

    private boolean hasVisaEntryWithEndDate(long personId, Date endDate) {
        List<VisaEntry> entries = database.visaDao().getByPersonSync(personId);
        for (VisaEntry entry : entries) {
            if (sameCalendarDay(entry.endDate, endDate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameCalendarDay(Date a, Date b) {
        if (a == null || b == null) {
            return false;
        }
        Calendar ca = Calendar.getInstance();
        ca.setTime(a);
        Calendar cb = Calendar.getInstance();
        cb.setTime(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    private static boolean isBetterSummaryReminder(Reminder candidate, Reminder current, Date today) {
        if (candidate.triggerDate == null) {
            return false;
        }
        if (current.triggerDate == null) {
            return true;
        }
        boolean candidateUpcoming = !candidate.triggerDate.before(today);
        boolean currentUpcoming = !current.triggerDate.before(today);
        if (candidateUpcoming != currentUpcoming) {
            return candidateUpcoming;
        }
        return candidate.triggerDate.before(current.triggerDate);
    }

    private static Date startOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
