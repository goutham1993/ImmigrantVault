package com.document.immigrantvault.ui.files;

import android.app.Activity;
import android.net.Uri;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.document.immigrantvault.util.DocumentScanHelper;

/**
 * Files-tab wrapper around {@link DocumentScanHelper} in multi-page mode.
 */
final class DocumentScanLauncher {

    interface ScanResultHandler {
        void onScanned(Uri uri, String mimeType, int pageCount);
    }

    private DocumentScanLauncher() {
    }

    /**
     * @param onUnavailable runs when Play Services cannot provide the scanner, so the caller
     *                      can fall back to a plain camera capture.
     */
    static void start(Activity activity,
                      ActivityResultLauncher<IntentSenderRequest> launcher,
                      Runnable onUnavailable) {
        DocumentScanHelper.start(activity, launcher, DocumentScanHelper.Mode.FILES, onUnavailable);
    }

    static void handleResult(ActivityResult activityResult, ScanResultHandler handler) {
        DocumentScanHelper.handleResult(activityResult, handler::onScanned);
    }
}
