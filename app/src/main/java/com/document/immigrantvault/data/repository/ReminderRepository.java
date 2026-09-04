package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.Document;
import com.document.immigrantvault.data.db.entity.LinkedEntityType;
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

    public static final int[] LEAD_DAY_OPTIONS = {7, 14, 30};
    public static final int DEFAULT_LEAD_DAYS = 14;

    private final AppDatabase database;
    private final ExecutorService executor;

    public ReminderRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<List<Reminder>> getAllEnabled() {
        return database.reminderDao().getAllEnabled();
    }

    public Reminder getByLinkedSync(LinkedEntityType linkedType, long linkedId) {
        return database.reminderDao().getByLinkedSync(linkedType, linkedId);
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
     * Creates or replaces a single document expiry reminder, or deletes if leadDays is null
     * or the document has no expiry date. Must be called on a background thread.
     */
    public void applyDocumentReminder(Document document, Integer leadDays) {
        database.reminderDao().deleteByLinked(LinkedEntityType.DOCUMENT, document.id);
        if (leadDays == null || document.expiryDate == null) {
            return;
        }
        int days = normalizeLeadDays(leadDays);
        String typeLabel = EnumLabels.documentType(document.type);
        String body = document.documentNumber + " expires on " + DateUtils.formatDate(document.expiryDate);
        insertReminder(
                LinkedEntityType.DOCUMENT,
                document.id,
                document.personId,
                ReminderKind.DOC_EXPIRY,
                document.expiryDate,
                days,
                typeLabel + " expiring soon",
                body
        );
    }

    /**
     * Creates or replaces a single visa expiry reminder, or deletes if leadDays is null
     * or the entry has no end date. Must be called on a background thread.
     */
    public void applyVisaReminder(VisaEntry entry, Integer leadDays) {
        database.reminderDao().deleteByLinked(LinkedEntityType.VISA, entry.id);
        if (leadDays == null || entry.endDate == null) {
            return;
        }
        int days = normalizeLeadDays(leadDays);
        String typeLabel = EnumLabels.visaType(entry.type);
        String numberPrefix = entry.visaNumber != null && !entry.visaNumber.isEmpty()
                ? entry.visaNumber + " · "
                : "";
        insertReminder(
                LinkedEntityType.VISA,
                entry.id,
                entry.personId,
                ReminderKind.VISA_EXPIRY,
                entry.endDate,
                days,
                typeLabel + " expiring soon",
                numberPrefix + typeLabel + " expires on " + DateUtils.formatDate(entry.endDate)
        );
    }

    /**
     * Collapses multiple lead-day copies into one row per linked entity for summary UIs.
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

    private void insertReminder(
            LinkedEntityType linkedType,
            long linkedId,
            long personId,
            ReminderKind kind,
            Date expiryDate,
            int leadDays,
            String title,
            String body
    ) {
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

    private static int normalizeLeadDays(int leadDays) {
        for (int option : LEAD_DAY_OPTIONS) {
            if (option == leadDays) {
                return leadDays;
            }
        }
        return DEFAULT_LEAD_DAYS;
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
