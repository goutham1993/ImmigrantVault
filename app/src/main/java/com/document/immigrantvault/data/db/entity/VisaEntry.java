package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "visa_entries",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("personId")}
)
public class VisaEntry {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public VisaType type;
    public String visaNumber;
    public String controlNumber;
    public Date startDate;
    public Date endDate;
    public String notes;

    public VisaEntry() {
        this.type = VisaType.OTHER;
    }
}
