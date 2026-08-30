package com.document.immigrantvault.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.document.immigrantvault.data.db.entity.VaultFile;

import java.io.File;

/**
 * Hands vault files to other apps through the app's FileProvider, so the bytes themselves
 * never leave app-private storage.
 */
public final class VaultFileSharing {

    private VaultFileSharing() {
    }

    public static Uri uriFor(Context context, File file) {
        String authority = context.getPackageName() + ".fileprovider";
        return FileProvider.getUriForFile(context, authority, file);
    }

    /** Returns false when no installed app can handle the file. */
    public static boolean view(Context context, VaultFileStorage storage, VaultFile file) {
        File source = storage.resolve(file.personId, file.storedName);
        if (!source.exists()) {
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uriFor(context, source), file.mimeType)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return launch(context, intent);
    }

    public static boolean share(Context context, VaultFileStorage storage, VaultFile file) {
        File source = storage.resolve(file.personId, file.storedName);
        if (!source.exists()) {
            return false;
        }
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType(file.mimeType != null ? file.mimeType : "application/octet-stream")
                .putExtra(Intent.EXTRA_STREAM, uriFor(context, source))
                .putExtra(Intent.EXTRA_SUBJECT, file.displayName)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return launch(context, Intent.createChooser(intent, file.displayName));
    }

    private static boolean launch(Context context, Intent intent) {
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }
}
