package com.document.immigrantvault.ui.files;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.VaultFolder;
import com.document.immigrantvault.data.repository.RepositoryCallback;
import com.document.immigrantvault.databinding.BottomSheetFolderFormBinding;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FolderFormBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_FOLDER_ID = "folder_id";
    private static final String ARG_FOLDER_NAME = "folder_name";

    private BottomSheetFolderFormBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private Long folderId;

    public static FolderFormBottomSheet newInstance(long personId) {
        FolderFormBottomSheet sheet = new FolderFormBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        sheet.setArguments(args);
        return sheet;
    }

    public static FolderFormBottomSheet forRename(long personId, long folderId, String name) {
        FolderFormBottomSheet sheet = new FolderFormBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        args.putLong(ARG_FOLDER_ID, folderId);
        args.putString(ARG_FOLDER_NAME, name);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetFolderFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UiUtils.autoCapitalizeInputs(view);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        personId = requireArguments().getLong(ARG_PERSON_ID);

        if (requireArguments().containsKey(ARG_FOLDER_ID)) {
            folderId = requireArguments().getLong(ARG_FOLDER_ID);
            binding.formTitle.setText(R.string.files_rename_folder);
            binding.inputName.setText(requireArguments().getString(ARG_FOLDER_NAME));
        } else {
            binding.formTitle.setText(R.string.files_add_folder);
        }

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String name = binding.inputName.getText() != null
                ? binding.inputName.getText().toString().trim()
                : "";
        if (name.isEmpty()) {
            binding.inputNameLayout.setError(getString(R.string.error_required));
            return;
        }
        binding.inputNameLayout.setError(null);

        if (folderId != null) {
            app.getVaultFolderRepository().rename(folderId, name, new RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    dismissSafely();
                }

                @Override
                public void onError(Exception error) {
                    showError();
                }
            });
            return;
        }

        VaultFolder folder = new VaultFolder();
        folder.personId = personId;
        folder.name = name;
        folder.sortOrder = Integer.MAX_VALUE;
        folder.isSystem = false;
        app.getVaultFolderRepository().insert(folder, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                dismissSafely();
            }

            @Override
            public void onError(Exception error) {
                showError();
            }
        });
    }

    private void showError() {
        if (isAdded()) {
            Toast.makeText(requireContext(), R.string.files_save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void dismissSafely() {
        if (isAdded()) {
            dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
