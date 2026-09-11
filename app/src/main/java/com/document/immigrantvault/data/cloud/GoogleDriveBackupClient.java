package com.document.immigrantvault.data.cloud;

import androidx.annotation.Nullable;

import com.document.immigrantvault.data.backup.ExportImportException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin Drive REST v3 client. Avoids the full google-api-services-drive stack so
 * debug installs stay small enough for the emulator to launch quickly.
 */
public final class GoogleDriveBackupClient {

    public static final String FOLDER_NAME = "ImmigrantVault Backups";
    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";
    private static final String ZIP_MIME = "application/zip";
    private static final String FILES_URL = "https://www.googleapis.com/drive/v3/files";
    private static final String UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files";

    public String ensureFolder(String accessToken, @Nullable String existingFolderId)
            throws ExportImportException {
        try {
            if (existingFolderId != null && !existingFolderId.isEmpty()) {
                try {
                    JsonObject existing = getJson(accessToken,
                            FILES_URL + "/" + existingFolderId + "?fields=id,trashed,mimeType");
                    if (existing != null
                            && !existing.has("error")
                            && !bool(existing, "trashed")
                            && FOLDER_MIME.equals(string(existing, "mimeType"))) {
                        return string(existing, "id");
                    }
                } catch (ExportImportException ignored) {
                    // Folder was deleted or is no longer visible; create a new one.
                }
            }
            String query = "mimeType = '" + FOLDER_MIME + "' and name = '"
                    + escapeQuery(FOLDER_NAME) + "' and trashed = false";
            JsonObject matches = getJson(accessToken, FILES_URL
                    + "?q=" + encode(query)
                    + "&spaces=drive&fields=files(id,name)&pageSize=1");
            JsonArray files = array(matches, "files");
            if (files.size() > 0) {
                return string(files.get(0).getAsJsonObject(), "id");
            }
            JsonObject metadata = new JsonObject();
            metadata.addProperty("name", FOLDER_NAME);
            metadata.addProperty("mimeType", FOLDER_MIME);
            JsonObject created = postJson(accessToken, FILES_URL + "?fields=id", metadata.toString());
            String id = string(created, "id");
            if (id == null || id.isEmpty()) {
                throw new ExportImportException("Could not create the Google Drive backup folder.");
            }
            return id;
        } catch (ExportImportException e) {
            throw e;
        } catch (IOException e) {
            throw new ExportImportException("Could not access Google Drive: " + message(e), e);
        }
    }

    public void upload(String accessToken, String folderId, String fileName, byte[] data,
                       boolean replaceSameName) throws ExportImportException {
        if (data == null) {
            throw new ExportImportException("Backup data was empty.");
        }
        try {
            if (replaceSameName) {
                String existingId = findFileId(accessToken, folderId, fileName);
                if (existingId != null) {
                    patchMedia(accessToken, existingId, data);
                    return;
                }
            }
            JsonObject metadata = new JsonObject();
            metadata.addProperty("name", fileName);
            metadata.addProperty("mimeType", ZIP_MIME);
            JsonArray parents = new JsonArray();
            parents.add(folderId);
            metadata.add("parents", parents);
            multipartCreate(accessToken, metadata.toString(), data);
        } catch (ExportImportException e) {
            throw e;
        } catch (IOException e) {
            throw new ExportImportException("Could not upload to Google Drive: " + message(e), e);
        }
    }

    public List<DriveBackupItem> listBackups(String accessToken, String folderId)
            throws ExportImportException {
        try {
            String query = "'" + escapeQuery(folderId) + "' in parents and trashed = false"
                    + " and mimeType != '" + FOLDER_MIME + "'";
            JsonObject list = getJson(accessToken, FILES_URL
                    + "?q=" + encode(query)
                    + "&spaces=drive&orderBy=modifiedTime desc"
                    + "&fields=files(id,name,modifiedTime)&pageSize=50");
            List<DriveBackupItem> items = new ArrayList<>();
            JsonArray files = array(list, "files");
            for (JsonElement element : files) {
                JsonObject file = element.getAsJsonObject();
                items.add(new DriveBackupItem(
                        string(file, "id"),
                        string(file, "name"),
                        string(file, "modifiedTime")));
            }
            return items;
        } catch (ExportImportException e) {
            throw e;
        } catch (IOException e) {
            throw new ExportImportException("Could not list Google Drive backups: " + message(e), e);
        }
    }

    public byte[] download(String accessToken, String fileId) throws ExportImportException {
        try {
            return getBytes(accessToken, FILES_URL + "/" + fileId + "?alt=media");
        } catch (IOException e) {
            throw new ExportImportException("Could not download the Google Drive backup: " + message(e), e);
        }
    }

    @Nullable
    private String findFileId(String accessToken, String folderId, String fileName) throws IOException,
            ExportImportException {
        String query = "'" + escapeQuery(folderId) + "' in parents and name = '"
                + escapeQuery(fileName) + "' and trashed = false";
        JsonObject existing = getJson(accessToken, FILES_URL
                + "?q=" + encode(query)
                + "&spaces=drive&fields=files(id)&pageSize=1");
        JsonArray files = array(existing, "files");
        if (files.size() == 0) {
            return null;
        }
        return string(files.get(0).getAsJsonObject(), "id");
    }

    private JsonObject getJson(String accessToken, String url) throws IOException, ExportImportException {
        HttpURLConnection conn = open(accessToken, "GET", url);
        try {
            return readJson(conn);
        } finally {
            conn.disconnect();
        }
    }

    private JsonObject postJson(String accessToken, String url, String json)
            throws IOException, ExportImportException {
        HttpURLConnection conn = open(accessToken, "POST", url);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(body.length);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body);
        }
        try {
            return readJson(conn);
        } finally {
            conn.disconnect();
        }
    }

    private void patchMedia(String accessToken, String fileId, byte[] data)
            throws IOException, ExportImportException {
        HttpURLConnection conn = open(accessToken, "POST",
                UPLOAD_URL + "/" + fileId + "?uploadType=media");
        conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", ZIP_MIME);
        conn.setFixedLengthStreamingMode(data.length);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(data);
        }
        try {
            readJson(conn);
        } finally {
            conn.disconnect();
        }
    }

    private void multipartCreate(String accessToken, String metadataJson, byte[] data)
            throws IOException, ExportImportException {
        String boundary = "immigrantvault_" + System.currentTimeMillis();
        byte[] preamble = ("--" + boundary + "\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                + metadataJson + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: " + ZIP_MIME + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] closing = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = open(accessToken, "POST", UPLOAD_URL + "?uploadType=multipart");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);
        conn.setFixedLengthStreamingMode(preamble.length + data.length + closing.length);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(preamble);
            out.write(data);
            out.write(closing);
        }
        try {
            readJson(conn);
        } finally {
            conn.disconnect();
        }
    }

    private byte[] getBytes(String accessToken, String url) throws IOException, ExportImportException {
        HttpURLConnection conn = open(accessToken, "GET", url);
        try {
            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] body = readAll(stream);
            if (code >= 400) {
                throw new ExportImportException(errorMessage(body, code));
            }
            return body;
        } finally {
            conn.disconnect();
        }
    }

    private JsonObject readJson(HttpURLConnection conn) throws IOException, ExportImportException {
        int code = conn.getResponseCode();
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String text = new String(readAll(stream), StandardCharsets.UTF_8);
        if (code >= 400) {
            throw new ExportImportException(errorMessage(text.getBytes(StandardCharsets.UTF_8), code));
        }
        if (text.isEmpty()) {
            return new JsonObject();
        }
        JsonElement parsed = JsonParser.parseString(text);
        return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
    }

    private static HttpURLConnection open(String accessToken, String method, String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(60_000);
        conn.setReadTimeout(120_000);
        conn.setInstanceFollowRedirects(true);
        return conn;
    }

    private static byte[] readAll(@Nullable InputStream stream) throws IOException {
        if (stream == null) {
            return new byte[0];
        }
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static String errorMessage(byte[] body, int code) {
        String text = new String(body, StandardCharsets.UTF_8);
        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            if (json.has("error") && json.get("error").isJsonObject()) {
                String message = string(json.getAsJsonObject("error"), "message");
                if (message != null && !message.isEmpty()) {
                    return message;
                }
            }
        } catch (Exception ignored) {
        }
        if (!text.isEmpty()) {
            return text;
        }
        return "HTTP " + code;
    }

    private static JsonArray array(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return new JsonArray();
        }
        return object.getAsJsonArray(key);
    }

    @Nullable
    private static String string(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsString();
    }

    private static boolean bool(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).getAsBoolean();
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }

    private static String escapeQuery(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static String message(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }
}
