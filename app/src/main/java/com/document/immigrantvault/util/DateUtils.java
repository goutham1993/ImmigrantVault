package com.document.immigrantvault.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class DateUtils {

    private static final SimpleDateFormat DISPLAY_FORMAT =
            new SimpleDateFormat("MMM d, yyyy", Locale.US);

    private DateUtils() {
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return "—";
        }
        return DISPLAY_FORMAT.format(date);
    }

    public static int daysUntil(Date date) {
        if (date == null) {
            return Integer.MAX_VALUE;
        }
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar target = Calendar.getInstance();
        target.setTime(date);
        target.set(Calendar.HOUR_OF_DAY, 0);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        long diff = target.getTimeInMillis() - today.getTimeInMillis();
        return (int) TimeUnit.MILLISECONDS.toDays(diff);
    }

    public static String daysUntilLabel(Date date) {
        if (date == null) {
            return "No date set";
        }
        int days = daysUntil(date);
        if (days < 0) {
            return "Expired " + Math.abs(days) + " days ago";
        }
        if (days == 0) {
            return "Expires today";
        }
        if (days == 1) {
            return "1 day remaining";
        }
        return days + " days remaining";
    }

    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }

    public static String formatEmploymentDateRange(Date start, Date end, boolean isCurrent) {
        String startStr = formatDate(start);
        String endStr = isCurrent ? "Present" : formatDate(end);
        return startStr + " – " + endStr;
    }

    public static String formatTravelDateRange(Date departure, Date arrival) {
        return formatDate(departure) + " – " + formatDate(arrival);
    }

    public static String formatTravelDays(Date departure, Date arrival) {
        int days = daysBetween(departure, arrival);
        if (days < 0) {
            return "";
        }
        if (days == 0) {
            return "Same day";
        }
        if (days == 1) {
            return "1 day";
        }
        return days + " days";
    }

    public static int daysBetween(Date start, Date end) {
        if (start == null || end == null) {
            return -1;
        }
        Calendar startCal = startOfDay(start);
        Calendar endCal = startOfDay(end);
        long diff = endCal.getTimeInMillis() - startCal.getTimeInMillis();
        return (int) TimeUnit.MILLISECONDS.toDays(Math.abs(diff));
    }

    private static Calendar startOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    public static String formatYearsMonths(Date start, Date end, boolean isCurrent) {
        if (start == null) {
            return "";
        }
        Date effectiveEnd = isCurrent || end == null ? new Date() : end;
        int[] parts = yearsAndMonthsBetween(start, effectiveEnd);
        StringBuilder sb = new StringBuilder();
        if (parts[0] > 0) {
            sb.append(parts[0]).append(parts[0] == 1 ? " year" : " years");
        }
        if (parts[1] > 0) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(parts[1]).append(parts[1] == 1 ? " month" : " months");
        }
        if (sb.length() == 0) {
            return "Less than 1 month";
        }
        return sb.toString();
    }

    private static int[] yearsAndMonthsBetween(Date start, Date end) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start);
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(end);

        int years = endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR);
        int months = endCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH);
        if (endCal.get(Calendar.DAY_OF_MONTH) < startCal.get(Calendar.DAY_OF_MONTH)) {
            months--;
        }
        if (months < 0) {
            years--;
            months += 12;
        }
        return new int[]{Math.max(years, 0), Math.max(months, 0)};
    }
}
