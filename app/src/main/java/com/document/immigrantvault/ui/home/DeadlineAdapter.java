package com.document.immigrantvault.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.document.immigrantvault.data.db.entity.Reminder;
import com.document.immigrantvault.databinding.ItemDeadlineChipBinding;
import com.document.immigrantvault.util.DateUtils;
import com.document.immigrantvault.util.StatusHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DeadlineAdapter extends RecyclerView.Adapter<DeadlineAdapter.ViewHolder> {

    private final List<Reminder> items = new ArrayList<>();

    public void setItems(List<Reminder> reminders) {
        items.clear();
        if (reminders != null) {
            items.addAll(reminders);
            Collections.sort(items, Comparator.comparing(r -> r.triggerDate));
        }
        notifyDataSetChanged();
    }

    public List<Reminder> getTop(int count) {
        if (items.size() <= count) {
            return new ArrayList<>(items);
        }
        return new ArrayList<>(items.subList(0, count));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDeadlineChipBinding binding = ItemDeadlineChipBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return Math.min(items.size(), 5);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDeadlineChipBinding binding;

        ViewHolder(ItemDeadlineChipBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Reminder reminder) {
            binding.deadlineTitle.setText(reminder.title);
            binding.deadlineBody.setText(reminder.body);
            int color = StatusHelper.deadlineColorRes(reminder.triggerDate);
            binding.deadlineTitle.setTextColor(itemView.getContext().getColor(color));
        }
    }
}
