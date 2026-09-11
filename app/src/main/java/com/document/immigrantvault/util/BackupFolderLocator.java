package com.document.immigrantvault.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;

import com.document.immigrantvault.data.backup.ExportImportException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

public final class BackupFolderLocator {

    private BackupFolderLocator() {
    }

    public static File getDefaultDirectory(Context context) {
        File documents = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File dir = documents != null
                ? new File(documents, "backups")
                : new File(context.getFilesDir(), "backups");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public static String getDisplayPath(Context context) {
        String stored = BackupPreferences.getFolderUri(context);
        if (stored == null || stored.isEmpty()) {
            return getDefaultDirectory(context).getAbsolutePath();
        }
        return friendlyTreePath(context, Uri.parse(stored));
    }

    public static boolean hasWritableFolder(Context context) {
        String stored = BackupPreferences.getFolderUri(context);
        if (stored == null || stored.isEmpty()) {
            File dir = getDefaultDirectory(context);
            return dir.exists() && dir.canWrite();
        }
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(context, Uri.parse(stored));
            return dir != null && dir.exists() && dir.canWrite();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isUsingCustomFolder(Context context) {
        String stored = BackupPreferences.getFolderUri(context);
        return stored != null && !stored.isEmpty();
    }

    public static void writeBackup(Context context, byte[] data, String fileName)
            throws Exception {
        String stored = BackupPreferences.getFolderUri(context);
        if (stored == null || stored.isEmpty()) {
            File out = new File(getDefaultDirectory(context), fileName);
            try (FileOutputStream stream = new FileOutputStream(out)) {
                stream.write(data);
            }
            return;
        }
        Uri treeUri = Uri.parse(stored);
        DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
        if (dir == null || !dir.exists() || !dir.canWrite()) {
            throw new ExportImportException(
                    "Backup folder is no longer available. Choose a new folder in Settings.");
        }
        deleteExisting(dir, fileName);
        String displayName = stripZipExtension(fileName);
        DocumentFile file = dir.createFile("application/zip", displayName);
        if (file == null) {
            throw new ExportImportException("Could not create the backup file in the selected folder.");
        }
        try (OutputStream stream = context.getContentResolver().openOutputStream(file.getUri())) {
            if (stream == null) {
                throw new ExportImportException("Could not write to the selected folder.");
            }
            stream.write(data);
        }
    }

    private static void deleteExisting(DocumentFile dir, String fileName) {
        String withoutExt = stripZipExtension(fileName);
        DocumentFile[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (DocumentFile child : children) {
            String name = child.getName();
            if (name == null) {
                continue;
            }
            if (name.equals(fileName) || name.equals(withoutExt) || name.equals(withoutExt + ".zip")) {
                child.delete();
            }
        }
    }

    private static String stripZipExtension(String fileName) {
        if (fileName != null && fileName.toLowerCase(Locale.US).endsWith(".zip")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }

    static String friendlyTreePath(Context context, Uri treeUri) {
        try {
            String docId = DocumentsContract.getTreeDocumentId(treeUri);
            String[] parts = docId.split(":", 2);
            String volume = parts[0];
            String rest = parts.length > 1 ? parts[1] : "";
            String rootLabel = "primary".equalsIgnoreCase(volume)
                    ? "Internal storage"
                    : volume;
            if (rest == null || rest.isEmpty()) {
                return folderNameOr(context, treeUri, rootLabel);
            }
            return rootLabel + "/" + rest;
        } catch (Exception ignored) {
            return folderNameOr(context, treeUri, treeUri.toString());
        }
    }

    private static String folderNameOr(Context context, Uri treeUri, String fallback) {
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
            if (dir != null && dir.getName() != null && !dir.getName().isEmpty()) {
                return dir.getName();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
