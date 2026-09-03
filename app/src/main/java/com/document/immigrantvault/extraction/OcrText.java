package com.document.immigrantvault.extraction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** Raw OCR output used by field parsers. */
public final class OcrText {

    @NonNull
    public final String fullText;

    @NonNull
    public final List<String> lines;

    public OcrText(@NonNull String fullText, @Nullable List<String> lines) {
        this.fullText = fullText;
        this.lines = lines != null ? lines : Collections.emptyList();
    }

    public boolean isEmpty() {
        return fullText.trim().isEmpty();
    }
}
