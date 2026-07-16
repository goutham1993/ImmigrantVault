package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "i94_entries",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = "personId", unique = true)}
)
public class I94Entry {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public String i94Number;
    public String documentNumber;
    public String countryOfCitizenship;
    public Date arrivalDate;
    public Date admitUntilDate;
    public String classOfAdmission;
    public String portOfEntry;
    public String notes;

    public I94Entry() {
    }
}
