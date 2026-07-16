package com.document.immigrantvault.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.document.immigrantvault.data.db.dao.BackupDao;
import com.document.immigrantvault.data.db.dao.AddressDao;
import com.document.immigrantvault.data.db.dao.DocumentDao;
import com.document.immigrantvault.data.db.dao.EmployerDao;
import com.document.immigrantvault.data.db.dao.I94Dao;
import com.document.immigrantvault.data.db.dao.PersonDao;
import com.document.immigrantvault.data.db.dao.PetitionDao;
import com.document.immigrantvault.data.db.dao.ReminderDao;
import com.document.immigrantvault.data.db.dao.TimelineDao;
import com.document.immigrantvault.data.db.dao.TravelDao;
import com.document.immigrantvault.data.db.entity.AddressEntry;
import com.document.immigrantvault.data.db.entity.Document;
import com.document.immigrantvault.data.db.entity.EmployerEntry;
import com.document.immigrantvault.data.db.entity.I94Entry;
import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.data.db.entity.Petition;
import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TravelEntry;

@Database(
        entities = {
                Person.class,
                Document.class,
                AddressEntry.class,
                EmployerEntry.class,
                I94Entry.class,
                TravelEntry.class,
                Petition.class,
                Reminder.class,
                TimelineEvent.class
        },
        version = 11,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract BackupDao backupDao();
    public abstract PersonDao personDao();
    public abstract DocumentDao documentDao();
    public abstract AddressDao addressDao();
    public abstract EmployerDao employerDao();
    public abstract I94Dao i94Dao();
    public abstract TravelDao travelDao();
    public abstract PetitionDao petitionDao();
    public abstract ReminderDao reminderDao();
    public abstract TimelineDao timelineDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "immigrant_vault_db"
                    ).addMigrations(
                            DatabaseMigrations.MIGRATION_1_2,
                            DatabaseMigrations.MIGRATION_2_3,
                            DatabaseMigrations.MIGRATION_3_4,
                            DatabaseMigrations.MIGRATION_4_5,
                            DatabaseMigrations.MIGRATION_5_6,
                            DatabaseMigrations.MIGRATION_6_7,
                            DatabaseMigrations.MIGRATION_7_8,
                            DatabaseMigrations.MIGRATION_8_9,
                            DatabaseMigrations.MIGRATION_9_10,
                            DatabaseMigrations.MIGRATION_10_11
                    )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
