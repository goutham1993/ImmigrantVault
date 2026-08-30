package com.document.immigrantvault.data.db.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(
        tableName = "vault_folders",
        foreignKeys = @ForeignKey(
                entity = Person.class,
                parentColumns = "id",
                childColumns = "personId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("personId")}
)
public class VaultFolder {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long personId;
    public String name;
    public int sortOrder;
    public boolean isSystem;
    public Date createdAt;

    public VaultFolder() {
        this.createdAt = new Date();
    }
}
