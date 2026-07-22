package com.document.immigrantvault.data.backup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class CsvUtils {

    private CsvUtils() {
    }

    static void writeRow(Writer writer, String... values) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(escape(values[i]));
        }
        writer.write(line.toString());
        writer.write('\n');
    }

    static List<Map<String, String>> readTable(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String headerLine = reader.readLine();
        if (headerLine == null) {
            return new ArrayList<>();
        }
        String[] headers = parseLine(headerLine);
        List<Map<String, String>> rows = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] values = parseLine(line);
            Map<String, String> row = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                row.put(headers[i], i < values.length ? values[i] : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String[] parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values.toArray(new String[0]);
    }

    static String get(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null || value.isEmpty() ? null : value;
    }

    static long getLong(Map<String, String> row, String key) {
        String value = get(row, key);
        return value == null ? 0L : Long.parseLong(value);
    }

    static int getInt(Map<String, String> row, String key) {
        String value = get(row, key);
        return value == null ? 0 : Integer.parseInt(value);
    }

    static Double getDouble(Map<String, String> row, String key) {
        String value = get(row, key);
        return value == null ? null : Double.parseDouble(value);
    }

    static boolean getBoolean(Map<String, String> row, String key) {
        String value = get(row, key);
        if (value == null) {
            return false;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    static java.util.Date getDate(Map<String, String> row, String key) {
        String value = get(row, key);
        if (value == null) {
            return null;
        }
        return new java.util.Date(Long.parseLong(value));
    }

    static String formatDate(java.util.Date date) {
        return date == null ? "" : String.valueOf(date.getTime());
    }

    static String formatBoolean(boolean value) {
        return value ? "true" : "false";
    }

    static String formatLong(long value) {
        return String.valueOf(value);
    }

    static String formatInt(int value) {
        return String.valueOf(value);
    }

    static String formatDouble(Double value) {
        return value == null ? "" : String.valueOf(value);
    }

    static String formatString(String value) {
        return value == null ? "" : value;
    }

    static String formatEnum(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    static <T extends Enum<T>> T parseEnum(String value, Class<T> type) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Enum.valueOf(type, value);
    }

    static void writeManifest(Writer writer, VaultBackup backup) throws IOException {
        writeRow(writer,
                "formatVersion",
                "app",
                "databaseVersion",
                "exportedAt");
        writeRow(writer,
                formatInt(backup.formatVersion),
                formatString(backup.app),
                formatInt(backup.databaseVersion),
                formatLong(backup.exportedAt));
    }

    static VaultBackup readManifest(List<Map<String, String>> rows) throws ExportImportException {
        if (rows.isEmpty()) {
            throw new ExportImportException("CSV backup is missing manifest.csv.");
        }
        Map<String, String> row = rows.get(0);
        VaultBackup backup = new VaultBackup();
        backup.formatVersion = getInt(row, "formatVersion");
        backup.app = get(row, "app");
        backup.databaseVersion = getInt(row, "databaseVersion");
        String exportedAt = get(row, "exportedAt");
        backup.exportedAt = exportedAt == null ? 0L : Long.parseLong(exportedAt);
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
        return backup;
    }
}
