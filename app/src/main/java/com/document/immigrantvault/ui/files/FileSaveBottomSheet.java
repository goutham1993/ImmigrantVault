package com.document.immigrantvault.ui.files;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.entity.FileSource;
import com.document.immigrantvault.data.db.entity.VaultFolder;
import com.document.immigrantvault.data.repository.RepositoryCallback;
import com.document.immigrantvault.databinding.BottomSheetFileSaveBinding;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Names a freshly captured or picked document and confirms which folder it lands in.
 */
public class FileSaveBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_FOLDER_ID = "folder_id";
    private static final String ARG_URI = "uri";
    private static final String ARG_MIME = "mime";
    private static final String ARG_PAGES = "pages";
    private static final String ARG_SOURCE = "source";

    private BottomSheetFileSaveBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private long initialFolderId;
    private Uri sourceUri;
    private String mimeType;
    private int pageCount;
    private FileSource source;
    private List<VaultFolder> folders = new ArrayList<>();
    private int selectedFolderIndex = 0;
    private boolean saving;

    public static FileSaveBottomSheet newInstance(long personId, long folderId, Uri uri,
                                                  String mimeType, int pageCount,
                                                  FileSource source) {
        FileSaveBottomSheet sheet = new FileSaveBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        args.putLong(ARG_FOLDER_ID, folderId);
        args.putParcelable(ARG_URI, uri);
        args.putString(ARG_MIME, mimeType);
        args.putInt(ARG_PAGES, pageCount);
        args.putString(ARG_SOURCE, source.name());
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetFileSaveBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        UiUtils.autoCapitalizeInputs(view);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();

        Bundle args = requireArguments();
        personId = args.getLong(ARG_PERSON_ID);
        initialFolderId = args.getLong(ARG_FOLDER_ID);
        sourceUri = args.getParcelable(ARG_URI);
        mimeType = args.getString(ARG_MIME);
        pageCount = Math.max(1, args.getInt(ARG_PAGES, 1));
        source = FileSource.valueOf(args.getString(ARG_SOURCE, FileSource.IMPORT.name()));

        if (mimeType == null) {
            mimeType = app.getVaultFileStorage().resolveMimeType(sourceUri);
        }

        binding.inputName.setText(suggestName());
        binding.inputName.setSelectAllOnFocus(true);
        binding.fileSummary.setText(summaryLine());

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSave.setOnClickListener(v -> save());

        loadFolders();
    }

    private void loadFolders() {
        app.getExecutor().execute(() -> {
            List<VaultFolder> loaded = app.getDatabase().vaultFolderDao().getByPersonSync(personId);
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (binding == null) {
                    return;
                }
                folders = loaded;
                String[] names = new String[folders.size()];
                for (int i = 0; i < folders.size(); i++) {
                    names[i] = folders.get(i).name;
                    if (folders.get(i).id == initialFolderId) {
                        selectedFolderIndex = i;
                    }
                }
                binding.inputFolder.setAdapter(new ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_dropdown_item_1line, names));
                if (names.length > 0) {
                    binding.inputFolder.setText(names[selectedFolderIndex], false);
                }
                binding.inputFolder.setOnItemClickListener(
                        (parent, itemView, position, id) -> selectedFolderIndex = position);
            });
        });
    }

    private String suggestName() {
        String pickedName = queryDisplayName();
        if (pickedName != null && !pickedName.isEmpty()) {
            int dot = pickedName.lastIndexOf('.');
            return dot > 0 ? pickedName.substring(0, dot) : pickedName;
        }
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String prefix = source == FileSource.SCAN ? "Scan" : "Photo";
        return prefix + " " + today;
    }

    private String queryDisplayName() {
        if (!"content".equals(sourceUri.getScheme())) {
            return null;
        }
        try (Cursor cursor = requireContext().getContentResolver()
                .query(sourceUri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
            // Some providers reject the projection; the date-based fallback is fine.
        }
        return null;
    }

    private String summaryLine() {
        StringBuilder builder = new StringBuilder(FileFormat.typeLabel(mimeType));
        if (pageCount > 1) {
            builder.append(" · ").append(getString(R.string.files_pages, pageCount));
        }
        return builder.toString();
    }

    private void save() {
        if (saving) {
            return;
        }
        String name = binding.inputName.getText() != null
                ? binding.inputName.getText().toString().trim()
                : "";
        if (name.isEmpty()) {
            binding.inputNameLayout.setError(getString(R.string.error_required));
            return;
        }
        if (folders.isEmpty()) {
            Toast.makeText(requireContext(), R.string.files_empty_folders, Toast.LENGTH_SHORT).show();
            return;
        }
        binding.inputNameLayout.setError(null);
        saving = true;
        binding.btnSave.setEnabled(false);

        long targetFolderId = folders.get(selectedFolderIndex).id;
        app.getVaultFileRepository().importFile(sourceUri, personId, targetFolderId, name,
                mimeType, pageCount, source, new RepositoryCallback<Long>() {
                    @Override
                    public void onSuccess(Long result) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.files_saved,
                                    Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        saving = false;
                        if (binding != null) {
                            binding.btnSave.setEnabled(true);
                        }
                        if (isAdded()) {
                            Toast.makeText(requireContext(), R.string.files_save_failed,
                                    Toast.LENGTH_LONG).show();
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
