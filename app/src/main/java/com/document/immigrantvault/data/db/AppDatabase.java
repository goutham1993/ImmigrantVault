package com.document.immigrantvault.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.document.immigrantvault.data.db.dao.BackupDao;
import com.document.immigrantvault.data.db.dao.AddressDao;
import com.document.immigrantvault.data.db.dao.DocumentDao;
import com.document.immigrantvault.data.db.dao.EducationDao;
import com.document.immigrantvault.data.db.dao.EmployerDao;
import com.document.immigrantvault.data.db.dao.I94Dao;
import com.document.immigrantvault.data.db.dao.PersonDao;
import com.document.immigrantvault.data.db.dao.PetitionDao;
import com.document.immigrantvault.data.db.dao.ReminderDao;
import com.document.immigrantvault.data.db.dao.TimelineDao;
import com.document.immigrantvault.data.db.dao.TravelDao;
import com.document.immigrantvault.data.db.dao.UsefulLinkDao;
import com.document.immigrantvault.data.db.dao.VisaDao;
import com.document.immigrantvault.data.db.dao.W2Dao;
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
import com.document.immigrantvault.data.db.entity.UsefulLink;
import com.document.immigrantvault.data.db.entity.VisaEntry;
import com.document.immigrantvault.data.db.entity.W2Entry;

@Database(
        entities = {
                Person.class,
                Document.class,
                AddressEntry.class,
                EmployerEntry.class,
                EducationEntry.class,
                I94Entry.class,
                TravelEntry.class,
                Petition.class,
                VisaEntry.class,
                UsefulLink.class,
                W2Entry.class,
                Reminder.class,
                TimelineEvent.class
        },
        version = 16,
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
    public abstract EducationDao educationDao();
    public abstract I94Dao i94Dao();
    public abstract TravelDao travelDao();
    public abstract PetitionDao petitionDao();
    public abstract VisaDao visaDao();
    public abstract UsefulLinkDao usefulLinkDao();
    public abstract W2Dao w2Dao();
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
                            DatabaseMigrations.MIGRATION_10_11,
                            DatabaseMigrations.MIGRATION_11_12,
                            DatabaseMigrations.MIGRATION_12_13,
                            DatabaseMigrations.MIGRATION_13_14,
                            DatabaseMigrations.MIGRATION_14_15,
                            DatabaseMigrations.MIGRATION_15_16
                    )
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
