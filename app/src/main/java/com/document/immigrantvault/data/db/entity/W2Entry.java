package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "w2_entries",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("personId")}
)
public class W2Entry {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public int taxYear;
    public String employerName;
    public String ein;

    public Double wages;
    public Double federalIncomeTax;
    public Double socialSecurityWages;
    public Double socialSecurityTax;
    public Double medicareWages;
    public Double medicareTax;

    public String box12aCode;
    public Double box12aAmount;
    public String box12bCode;
    public Double box12bAmount;
    public String box12cCode;
    public Double box12cAmount;
    public String box12dCode;
    public Double box12dAmount;

    public String box14;

    public String state;
    public Double stateWages;
    public Double stateIncomeTax;

    public String notes;

    public W2Entry() {
    }
}
