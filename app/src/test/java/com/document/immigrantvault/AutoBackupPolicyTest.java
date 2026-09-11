package com.document.immigrantvault;

import com.document.immigrantvault.data.backup.AutoBackupPolicy;

import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AutoBackupPolicyTest {

    @Test
    public void runsOnFirstOfMonthWhenNeverBackedUp() {
        Calendar now = date(2026, Calendar.SEPTEMBER, 1);
        assertTrue(AutoBackupPolicy.shouldRun(now, 0L, null));
    }

    @Test
    public void skipsMidMonthWhenJustEnabled() {
        Calendar now = date(2026, Calendar.SEPTEMBER, 11);
        long enabledAt = date(2026, Calendar.SEPTEMBER, 11).getTimeInMillis();
        assertFalse(AutoBackupPolicy.shouldRun(now, enabledAt, null));
    }

    @Test
    public void catchUpIfEnabledBeforeThisMonthAndFirstWasMissed() {
        Calendar now = date(2026, Calendar.SEPTEMBER, 11);
        long enabledAt = date(2026, Calendar.AUGUST, 20).getTimeInMillis();
        assertTrue(AutoBackupPolicy.shouldRun(now, enabledAt, null));
    }

    @Test
    public void skipsWhenThisMonthAlreadySucceeded() {
        Calendar now = date(2026, Calendar.SEPTEMBER, 1);
        assertFalse(AutoBackupPolicy.shouldRun(now, 1L, "2026-09"));
    }

    @Test
    public void runsNextMonthAfterPriorSuccess() {
        Calendar now = date(2026, Calendar.OCTOBER, 1);
        assertTrue(AutoBackupPolicy.shouldRun(now, 1L, "2026-09"));
    }

    @Test
    public void yearMonthIsZeroPadded() {
        Calendar now = date(2026, Calendar.JANUARY, 5);
        assertEquals("2026-01", AutoBackupPolicy.yearMonth(now));
    }

    private static Calendar date(int year, int month, int day) {
        Calendar calendar = new GregorianCalendar(year, month, day, 9, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
}
