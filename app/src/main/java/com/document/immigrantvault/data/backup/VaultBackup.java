package com.document.immigrantvault.data.backup;

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

import java.util.ArrayList;
import java.util.List;

public class VaultBackup {

    public static final int CURRENT_FORMAT_VERSION = 1;
    public static final String APP_IDENTIFIER = "ImmigrantVault";

    public int formatVersion = CURRENT_FORMAT_VERSION;
    public String app = APP_IDENTIFIER;
    public int databaseVersion;
    public long exportedAt;
    public List<Person> persons = new ArrayList<>();
    public List<Document> documents = new ArrayList<>();
    public List<AddressEntry> addresses = new ArrayList<>();
    public List<EmployerEntry> employers = new ArrayList<>();
    public List<EducationEntry> educationEntries = new ArrayList<>();
    public List<I94Entry> i94Entries = new ArrayList<>();
    public List<TravelEntry> travelEntries = new ArrayList<>();
    public List<Petition> petitions = new ArrayList<>();
    public List<VisaEntry> visaEntries = new ArrayList<>();
    public List<UsefulLink> usefulLinks = new ArrayList<>();
    public List<Reminder> reminders = new ArrayList<>();
    public List<TimelineEvent> timelineEvents = new ArrayList<>();
}
