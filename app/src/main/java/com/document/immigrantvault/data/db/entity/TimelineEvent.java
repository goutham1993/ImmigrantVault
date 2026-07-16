package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "timeline_events",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("personId")}
)
public class TimelineEvent {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public TimelineEventType eventType;
    public String title;
    public String description;
    public Date eventDate;
    public SourceEntityType sourceEntityType;
    public long sourceEntityId;

    public TimelineEvent() {
    }
}
