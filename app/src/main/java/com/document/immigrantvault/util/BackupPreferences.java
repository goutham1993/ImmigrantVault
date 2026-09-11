package com.document.immigrantvault.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class BackupPreferences {

    static final String PREFS_NAME = "immigrant_vault_prefs";

    private static final String KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled";
    private static final String KEY_AUTO_BACKUP_FOLDER_URI = "auto_backup_folder_uri";
    private static final String KEY_AUTO_BACKUP_ENABLED_AT = "auto_backup_enabled_at";
    private static final String KEY_AUTO_BACKUP_LAST_SUCCESS_YM = "auto_backup_last_success_ym";
    private static final String KEY_AUTO_BACKUP_LAST_SUCCESS_AT = "auto_backup_last_success_at";

    private static final String KEY_CLOUD_BACKUP_ENABLED = "cloud_backup_enabled";
    private static final String KEY_CLOUD_BACKUP_ENABLED_AT = "cloud_backup_enabled_at";
    private static final String KEY_CLOUD_ACCOUNT_EMAIL = "cloud_backup_account_email";
    private static final String KEY_DRIVE_FOLDER_ID = "cloud_backup_drive_folder_id";
    private static final String KEY_CLOUD_LAST_SUCCESS_YM = "cloud_backup_last_success_ym";
    private static final String KEY_CLOUD_LAST_SUCCESS_AT = "cloud_backup_last_success_at";

    private BackupPreferences() {
    }

    public static boolean isAutoBackupEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_BACKUP_ENABLED, true);
    }

    public static void setAutoBackupEnabled(Context context, boolean enabled) {
        boolean wasEnabled = isAutoBackupEnabled(context);
        SharedPreferences.Editor editor = prefs(context).edit()
                .putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled);
        if (enabled && !wasEnabled) {
            editor.putLong(KEY_AUTO_BACKUP_ENABLED_AT, System.currentTimeMillis());
        } else if (!enabled) {
            editor.remove(KEY_AUTO_BACKUP_ENABLED_AT);
        }
        editor.apply();
    }

    public static void ensureEnabledAt(Context context) {
        if (isAutoBackupEnabled(context) && getEnabledAtMillis(context) <= 0L) {
            prefs(context).edit()
                    .putLong(KEY_AUTO_BACKUP_ENABLED_AT, System.currentTimeMillis())
                    .apply();
        }
    }

    public static long getEnabledAtMillis(Context context) {
        return prefs(context).getLong(KEY_AUTO_BACKUP_ENABLED_AT, 0L);
    }

    public static String getFolderUri(Context context) {
        return prefs(context).getString(KEY_AUTO_BACKUP_FOLDER_URI, null);
    }

    public static void setFolderUri(Context context, String uri) {
        SharedPreferences.Editor editor = prefs(context).edit();
        if (uri == null || uri.isEmpty()) {
            editor.remove(KEY_AUTO_BACKUP_FOLDER_URI);
        } else {
            editor.putString(KEY_AUTO_BACKUP_FOLDER_URI, uri);
        }
        editor.apply();
    }

    public static String getLastSuccessYearMonth(Context context) {
        return prefs(context).getString(KEY_AUTO_BACKUP_LAST_SUCCESS_YM, null);
    }

    public static long getLastSuccessAtMillis(Context context) {
        return prefs(context).getLong(KEY_AUTO_BACKUP_LAST_SUCCESS_AT, 0L);
    }

    public static void markSuccess(Context context, String yearMonth, long atMillis) {
        prefs(context).edit()
                .putString(KEY_AUTO_BACKUP_LAST_SUCCESS_YM, yearMonth)
                .putLong(KEY_AUTO_BACKUP_LAST_SUCCESS_AT, atMillis)
                .apply();
    }

    public static boolean isCloudBackupEnabled(Context context) {
        return prefs(context).getBoolean(KEY_CLOUD_BACKUP_ENABLED, false);
    }

    public static void setCloudBackupEnabled(Context context, boolean enabled) {
        boolean wasEnabled = isCloudBackupEnabled(context);
        SharedPreferences.Editor editor = prefs(context).edit()
                .putBoolean(KEY_CLOUD_BACKUP_ENABLED, enabled);
        if (enabled && !wasEnabled) {
            editor.putLong(KEY_CLOUD_BACKUP_ENABLED_AT, System.currentTimeMillis());
        } else if (!enabled) {
            editor.remove(KEY_CLOUD_BACKUP_ENABLED_AT);
        }
        editor.apply();
    }

    public static void ensureCloudEnabledAt(Context context) {
        if (isCloudBackupEnabled(context) && getCloudEnabledAtMillis(context) <= 0L) {
            prefs(context).edit()
                    .putLong(KEY_CLOUD_BACKUP_ENABLED_AT, System.currentTimeMillis())
                    .apply();
        }
    }

    public static long getCloudEnabledAtMillis(Context context) {
        return prefs(context).getLong(KEY_CLOUD_BACKUP_ENABLED_AT, 0L);
    }

    public static String getCloudAccountEmail(Context context) {
        return prefs(context).getString(KEY_CLOUD_ACCOUNT_EMAIL, null);
    }

    public static void setCloudAccountEmail(Context context, String email) {
        SharedPreferences.Editor editor = prefs(context).edit();
        if (email == null || email.isEmpty()) {
            editor.remove(KEY_CLOUD_ACCOUNT_EMAIL);
        } else {
            editor.putString(KEY_CLOUD_ACCOUNT_EMAIL, email);
        }
        editor.apply();
    }

    public static boolean isCloudConnected(Context context) {
        String email = getCloudAccountEmail(context);
        return email != null && !email.isEmpty();
    }

    public static String getDriveFolderId(Context context) {
        return prefs(context).getString(KEY_DRIVE_FOLDER_ID, null);
    }

    public static void setDriveFolderId(Context context, String folderId) {
        SharedPreferences.Editor editor = prefs(context).edit();
        if (folderId == null || folderId.isEmpty()) {
            editor.remove(KEY_DRIVE_FOLDER_ID);
        } else {
            editor.putString(KEY_DRIVE_FOLDER_ID, folderId);
        }
        editor.apply();
    }

    public static String getCloudLastSuccessYearMonth(Context context) {
        return prefs(context).getString(KEY_CLOUD_LAST_SUCCESS_YM, null);
    }

    public static long getCloudLastSuccessAtMillis(Context context) {
        return prefs(context).getLong(KEY_CLOUD_LAST_SUCCESS_AT, 0L);
    }

    public static void markCloudSuccess(Context context, String yearMonth, long atMillis) {
        prefs(context).edit()
                .putString(KEY_CLOUD_LAST_SUCCESS_YM, yearMonth)
                .putLong(KEY_CLOUD_LAST_SUCCESS_AT, atMillis)
                .apply();
    }

    public static void disconnectCloud(Context context) {
        prefs(context).edit()
                .putBoolean(KEY_CLOUD_BACKUP_ENABLED, false)
                .remove(KEY_CLOUD_BACKUP_ENABLED_AT)
                .remove(KEY_CLOUD_ACCOUNT_EMAIL)
                .remove(KEY_DRIVE_FOLDER_ID)
                .remove(KEY_CLOUD_LAST_SUCCESS_YM)
                .remove(KEY_CLOUD_LAST_SUCCESS_AT)
                .apply();
    }

    public static boolean shouldScheduleWorker(Context context) {
        return isAutoBackupEnabled(context) || isCloudBackupEnabled(context);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
