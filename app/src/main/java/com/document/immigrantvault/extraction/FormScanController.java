package com.document.immigrantvault.extraction;

import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.util.DocumentScanHelper;
import com.google.android.material.snackbar.Snackbar;

/**
 * Scan → on-device OCR → callback. Does not persist the image anywhere in the vault.
 * Call {@link #register()} from the host fragment's {@code onCreate}.
 */
public final class FormScanController {

    public interface OcrCallback {
        void onOcrResult(@NonNull OcrText text);
    }

    private final Fragment host;
    private final ImmigrantVaultApplication app;

    private ActivityResultLauncher<IntentSenderRequest> scanLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;

    @Nullable
    private View progressView;
    @Nullable
    private View scanButton;
    @Nullable
    private View anchorView;
    @Nullable
    private OcrCallback pendingCallback;
    private boolean busy;

    public FormScanController(@NonNull Fragment host, @NonNull ImmigrantVaultApplication app) {
        this.host = host;
        this.app = app;
    }

    public void register() {
        scanLauncher = host.registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> {
                    if (result.getResultCode() != android.app.Activity.RESULT_OK) {
                        setBusy(false);
                        return;
                    }
                    DocumentScanHelper.handleResult(result, (uri, mimeType, pageCount) ->
                            processImage(uri));
                });

        pickImageLauncher = host.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        processImage(uri);
                    } else {
                        setBusy(false);
                    }
                });
    }

    /**
     * @param progressView optional progress indicator shown while OCR runs
     * @param scanButton   optional Scan control disabled while busy
     * @param anchorView   view used for Snackbars (form root)
     */
    public void startScan(@Nullable View progressView,
                          @Nullable View scanButton,
                          @Nullable View anchorView,
                          @NonNull OcrCallback callback) {
        if (busy || !host.isAdded()) {
            return;
        }
        this.progressView = progressView;
        this.scanButton = scanButton;
        this.anchorView = anchorView;
        this.pendingCallback = callback;
        setBusy(true);

        DocumentScanHelper.start(
                host.requireActivity(),
                scanLauncher,
                DocumentScanHelper.Mode.FORM,
                () -> pickImageLauncher.launch(
                        new PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                .build()));
    }

    private void processImage(@NonNull Uri uri) {
        OcrCallback callback = pendingCallback;
        if (callback == null || !host.isAdded()) {
            setBusy(false);
            return;
        }
        setBusy(true);
        app.getExecutor().execute(() -> {
            try {
                OcrText text = TextRecognizerHelper.recognize(host.requireContext(), uri);
                host.requireActivity().runOnUiThread(() -> {
                    setBusy(false);
                    if (!host.isAdded()) {
                        return;
                    }
                    if (text.isEmpty()) {
                        showFailed();
                        return;
                    }
                    callback.onOcrResult(text);
                });
            } catch (Exception e) {
                host.requireActivity().runOnUiThread(() -> {
                    setBusy(false);
                    if (host.isAdded()) {
                        showFailed();
                    }
                });
            }
        });
    }

    public void showFailed() {
        CharSequence message = host.getString(R.string.form_scan_failed);
        if (anchorView != null) {
            Snackbar.make(anchorView, message, Snackbar.LENGTH_LONG).show();
        } else if (host.getContext() != null) {
            Toast.makeText(host.getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    public void showFilled() {
        CharSequence message = host.getString(R.string.form_scan_filled);
        if (anchorView != null) {
            Snackbar.make(anchorView, message, Snackbar.LENGTH_SHORT).show();
        } else if (host.getContext() != null) {
            Toast.makeText(host.getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void setBusy(boolean value) {
        busy = value;
        if (progressView != null) {
            progressView.setVisibility(value ? View.VISIBLE : View.GONE);
            if (value) {
                progressView.setContentDescription(host.getString(R.string.form_scanning));
            }
        }
        if (scanButton != null) {
            scanButton.setEnabled(!value);
        }
    }
}
