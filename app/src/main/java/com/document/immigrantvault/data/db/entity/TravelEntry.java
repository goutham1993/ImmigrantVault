package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "travel_entries",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("personId")}
)
public class TravelEntry {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public Date arrivalDate;
    public Date departureDate;
    public String arrivalCity;
    public String departureCity;
    public String portOfEntry;
    public String airline;
    public String notes;

    public TravelEntry() {
    }
}
