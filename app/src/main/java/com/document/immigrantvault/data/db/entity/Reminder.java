package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "reminders")
public class Reminder {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public LinkedEntityType linkedType;
    public long linkedId;
    public long personId;
    public ReminderKind reminderKind;
    public Date triggerDate;
    public int leadDays;
    public boolean enabled;
    public String title;
    public String body;

    public Reminder() {
        this.enabled = true;
    }
}
