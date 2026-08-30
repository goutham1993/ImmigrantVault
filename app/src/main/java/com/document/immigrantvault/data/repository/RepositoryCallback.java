package com.document.immigrantvault.data.repository;

/**
 * Result callback for repository work that can fail. Always invoked on the main thread.
 */
public interface RepositoryCallback<T> {

    void onSuccess(T result);

    default void onError(Exception error) {
    }
}
