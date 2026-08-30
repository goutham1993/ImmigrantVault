package com.document.immigrantvault.data.backup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public enum ExportFormat {
    JSON("application/json", "json"),
    CSV("application/zip", "zip");

    private final String mimeType;
    private final String extension;

    ExportFormat(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getExtension() {
        return extension;
    }

    public String buildFileName() {
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(new Date());
        return "immigrant_vault_backup_" + stamp + "." + extension;
    }
}
