package com.document.immigrantvault.ui.files;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.data.db.entity.VaultFile;
import com.document.immigrantvault.databinding.FragmentFileViewerBinding;

import java.io.File;

/**
 * Full-screen viewer for image documents. Non-image types never reach here; the browser hands
 * those to an external app instead.
 */
public class FileViewerFragment extends Fragment {

    private static final String ARG_FILE_ID = "fileId";

    private FragmentFileViewerBinding binding;
    private String title;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFileViewerBinding.inflate(inflater, container, false);

        long fileId = requireArguments().getLong(ARG_FILE_ID);
        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();

        app.getVaultFileRepository().getById(fileId)
                .observe(getViewLifecycleOwner(), this::bind);

        return binding.getRoot();
    }

    private void bind(VaultFile file) {
        if (binding == null) {
            return;
        }
        if (file == null) {
            showError();
            return;
        }

        title = file.displayName;
        applyTitle();

        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        File source = app.getVaultFileStorage().resolve(file.personId, file.storedName);
        if (!source.exists()) {
            showError();
            return;
        }

        binding.viewerError.setVisibility(View.GONE);
        binding.viewerImage.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(source)
                .fitCenter()
                .into(binding.viewerImage);
    }

    private void showError() {
        binding.viewerImage.setVisibility(View.GONE);
        binding.viewerError.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        applyTitle();
    }

    private void applyTitle() {
        if (title == null || title.isEmpty() || !isAdded()) {
            return;
        }
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(title);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
