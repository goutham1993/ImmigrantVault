package com.document.immigrantvault.ui.common;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.document.immigrantvault.databinding.ItemListEntryBinding;

import java.util.ArrayList;
import java.util.List;

public class ListEntryAdapter extends RecyclerView.Adapter<ListEntryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public static class ListItem {
        public final String title;
        public final String subtitle;
        public final String meta;

        public ListItem(String title, String subtitle, String meta) {
            this.title = title;
            this.subtitle = subtitle;
            this.meta = meta;
        }
    }

    private final List<ListItem> items = new ArrayList<>();
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    public void setItems(List<ListItem> listItems) {
        items.clear();
        if (listItems != null) {
            items.addAll(listItems);
        }
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListEntryBinding binding = ItemListEntryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemListEntryBinding binding;

        ViewHolder(ItemListEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ListItem item, int position) {
            binding.itemTitle.setText(item.title);
            binding.itemSubtitle.setText(item.subtitle);
            binding.itemMeta.setText(item.meta);
            binding.itemSubtitle.setVisibility(
                    item.subtitle != null && !item.subtitle.isEmpty() ? View.VISIBLE : View.GONE);
            binding.itemMeta.setVisibility(
                    item.meta != null && !item.meta.isEmpty() ? View.VISIBLE : View.GONE);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(position);
                }
            });
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(position);
                    return true;
                }
                return false;
            });
        }
    }
}
