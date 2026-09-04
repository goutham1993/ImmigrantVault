package com.document.immigrantvault.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.document.immigrantvault.R;

import java.util.Arrays;

public final class UiUtils {

    private static final InputFilter CAPITALIZE_FIRST_LETTER =
            (source, start, end, dest, dstart, dend) -> {
                if (dstart != 0 || start >= end) return null;
                char first = source.charAt(start);
                char upper = Character.toUpperCase(first);
                if (upper == first) return null;
                SpannableStringBuilder replacement = new SpannableStringBuilder(source, start, end);
                replacement.replace(0, 1, String.valueOf(upper));
                return replacement;
            };

    private UiUtils() {
    }

    /**
     * Makes every free-text field under {@code root} start with a capital letter, both as a
     * keyboard hint and as an enforced rule for text typed or pasted at the start of the field.
     * Numeric, password, email, URL and all-caps fields are left untouched.
     */
    public static void autoCapitalizeInputs(View root) {
        if (root instanceof EditText) {
            autoCapitalizeField((EditText) root);
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                autoCapitalizeInputs(group.getChildAt(i));
            }
        }
    }

    private static void autoCapitalizeField(EditText field) {
        if (field instanceof AutoCompleteTextView || !field.isFocusable()) return;

        int inputType = field.getInputType();
        if ((inputType & InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) return;
        if ((inputType & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0
                || (inputType & InputType.TYPE_TEXT_FLAG_CAP_WORDS) != 0) return;

        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        if (variation == InputType.TYPE_TEXT_VARIATION_URI
                || variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
                || variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) return;

        field.setRawInputType(inputType | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        InputFilter[] filters = field.getFilters();
        if (filters == null || filters.length == 0) {
            field.setFilters(new InputFilter[]{CAPITALIZE_FIRST_LETTER});
            return;
        }
        for (InputFilter filter : filters) {
            if (filter == CAPITALIZE_FIRST_LETTER) return;
        }
        InputFilter[] updated = Arrays.copyOf(filters, filters.length + 1);
        updated[filters.length] = CAPITALIZE_FIRST_LETTER;
        field.setFilters(updated);
    }

    public static void openUrl(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(intent);
    }

    /**
     * Copies {@code text} to the clipboard and shows a short toast.
     * No-ops (no toast) when {@code text} is null or blank.
     *
     * @return true if text was copied
     */
    public static boolean copyText(Context context, String text, String toastMessage) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        android.content.ClipboardManager clipboard =
                (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("copied", text));
        }
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * Long-press on {@code view} copies {@code value} (raw, not display text).
     * Clears the listener when {@code value} is blank.
     */
    public static void bindCopyOnLongPress(View view, String value, String toastMessage) {
        if (value == null || value.trim().isEmpty()) {
            view.setOnLongClickListener(null);
            return;
        }
        view.setOnLongClickListener(v -> {
            copyText(v.getContext(), value, toastMessage);
            return true;
        });
    }

    public static void copyAndOpen(Context context, String text, String url, String toastMessage) {
        copyText(context, text, toastMessage);
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
