package com.document.immigrantvault.ui.files;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.document.immigrantvault.databinding.ItemVaultRowBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Icon + title + subtitle rows, shared by the person list and the folder list.
 */
public class VaultRowAdapter extends RecyclerView.Adapter<VaultRowAdapter.ViewHolder> {

    public interface OnRowClickListener {
        void onRowClick(int position);
    }

    public interface OnRowOverflowListener {
        void onRowOverflow(int position, View anchor);
    }

    public static class Row {
        public final String title;
        public final String subtitle;
        @DrawableRes
        public final int iconRes;
        public final boolean showOverflow;

        public Row(String title, String subtitle, @DrawableRes int iconRes, boolean showOverflow) {
            this.title = title;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
            this.showOverflow = showOverflow;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private OnRowClickListener clickListener;
    private OnRowOverflowListener overflowListener;

    public void setRows(List<Row> newRows) {
        rows.clear();
        if (newRows != null) {
            rows.addAll(newRows);
        }
        notifyDataSetChanged();
    }

    public void setOnRowClickListener(OnRowClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnRowOverflowListener(OnRowOverflowListener listener) {
        this.overflowListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVaultRowBinding binding = ItemVaultRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemVaultRowBinding binding;

        ViewHolder(ItemVaultRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Row row) {
            binding.rowTitle.setText(row.title);
            binding.rowSubtitle.setText(row.subtitle);
            binding.rowSubtitle.setVisibility(
                    row.subtitle != null && !row.subtitle.isEmpty() ? View.VISIBLE : View.GONE);
            binding.rowIcon.setImageResource(row.iconRes);
            binding.rowOverflow.setVisibility(row.showOverflow ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (clickListener != null && position != RecyclerView.NO_POSITION) {
                    clickListener.onRowClick(position);
                }
            });
            binding.rowOverflow.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (overflowListener != null && position != RecyclerView.NO_POSITION) {
                    overflowListener.onRowOverflow(position, v);
                }
            });
        }
    }
}
