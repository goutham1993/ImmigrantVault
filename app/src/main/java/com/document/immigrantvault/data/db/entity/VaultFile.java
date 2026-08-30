package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "vault_files",
        foreignKeys = @ForeignKey(
                entity = VaultFolder.class,
                parentColumns = "id",
                childColumns = "folderId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("folderId"), @Index("personId")}
)
public class VaultFile {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long folderId;
    public long personId;
    public String displayName;
    public String storedName;
    public String mimeType;
    public long sizeBytes;
    public int pageCount;
    public FileSource source;
    public Date createdAt;
    public Date updatedAt;

    public VaultFile() {
        this.source = FileSource.IMPORT;
        this.pageCount = 1;
        this.createdAt = new Date();
        this.updatedAt = this.createdAt;
    }
}
