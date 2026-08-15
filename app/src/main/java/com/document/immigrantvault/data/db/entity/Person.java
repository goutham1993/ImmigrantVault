package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity(tableName = "persons")
public class Person {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /**
     * Retained for backward-compatible database and backup restores. New code should use the
     * structured name fields and {@link #getDisplayName()}.
     */
    @Deprecated
    public String name;
    public String firstName;
    public String middleName;
    public String lastName;
    public Date dateOfBirth;
    public Relationship relationship;
    public String currentVisaType;
    public Date visaStartDate;
    public Date visaEndDate;
    public String aNumber;
    public String ssnLast4;
    public String countryOfBirth;
    public String currentEmployer;
    public String currentRole;
    public String notes;
    public int sortOrder;

    public Person() {
    }

    @Ignore
    public Person(String firstName, String lastName, Relationship relationship) {
        setNameParts(firstName, null, lastName);
        this.relationship = relationship;
    }

    public void setNameParts(String firstName, String middleName, String lastName) {
        this.firstName = clean(firstName);
        this.middleName = clean(middleName);
        this.lastName = clean(lastName);
        this.name = joinNameParts(this.firstName, this.middleName, this.lastName);
    }

    public String getDisplayName() {
        String structuredName = joinNameParts(firstName, middleName, lastName);
        if (!structuredName.isEmpty()) {
            return structuredName;
        }
        return clean(name);
    }

    /**
     * Populates structured fields when reading an older backup that only contains {@code name}.
     */
    public void populateNamePartsFromLegacyIfNeeded() {
        if (!clean(firstName).isEmpty() || !clean(middleName).isEmpty()
                || !clean(lastName).isEmpty()) {
            if (clean(name).isEmpty()) {
                name = getDisplayName();
            }
            return;
        }
        String[] parts = splitLegacyName(name);
        firstName = parts[0];
        middleName = parts[1];
        lastName = parts[2];
    }

    public static String[] splitLegacyName(String fullName) {
        String cleaned = clean(fullName);
        if (cleaned.isEmpty()) {
            return new String[]{"", "", ""};
        }

        String[] tokens = cleaned.split("\\s+");
        if (tokens.length == 1) {
            return new String[]{tokens[0], "", ""};
        }
        if (tokens.length == 2) {
            return new String[]{tokens[0], "", tokens[1]};
        }

        StringBuilder middle = new StringBuilder();
        for (int i = 1; i < tokens.length - 1; i++) {
            if (middle.length() > 0) {
                middle.append(' ');
            }
            middle.append(tokens[i]);
        }
        return new String[]{tokens[0], middle.toString(), tokens[tokens.length - 1]};
    }

    private static String joinNameParts(String firstName, String middleName, String lastName) {
        List<String> parts = new ArrayList<>(3);
        addIfPresent(parts, firstName);
        addIfPresent(parts, middleName);
        addIfPresent(parts, lastName);
        return String.join(" ", parts);
    }

    private static void addIfPresent(List<String> parts, String value) {
        String cleaned = clean(value);
        if (!cleaned.isEmpty()) {
            parts.add(cleaned);
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
