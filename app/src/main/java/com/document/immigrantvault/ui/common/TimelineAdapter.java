package com.document.immigrantvault.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.data.db.entity.TimelineEventType;
import com.document.immigrantvault.databinding.ItemTimelineBinding;
import com.document.immigrantvault.databinding.ItemTimelineYearBinding;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_YEAR = 0;
    private static final int TYPE_EVENT = 1;

    private static final SimpleDateFormat MONTH_DAY =
            new SimpleDateFormat("MMMM d", Locale.US);

    private final List<Row> rows = new ArrayList<>();

    public void setItems(List<TimelineEvent> events) {
        rows.clear();
        if (events != null && !events.isEmpty()) {
            Integer lastYear = null;
            for (int i = 0; i < events.size(); i++) {
                TimelineEvent event = events.get(i);
                Integer year = yearOf(event);
                if (year != null && !year.equals(lastYear)) {
                    rows.add(Row.year(String.valueOf(year)));
                    lastYear = year;
                } else if (year == null && lastYear != null) {
                    rows.add(Row.year("—"));
                    lastYear = null;
                } else if (year == null && lastYear == null && rows.isEmpty()) {
                    rows.add(Row.year("—"));
                }

                boolean firstInGroup = i == 0
                        || !sameYear(events.get(i - 1), event);
                boolean lastInGroup = i == events.size() - 1
                        || !sameYear(event, events.get(i + 1));
                rows.add(Row.event(event, firstInGroup, lastInGroup));
            }
        }
        notifyDataSetChanged();
    }

    private static Integer yearOf(TimelineEvent event) {
        if (event == null || event.eventDate == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(event.eventDate);
        return cal.get(Calendar.YEAR);
    }

    private static boolean sameYear(TimelineEvent a, TimelineEvent b) {
        Integer ya = yearOf(a);
        Integer yb = yearOf(b);
        if (ya == null && yb == null) {
            return true;
        }
        if (ya == null || yb == null) {
            return false;
        }
        return ya.equals(yb);
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).isYear() ? TYPE_YEAR : TYPE_EVENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_YEAR) {
            return new YearViewHolder(ItemTimelineYearBinding.inflate(inflater, parent, false));
        }
        return new EventViewHolder(ItemTimelineBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof YearViewHolder) {
            ((YearViewHolder) holder).bind(row.yearLabel, position == 0);
        } else {
            ((EventViewHolder) holder).bind(row);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static final class Row {
        final String yearLabel;
        final TimelineEvent event;
        final boolean firstInGroup;
        final boolean lastInGroup;

        private Row(String yearLabel, TimelineEvent event, boolean firstInGroup, boolean lastInGroup) {
            this.yearLabel = yearLabel;
            this.event = event;
            this.firstInGroup = firstInGroup;
            this.lastInGroup = lastInGroup;
        }

        static Row year(String label) {
            return new Row(label, null, false, false);
        }

        static Row event(TimelineEvent event, boolean firstInGroup, boolean lastInGroup) {
            return new Row(null, event, firstInGroup, lastInGroup);
        }

        boolean isYear() {
            return event == null;
        }
    }

    static class YearViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimelineYearBinding binding;

        YearViewHolder(ItemTimelineYearBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String year, boolean isFirst) {
            binding.timelineYearHeader.setText(year);
            int top = isFirst ? 4 : 20;
            float density = binding.getRoot().getResources().getDisplayMetrics().density;
            binding.timelineYearHeader.setPadding(
                    binding.timelineYearHeader.getPaddingLeft(),
                    Math.round(top * density),
                    binding.timelineYearHeader.getPaddingRight(),
                    binding.timelineYearHeader.getPaddingBottom());
        }
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimelineBinding binding;

        EventViewHolder(ItemTimelineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Row row) {
            TimelineEvent event = row.event;

            binding.timelineLineTop.setVisibility(row.firstInGroup ? View.INVISIBLE : View.VISIBLE);
            binding.timelineLineBottom.setVisibility(row.lastInGroup ? View.INVISIBLE : View.VISIBLE);

            if (event.eventDate != null) {
                binding.timelineDate.setText(MONTH_DAY.format(event.eventDate));
            } else {
                binding.timelineDate.setText("—");
            }

            String chipLabel = documentChipLabel(event);
            if (chipLabel != null) {
                boolean added = event.eventType == TimelineEventType.DOCUMENT_ADDED;
                binding.timelineTitle.setText(added ? "Document added" : "Document expires");
            } else {
                binding.timelineTitle.setText(event.title);
            }

            boolean hasDescription = event.description != null && !event.description.isEmpty();
            binding.timelineDescription.setText(hasDescription ? event.description : null);
            binding.timelineDescription.setVisibility(hasDescription ? View.VISIBLE : View.GONE);

            bindDocumentChip(chipLabel);
        }

        private void bindDocumentChip(String chipLabel) {
            binding.timelineChips.removeAllViews();
            if (chipLabel == null) {
                binding.timelineChips.setVisibility(View.GONE);
                return;
            }

            Context context = binding.getRoot().getContext();
            Chip chip = new Chip(context);
            chip.setText(chipLabel);
            chip.setClickable(false);
            chip.setCheckable(false);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.md_theme_primary_container)));
            chip.setTextColor(ContextCompat.getColor(context, R.color.md_theme_on_primary_container));

            binding.timelineChips.addView(chip);
            binding.timelineChips.setVisibility(View.VISIBLE);
        }

        private static String documentChipLabel(TimelineEvent event) {
            if (event == null || event.eventType == null) {
                return null;
            }
            if (event.eventType != TimelineEventType.DOCUMENT_ADDED
                    && event.eventType != TimelineEventType.DOCUMENT_EXPIRY) {
                return null;
            }
            String title = event.title;
            if (title == null || title.isEmpty()) {
                return null;
            }
            if (title.endsWith(" added")) {
                return title.substring(0, title.length() - " added".length()).trim();
            }
            if (title.endsWith(" expires")) {
                return title.substring(0, title.length() - " expires".length()).trim();
            }
            return title;
        }
    }
}
