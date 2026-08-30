package com.document.immigrantvault.ui.files;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.document.immigrantvault.data.db.entity.VaultFile;
import com.document.immigrantvault.databinding.ItemVaultFileBinding;
import com.document.immigrantvault.util.VaultFileStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VaultFileAdapter extends RecyclerView.Adapter<VaultFileAdapter.ViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(VaultFile file);
    }

    public interface OnFileOverflowListener {
        void onFileOverflow(VaultFile file, View anchor);
    }

    private final List<VaultFile> files = new ArrayList<>();
    private final VaultFileStorage storage;
    private OnFileClickListener clickListener;
    private OnFileOverflowListener overflowListener;

    public VaultFileAdapter(VaultFileStorage storage) {
        this.storage = storage;
    }

    public void setFiles(List<VaultFile> newFiles) {
        files.clear();
        if (newFiles != null) {
            files.addAll(newFiles);
        }
        notifyDataSetChanged();
    }

    public void setOnFileClickListener(OnFileClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnFileOverflowListener(OnFileOverflowListener listener) {
        this.overflowListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVaultFileBinding binding = ItemVaultFileBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(files.get(position));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemVaultFileBinding binding;

        ViewHolder(ItemVaultFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(VaultFile file) {
            binding.fileName.setText(file.displayName);
            binding.fileMeta.setText(FileFormat.metaFor(itemView.getContext(), file));

            if (FileFormat.isImage(file.mimeType)) {
                File source = storage.resolve(file.personId, file.storedName);
                binding.fileThumbnail.setVisibility(View.VISIBLE);
                binding.fileTypeIcon.setVisibility(View.GONE);
                Glide.with(binding.fileThumbnail)
                        .load(source)
                        .centerCrop()
                        .into(binding.fileThumbnail);
            } else {
                Glide.with(binding.fileThumbnail).clear(binding.fileThumbnail);
                binding.fileThumbnail.setImageDrawable(null);
                binding.fileThumbnail.setVisibility(View.GONE);
                binding.fileTypeIcon.setVisibility(View.VISIBLE);
                binding.fileTypeIcon.setImageResource(FileFormat.iconFor(file.mimeType));
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onFileClick(file);
                }
            });
            binding.fileOverflow.setOnClickListener(v -> {
                if (overflowListener != null) {
                    overflowListener.onFileOverflow(file, v);
                }
            });
        }
    }
}
