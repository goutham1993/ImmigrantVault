package com.document.immigrantvault.ui.common;

import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class TimelineCategoryFilter {

    public enum Category {
        VISA,
        ADDRESS,
        EMPLOYER
    }

    private TimelineCategoryFilter() {
    }

    public static List<TimelineEvent> filter(List<TimelineEvent> events, Set<Category> selected) {
        if (events == null || events.isEmpty()) {
            return events == null ? Collections.emptyList() : events;
        }
        if (selected == null || selected.isEmpty()) {
            return events;
        }

        List<TimelineEvent> filtered = new ArrayList<>();
        for (TimelineEvent event : events) {
            if (matches(event, selected)) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private static boolean matches(TimelineEvent event, Set<Category> selected) {
        if (event == null || event.eventType == null) {
            return false;
        }
        Category category = categoryOf(event.eventType);
        return category != null && selected.contains(category);
    }

    private static Category categoryOf(TimelineEventType type) {
        switch (type) {
            case VISA_START:
            case VISA_END:
                return Category.VISA;
            case ADDRESS_CHANGE:
                return Category.ADDRESS;
            case EMPLOYER_CHANGE:
                return Category.EMPLOYER;
            default:
                return null;
        }
    }
}
