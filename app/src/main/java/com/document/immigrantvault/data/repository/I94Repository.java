package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.I94Entry;

import java.util.concurrent.ExecutorService;

public class I94Repository {

    private final AppDatabase database;
    private final ExecutorService executor;

    public I94Repository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<I94Entry> getByPerson(long personId) {
        return database.i94Dao().getByPerson(personId);
    }

    public void save(I94Entry entry, Runnable onComplete) {
        executor.execute(() -> {
            I94Entry existing = database.i94Dao().getByPersonSync(entry.personId);
            if (existing == null) {
                long id = database.i94Dao().insert(entry);
                entry.id = id;
            } else {
                entry.id = existing.id;
                database.i94Dao().update(entry);
            }
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
}
