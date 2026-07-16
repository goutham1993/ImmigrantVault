package com.document.immigrantvault.util;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.document.immigrantvault.worker.ReminderWorker;

import java.util.concurrent.TimeUnit;

public final class ReminderScheduler {

    private static final String REMINDER_WORK_NAME = "reminder_check_work";

    private ReminderScheduler() {
    }

    public static void schedule(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class,
                1,
                TimeUnit.DAYS
        ).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                REMINDER_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }
}
