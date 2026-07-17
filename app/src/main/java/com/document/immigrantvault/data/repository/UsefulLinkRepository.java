package com.document.immigrantvault.data.repository;

import androidx.lifecycle.LiveData;

import com.document.immigrantvault.data.db.AppDatabase;
import com.document.immigrantvault.data.db.entity.UsefulLink;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class UsefulLinkRepository {

    private final AppDatabase database;
    private final ExecutorService executor;

    public UsefulLinkRepository(AppDatabase database, ExecutorService executor) {
        this.database = database;
        this.executor = executor;
    }

    public LiveData<List<UsefulLink>> getByPerson(long personId) {
        return database.usefulLinkDao().getByPerson(personId);
    }

    public void insert(UsefulLink link, Runnable onComplete) {
        executor.execute(() -> {
            long id = database.usefulLinkDao().insert(link);
            link.id = id;
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void update(UsefulLink link, Runnable onComplete) {
        executor.execute(() -> {
            database.usefulLinkDao().update(link);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void delete(UsefulLink link, Runnable onComplete) {
        executor.execute(() -> {
            database.usefulLinkDao().delete(link);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
}
