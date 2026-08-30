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

import java.util.Locale;

/**
 * Scan or upload → on-device OCR → callback. Does not persist the image anywhere in the vault.
 * Call {@link #register()} from the host fragment's {@code onCreate}.
 */
public final class FormScanController {

    private static final String[] UPLOAD_MIME_TYPES = {"image/*", "application/pdf"};

    public interface OcrCallback {
        void onOcrResult(@NonNull OcrText text);
    }

    private final Fragment host;
    private final ImmigrantVaultApplication app;

    private ActivityResultLauncher<IntentSenderRequest> scanLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;
    private ActivityResultLauncher<String[]> openDocumentLauncher;

    @Nullable
    private View progressView;
    @Nullable
    private View[] actionButtons;
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

        openDocumentLauncher = host.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        processImage(uri);
                    } else {
                        setBusy(false);
                    }
                });
    }

    /**
     * @param actionButtons Scan/Upload controls disabled while busy (may be null entries)
     */
    public void startScan(@Nullable View progressView,
                          @Nullable View anchorView,
                          @NonNull OcrCallback callback,
                          @Nullable View... actionButtons) {
        if (busy || !host.isAdded()) {
            return;
        }
        beginFill(progressView, anchorView, callback, actionButtons);
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

    /**
     * @param actionButtons Scan/Upload controls disabled while busy (may be null entries)
     */
    public void startUpload(@Nullable View progressView,
                            @Nullable View anchorView,
                            @NonNull OcrCallback callback,
                            @Nullable View... actionButtons) {
        if (busy || !host.isAdded()) {
            return;
        }
        beginFill(progressView, anchorView, callback, actionButtons);
        setBusy(true);
        openDocumentLauncher.launch(UPLOAD_MIME_TYPES);
    }

    private void beginFill(@Nullable View progressView,
                           @Nullable View anchorView,
                           @NonNull OcrCallback callback,
                           @Nullable View[] actionButtons) {
        this.progressView = progressView;
        this.anchorView = anchorView;
        this.pendingCallback = callback;
        this.actionButtons = actionButtons;
    }

    private void processImage(@NonNull Uri uri) {
        OcrCallback callback = pendingCallback;
        if (callback == null || !host.isAdded()) {
            setBusy(false);
            return;
        }
        if (!isSupportedForOcr(uri)) {
            setBusy(false);
            showUnsupportedFile();
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

    private boolean isSupportedForOcr(@NonNull Uri uri) {
        String type = host.requireContext().getContentResolver().getType(uri);
        if (type != null) {
            if ("application/pdf".equals(type)) {
                return false;
            }
            return type.startsWith("image/");
        }
        String segment = uri.getLastPathSegment();
        if (segment != null && segment.toLowerCase(Locale.US).endsWith(".pdf")) {
            return false;
        }
        return true;
    }

    public void showFailed() {
        CharSequence message = host.getString(R.string.form_scan_failed);
        if (anchorView != null) {
            Snackbar.make(anchorView, message, Snackbar.LENGTH_LONG).show();
        } else if (host.getContext() != null) {
            Toast.makeText(host.getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    public void showUnsupportedFile() {
        CharSequence message = host.getString(R.string.form_upload_pdf_unsupported);
        if (anchorView != null) {
            Snackbar.make(anchorView, message, Snackbar.LENGTH_LONG).show();
        } else if (host.getContext() != null) {
            Toast.makeText(host.getContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    public void showFilled() {
        CharSequence message = host.getString(R.string.form_fill_filled);
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
        if (actionButtons != null) {
            for (View button : actionButtons) {
                if (button != null) {
                    button.setEnabled(!value);
                }
            }
        }
    }
}
