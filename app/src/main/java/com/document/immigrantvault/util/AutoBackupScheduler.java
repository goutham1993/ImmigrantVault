package com.document.immigrantvault.util;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.document.immigrantvault.worker.AutoBackupWorker;

import java.util.concurrent.TimeUnit;

public final class AutoBackupScheduler {

    static final String AUTO_BACKUP_WORK_NAME = "monthly_auto_backup_work";

    private AutoBackupScheduler() {
    }

    public static void schedule(Context context) {
        schedule(context, false);
    }

    public static void reschedule(Context context) {
        schedule(context, true);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(AUTO_BACKUP_WORK_NAME);
    }

    private static void schedule(Context context, boolean replace) {
        BackupPreferences.ensureEnabledAt(context);
        BackupPreferences.ensureCloudEnabledAt(context);
        if (!BackupPreferences.shouldScheduleWorker(context)) {
            cancel(context);
            return;
        }
        Constraints.Builder constraints = new Constraints.Builder();
        if (BackupPreferences.isCloudBackupEnabled(context)) {
            constraints.setRequiredNetworkType(NetworkType.CONNECTED);
        }
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                AutoBackupWorker.class,
                1,
                TimeUnit.DAYS
        ).setConstraints(constraints.build()).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                AUTO_BACKUP_WORK_NAME,
                replace ? ExistingPeriodicWorkPolicy.UPDATE : ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }
}
