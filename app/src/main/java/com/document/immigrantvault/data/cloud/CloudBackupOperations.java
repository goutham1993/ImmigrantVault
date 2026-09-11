package com.document.immigrantvault.data.cloud;

import android.content.Context;

import com.document.immigrantvault.util.BackupPreferences;

import java.util.List;

/**
 * Shared Drive upload/list/download used by Settings and the monthly worker.
 */
public final class CloudBackupOperations {

    private CloudBackupOperations() {
    }

    public static void upload(Context context, byte[] data, String fileName, boolean replaceSameName)
            throws Exception {
        GoogleDriveBackupClient client = new GoogleDriveBackupClient();
        String token = DriveAuthHelper.silentAccessToken(context);
        String folderId = client.ensureFolder(token, BackupPreferences.getDriveFolderId(context));
        BackupPreferences.setDriveFolderId(context, folderId);
        client.upload(token, folderId, fileName, data, replaceSameName);
    }

    public static List<DriveBackupItem> list(Context context) throws Exception {
        GoogleDriveBackupClient client = new GoogleDriveBackupClient();
        String token = DriveAuthHelper.silentAccessToken(context);
        String folderId = client.ensureFolder(token, BackupPreferences.getDriveFolderId(context));
        BackupPreferences.setDriveFolderId(context, folderId);
        return client.listBackups(token, folderId);
    }

    public static byte[] download(Context context, String fileId) throws Exception {
        String token = DriveAuthHelper.silentAccessToken(context);
        return new GoogleDriveBackupClient().download(token, fileId);
    }
}
