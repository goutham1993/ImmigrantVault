package com.document.immigrantvault.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.document.immigrantvault.R;

public final class UiUtils {

    private UiUtils() {
    }

    public static void openUrl(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(intent);
    }

    public static void copyAndOpen(Context context, String text, String url, String toastMessage) {
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("copied", text));
        }
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
        openUrl(context, url);
    }

    public static void confirmDelete(Context context, Runnable onConfirm) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.action_delete, (d, w) -> onConfirm.run())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
