package com.document.immigrantvault.data.db;

import androidx.room.TypeConverter;

import com.document.immigrantvault.data.db.entity.DocumentType;
import com.document.immigrantvault.data.db.entity.LinkedEntityType;
import com.document.immigrantvault.data.db.entity.PetitionStatus;
import com.document.immigrantvault.data.db.entity.PetitionType;
import com.document.immigrantvault.data.db.entity.Relationship;
import com.document.immigrantvault.data.db.entity.ReminderKind;
import com.document.immigrantvault.data.db.entity.SourceEntityType;
import com.document.immigrantvault.data.db.entity.TimelineEventType;

import java.util.Date;

public class Converters {

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Date timestampToDate(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static String relationshipToString(Relationship value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static Relationship stringToRelationship(String value) {
        return value == null ? null : Relationship.valueOf(value);
    }

    @TypeConverter
    public static String documentTypeToString(DocumentType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static DocumentType stringToDocumentType(String value) {
        return value == null ? null : DocumentType.valueOf(value);
    }

    @TypeConverter
    public static String petitionTypeToString(PetitionType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static PetitionType stringToPetitionType(String value) {
        return value == null ? null : PetitionType.valueOf(value);
    }

    @TypeConverter
    public static String petitionStatusToString(PetitionStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static PetitionStatus stringToPetitionStatus(String value) {
        return value == null ? null : PetitionStatus.valueOf(value);
    }

    @TypeConverter
    public static String reminderKindToString(ReminderKind value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static ReminderKind stringToReminderKind(String value) {
        return value == null ? null : ReminderKind.valueOf(value);
    }

    @TypeConverter
    public static String linkedEntityTypeToString(LinkedEntityType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static LinkedEntityType stringToLinkedEntityType(String value) {
        return value == null ? null : LinkedEntityType.valueOf(value);
    }

    @TypeConverter
    public static String timelineEventTypeToString(TimelineEventType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static TimelineEventType stringToTimelineEventType(String value) {
        return value == null ? null : TimelineEventType.valueOf(value);
    }

    @TypeConverter
    public static String sourceEntityTypeToString(SourceEntityType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static SourceEntityType stringToSourceEntityType(String value) {
        return value == null ? null : SourceEntityType.valueOf(value);
    }
}
