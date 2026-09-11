package com.document.immigrantvault;

import com.document.immigrantvault.data.backup.AutoBackupPolicy;
import com.document.immigrantvault.data.backup.BackupSchedulePolicy;
import com.document.immigrantvault.data.cloud.DriveBackupItem;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BackupSchedulePolicyTest {

    @Test
    public void schedulesWhenEitherDestinationIsEnabled() {
        assertFalse(BackupSchedulePolicy.shouldScheduleWorker(false, false));
        assertTrue(BackupSchedulePolicy.shouldScheduleWorker(true, false));
        assertTrue(BackupSchedulePolicy.shouldScheduleWorker(false, true));
        assertTrue(BackupSchedulePolicy.shouldScheduleWorker(true, true));
    }

    @Test
    public void skipsDisabledDestinationEvenOnFirstOfMonth() {
        Calendar now = date(2026, Calendar.SEPTEMBER, 1);
        assertFalse(BackupSchedulePolicy.needsDestination(now, false, 0L, null));
        assertTrue(BackupSchedulePolicy.needsDestination(now, true, 0L, null));
    }

    @Test
    public void localAndCloudSuccessAreIndependent() {
        Calendar now = date(2026, Calendar.SEPTEMBER, 1);
        assertFalse(BackupSchedulePolicy.needsDestination(now, true, 1L, "2026-09"));
        assertTrue(BackupSchedulePolicy.needsDestination(now, true, 1L, null));
    }

    @Test
    public void cloudCatchUpUsesSameMonthlyPolicy() {
        Calendar now = date(2026, Calendar.SEPTEMBER, 11);
        long enabledThisMonth = date(2026, Calendar.SEPTEMBER, 11).getTimeInMillis();
        long enabledLastMonth = date(2026, Calendar.AUGUST, 20).getTimeInMillis();
        assertFalse(BackupSchedulePolicy.needsDestination(now, true, enabledThisMonth, null));
        assertTrue(BackupSchedulePolicy.needsDestination(now, true, enabledLastMonth, null));
        assertEquals("2026-09", AutoBackupPolicy.yearMonth(now));
    }

    @Test
    public void driveBackupItemShowsDateWhenPresent() {
        DriveBackupItem withDate = new DriveBackupItem(
                "id1", "immigrant_vault_backup.zip", "2026-09-11T18:00:00.000Z");
        assertEquals("immigrant_vault_backup.zip\n2026-09-11", withDate.displayLabel());
        DriveBackupItem noDate = new DriveBackupItem("id2", "manual.zip", null);
        assertEquals("manual.zip", noDate.displayLabel());
    }

    private static Calendar date(int year, int month, int day) {
        Calendar calendar = new GregorianCalendar(year, month, day, 9, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
}
