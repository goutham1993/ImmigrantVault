package com.document.immigrantvault.data.cloud;

public final class DriveBackupItem {

    public final String id;
    public final String name;
    public final String modifiedTime;

    public DriveBackupItem(String id, String name, String modifiedTime) {
        this.id = id;
        this.name = name;
        this.modifiedTime = modifiedTime;
    }

    public String displayLabel() {
        if (modifiedTime != null && modifiedTime.length() >= 10) {
            return name + "\n" + modifiedTime.substring(0, 10);
        }
        return name != null ? name : id;
    }
}
