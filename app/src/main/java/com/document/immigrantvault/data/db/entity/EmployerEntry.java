package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "employer_entries",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("personId")}
)
public class EmployerEntry {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public String employerName;
    public String jobTitle;
    public Date startDate;
    public Date endDate;
    public boolean isCurrent;
    public String city;
    public String address;
    public String notes;

    public EmployerEntry() {
    }
}
