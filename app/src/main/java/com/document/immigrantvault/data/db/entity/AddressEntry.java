package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "address_entries",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("personId")}
)
public class AddressEntry {

    public static final String DWELLING_HOME = "home";
    public static final String DWELLING_APARTMENT = "apartment";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public String line1;
    public String line2;
    public String city;
    public String state;
    public String zip;
    public String country;
    public String dwellingType;
    public String apartmentName;
    public String apartmentNumber;
    public Date startDate;
    public Date endDate;
    public boolean isCurrent;

    public AddressEntry() {
        dwellingType = DWELLING_HOME;
    }

    public boolean isApartment() {
        return DWELLING_APARTMENT.equals(dwellingType);
    }
}
