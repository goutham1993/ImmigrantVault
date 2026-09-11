package com.document.immigrantvault.data.backup;

import java.util.Calendar;

/**
 * Combines local and Google Drive destinations when deciding whether the
 * monthly worker should be scheduled or should write a given destination.
 */
public final class BackupSchedulePolicy {

    private BackupSchedulePolicy() {
    }

    public static boolean shouldScheduleWorker(boolean localEnabled, boolean cloudEnabled) {
        return localEnabled || cloudEnabled;
    }

    public static boolean needsDestination(Calendar now, boolean enabled, long enabledAtMillis,
                                           String lastSuccessYearMonth) {
        return enabled && AutoBackupPolicy.shouldRun(now, enabledAtMillis, lastSuccessYearMonth);
    }
}
