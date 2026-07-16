package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "petitions",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("personId")}
)
public class Petition {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public PetitionType type;
    public String receiptNumber;
    public Date filedDate;
    public PetitionStatus status;
    public Date lastCheckedDate;
    public int checkIntervalDays;
    public String notes;

    public Petition() {
        this.status = PetitionStatus.PENDING;
        this.checkIntervalDays = 14;
    }
}
