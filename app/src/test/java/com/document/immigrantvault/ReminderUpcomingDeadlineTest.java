package com.document.immigrantvault;

import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.data.repository.ReminderRepository;
import com.document.immigrantvault.util.DateUtils;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class ReminderUpcomingDeadlineTest {

    @Test
    public void filter_keepsOnlyDeadlinesFromTodayThrough30Days() {
        Reminder overdue = reminder("overdue", DateUtils.addDays(new Date(), -1), 0);
        Reminder today = reminder("today", new Date(), 0);
        Reminder in30 = reminder("in30", DateUtils.addDays(new Date(), 30), 0);
        Reminder in31 = reminder("in31", DateUtils.addDays(new Date(), 31), 0);
        // Trigger already passed, but actual expiry is still within 30 days.
        Reminder soonAfterLead = reminder("soon", DateUtils.addDays(new Date(), -20), 30);
        // Trigger is soon, but actual expiry is more than 30 days out.
        Reminder farAfterLead = reminder("far", DateUtils.addDays(new Date(), 10), 30);

        List<Reminder> result = ReminderRepository.filterArrivingWithinUpcomingWindow(
                Arrays.asList(overdue, today, in30, in31, soonAfterLead, farAfterLead));

        assertEquals(Arrays.asList("today", "in30", "soon"), titles(result));
    }

    private static Reminder reminder(String title, Date trigger, int leadDays) {
        Reminder reminder = new Reminder();
        reminder.title = title;
        reminder.triggerDate = trigger;
        reminder.leadDays = leadDays;
        return reminder;
    }

    private static List<String> titles(List<Reminder> reminders) {
        return reminders.stream().map(reminder -> reminder.title).collect(Collectors.toList());
    }
}
