package com.document.immigrantvault.data.backup;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A backup plus the raw bytes of any saved documents. Only the CSV/ZIP format carries the
 * binaries; JSON exports leave {@link #files} empty and keep metadata only.
 */
public class BackupPayload {

    /** Keys are {@code personId/storedName}, matching the layout inside the ZIP. */
    public final Map<String, byte[]> files = new LinkedHashMap<>();

    public VaultBackup backup;

    public BackupPayload(VaultBackup backup) {
        this.backup = backup;
    }

    public static String key(long personId, String storedName) {
        return personId + "/" + storedName;
    }
}
