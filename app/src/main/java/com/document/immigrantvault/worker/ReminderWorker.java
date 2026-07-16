package com.document.immigrantvault.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.data.repository.ReminderRepository;

import java.util.Calendar;
import java.util.List;

public class ReminderWorker extends Worker {

    public static final String CHANNEL_ID = "immigrant_vault_reminders";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        createNotificationChannel();
        ImmigrantVaultApplication app = (ImmigrantVaultApplication) getApplicationContext();
        ReminderRepository repo = app.getReminderRepository();
        List<Reminder> due = repo.getDueRemindersSync();
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        NotificationManager manager = (NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        for (Reminder reminder : due) {
            if (!reminder.enabled || reminder.triggerDate == null) {
                continue;
            }
            Calendar trigger = Calendar.getInstance();
            trigger.setTime(reminder.triggerDate);
            trigger.set(Calendar.HOUR_OF_DAY, 0);
            trigger.set(Calendar.MINUTE, 0);
            trigger.set(Calendar.SECOND, 0);
            trigger.set(Calendar.MILLISECOND, 0);

            if (!trigger.after(today)) {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(
                        getApplicationContext(), CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(reminder.title)
                        .setContentText(reminder.body)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

                manager.notify((int) reminder.id, builder.build());
            }
        }
        return Result.success();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Immigration Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Document expiry and petition status reminders");
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
