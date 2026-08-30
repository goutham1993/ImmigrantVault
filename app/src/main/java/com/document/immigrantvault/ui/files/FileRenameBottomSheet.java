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
import com.document.immigrantvault.data.repository.RepositoryCallback;
import com.document.immigrantvault.databinding.BottomSheetFolderFormBinding;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class FileRenameBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_FILE_ID = "file_id";
    private static final String ARG_NAME = "name";

    private BottomSheetFolderFormBinding binding;
    private ImmigrantVaultApplication app;
    private long fileId;

    public static FileRenameBottomSheet newInstance(long fileId, String currentName) {
        FileRenameBottomSheet sheet = new FileRenameBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_FILE_ID, fileId);
        args.putString(ARG_NAME, currentName);
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
        fileId = requireArguments().getLong(ARG_FILE_ID);

        binding.formTitle.setText(R.string.files_rename_file);
        binding.inputNameLayout.setHint(getString(R.string.files_document_name));
        binding.inputName.setText(requireArguments().getString(ARG_NAME));
        binding.inputName.setSelectAllOnFocus(true);

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
        app.getVaultFileRepository().rename(fileId, name, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.files_renamed,
                            Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            }

            @Override
            public void onError(Exception error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.files_save_failed,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
