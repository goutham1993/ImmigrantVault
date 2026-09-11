package com.document.immigrantvault.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.backup.AutoBackupPolicy;
import com.document.immigrantvault.data.backup.BackupSchedulePolicy;
import com.document.immigrantvault.data.backup.ExportFormat;
import com.document.immigrantvault.data.cloud.CloudBackupOperations;
import com.document.immigrantvault.data.repository.ExportImportRepository;
import com.document.immigrantvault.util.BackupFolderLocator;
import com.document.immigrantvault.util.BackupPreferences;

import java.util.Calendar;

public class AutoBackupWorker extends Worker {

    public static final String CHANNEL_ID = "immigrant_vault_backups";
    private static final int FOREGROUND_NOTIFICATION_ID = 9101;
    private static final int STATUS_NOTIFICATION_ID = 9102;

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        boolean localEnabled = BackupPreferences.isAutoBackupEnabled(context);
        boolean cloudEnabled = BackupPreferences.isCloudBackupEnabled(context);
        if (!BackupSchedulePolicy.shouldScheduleWorker(localEnabled, cloudEnabled)) {
            return Result.success();
        }

        Calendar now = Calendar.getInstance();
        boolean needLocal = BackupSchedulePolicy.needsDestination(
                now,
                localEnabled,
                BackupPreferences.getEnabledAtMillis(context),
                BackupPreferences.getLastSuccessYearMonth(context));
        boolean needCloud = BackupSchedulePolicy.needsDestination(
                now,
                cloudEnabled,
                BackupPreferences.getCloudEnabledAtMillis(context),
                BackupPreferences.getCloudLastSuccessYearMonth(context));
        if (!needLocal && !needCloud) {
            return Result.success();
        }

        createNotificationChannel();
        try {
            setForegroundAsync(buildForegroundInfo());
        } catch (Exception ignored) {
            // Foreground is best-effort; the backup can still complete without it.
        }

        String fileName = ExportFormat.CSV.buildMonthlyFileName();
        byte[] data;
        try {
            ImmigrantVaultApplication app = (ImmigrantVaultApplication) context;
            ExportImportRepository repository = app.getExportImportRepository();
            data = repository.export(ExportFormat.CSV);
        } catch (Exception e) {
            notifyStatus(
                    context.getString(R.string.auto_backup_notification_failed_title),
                    context.getString(R.string.auto_backup_notification_failed_text, messageOf(e)));
            return Result.retry();
        }

        boolean localOk = !needLocal;
        boolean cloudOk = !needCloud;
        String localError = null;
        String cloudError = null;

        if (needLocal) {
            try {
                BackupFolderLocator.writeBackup(context, data, fileName);
                BackupPreferences.markSuccess(
                        context,
                        AutoBackupPolicy.yearMonth(now),
                        System.currentTimeMillis());
                localOk = true;
            } catch (Exception e) {
                localOk = false;
                localError = messageOf(e);
            }
        }

        if (needCloud) {
            try {
                CloudBackupOperations.upload(context, data, fileName, true);
                BackupPreferences.markCloudSuccess(
                        context,
                        AutoBackupPolicy.yearMonth(now),
                        System.currentTimeMillis());
                cloudOk = true;
            } catch (Exception e) {
                cloudOk = false;
                cloudError = messageOf(e);
            }
        }

        if (localOk && cloudOk) {
            String text;
            if (needLocal && needCloud) {
                text = context.getString(R.string.auto_backup_notification_success_local_and_cloud, fileName);
            } else if (needCloud) {
                text = context.getString(R.string.auto_backup_notification_success_cloud, fileName);
            } else {
                text = context.getString(R.string.auto_backup_notification_success_text, fileName);
            }
            notifyStatus(context.getString(R.string.auto_backup_notification_success_title), text);
            return Result.success();
        }

        String failureText;
        if (localOk && cloudError != null) {
            failureText = context.getString(
                    R.string.auto_backup_notification_partial_cloud_failed, cloudError);
        } else if (cloudOk && localError != null) {
            failureText = localError;
        } else if (cloudError != null) {
            failureText = cloudError;
        } else {
            failureText = localError != null ? localError : "Unknown error";
        }
        notifyStatus(
                context.getString(R.string.auto_backup_notification_failed_title),
                context.getString(R.string.auto_backup_notification_failed_text, failureText));
        return Result.retry();
    }

    private ForegroundInfo buildForegroundInfo() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getApplicationContext().getString(R.string.auto_backup_in_progress))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new ForegroundInfo(
                    FOREGROUND_NOTIFICATION_ID,
                    builder.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        }
        return new ForegroundInfo(FOREGROUND_NOTIFICATION_ID, builder.build());
    }

    private void notifyStatus(String title, String text) {
        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        manager.notify(STATUS_NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getApplicationContext().getString(R.string.auto_backup_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(
                    getApplicationContext().getString(R.string.auto_backup_channel_description));
            NotificationManager manager = getApplicationContext()
                    .getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static String messageOf(Exception e) {
        return e.getMessage() != null ? e.getMessage() : "Unknown error";
    }
}
