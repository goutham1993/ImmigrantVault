package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "tax_return_entries",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("personId")}
)
public class TaxReturnEntry {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public int taxYear;
    public TaxReturnType returnType;
    public String state;
    public TaxReturnOutcome outcome;
    public Double amount;
    public Double agi;
    public Double totalTax;
    public Date filedDate;
    public Date refundReceivedDate;
    public String notes;

    public TaxReturnEntry() {
    }
}
