package com.document.immigrantvault.data.demo;

import android.content.pm.ApplicationInfo;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.data.db.AppDatabase;
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
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TaxReturnEntry;
import com.document.immigrantvault.data.db.entity.TaxReturnOutcome;
import com.document.immigrantvault.data.db.entity.TaxReturnType;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;
import com.document.immigrantvault.data.db.entity.TravelEntry;
import com.document.immigrantvault.data.db.entity.UsefulLink;
import com.document.immigrantvault.data.db.entity.VisaEntry;
import com.document.immigrantvault.data.db.entity.VisaType;
import com.document.immigrantvault.data.db.entity.W2Entry;
import com.document.immigrantvault.data.repository.ReminderRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Adds screenshot-friendly records to debuggable builds without touching release data.
 */
public final class DemoDataSeeder {

    private static final String MARKER = "[immigrant-vault-demo-v1]";

    private DemoDataSeeder() {
    }

    public static void initialize(ImmigrantVaultApplication app) {
        boolean isDebuggable = (app.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (!isDebuggable) {
            app.getPersonRepository().ensureSelfExists();
            return;
        }

        app.getExecutor().execute(() -> app.getDatabase().runInTransaction(
                () -> seedIfNeeded(app.getDatabase(), app.getReminderRepository())));
    }

    private static void seedIfNeeded(AppDatabase database, ReminderRepository reminders) {
        List<Person> people = database.personDao().getAllSync();
        for (Person person : people) {
            if (person.notes != null && person.notes.contains(MARKER)) {
                return;
            }
        }

        Person primary = findBareSelf(people);
        if (primary == null) {
            primary = createPrimary(people);
            primary.id = database.personDao().insert(primary);
        } else {
            populatePrimary(primary);
            database.personDao().update(primary);
        }

        Person spouse = createSpouse();
        spouse.id = database.personDao().insert(spouse);
        Person child = createChild();
        child.id = database.personDao().insert(child);

        seedPrimaryDetails(database, reminders, primary);
        seedSpouseDetails(database, reminders, spouse);
        seedChildDetails(database, reminders, child);
    }

    private static Person findBareSelf(List<Person> people) {
        for (Person person : people) {
            if (person.relationship == Relationship.SELF
                    && "Me".equals(person.getDisplayName())
                    && isBlank(person.currentVisaType)
                    && isBlank(person.notes)) {
                return person;
            }
        }
        return null;
    }

    private static Person createPrimary(List<Person> people) {
        Person person = new Person();
        person.relationship = hasSelf(people) ? Relationship.FRIEND : Relationship.SELF;
        populatePrimary(person);
        return person;
    }

    private static void populatePrimary(Person person) {
        person.setNameParts("Arjun", "K.", "Rao");
        person.dateOfBirth = yearsFromNow(-34);
        person.currentVisaType = "H-1B";
        person.visaStartDate = monthsFromNow(-18);
        person.visaEndDate = monthsFromNow(10);
        person.aNumber = "A000000001";
        person.ssnLast4 = "4821";
        person.countryOfBirth = "India";
        person.currentEmployer = "Northstar Analytics";
        person.currentRole = "Senior Software Engineer";
        person.notes = "Primary applicant · Sample data " + MARKER;
        person.sortOrder = 0;
    }

    private static Person createSpouse() {
        Person person = new Person("Maya", "Rao", Relationship.SPOUSE);
        person.dateOfBirth = yearsFromNow(-32);
        person.currentVisaType = "H-4 EAD";
        person.visaStartDate = monthsFromNow(-12);
        person.visaEndDate = monthsFromNow(16);
        person.aNumber = "A000000002";
        person.ssnLast4 = "7316";
        person.countryOfBirth = "India";
        person.currentEmployer = "Brightline Health";
        person.currentRole = "Product Designer";
        person.notes = "Dependent spouse · Sample data " + MARKER;
        person.sortOrder = 1;
        return person;
    }

    private static Person createChild() {
        Person person = new Person("Anika", "Rao", Relationship.CHILD);
        person.dateOfBirth = yearsFromNow(-7);
        person.currentVisaType = "H-4";
        person.visaStartDate = monthsFromNow(-12);
        person.visaEndDate = monthsFromNow(16);
        person.countryOfBirth = "United States";
        person.notes = "Dependent child · Sample data " + MARKER;
        person.sortOrder = 2;
        return person;
    }

    private static void seedPrimaryDetails(
            AppDatabase db,
            ReminderRepository reminders,
            Person person
    ) {
        AddressEntry address = address(person.id, monthsFromNow(-20));
        long addressId = db.addressDao().insert(address);
        timeline(db, person.id, TimelineEventType.ADDRESS_CHANGE, "Moved to Austin",
                "Started current residence", address.startDate, SourceEntityType.ADDRESS, addressId);

        EmployerEntry employer = new EmployerEntry();
        employer.personId = person.id;
        employer.employerName = "Northstar Analytics";
        employer.client = "Internal platform team";
        employer.jobTitle = "Senior Software Engineer";
        employer.startDate = monthsFromNow(-18);
        employer.isCurrent = true;
        employer.city = "Austin, TX";
        employer.address = "500 Congress Ave, Austin, TX 78701";
        employer.notes = "H-1B sponsoring employer";
        long employerId = db.employerDao().insert(employer);
        timeline(db, person.id, TimelineEventType.EMPLOYER_CHANGE, "Joined Northstar Analytics",
                employer.jobTitle, employer.startDate, SourceEntityType.EMPLOYER, employerId);

        EducationEntry education = new EducationEntry();
        education.personId = person.id;
        education.institutionName = "University of Texas at Dallas";
        education.degree = "Master of Science";
        education.fieldOfStudy = "Computer Science";
        education.city = "Richardson";
        education.country = "United States";
        education.gpa = "3.8";
        education.startDate = yearsFromNow(-8);
        education.endDate = yearsFromNow(-6);
        long educationId = db.educationDao().insert(education);
        timeline(db, person.id, TimelineEventType.EDUCATION, "Completed master's degree",
                education.institutionName, education.endDate, SourceEntityType.EDUCATION, educationId);

        I94Entry i94 = new I94Entry();
        i94.personId = person.id;
        i94.i94Number = "10000000001";
        i94.documentNumber = "P0000001";
        i94.countryOfCitizenship = "India";
        i94.arrivalDate = monthsFromNow(-5);
        i94.admitUntilDate = person.visaEndDate;
        i94.classOfAdmission = "H1B";
        i94.portOfEntry = "DFW - Dallas/Fort Worth";
        i94.notes = "Most recent electronic I-94";
        db.i94Dao().insert(i94);

        VisaEntry visa = visa(person.id, VisaType.H1B, "H1B000001",
                "Northstar Analytics", person.visaStartDate, person.visaEndDate);
        visa.id = db.visaDao().insert(visa);
        reminders.syncVisaEntryReminders(visa);
        timeline(db, person.id, TimelineEventType.VISA_START, "H-1B status began",
                "Northstar Analytics", visa.startDate, SourceEntityType.VISA, visa.id);

        addDocument(db, reminders, person.id, DocumentType.PASSPORT, "P0000001",
                yearsFromNow(-5), monthsFromNow(9), "India", "Renewal planning started");
        addDocument(db, reminders, person.id, DocumentType.DRIVERS_LICENSE, "TX-DL-000001",
                yearsFromNow(-2), daysFromNow(22), "United States", "Renewal appointment needed");
        addDocument(db, reminders, person.id, DocumentType.I797, "I797-000001",
                monthsFromNow(-18), monthsFromNow(10), "United States", "H-1B approval notice");

        Petition petition = new Petition();
        petition.personId = person.id;
        petition.type = PetitionType.I140;
        petition.receiptNumber = "LIN0000000001";
        petition.filedDate = monthsFromNow(-4);
        petition.status = PetitionStatus.PENDING;
        petition.lastCheckedDate = daysFromNow(-10);
        petition.checkIntervalDays = 14;
        petition.notes = "Premium processing not requested";
        petition.id = db.petitionDao().insert(petition);
        reminders.syncPetitionReminders(petition);
        timeline(db, person.id, TimelineEventType.PETITION_FILED, "I-140 petition filed",
                petition.receiptNumber, petition.filedDate, SourceEntityType.PETITION, petition.id);

        TravelEntry travel = new TravelEntry();
        travel.personId = person.id;
        travel.departureDate = monthsFromNow(-6);
        travel.arrivalDate = monthsFromNow(-5);
        travel.departureCity = "Austin";
        travel.arrivalCity = "Dallas";
        travel.portOfEntry = "DFW";
        travel.airline = "American Airlines";
        travel.notes = "Family visit to Hyderabad";
        long travelId = db.travelDao().insert(travel);
        timeline(db, person.id, TimelineEventType.TRAVEL_ENTRY, "Returned to the United States",
                "Entered through DFW", travel.arrivalDate, SourceEntityType.TRAVEL, travelId);

        W2Entry w2 = new W2Entry();
        w2.personId = person.id;
        w2.taxYear = previousYear();
        w2.employerName = "Northstar Analytics";
        w2.ein = "00-0000001";
        w2.wages = 142500.00;
        w2.federalIncomeTax = 25120.00;
        w2.socialSecurityWages = 142500.00;
        w2.socialSecurityTax = 8835.00;
        w2.medicareWages = 142500.00;
        w2.medicareTax = 2066.25;
        w2.state = "TX";
        w2.notes = "Sample W-2";
        long w2Id = db.w2Dao().insert(w2);
        timeline(db, person.id, TimelineEventType.W2_ADDED, "W-2 added",
                String.valueOf(w2.taxYear), daysFromNow(-120), SourceEntityType.W2, w2Id);

        TaxReturnEntry taxReturn = new TaxReturnEntry();
        taxReturn.personId = person.id;
        taxReturn.taxYear = previousYear();
        taxReturn.returnType = TaxReturnType.FEDERAL;
        taxReturn.outcome = TaxReturnOutcome.REFUND;
        taxReturn.amount = 1840.00;
        taxReturn.agi = 138200.00;
        taxReturn.totalTax = 23690.00;
        taxReturn.filedDate = daysFromNow(-105);
        taxReturn.refundReceivedDate = daysFromNow(-91);
        taxReturn.notes = "Married filing jointly";
        long taxId = db.taxReturnDao().insert(taxReturn);
        timeline(db, person.id, TimelineEventType.TAX_RETURN_ADDED, "Federal return filed",
                String.valueOf(taxReturn.taxYear), taxReturn.filedDate,
                SourceEntityType.TAX_RETURN, taxId);

        UsefulLink link = new UsefulLink();
        link.personId = person.id;
        link.title = "USCIS case status";
        link.url = "https://egov.uscis.gov/";
        link.notes = "Check the pending I-140 receipt";
        db.usefulLinkDao().insert(link);
    }

    private static void seedSpouseDetails(
            AppDatabase db,
            ReminderRepository reminders,
            Person person
    ) {
        db.addressDao().insert(address(person.id, monthsFromNow(-20)));

        EmployerEntry employer = new EmployerEntry();
        employer.personId = person.id;
        employer.employerName = "Brightline Health";
        employer.jobTitle = "Product Designer";
        employer.startDate = monthsFromNow(-8);
        employer.isCurrent = true;
        employer.city = "Austin, TX";
        employer.notes = "Working with H-4 EAD authorization";
        db.employerDao().insert(employer);

        I94Entry i94 = new I94Entry();
        i94.personId = person.id;
        i94.i94Number = "10000000002";
        i94.documentNumber = "P0000002";
        i94.countryOfCitizenship = "India";
        i94.arrivalDate = monthsFromNow(-5);
        i94.admitUntilDate = person.visaEndDate;
        i94.classOfAdmission = "H4";
        i94.portOfEntry = "DFW - Dallas/Fort Worth";
        db.i94Dao().insert(i94);

        VisaEntry visa = visa(person.id, VisaType.H4_EAD, "H4EAD00002",
                null, person.visaStartDate, person.visaEndDate);
        visa.id = db.visaDao().insert(visa);
        reminders.syncVisaEntryReminders(visa);

        addDocument(db, reminders, person.id, DocumentType.PASSPORT, "P0000002",
                yearsFromNow(-3), yearsFromNow(2), "India", null);
        addDocument(db, reminders, person.id, DocumentType.EAD, "EAD-000002",
                monthsFromNow(-12), daysFromNow(96), "United States", "Renewal window opens soon");

        Petition petition = new Petition();
        petition.personId = person.id;
        petition.type = PetitionType.I765;
        petition.receiptNumber = "IOE0000000002";
        petition.filedDate = monthsFromNow(-7);
        petition.status = PetitionStatus.APPROVED;
        petition.lastCheckedDate = monthsFromNow(-5);
        petition.notes = "Employment authorization approved";
        petition.id = db.petitionDao().insert(petition);

        timeline(db, person.id, TimelineEventType.PETITION_STATUS, "EAD approved",
                petition.receiptNumber, petition.lastCheckedDate,
                SourceEntityType.PETITION, petition.id);
    }

    private static void seedChildDetails(
            AppDatabase db,
            ReminderRepository reminders,
            Person person
    ) {
        db.addressDao().insert(address(person.id, monthsFromNow(-20)));

        I94Entry i94 = new I94Entry();
        i94.personId = person.id;
        i94.i94Number = "10000000003";
        i94.documentNumber = "P0000003";
        i94.countryOfCitizenship = "India";
        i94.arrivalDate = monthsFromNow(-5);
        i94.admitUntilDate = person.visaEndDate;
        i94.classOfAdmission = "H4";
        i94.portOfEntry = "DFW - Dallas/Fort Worth";
        db.i94Dao().insert(i94);

        VisaEntry visa = visa(person.id, VisaType.H4, "H4000003",
                null, person.visaStartDate, person.visaEndDate);
        visa.id = db.visaDao().insert(visa);
        reminders.syncVisaEntryReminders(visa);

        addDocument(db, reminders, person.id, DocumentType.PASSPORT, "P0000003",
                yearsFromNow(-2), monthsFromNow(15), "India", "Minor passport");

        EducationEntry education = new EducationEntry();
        education.personId = person.id;
        education.institutionName = "Cedar Grove Elementary";
        education.degree = "Grade 2";
        education.fieldOfStudy = "Elementary education";
        education.city = "Austin";
        education.country = "United States";
        education.startDate = monthsFromNow(-6);
        long educationId = db.educationDao().insert(education);

        timeline(db, person.id, TimelineEventType.EDUCATION, "Started second grade",
                education.institutionName, education.startDate,
                SourceEntityType.EDUCATION, educationId);
    }

    private static AddressEntry address(long personId, Date startDate) {
        AddressEntry entry = new AddressEntry();
        entry.personId = personId;
        entry.line1 = "2401 Sample Oak Drive";
        entry.city = "Austin";
        entry.state = "TX";
        entry.zip = "78704";
        entry.country = "United States";
        entry.dwellingType = AddressEntry.DWELLING_HOME;
        entry.startDate = startDate;
        entry.isCurrent = true;
        return entry;
    }

    private static VisaEntry visa(
            long personId,
            VisaType type,
            String number,
            String employer,
            Date start,
            Date end
    ) {
        VisaEntry entry = new VisaEntry();
        entry.personId = personId;
        entry.type = type;
        entry.visaNumber = number;
        entry.controlNumber = "CTRL-" + number;
        entry.employer = employer;
        entry.startDate = start;
        entry.endDate = end;
        entry.notes = "Sample visa history";
        return entry;
    }

    private static void addDocument(
            AppDatabase db,
            ReminderRepository reminders,
            long personId,
            DocumentType type,
            String number,
            Date issued,
            Date expires,
            String country,
            String notes
    ) {
        Document document = new Document();
        document.personId = personId;
        document.type = type;
        document.documentNumber = number;
        document.issuingCountry = country;
        document.placeOfIssue = "Sample office";
        document.nationality = "Indian";
        document.issueDate = issued;
        document.expiryDate = expires;
        document.notes = notes;
        document.id = db.documentDao().insert(document);
        reminders.syncDocumentReminders(document);
        timeline(db, personId, TimelineEventType.DOCUMENT_ADDED, type.name() + " added",
                number, issued, SourceEntityType.DOCUMENT, document.id);
    }

    private static void timeline(
            AppDatabase db,
            long personId,
            TimelineEventType type,
            String title,
            String description,
            Date date,
            SourceEntityType sourceType,
            long sourceId
    ) {
        TimelineEvent event = new TimelineEvent();
        event.personId = personId;
        event.eventType = type;
        event.title = title;
        event.description = description;
        event.eventDate = date;
        event.sourceEntityType = sourceType;
        event.sourceEntityId = sourceId;
        db.timelineDao().insert(event);
    }

    private static boolean hasSelf(List<Person> people) {
        for (Person person : people) {
            if (person.relationship == Relationship.SELF) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int previousYear() {
        return Calendar.getInstance().get(Calendar.YEAR) - 1;
    }

    private static Date yearsFromNow(int years) {
        return shifted(Calendar.YEAR, years);
    }

    private static Date monthsFromNow(int months) {
        return shifted(Calendar.MONTH, months);
    }

    private static Date daysFromNow(int days) {
        return shifted(Calendar.DAY_OF_YEAR, days);
    }

    private static Date shifted(int field, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(field, amount);
        return calendar.getTime();
    }
}
