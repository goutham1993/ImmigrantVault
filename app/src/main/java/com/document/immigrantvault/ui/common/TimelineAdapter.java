package com.document.immigrantvault.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.document.immigrantvault.data.db.entity.TimelineEvent;
import com.document.immigrantvault.databinding.ItemTimelineBinding;
import com.document.immigrantvault.util.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    private static final SimpleDateFormat MONTH_DAY =
            new SimpleDateFormat("MMM d", Locale.US);
    private static final SimpleDateFormat YEAR =
            new SimpleDateFormat("yyyy", Locale.US);

    private final List<TimelineEvent> items = new ArrayList<>();

    public void setItems(List<TimelineEvent> events) {
        items.clear();
        if (events != null) {
            items.addAll(events);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTimelineBinding binding = ItemTimelineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimelineBinding binding;

        ViewHolder(ItemTimelineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TimelineEvent event) {
            if (event.eventDate != null) {
                binding.timelineDate.setText(MONTH_DAY.format(event.eventDate));
                binding.timelineYear.setText(YEAR.format(event.eventDate));
            } else {
                binding.timelineDate.setText("—");
                binding.timelineYear.setText("");
            }
            binding.timelineTitle.setText(event.title);
            binding.timelineDescription.setText(event.description);
            binding.timelineDescription.setVisibility(
                    event.description != null && !event.description.isEmpty()
                            ? View.VISIBLE : View.GONE);
        }
    }
}
