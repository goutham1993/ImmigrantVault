package com.document.immigrantvault.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the on-disk layout for vault attachments: {filesDir}/vault_files/{personId}/{uuid}.{ext}.
 * All methods do blocking IO and must be called off the main thread.
 */
public class VaultFileStorage {

    public static final String ROOT_DIR = "vault_files";
    private static final String CAMERA_CACHE_DIR = "camera";

    private final Context context;

    public VaultFileStorage(Context context) {
        this.context = context.getApplicationContext();
    }

    public File getRoot() {
        File root = new File(context.getFilesDir(), ROOT_DIR);
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    public File getPersonDir(long personId) {
        File dir = new File(getRoot(), String.valueOf(personId));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public File resolve(long personId, String storedName) {
        return new File(getPersonDir(personId), storedName);
    }

    public boolean exists(long personId, String storedName) {
        return storedName != null && resolve(personId, storedName).exists();
    }

    /** Copies the contents of {@code source} into the vault and returns the generated stored name. */
    public String importFrom(Uri source, long personId, String mimeType) throws IOException {
        String storedName = generateStoredName(mimeType);
        File target = resolve(personId, storedName);
        ContentResolver resolver = context.getContentResolver();
        try (InputStream in = resolver.openInputStream(source)) {
            if (in == null) {
                throw new IOException("Could not open source file.");
            }
            writeStream(in, target);
        }
        return storedName;
    }

    public String write(byte[] data, long personId, String storedName) throws IOException {
        File target = resolve(personId, storedName);
        try (OutputStream out = new FileOutputStream(target)) {
            out.write(data);
        }
        return storedName;
    }

    /** Duplicates a stored file and returns the new stored name. */
    public String copy(long personId, String storedName, String mimeType) throws IOException {
        File source = resolve(personId, storedName);
        if (!source.exists()) {
            throw new IOException("Missing vault file: " + storedName);
        }
        String copiedName = generateStoredName(mimeType);
        File target = resolve(personId, copiedName);
        try (InputStream in = new java.io.FileInputStream(source)) {
            writeStream(in, target);
        }
        return copiedName;
    }

    public void exportTo(Uri destination, long personId, String storedName) throws IOException {
        File source = resolve(personId, storedName);
        if (!source.exists()) {
            throw new IOException("Missing vault file: " + storedName);
        }
        try (InputStream in = new java.io.FileInputStream(source);
             OutputStream out = context.getContentResolver().openOutputStream(destination)) {
            if (out == null) {
                throw new IOException("Could not open destination.");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    public String generateStoredName(String mimeType) {
        String extension = extensionFor(mimeType);
        return UUID.randomUUID() + (extension != null ? "." + extension : "");
    }

    public long sizeOf(long personId, String storedName) {
        File file = resolve(personId, storedName);
        return file.exists() ? file.length() : 0L;
    }

    public void delete(long personId, String storedName) {
        if (storedName == null) {
            return;
        }
        File file = resolve(personId, storedName);
        if (file.exists()) {
            file.delete();
        }
    }

    public void deletePersonDir(long personId) {
        deleteRecursively(new File(getRoot(), String.valueOf(personId)));
    }

    public void deleteAll() {
        deleteRecursively(new File(context.getFilesDir(), ROOT_DIR));
    }

    /**
     * Removes files on disk that no longer have a matching database row. Stored names are
     * globally unique, so a single set covers every person directory.
     */
    public int deleteOrphans(Set<String> knownStoredNames) {
        File root = new File(context.getFilesDir(), ROOT_DIR);
        File[] personDirs = root.listFiles();
        if (personDirs == null) {
            return 0;
        }
        int removed = 0;
        for (File personDir : personDirs) {
            if (!personDir.isDirectory()) {
                continue;
            }
            File[] files = personDir.listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                if (!knownStoredNames.contains(file.getName()) && file.delete()) {
                    removed++;
                }
            }
            String[] remaining = personDir.list();
            if (remaining != null && remaining.length == 0) {
                personDir.delete();
            }
        }
        return removed;
    }

    public File createCameraTempFile() throws IOException {
        File dir = new File(context.getCacheDir(), CAMERA_CACHE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return File.createTempFile("capture_", ".jpg", dir);
    }

    public void clearCameraCache() {
        File dir = new File(context.getCacheDir(), CAMERA_CACHE_DIR);
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            file.delete();
        }
    }

    public Set<String> listStoredNames() {
        Set<String> names = new HashSet<>();
        File[] personDirs = new File(context.getFilesDir(), ROOT_DIR).listFiles();
        if (personDirs == null) {
            return names;
        }
        for (File personDir : personDirs) {
            File[] files = personDir.listFiles();
            if (files == null) {
                continue;
            }
            for (File file : files) {
                names.add(file.getName());
            }
        }
        return names;
    }

    public byte[] read(long personId, String storedName) throws IOException {
        File file = resolve(personId, storedName);
        if (!file.exists()) {
            throw new IOException("Missing vault file: " + storedName);
        }
        byte[] data = new byte[(int) file.length()];
        try (InputStream in = new java.io.FileInputStream(file)) {
            int offset = 0;
            while (offset < data.length) {
                int read = in.read(data, offset, data.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
        }
        return data;
    }

    public static String extensionFor(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        if ("application/pdf".equals(mimeType)) {
            return "pdf";
        }
        if ("image/jpeg".equals(mimeType)) {
            return "jpg";
        }
        String fromMap = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        return fromMap != null ? fromMap : null;
    }

    public String resolveMimeType(Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            return mimeType;
        }
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (extension != null) {
            String fromMap = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension.toLowerCase());
            if (fromMap != null) {
                return fromMap;
            }
        }
        return "application/octet-stream";
    }

    private static void writeStream(InputStream in, File target) throws IOException {
        try (OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
