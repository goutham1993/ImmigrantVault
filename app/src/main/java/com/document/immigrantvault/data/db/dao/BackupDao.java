package com.document.immigrantvault.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.document.immigrantvault.data.backup.VaultBackup;
import com.document.immigrantvault.data.db.entity.AddressEntry;
import com.document.immigrantvault.data.db.entity.Document;
import com.document.immigrantvault.data.db.entity.EducationEntry;
import com.document.immigrantvault.data.db.entity.EmployerEntry;
import com.document.immigrantvault.data.db.entity.I94Entry;
import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.data.db.entity.Petition;
import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TravelEntry;

import java.util.List;

@Dao
public interface BackupDao {

    @Query("SELECT * FROM persons ORDER BY sortOrder ASC, id ASC")
    List<Person> getAllPersonsSync();

    @Query("SELECT * FROM documents")
    List<Document> getAllDocumentsSync();

    @Query("SELECT * FROM address_entries")
    List<AddressEntry> getAllAddressesSync();

    @Query("SELECT * FROM employer_entries")
    List<EmployerEntry> getAllEmployersSync();

    @Query("SELECT * FROM education_entries")
    List<EducationEntry> getAllEducationSync();

    @Query("SELECT * FROM i94_entries")
    List<I94Entry> getAllI94Sync();

    @Query("SELECT * FROM travel_entries")
    List<TravelEntry> getAllTravelSync();

    @Query("SELECT * FROM petitions")
    List<Petition> getAllPetitionsSync();

    @Query("SELECT * FROM reminders")
    List<Reminder> getAllRemindersSync();

    @Query("SELECT * FROM timeline_events")
    List<TimelineEvent> getAllTimelineEventsSync();

    @Query("DELETE FROM timeline_events")
    void deleteAllTimelineEvents();

    @Query("DELETE FROM reminders")
    void deleteAllReminders();

    @Query("DELETE FROM travel_entries")
    void deleteAllTravelEntries();

    @Query("DELETE FROM i94_entries")
    void deleteAllI94Entries();

    @Query("DELETE FROM petitions")
    void deleteAllPetitions();

    @Query("DELETE FROM education_entries")
    void deleteAllEducationEntries();

    @Query("DELETE FROM employer_entries")
    void deleteAllEmployerEntries();

    @Query("DELETE FROM address_entries")
    void deleteAllAddressEntries();

    @Query("DELETE FROM documents")
    void deleteAllDocuments();

    @Query("DELETE FROM persons")
    void deleteAllPersons();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPersons(List<Person> persons);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDocuments(List<Document> documents);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAddresses(List<AddressEntry> addresses);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEmployers(List<EmployerEntry> employers);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertEducationEntries(List<EducationEntry> entries);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertI94Entries(List<I94Entry> entries);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTravelEntries(List<TravelEntry> entries);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPetitions(List<Petition> petitions);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReminders(List<Reminder> reminders);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTimelineEvents(List<TimelineEvent> events);

    default VaultBackup exportAll() {
        VaultBackup backup = new VaultBackup();
        backup.exportedAt = System.currentTimeMillis();
        backup.persons = getAllPersonsSync();
        backup.documents = getAllDocumentsSync();
        backup.addresses = getAllAddressesSync();
        backup.employers = getAllEmployersSync();
        backup.educationEntries = getAllEducationSync();
        backup.i94Entries = getAllI94Sync();
        backup.travelEntries = getAllTravelSync();
        backup.petitions = getAllPetitionsSync();
        backup.reminders = getAllRemindersSync();
        backup.timelineEvents = getAllTimelineEventsSync();
        return backup;
    }

    @Transaction
    default void replaceAll(VaultBackup backup) {
        deleteAllTimelineEvents();
        deleteAllReminders();
        deleteAllTravelEntries();
        deleteAllI94Entries();
        deleteAllPetitions();
        deleteAllEducationEntries();
        deleteAllEmployerEntries();
        deleteAllAddressEntries();
        deleteAllDocuments();
        deleteAllPersons();

        if (backup.persons != null && !backup.persons.isEmpty()) {
            insertPersons(backup.persons);
        }
        if (backup.documents != null && !backup.documents.isEmpty()) {
            insertDocuments(backup.documents);
        }
        if (backup.addresses != null && !backup.addresses.isEmpty()) {
            insertAddresses(backup.addresses);
        }
        if (backup.employers != null && !backup.employers.isEmpty()) {
            insertEmployers(backup.employers);
        }
        if (backup.educationEntries != null && !backup.educationEntries.isEmpty()) {
            insertEducationEntries(backup.educationEntries);
        }
        if (backup.i94Entries != null && !backup.i94Entries.isEmpty()) {
            insertI94Entries(backup.i94Entries);
        }
        if (backup.travelEntries != null && !backup.travelEntries.isEmpty()) {
            insertTravelEntries(backup.travelEntries);
        }
        if (backup.petitions != null && !backup.petitions.isEmpty()) {
            insertPetitions(backup.petitions);
        }
        if (backup.reminders != null && !backup.reminders.isEmpty()) {
            insertReminders(backup.reminders);
        }
        if (backup.timelineEvents != null && !backup.timelineEvents.isEmpty()) {
            insertTimelineEvents(backup.timelineEvents);
        }
    }
}
