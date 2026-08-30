package com.document.immigrantvault.ui.files;

import android.app.Activity;
import android.net.Uri;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.google.mlkit.vision.documentscanner.GmsDocumentScanner;
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning;
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult;

import java.util.List;

/**
 * Wraps the Play Services document scanner. A single scanned page is kept as a JPEG so it can
 * be previewed in the app; multi-page scans are stored as the PDF the scanner produces.
 */
final class DocumentScanLauncher {

    private static final int PAGE_LIMIT = 20;

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
        GmsDocumentScannerOptions options = new GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setPageLimit(PAGE_LIMIT)
                .setResultFormats(
                        GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                        GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build();

        GmsDocumentScanner scanner = GmsDocumentScanning.getClient(options);
        scanner.getStartScanIntent(activity)
                .addOnSuccessListener(intentSender -> launcher.launch(
                        new IntentSenderRequest.Builder(intentSender).build()))
                .addOnFailureListener(error -> onUnavailable.run());
    }

    static void handleResult(ActivityResult activityResult, ScanResultHandler handler) {
        if (activityResult.getResultCode() != Activity.RESULT_OK) {
            return;
        }
        GmsDocumentScanningResult result =
                GmsDocumentScanningResult.fromActivityResultIntent(activityResult.getData());
        if (result == null) {
            return;
        }

        List<GmsDocumentScanningResult.Page> pages = result.getPages();
        if (pages != null && pages.size() == 1) {
            handler.onScanned(pages.get(0).getImageUri(), "image/jpeg", 1);
            return;
        }

        GmsDocumentScanningResult.Pdf pdf = result.getPdf();
        if (pdf != null) {
            handler.onScanned(pdf.getUri(), "application/pdf", pdf.getPageCount());
            return;
        }

        if (pages != null && !pages.isEmpty()) {
            handler.onScanned(pages.get(0).getImageUri(), "image/jpeg", 1);
        }
    }
}
