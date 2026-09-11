package com.document.immigrantvault.data.backup;

import java.util.Calendar;
import java.util.Locale;

/**
 * Decides whether a monthly auto-backup should run for a given moment.
 * Backups are due on the 1st. If the 1st was missed (device off, etc.), a catch-up
 * run is allowed later in the same month only if auto-backup was already enabled
 * before that month began — so turning it on mid-month does not fire immediately.
 */
public final class AutoBackupPolicy {

    private AutoBackupPolicy() {
    }

    public static boolean shouldRun(Calendar now, long enabledAtMillis, String lastSuccessYearMonth) {
        if (now == null) {
            return false;
        }
        String thisYearMonth = yearMonth(now);
        if (thisYearMonth.equals(lastSuccessYearMonth)) {
            return false;
        }
        if (now.get(Calendar.DAY_OF_MONTH) == 1) {
            return true;
        }
        if (enabledAtMillis <= 0L) {
            return false;
        }
        Calendar firstOfMonth = (Calendar) now.clone();
        firstOfMonth.set(Calendar.DAY_OF_MONTH, 1);
        firstOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        firstOfMonth.set(Calendar.MINUTE, 0);
        firstOfMonth.set(Calendar.SECOND, 0);
        firstOfMonth.set(Calendar.MILLISECOND, 0);
        return enabledAtMillis < firstOfMonth.getTimeInMillis();
    }

    public static String yearMonth(Calendar calendar) {
        return String.format(Locale.US, "%04d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1);
    }
}
