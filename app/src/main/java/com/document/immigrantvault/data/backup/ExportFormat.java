package com.document.immigrantvault.data.backup;

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
        return "immigrant_vault_backup." + extension;
    }
}
