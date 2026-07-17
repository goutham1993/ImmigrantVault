package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "persons")
public class Person {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
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
    public Person(String name, Relationship relationship) {
        this.name = name;
        this.relationship = relationship;
    }
}
