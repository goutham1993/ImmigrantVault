package com.document.immigrantvault.data.backup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class JsonBackupSerializer {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateAdapter())
            .serializeNulls()
            .create();

    private JsonBackupSerializer() {
    }

    public static byte[] toBytes(VaultBackup backup) {
        return GSON.toJson(backup).getBytes(StandardCharsets.UTF_8);
    }

    public static VaultBackup fromBytes(byte[] data) throws ExportImportException {
        try {
            String json = new String(data, StandardCharsets.UTF_8);
            VaultBackup backup = GSON.fromJson(json, VaultBackup.class);
            if (backup == null) {
                throw new ExportImportException("Backup file is empty or invalid.");
            }
            validate(backup);
            return backup;
        } catch (ExportImportException e) {
            throw e;
        } catch (Exception e) {
            throw new ExportImportException("Could not read JSON backup.", e);
        }
    }

    private static void validate(VaultBackup backup) throws ExportImportException {
        if (backup.formatVersion <= 0) {
            throw new ExportImportException("Unsupported or missing backup format version.");
        }
        if (backup.formatVersion > VaultBackup.CURRENT_FORMAT_VERSION) {
            throw new ExportImportException(
                    "This backup was created with a newer app version. Please update ImmigrantVault.");
        }
        if (backup.app != null && !VaultBackup.APP_IDENTIFIER.equals(backup.app)) {
            throw new ExportImportException("This file does not appear to be an ImmigrantVault backup.");
        }
        if (backup.persons == null) {
            backup.persons = new java.util.ArrayList<>();
        }
        if (backup.documents == null) {
            backup.documents = new java.util.ArrayList<>();
        }
        if (backup.addresses == null) {
            backup.addresses = new java.util.ArrayList<>();
        }
        if (backup.employers == null) {
            backup.employers = new java.util.ArrayList<>();
        }
        if (backup.i94Entries == null) {
            backup.i94Entries = new java.util.ArrayList<>();
        }
        if (backup.travelEntries == null) {
            backup.travelEntries = new java.util.ArrayList<>();
        }
        if (backup.petitions == null) {
            backup.petitions = new java.util.ArrayList<>();
        }
        if (backup.reminders == null) {
            backup.reminders = new java.util.ArrayList<>();
        }
        if (backup.timelineEvents == null) {
            backup.timelineEvents = new java.util.ArrayList<>();
        }
    }

    private static final class DateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null ? null : new JsonPrimitive(src.getTime());
        }

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            return new Date(json.getAsLong());
        }
    }
}
