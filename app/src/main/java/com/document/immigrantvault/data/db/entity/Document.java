package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "documents",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("personId")}
)
public class Document {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public DocumentType type;
    public String documentNumber;
    public String issuingCountry;
    public String placeOfIssue;
    public String nationality;
    public Date issueDate;
    public Date expiryDate;
    public String notes;

    public Document() {
    }
}
