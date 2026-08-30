package com.document.immigrantvault.ui.files;

import android.content.Context;

import androidx.annotation.DrawableRes;

import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.VaultFile;

import java.util.Locale;

/**
 * Display helpers shared by the Files screens.
 */
public final class FileFormat {

    private FileFormat() {
    }

    public static String fileCountLabel(Context context, int count) {
        if (count <= 0) {
            return context.getString(R.string.files_no_files);
        }
        if (count == 1) {
            return context.getString(R.string.files_file_count_one);
        }
        return context.getString(R.string.files_file_count, count);
    }

    public static boolean isImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public static boolean isPdf(String mimeType) {
        return "application/pdf".equals(mimeType);
    }

    @DrawableRes
    public static int iconFor(String mimeType) {
        if (isPdf(mimeType)) {
            return R.drawable.ic_pdf;
        }
        if (isImage(mimeType)) {
            return R.drawable.ic_image;
        }
        return R.drawable.ic_file;
    }

    public static String metaFor(Context context, VaultFile file) {
        StringBuilder builder = new StringBuilder(typeLabel(file.mimeType));
        if (file.pageCount > 1) {
            builder.append(" · ").append(context.getString(R.string.files_pages, file.pageCount));
        }
        if (file.sizeBytes > 0) {
            builder.append(" · ").append(formatSize(file.sizeBytes));
        }
        return builder.toString();
    }

    public static String typeLabel(String mimeType) {
        if (isPdf(mimeType)) {
            return "PDF";
        }
        if (mimeType == null) {
            return "File";
        }
        int slash = mimeType.indexOf('/');
        if (slash >= 0 && slash < mimeType.length() - 1) {
            return mimeType.substring(slash + 1).toUpperCase(Locale.US);
        }
        return mimeType.toUpperCase(Locale.US);
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0);
        }
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
