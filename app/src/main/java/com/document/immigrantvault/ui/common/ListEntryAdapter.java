package com.document.immigrantvault.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
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
        public final CharSequence meta;
        public final String badge;
        @ColorRes
        public final int badgeTextColorRes;
        @ColorRes
        public final int badgeBackgroundColorRes;
        /** Raw identifier to copy on long-press; null when not copyable. */
        public final String copyText;

        public ListItem(String title, String subtitle, CharSequence meta) {
            this(title, subtitle, meta, null, 0, 0, null);
        }

        public ListItem(String title, String subtitle, CharSequence meta, String copyText) {
            this(title, subtitle, meta, null, 0, 0, copyText);
        }

        public ListItem(String title, String subtitle, CharSequence meta, String badge,
                        @ColorRes int badgeTextColorRes, @ColorRes int badgeBackgroundColorRes) {
            this(title, subtitle, meta, badge, badgeTextColorRes, badgeBackgroundColorRes, null);
        }

        public ListItem(String title, String subtitle, CharSequence meta, String badge,
                        @ColorRes int badgeTextColorRes, @ColorRes int badgeBackgroundColorRes,
                        String copyText) {
            this.title = title;
            this.subtitle = subtitle;
            this.meta = meta;
            this.badge = badge;
            this.badgeTextColorRes = badgeTextColorRes;
            this.badgeBackgroundColorRes = badgeBackgroundColorRes;
            this.copyText = copyText;
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

    public ListItem getItem(int position) {
        return items.get(position);
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
                    item.meta != null && item.meta.length() > 0 ? View.VISIBLE : View.GONE);
            bindBadge(item);
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

        private void bindBadge(ListItem item) {
            if (item.badge == null || item.badge.isEmpty()) {
                binding.itemBadge.setVisibility(View.GONE);
                return;
            }
            Context context = itemView.getContext();
            binding.itemBadge.setVisibility(View.VISIBLE);
            binding.itemBadge.setText(item.badge);
            if (item.badgeTextColorRes != 0) {
                binding.itemBadge.setTextColor(context.getColor(item.badgeTextColorRes));
            }
            if (item.badgeBackgroundColorRes != 0) {
                binding.itemBadge.setBackgroundTintList(ColorStateList.valueOf(
                        context.getColor(item.badgeBackgroundColorRes)));
            }
        }
    }
}
