package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "education_entries",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("personId")}
)
public class EducationEntry {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public String institutionName;
    public String degree;
    public String fieldOfStudy;
    public String city;
    public String country;
    public String gpa;
    public Date startDate;
    public Date endDate;

    public EducationEntry() {
    }
}
