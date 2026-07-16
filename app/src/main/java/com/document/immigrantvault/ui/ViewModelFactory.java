package com.document.immigrantvault.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.document.immigrantvault.ImmigrantVaultApplication;

public class ViewModelFactory implements ViewModelProvider.Factory {

    private final ImmigrantVaultApplication application;

    public ViewModelFactory(ImmigrantVaultApplication application) {
        this.application = application;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            return modelClass.getConstructor(ImmigrantVaultApplication.class).newInstance(application);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create ViewModel: " + modelClass.getSimpleName(), e);
        }
    }
}
