package com.document.immigrantvault.extraction;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** On-device Latin text recognition. Call from a background thread. */
public final class TextRecognizerHelper {

    private static final long TIMEOUT_SECONDS = 30;

    private TextRecognizerHelper() {
    }

    @NonNull
    public static OcrText recognize(@NonNull Context context, @NonNull Uri imageUri)
            throws Exception {
        TextRecognizer recognizer =
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        try {
            InputImage image = InputImage.fromFilePath(context, imageUri);
            Text result = Tasks.await(recognizer.process(image), TIMEOUT_SECONDS, TimeUnit.SECONDS);
            List<String> lines = new ArrayList<>();
            for (Text.TextBlock block : result.getTextBlocks()) {
                for (Text.Line line : block.getLines()) {
                    String value = line.getText();
                    if (value != null && !value.trim().isEmpty()) {
                        lines.add(value.trim());
                    }
                }
            }
            String full = result.getText() != null ? result.getText().trim() : "";
            if (full.isEmpty() && !lines.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < lines.size(); i++) {
                    if (i > 0) {
                        sb.append('\n');
                    }
                    sb.append(lines.get(i));
                }
                full = sb.toString();
            }
            return new OcrText(full, lines);
        } finally {
            recognizer.close();
        }
    }
}
