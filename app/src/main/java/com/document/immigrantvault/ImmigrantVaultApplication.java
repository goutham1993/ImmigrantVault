package com.document.immigrantvault;

import android.app.Application;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.repository.ExportImportRepository;
import com.document.immigrantvault.data.repository.AddressRepository;
import com.document.immigrantvault.data.repository.DocumentRepository;
import com.document.immigrantvault.data.repository.EducationRepository;
import com.document.immigrantvault.data.repository.EmployerRepository;
import com.document.immigrantvault.data.repository.I94Repository;
import com.document.immigrantvault.data.repository.PersonRepository;
import com.document.immigrantvault.data.repository.PetitionRepository;
import com.document.immigrantvault.data.repository.ReminderRepository;
import com.document.immigrantvault.data.repository.TimelineRepository;
import com.document.immigrantvault.data.repository.TravelRepository;
import com.document.immigrantvault.data.repository.UsefulLinkRepository;
import com.document.immigrantvault.data.repository.VisaRepository;
import com.document.immigrantvault.util.ReminderScheduler;
import com.document.immigrantvault.util.ThemePreferences;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImmigrantVaultApplication extends Application {

    private AppDatabase database;
    private ExecutorService executor;
    private PersonRepository personRepository;
    private DocumentRepository documentRepository;
    private AddressRepository addressRepository;
    private EmployerRepository employerRepository;
    private EducationRepository educationRepository;
    private I94Repository i94Repository;
    private TravelRepository travelRepository;
    private PetitionRepository petitionRepository;
    private VisaRepository visaRepository;
    private UsefulLinkRepository usefulLinkRepository;
    private ReminderRepository reminderRepository;
    private TimelineRepository timelineRepository;
    private ExportImportRepository exportImportRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.applySaved(this);
        database = AppDatabase.getInstance(this);
        executor = Executors.newFixedThreadPool(4);
        personRepository = new PersonRepository(database, executor);
        documentRepository = new DocumentRepository(database, executor);
        addressRepository = new AddressRepository(database, executor);
        employerRepository = new EmployerRepository(database, executor);
        educationRepository = new EducationRepository(database, executor);
        i94Repository = new I94Repository(database, executor);
        travelRepository = new TravelRepository(database, executor);
        petitionRepository = new PetitionRepository(database, executor);
        visaRepository = new VisaRepository(database, executor);
        usefulLinkRepository = new UsefulLinkRepository(database, executor);
        reminderRepository = new ReminderRepository(database, executor);
        timelineRepository = new TimelineRepository(database, executor);
        exportImportRepository = new ExportImportRepository(database, executor);
        reminderRepository.reconcileOverlappingVisaReminders();
        ReminderScheduler.schedule(this);
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public PersonRepository getPersonRepository() {
        return personRepository;
    }

    public DocumentRepository getDocumentRepository() {
        return documentRepository;
    }

    public AddressRepository getAddressRepository() {
        return addressRepository;
    }

    public EmployerRepository getEmployerRepository() {
        return employerRepository;
    }

    public EducationRepository getEducationRepository() {
        return educationRepository;
    }

    public I94Repository getI94Repository() {
        return i94Repository;
    }

    public TravelRepository getTravelRepository() {
        return travelRepository;
    }

    public PetitionRepository getPetitionRepository() {
        return petitionRepository;
    }

    public VisaRepository getVisaRepository() {
        return visaRepository;
    }

    public UsefulLinkRepository getUsefulLinkRepository() {
        return usefulLinkRepository;
    }

    public ReminderRepository getReminderRepository() {
        return reminderRepository;
    }

    public TimelineRepository getTimelineRepository() {
        return timelineRepository;
    }

    public ExportImportRepository getExportImportRepository() {
        return exportImportRepository;
    }
}
