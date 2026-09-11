package com.document.immigrantvault;

import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;
import com.document.immigrantvault.ui.common.TimelineCategoryFilter;
import com.document.immigrantvault.ui.common.TimelineCategoryFilter.Category;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TimelineCategoryFilterTest {

    @Test
    public void filter_allOrEmptySelection_returnsEveryEvent() {
        List<TimelineEvent> events = mixedEvents();

        assertSame(events, TimelineCategoryFilter.filter(events, Collections.emptySet()));
        assertSame(events, TimelineCategoryFilter.filter(events, null));
        assertEquals(events, TimelineCategoryFilter.filter(events, EnumSet.noneOf(Category.class)));
    }

    @Test
    public void filter_nullOrEmptySource_returnsEmpty() {
        assertTrue(TimelineCategoryFilter.filter(null, EnumSet.of(Category.VISA)).isEmpty());
        assertTrue(TimelineCategoryFilter.filter(Collections.emptyList(), EnumSet.of(Category.VISA))
                .isEmpty());
    }

    @Test
    public void filter_visaOnly_includesStartAndEnd() {
        List<TimelineEvent> filtered = TimelineCategoryFilter.filter(
                mixedEvents(), EnumSet.of(Category.VISA));

        assertEquals(Arrays.asList(
                TimelineEventType.VISA_START,
                TimelineEventType.VISA_END
        ), typesOf(filtered));
    }

    @Test
    public void filter_addressOnly_includesAddressChanges() {
        List<TimelineEvent> filtered = TimelineCategoryFilter.filter(
                mixedEvents(), EnumSet.of(Category.ADDRESS));

        assertEquals(Collections.singletonList(TimelineEventType.ADDRESS_CHANGE), typesOf(filtered));
    }

    @Test
    public void filter_employerOnly_includesEmployerChanges() {
        List<TimelineEvent> filtered = TimelineCategoryFilter.filter(
                mixedEvents(), EnumSet.of(Category.EMPLOYER));

        assertEquals(Collections.singletonList(TimelineEventType.EMPLOYER_CHANGE), typesOf(filtered));
    }

    @Test
    public void filter_visaAndEmployer_excludesUnrelatedTypes() {
        List<TimelineEvent> filtered = TimelineCategoryFilter.filter(
                mixedEvents(), EnumSet.of(Category.VISA, Category.EMPLOYER));

        assertEquals(Arrays.asList(
                TimelineEventType.VISA_START,
                TimelineEventType.EMPLOYER_CHANGE,
                TimelineEventType.VISA_END
        ), typesOf(filtered));
    }

    @Test
    public void filter_skipsNullEventTypesWhenCategorySelected() {
        TimelineEvent unlabeled = event(null);
        List<TimelineEvent> events = Arrays.asList(
                event(TimelineEventType.VISA_START),
                unlabeled,
                event(TimelineEventType.DOCUMENT_ADDED)
        );

        List<TimelineEvent> filtered = TimelineCategoryFilter.filter(
                events, EnumSet.of(Category.VISA));

        assertEquals(Collections.singletonList(TimelineEventType.VISA_START), typesOf(filtered));
    }

    private static List<TimelineEvent> mixedEvents() {
        return Arrays.asList(
                event(TimelineEventType.VISA_START),
                event(TimelineEventType.DOCUMENT_ADDED),
                event(TimelineEventType.ADDRESS_CHANGE),
                event(TimelineEventType.EMPLOYER_CHANGE),
                event(TimelineEventType.TRAVEL_ENTRY),
                event(TimelineEventType.VISA_END),
                event(TimelineEventType.PETITION_FILED),
                event(TimelineEventType.W2_ADDED),
                event(TimelineEventType.EDUCATION)
        );
    }

    private static TimelineEvent event(TimelineEventType type) {
        TimelineEvent event = new TimelineEvent();
        event.eventType = type;
        return event;
    }

    private static List<TimelineEventType> typesOf(List<TimelineEvent> events) {
        List<TimelineEventType> types = new ArrayList<>();
        for (TimelineEvent event : events) {
            types.add(event.eventType);
        }
        return types;
    }
}
