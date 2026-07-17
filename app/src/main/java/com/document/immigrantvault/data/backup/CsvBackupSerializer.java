package com.document.immigrantvault.data.backup;

import com.document.immigrantvault.data.db.entity.AddressEntry;
import com.document.immigrantvault.data.db.entity.Document;
import com.document.immigrantvault.data.db.entity.DocumentType;
import com.document.immigrantvault.data.db.entity.EducationEntry;
import com.document.immigrantvault.data.db.entity.EmployerEntry;
import com.document.immigrantvault.data.db.entity.I94Entry;
import com.document.immigrantvault.data.db.entity.LinkedEntityType;
import com.document.immigrantvault.data.db.entity.Person;
import com.document.immigrantvault.data.db.entity.Petition;
import com.document.immigrantvault.data.db.entity.PetitionStatus;
import com.document.immigrantvault.data.db.entity.PetitionType;
import com.document.immigrantvault.data.db.entity.Relationship;
import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.data.db.entity.ReminderKind;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;
import com.document.immigrantvault.data.db.entity.TravelEntry;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class CsvBackupSerializer {

    private static final String MANIFEST = "manifest.csv";
    private static final String PERSONS = "persons.csv";
    private static final String DOCUMENTS = "documents.csv";
    private static final String ADDRESSES = "addresses.csv";
    private static final String EMPLOYERS = "employers.csv";
    private static final String EDUCATION = "education_entries.csv";
    private static final String I94 = "i94_entries.csv";
    private static final String TRAVEL = "travel_entries.csv";
    private static final String PETITIONS = "petitions.csv";
    private static final String REMINDERS = "reminders.csv";
    private static final String TIMELINE = "timeline_events.csv";

    private CsvBackupSerializer() {
    }

    public static byte[] toBytes(VaultBackup backup) throws ExportImportException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(outputStream)) {
            writeEntry(zip, MANIFEST, writer -> CsvUtils.writeManifest(writer, backup));
            writeEntry(zip, PERSONS, writer -> writePersons(writer, backup.persons));
            writeEntry(zip, DOCUMENTS, writer -> writeDocuments(writer, backup.documents));
            writeEntry(zip, ADDRESSES, writer -> writeAddresses(writer, backup.addresses));
            writeEntry(zip, EMPLOYERS, writer -> writeEmployers(writer, backup.employers));
            writeEntry(zip, EDUCATION, writer -> writeEducation(writer, backup.educationEntries));
            writeEntry(zip, I94, writer -> writeI94(writer, backup.i94Entries));
            writeEntry(zip, TRAVEL, writer -> writeTravel(writer, backup.travelEntries));
            writeEntry(zip, PETITIONS, writer -> writePetitions(writer, backup.petitions));
            writeEntry(zip, REMINDERS, writer -> writeReminders(writer, backup.reminders));
            writeEntry(zip, TIMELINE, writer -> writeTimeline(writer, backup.timelineEvents));
            zip.finish();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ExportImportException("Could not create CSV backup.", e);
        }
    }

    public static VaultBackup fromBytes(byte[] data) throws ExportImportException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    entries.put(entry.getName(), readEntry(zip));
                }
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new ExportImportException("Could not read CSV backup archive.", e);
        }

        if (!entries.containsKey(MANIFEST)) {
            throw new ExportImportException("CSV backup is missing manifest.csv.");
        }

        try {
            VaultBackup backup = CsvUtils.readManifest(
                    CsvUtils.readTable(new ByteArrayInputStream(entries.get(MANIFEST))));
            backup.persons = readPersons(entries.get(PERSONS));
            backup.documents = readDocuments(entries.get(DOCUMENTS));
            backup.addresses = readAddresses(entries.get(ADDRESSES));
            backup.employers = readEmployers(entries.get(EMPLOYERS));
            backup.educationEntries = readEducation(entries.get(EDUCATION));
            backup.i94Entries = readI94(entries.get(I94));
            backup.travelEntries = readTravel(entries.get(TRAVEL));
            backup.petitions = readPetitions(entries.get(PETITIONS));
            backup.reminders = readReminders(entries.get(REMINDERS));
            backup.timelineEvents = readTimeline(entries.get(TIMELINE));
            return backup;
        } catch (ExportImportException e) {
            throw e;
        } catch (Exception e) {
            throw new ExportImportException("Could not parse CSV backup.", e);
        }
    }

    private interface ZipWriter {
        void write(Writer writer) throws IOException;
    }

    private static void writeEntry(ZipOutputStream zip, String name, ZipWriter writer) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Writer outputWriter = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
        writer.write(outputWriter);
        outputWriter.flush();
        zip.closeEntry();
    }

    private static byte[] readEntry(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static void writePersons(Writer writer, List<Person> persons) throws IOException {
        CsvUtils.writeRow(writer,
                "id", "name", "dateOfBirth", "relationship", "currentVisaType",
                "visaStartDate", "visaEndDate", "aNumber", "ssnLast4", "countryOfBirth",
                "currentEmployer", "currentRole", "notes", "sortOrder");
        for (Person person : persons) {
            CsvUtils.writeRow(writer,
                    CsvUtils.formatLong(person.id),
                    CsvUtils.formatString(person.name),
                    CsvUtils.formatDate(person.dateOfBirth),
                    CsvUtils.formatEnum(person.relationship),
                    CsvUtils.formatString(person.currentVisaType),
                    CsvUtils.formatDate(person.visaStartDate),
                    CsvUtils.formatDate(person.visaEndDate),
                    CsvUtils.formatString(person.aNumber),
                    CsvUtils.formatString(person.ssnLast4),
                    CsvUtils.formatString(person.countryOfBirth),
                    CsvUtils.formatString(person.currentEmployer),
                    CsvUtils.formatString(person.currentRole),
                    CsvUtils.formatString(person.notes),
                    CsvUtils.formatInt(person.sortOrder));
        }
    }

    private static List<Person> readPersons(byte[] data) throws IOException {
        if (data == null) {
            return new ArrayList<>();
        }
        List<Person> persons = new ArrayList<>();
        for (Map<String, String> row : CsvUtils.readTable(new ByteArrayInputStream(data))) {
            Person person = new Person();
            person.id = CsvUtils.getLong(row, "id");
            person.name = CsvUtils.get(row, "name");
            person.dateOfBirth = CsvUtils.getDate(row, "dateOfBirth");
            person.relationship = CsvUtils.parseEnum(CsvUtils.get(row, "relationship"), Relationship.class);
            person.currentVisaType = CsvUtils.get(row, "currentVisaType");
            person.visaStartDate = CsvUtils.getDate(row, "visaStartDate");
            person.visaEndDate = CsvUtils.getDate(row, "visaEndDate");
            person.aNumber = CsvUtils.get(row, "aNumber");
            person.ssnLast4 = CsvUtils.get(row, "ssnLast4");
            person.countryOfBirth = CsvUtils.get(row, "countryOfBirth");
            person.currentEmployer = CsvUtils.get(row, "currentEmployer");
            person.currentRole = CsvUtils.get(row, "currentRole");
            person.notes = CsvUtils.get(row, "notes");
            person.sortOrder = CsvUtils.getInt(row, "sortOrder");
            persons.add(person);
        }
        return persons;
    }

    private static void writeDocuments(Writer writer, List<Document> documents) throws IOException {
        CsvUtils.writeRow(writer,
                "id", "personId", "type", "documentNumber", "issuingCountry",
                "placeOfIssue", "nationality", "issueDate", "expiryDate", "notes");
        for (Document document : documents) {
            CsvUtils.writeRow(writer,
                    CsvUtils.formatLong(document.id),
                    CsvUtils.formatLong(document.personId),
                    CsvUtils.formatEnum(document.type),
                    CsvUtils.formatString(document.documentNumber),
                    CsvUtils.formatString(document.issuingCountry),
                    CsvUtils.formatString(document.placeOfIssue),
                    CsvUtils.formatString(document.nationality),
                    CsvUtils.formatDate(document.issueDate),
                    CsvUtils.formatDate(document.expiryDate),
                    CsvUtils.formatString(document.notes));
        }
    }

    private static List<Document> readDocuments(byte[] data) throws IOException {
        if (data == null) {
            return new ArrayList<>();
        }
        List<Document> documents = new ArrayList<>();
        for (Map<String, String> row : CsvUtils.readTable(new ByteArrayInputStream(data))) {
            Document document = new Document();
            document.id = CsvUtils.getLong(row, "id");
            document.personId = CsvUtils.getLong(row, "personId");
            document.type = CsvUtils.parseEnum(CsvUtils.get(row, "type"), DocumentType.class);
            document.documentNumber = CsvUtils.get(row, "documentNumber");
            document.issuingCountry = CsvUtils.get(row, "issuingCountry");
            document.placeOfIssue = CsvUtils.get(row, "placeOfIssue");
            document.nationality = CsvUtils.get(row, "nationality");
            document.issueDate = CsvUtils.getDate(row, "issueDate");
            document.expiryDate = CsvUtils.getDate(row, "expiryDate");
            document.notes = CsvUtils.get(row, "notes");
            documents.add(document);
        }
        return documents;
    }

    private static void writeAddresses(Writer writer, List<AddressEntry> addresses) throws IOException {
        CsvUtils.writeRow(writer,
                "id", "personId", "line1", "line2", "city", "state", "zip", "country",
                "dwellingType", "apartmentName", "apartmentNumber",
                "startDate", "endDate", "isCurrent");
        for (AddressEntry address : addresses) {
            CsvUtils.writeRow(writer,
                    CsvUtils.formatLong(address.id),
                    CsvUtils.formatLong(address.personId),
                    CsvUtils.formatString(address.line1),
                    CsvUtils.formatString(address.line2),
                    CsvUtils.formatString(address.city),
                    CsvUtils.formatString(address.state),
                    CsvUtils.formatString(address.zip),
                    CsvUtils.formatString(address.country),
                    CsvUtils.formatString(address.dwellingType),
                    CsvUtils.formatString(address.apartmentName),
                    CsvUtils.formatString(address.apartmentNumber),
                    CsvUtils.formatDate(address.startDate),
                    CsvUtils.formatDate(address.endDate),
                    CsvUtils.formatBoolean(address.isCurrent));
        }
    }

    private static List<AddressEntry> readAddresses(byte[] data) throws IOException {
        if (data == null) {
            return new ArrayList<>();
        }
        List<AddressEntry> addresses = new ArrayList<>();
        for (Map<String, String> row : CsvUtils.readTable(new ByteArrayInputStream(data))) {
            AddressEntry address = new AddressEntry();
            address.id = CsvUtils.getLong(row, "id");
            address.personId = CsvUtils.getLong(row, "personId");
            address.line1 = CsvUtils.get(row, "line1");
            address.line2 = CsvUtils.get(row, "line2");
            address.city = CsvUtils.get(row, "city");
            address.state = CsvUtils.get(row, "state");
            address.zip = CsvUtils.get(row, "zip");
            address.country = CsvUtils.get(row, "country");
            address.dwellingType = CsvUtils.get(row, "dwellingType");
            address.apartmentName = CsvUtils.get(row, "apartmentName");
            address.apartmentNumber = CsvUtils.get(row, "apartmentNumber");
            address.startDate = CsvUtils.getDate(row, "startDate");
            address.endDate = CsvUtils.getDate(row, "endDate");
            address.isCurrent = CsvUtils.getBoolean(row, "isCurrent");
            addresses.add(address);
        }
        return addresses;
    }

    private static void writeEmployers(Writer writer, List<EmployerEntry> employers) throws IOException {
        CsvUtils.writeRow(writer,
                "id", "personId", "employerName", "jobTitle", "startDate", "endDate",
                "isCurrent", "city", "address", "notes");
        for (EmployerEntry employer : employers) {
            CsvUtils.writeRow(writer,
                    CsvUtils.formatLong(employer.id),
                    CsvUtils.formatLong(employer.personId),
                    CsvUtils.formatString(employer.employerName),
                    CsvUtils.formatString(employer.jobTitle),
                    CsvUtils.formatDate(employer.startDate),
                    CsvUtils.formatDate(employer.endDate),
                    CsvUtils.formatBoolean(employer.isCurrent),
                    CsvUtils.formatString(employer.city),
                    CsvUtils.formatString(employer.address),
                    CsvUtils.formatString(employer.notes));
        }
    }

    private static List<EmployerEntry> readEmployers(byte[] data) throws IOException {
        if (data == null) {
            return new ArrayList<>();
        }
        List<EmployerEntry> employers = new ArrayList<>();
        for (Map<String, String> row : CsvUtils.readTable(new ByteArrayInputStream(data))) {
            EmployerEntry employer = new EmployerEntry();
            employer.id = CsvUtils.getLong(row, "id");
            employer.personId = CsvUtils.getLong(row, "personId");
            employer.employerName = CsvUtils.get(row, "employerName");
            employer.jobTitle = CsvUtils.get(row, "jobTitle");
            employer.startDate = CsvUtils.getDate(row, "startDate");
            employer.endDate = CsvUtils.getDate(row, "endDate");
            employer.isCurrent = CsvUtils.getBoolean(row, "isCurrent");
            employer.city = CsvUtils.get(row, "city");
            employer.address = CsvUtils.get(row, "address");
            employer.notes = CsvUtils.get(row, "notes");
            employers.add(employer);
        }
        return employers;
    }

    private static void writeEducation(Writer writer, List<EducationEntry> entries) throws IOException {
        CsvUtils.writeRow(writer,
                "id", "personId", "institutionName", "degree", "fieldOfStudy",
                "city", "country", "gpa", "startDate", "endDate");
        for (EducationEntry entry : entries) {
            CsvUtils.writeRow(writer,
                    CsvUtils.formatLong(entry.id),
                    CsvUtils.formatLong(entry.personId),
                    CsvUtils.formatString(entry.institutionName),
                    CsvUtils.formatString(entry.degree),
                    CsvUtils.formatString(entry.fieldOfStudy),
                    CsvUtils.formatString(entry.city),
                    CsvUtils.formatString(entry.country),
                    CsvUtils.formatString(entry.gpa),
                    CsvUtils.formatDate(entry.startDate),
                    CsvUtils.formatDate(entry.endDate));
        }
    }

    private static List<EducationEntry> readEducation(byte[] data) throws IOException {
        if (data == null) {
            return new ArrayList<>();
        }
        List<EducationEntry> entries = new ArrayList<>();
        for (Map<String, String> row : CsvUtils.readTable(new ByteArrayInputStream(data))) {
            EducationEntry entry = new EducationEntry();
            entry.id = CsvUtils.getLong(row, "id");
            entry.personId = CsvUtils.getLong(row, "personId");
            entry.institutionName = CsvUtils.get(row, "institutionName");
            entry.degree = CsvUtils.get(row, "degree");
            entry.fieldOfStudy = CsvUtils.get(row, "fieldOfStudy");
            entry.city = CsvUtils.get(row, "city");
            entry.country = CsvUtils.get(row, "country");
            entry.gpa = CsvUtils.get(row, "gpa");
            entry.startDate = CsvUtils.getDate(row, "startDate");
            entry.endDate = CsvUtils.getDate(row, "endDate");
            entries.add(entry);
        }
        return entries;
    }

    private static void writeI94(Writer writer, List<I94Entry> entries) throws IOException {
        CsvUtils.writeRow(writer,
                "id", "personId", "i94Number", "documentNumber", "countryOfCitizenship",
                "arrivalDate", "admitUntilDate", "classOfAdmission", "portOfEntry", "notes");
        for (I94Entry entry : entries) {
            CsvUtils.writeRow(writer,
                    CsvUtils.formatLong(entry.id),
                    CsvUtils.formatLong(entry.personId),
                    CsvUtils.formatString(entry.i94Number),
                    CsvUtils.formatString(entry.documentNumber),
                    CsvUtils.formatString(entry.countryOfCitizenship),
                    CsvUtils.formatDate(entry.arrivalDate),
                    CsvUtils.formatDate(entry.admitUntilDate),
                    CsvUtils.formatString(entry.classOfAdmission),
                    CsvUtils.formatString(entry.portOfEntry),
                    CsvUtils.formatString(entry.notes));
        }
    }

    private static List<I94Entry> readI94(byte[] data) throws IOException {
        if (data == null) {
            return new ArrayList<>();
        }
        List<I94Entry> entries = new ArrayList<>();
        for (Map<String, String> row : CsvUtils.readTable(new ByteArrayInputStream(data))) {
            I94Entry entry = new I94Entry();
            entry.id = CsvUtils.getLong(row, "id");
            entry.personId = CsvUtils.getLong(row, "personId");
            entry.i94Number = CsvUtils.get(row, "i94Number");
            entry.documentNumber = CsvUtils.get(row, "documentNumber");
            entry.countryOfCitizenship = CsvUtils.get(row, "countryOfCitizenship");
            entry.arrivalDate = CsvUtils.getDate(row, "arrivalDate");
            entry.admitUntilDate = CsvUtils.getDate(row, "admitUntilDate");
            entry.classOfAdmission = CsvUtils.get(row, "classOfAdmission");
            entry.portOfEntry = CsvUtils.get(row, "portOfEntry");
            entry.notes = CsvUtils.get(row, "notes");
            entries.add(entry);
        }
        return entries;
    }

    private static void writeTravel(Writer writer, List<TravelEntry> entries) throws IOException {
        CsvUtils.writeRow(writer,
                "id", "personId", "arrivalDate", "departureDate", "arrivalCity",
                "departureCity", "portOfEntry", "airline", "notes");
        for (TravelEntry entry : entries) {
            CsvUtils.writeRow(writer,
                    CsvUtils.formatLong(entry.id),
                    CsvUtils.formatLong(entry.personId),
                    CsvUtils.formatDate(entry.arrivalDate),
                    CsvUtils.formatDate(entry.departureDate),
                    CsvUtils.formatString(entry.arrivalCity),
                    CsvUtils.formatString(entry.departureCity),
                    CsvUtils.formatString(entry.portOfEntry),
                    CsvUtils.formatString(entry.airline),
                    CsvUtils.formatString(entry.notes));
        }
    }

    private static List<TravelEntry> readTravel(byte[] data) throws IOException {
        if (data == null) {
            return new ArrayList<>();
        }
        List<TravelEntry> entries = new ArrayList<>();
        for (Map<String, String> row : CsvUtils.readTable(new ByteArrayInputStream(data))) {
            TravelEntry entry = new TravelEntry();
            entry.id = CsvUtils.getLong(row, "id");
            entry.personId = CsvUtils.getLong(row, "personId");
            entry.arrivalDate = CsvUtils.getDate(row, "arrivalDate");
            entry.departureDate = CsvUtils.getDate(row, "departureDate");
            entry.arrivalCity = CsvUtils.get(row, "arrivalCity");
            entry.departureCity = CsvUtils.get(row, "departureCity");
            entry.portOfEntry = CsvUtils.get(row, "portOfEntry");
            entry.airline = CsvUtils.get(row, "airline");
            entry.notes = CsvUtils.get(row, "notes");
            entries.add(entry);
        }
        return entries;
    }

    private static void writePetitions(Writer writer, List<Petition> petitions) throws IOException {
        CsvUtils.writeRow(writer,
                "id", "personId", "type", "receiptNumber", "filedDate", "status",
                "lastCheckedDate", "checkIntervalDays", "notes");
        for (Petition petition : petitions) {
            CsvUtils.writeRow(writer,
                    CsvUtils.formatLong(petition.id),
                    CsvUtils.formatLong(petition.personId),
                    CsvUtils.formatEnum(petition.type),
                    CsvUtils.formatString(petition.receiptNumber),
                    CsvUtils.formatDate(petition.filedDate),
                    CsvUtils.formatEnum(petition.status),
                    CsvUtils.formatDate(petition.lastCheckedDate),
                    CsvUtils.formatInt(petition.checkIntervalDays),
                    CsvUtils.formatString(petition.notes));
        }
    }

    private static List<Petition> readPetitions(byte[] data) throws IOException {
        if (data == null) {
            return new ArrayList<>();
        }
        List<Petition> petitions = new ArrayList<>();
        for (Map<String, String> row : CsvUtils.readTable(new ByteArrayInputStream(data))) {
            Petition petition = new Petition();
            petition.id = CsvUtils.getLong(row, "id");
            petition.personId = CsvUtils.getLong(row, "personId");
            petition.type = CsvUtils.parseEnum(CsvUtils.get(row, "type"), PetitionType.class);
            petition.receiptNumber = CsvUtils.get(row, "receiptNumber");
            petition.filedDate = CsvUtils.getDate(row, "filedDate");
            petition.status = CsvUtils.parseEnum(CsvUtils.get(row, "status"), PetitionStatus.class);
            petition.lastCheckedDate = CsvUtils.getDate(row, "lastCheckedDate");
            petition.checkIntervalDays = CsvUtils.getInt(row, "checkIntervalDays");
            petition.notes = CsvUtils.get(row, "notes");
            petitions.add(petition);
        }
        return petitions;
    }

    private static void writeReminders(Writer writer, List<Reminder> reminders) throws IOException {
        CsvUtils.writeRow(writer,
                "id", "linkedType", "linkedId", "personId", "reminderKind",
                "triggerDate", "leadDays", "enabled", "title", "body");
        for (Reminder reminder : reminders) {
            CsvUtils.writeRow(writer,
                    CsvUtils.formatLong(reminder.id),
                    CsvUtils.formatEnum(reminder.linkedType),
                    CsvUtils.formatLong(reminder.linkedId),
                    CsvUtils.formatLong(reminder.personId),
                    CsvUtils.formatEnum(reminder.reminderKind),
                    CsvUtils.formatDate(reminder.triggerDate),
                    CsvUtils.formatInt(reminder.leadDays),
                    CsvUtils.formatBoolean(reminder.enabled),
                    CsvUtils.formatString(reminder.title),
                    CsvUtils.formatString(reminder.body));
        }
    }

    private static List<Reminder> readReminders(byte[] data) throws IOException {
        if (data == null) {
            return new ArrayList<>();
        }
        List<Reminder> reminders = new ArrayList<>();
        for (Map<String, String> row : CsvUtils.readTable(new ByteArrayInputStream(data))) {
            Reminder reminder = new Reminder();
            reminder.id = CsvUtils.getLong(row, "id");
            reminder.linkedType = CsvUtils.parseEnum(CsvUtils.get(row, "linkedType"), LinkedEntityType.class);
            reminder.linkedId = CsvUtils.getLong(row, "linkedId");
            reminder.personId = CsvUtils.getLong(row, "personId");
            reminder.reminderKind = CsvUtils.parseEnum(CsvUtils.get(row, "reminderKind"), ReminderKind.class);
            reminder.triggerDate = CsvUtils.getDate(row, "triggerDate");
            reminder.leadDays = CsvUtils.getInt(row, "leadDays");
            reminder.enabled = CsvUtils.getBoolean(row, "enabled");
            reminder.title = CsvUtils.get(row, "title");
            reminder.body = CsvUtils.get(row, "body");
            reminders.add(reminder);
        }
        return reminders;
    }

    private static void writeTimeline(Writer writer, List<TimelineEvent> events) throws IOException {
        CsvUtils.writeRow(writer,
                "id", "personId", "eventType", "title", "description",
                "eventDate", "sourceEntityType", "sourceEntityId");
        for (TimelineEvent event : events) {
            CsvUtils.writeRow(writer,
                    CsvUtils.formatLong(event.id),
                    CsvUtils.formatLong(event.personId),
                    CsvUtils.formatEnum(event.eventType),
                    CsvUtils.formatString(event.title),
                    CsvUtils.formatString(event.description),
                    CsvUtils.formatDate(event.eventDate),
                    CsvUtils.formatEnum(event.sourceEntityType),
                    CsvUtils.formatLong(event.sourceEntityId));
        }
    }

    private static List<TimelineEvent> readTimeline(byte[] data) throws IOException {
        if (data == null) {
            return new ArrayList<>();
        }
        List<TimelineEvent> events = new ArrayList<>();
        for (Map<String, String> row : CsvUtils.readTable(new ByteArrayInputStream(data))) {
            TimelineEvent event = new TimelineEvent();
            event.id = CsvUtils.getLong(row, "id");
            event.personId = CsvUtils.getLong(row, "personId");
            event.eventType = CsvUtils.parseEnum(CsvUtils.get(row, "eventType"), TimelineEventType.class);
            event.title = CsvUtils.get(row, "title");
            event.description = CsvUtils.get(row, "description");
            event.eventDate = CsvUtils.getDate(row, "eventDate");
            event.sourceEntityType = CsvUtils.parseEnum(
                    CsvUtils.get(row, "sourceEntityType"), SourceEntityType.class);
            event.sourceEntityId = CsvUtils.getLong(row, "sourceEntityId");
            events.add(event);
        }
        return events;
    }
}
