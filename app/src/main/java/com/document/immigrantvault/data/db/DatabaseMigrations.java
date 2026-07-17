package com.document.immigrantvault.data.db;

import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

final class DatabaseMigrations {

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE persons ADD COLUMN currentEmployer TEXT");
            db.execSQL("ALTER TABLE persons ADD COLUMN currentRole TEXT");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!columnExists(db, "persons", "dateOfBirth")) {
                db.execSQL("ALTER TABLE persons ADD COLUMN dateOfBirth INTEGER");
            }
            if (columnExists(db, "documents", "issuingAuthority")) {
                rebuildDocumentsTable(db, false);
            }
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (columnExists(db, "documents", "issuingAuthority")) {
                rebuildDocumentsTable(db, true);
            }
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (columnExists(db, "travel_entries", "classOfAdmission")
                    || columnExists(db, "travel_entries", "i94Number")
                    || columnExists(db, "travel_entries", "admitUntilDate")
                    || !columnExists(db, "travel_entries", "arrivalCity")) {
                rebuildTravelEntriesTable(db);
            }
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `i94_entries` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`personId` INTEGER NOT NULL, "
                    + "`i94Number` TEXT, "
                    + "`arrivalDate` INTEGER, "
                    + "`admitUntilDate` INTEGER, "
                    + "`classOfAdmission` TEXT, "
                    + "`portOfEntry` TEXT, "
                    + "`notes` TEXT, "
                    + "FOREIGN KEY(`personId`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
                    + ")");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_i94_entries_personId` "
                    + "ON `i94_entries` (`personId`)");
        }
    };

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!columnExists(db, "address_entries", "dwellingType")) {
                db.execSQL("ALTER TABLE address_entries ADD COLUMN dwellingType TEXT");
                db.execSQL("UPDATE address_entries SET dwellingType = 'home' WHERE dwellingType IS NULL");
            }
            if (!columnExists(db, "address_entries", "apartmentName")) {
                db.execSQL("ALTER TABLE address_entries ADD COLUMN apartmentName TEXT");
            }
            if (!columnExists(db, "address_entries", "apartmentNumber")) {
                db.execSQL("ALTER TABLE address_entries ADD COLUMN apartmentNumber TEXT");
            }
        }
    };

    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!columnExists(db, "employer_entries", "isCurrent")) {
                db.execSQL("ALTER TABLE employer_entries ADD COLUMN isCurrent INTEGER NOT NULL DEFAULT 0");
                db.execSQL("UPDATE employer_entries SET isCurrent = 1 WHERE endDate IS NULL");
            }
        }
    };

    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!columnExists(db, "employer_entries", "city")) {
                db.execSQL("ALTER TABLE employer_entries ADD COLUMN city TEXT");
            }
        }
    };

    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!columnExists(db, "i94_entries", "documentNumber")) {
                db.execSQL("ALTER TABLE i94_entries ADD COLUMN documentNumber TEXT");
            }
            if (!columnExists(db, "i94_entries", "countryOfCitizenship")) {
                db.execSQL("ALTER TABLE i94_entries ADD COLUMN countryOfCitizenship TEXT");
            }
        }
    };

    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!columnExists(db, "travel_entries", "airline")) {
                db.execSQL("ALTER TABLE travel_entries ADD COLUMN airline TEXT");
            }
        }
    };

    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `education_entries` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`personId` INTEGER NOT NULL, "
                    + "`institutionName` TEXT, "
                    + "`degree` TEXT, "
                    + "`fieldOfStudy` TEXT, "
                    + "`city` TEXT, "
                    + "`country` TEXT, "
                    + "`gpa` TEXT, "
                    + "`startDate` INTEGER, "
                    + "`endDate` INTEGER, "
                    + "FOREIGN KEY(`personId`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
                    + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_education_entries_personId` "
                    + "ON `education_entries` (`personId`)");
        }
    };

    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            if (!columnExists(db, "persons", "ssnLast4")) {
                db.execSQL("ALTER TABLE persons ADD COLUMN ssnLast4 TEXT");
            }
        }
    };

    private static void rebuildTravelEntriesTable(SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS travel_entries_migration_tmp");
        db.execSQL("CREATE TABLE travel_entries_migration_tmp ("
                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`personId` INTEGER NOT NULL, "
                + "`arrivalDate` INTEGER, "
                + "`departureDate` INTEGER, "
                + "`arrivalCity` TEXT, "
                + "`departureCity` TEXT, "
                + "`portOfEntry` TEXT, "
                + "`notes` TEXT, "
                + "FOREIGN KEY(`personId`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
                + ")");
        db.execSQL("INSERT INTO travel_entries_migration_tmp "
                + "(id, personId, arrivalDate, departureDate, arrivalCity, departureCity, portOfEntry, notes) "
                + "SELECT id, personId, arrivalDate, departureDate, NULL, NULL, portOfEntry, notes "
                + "FROM travel_entries");
        db.execSQL("DROP TABLE travel_entries");
        db.execSQL("ALTER TABLE travel_entries_migration_tmp RENAME TO travel_entries");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_travel_entries_personId` "
                + "ON `travel_entries` (`personId`)");
    }

    private static void rebuildDocumentsTable(SupportSQLiteDatabase db, boolean preferIssuingCountry) {
        db.execSQL("DROP TABLE IF EXISTS documents_migration_tmp");
        db.execSQL("CREATE TABLE documents_migration_tmp ("
                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`personId` INTEGER NOT NULL, "
                + "`type` TEXT, "
                + "`documentNumber` TEXT, "
                + "`issuingCountry` TEXT, "
                + "`placeOfIssue` TEXT, "
                + "`nationality` TEXT, "
                + "`issueDate` INTEGER, "
                + "`expiryDate` INTEGER, "
                + "`notes` TEXT, "
                + "FOREIGN KEY(`personId`) REFERENCES `persons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
                + ")");
        String issuingCountryExpr = preferIssuingCountry && columnExists(db, "documents", "issuingCountry")
                ? "COALESCE(issuingCountry, issuingAuthority)"
                : "issuingAuthority";
        String placeOfIssueExpr = columnExists(db, "documents", "placeOfIssue")
                ? "placeOfIssue"
                : "NULL";
        String nationalityExpr = columnExists(db, "documents", "nationality")
                ? "nationality"
                : "NULL";
        db.execSQL("INSERT INTO documents_migration_tmp "
                + "(id, personId, type, documentNumber, issuingCountry, placeOfIssue, nationality, "
                + "issueDate, expiryDate, notes) "
                + "SELECT id, personId, type, documentNumber, " + issuingCountryExpr + ", "
                + placeOfIssueExpr + ", " + nationalityExpr + ", issueDate, expiryDate, notes "
                + "FROM documents");
        db.execSQL("DROP TABLE documents");
        db.execSQL("ALTER TABLE documents_migration_tmp RENAME TO documents");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_documents_personId` ON `documents` (`personId`)");
    }

    private static boolean columnExists(SupportSQLiteDatabase db, String table, String column) {
        try (Cursor cursor = db.query("PRAGMA table_info(" + table + ")")) {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(nameIndex))) {
                    return true;
                }
            }
            return false;
        }
    }

    private DatabaseMigrations() {
    }
}
