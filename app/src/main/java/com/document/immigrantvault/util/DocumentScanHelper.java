package com.document.immigrantvault.util;

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
 * Shared Play Services document scanner. Files mode allows multi-page PDF; form mode forces a
 * single JPEG page so OCR can run without persisting anything.
 */
public final class DocumentScanHelper {

    public enum Mode {
        /** Multi-page JPEG/PDF for the Files vault. */
        FILES,
        /** Single JPEG page for scan-to-fill forms (never stored). */
        FORM
    }

    public interface ScanResultHandler {
        void onScanned(Uri uri, String mimeType, int pageCount);
    }

    private static final int FILES_PAGE_LIMIT = 20;

    private DocumentScanHelper() {
    }

    /**
     * @param onUnavailable runs when Play Services cannot provide the scanner
     */
    public static void start(Activity activity,
                             ActivityResultLauncher<IntentSenderRequest> launcher,
                             Mode mode,
                             Runnable onUnavailable) {
        GmsDocumentScannerOptions.Builder builder = new GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL);

        if (mode == Mode.FORM) {
            builder.setPageLimit(1)
                    .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG);
        } else {
            builder.setPageLimit(FILES_PAGE_LIMIT)
                    .setResultFormats(
                            GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                            GmsDocumentScannerOptions.RESULT_FORMAT_PDF);
        }

        GmsDocumentScanner scanner = GmsDocumentScanning.getClient(builder.build());
        scanner.getStartScanIntent(activity)
                .addOnSuccessListener(intentSender -> launcher.launch(
                        new IntentSenderRequest.Builder(intentSender).build()))
                .addOnFailureListener(error -> onUnavailable.run());
    }

    public static void handleResult(ActivityResult activityResult, ScanResultHandler handler) {
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
