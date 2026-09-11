package com.document.immigrantvault.ui.files;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.db.FolderTree;
import com.document.immigrantvault.data.db.dao.VaultFileDao;
import com.document.immigrantvault.data.db.entity.FileSource;
import com.document.immigrantvault.data.db.entity.VaultFile;
import com.document.immigrantvault.data.db.entity.VaultFolder;
import com.document.immigrantvault.data.repository.RepositoryCallback;
import com.document.immigrantvault.databinding.FragmentFileBrowserBinding;
import com.document.immigrantvault.databinding.ViewEmptyStateBinding;
import com.document.immigrantvault.util.VaultFileSharing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Files for one person. With no folder argument it lists that person's top-level
 * folders; inside a folder it shows nested folders and documents together.
 */
public class FileBrowserFragment extends Fragment {

    private static final String ARG_PERSON_ID = "personId";
    private static final String ARG_FOLDER_ID = "folderId";
    private static final String ARG_TITLE = "title";

    private FragmentFileBrowserBinding binding;
    private ImmigrantVaultApplication app;
    private long personId;
    private long folderId = -1L;
    private String title;

    private VaultRowAdapter folderAdapter;
    private VaultFileAdapter fileAdapter;
    private List<VaultFolder> allFolders = new ArrayList<>();
    private List<VaultFolder> folders = new ArrayList<>();
    private List<VaultFile> folderFiles = new ArrayList<>();
    private String searchQuery = "";
    private final Map<Long, Integer> folderCounts = new HashMap<>();

    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;
    private ActivityResultLauncher<String[]> openDocumentLauncher;
    private ActivityResultLauncher<IntentSenderRequest> scanLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<VaultFileSharing.CreateNamedDocument.Request> createDocumentLauncher;
    private Uri pendingCameraUri;
    private long pendingDownloadFileId = -1L;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = requireArguments();
        personId = args.getLong(ARG_PERSON_ID);
        folderId = args.getLong(ARG_FOLDER_ID, -1L);
        title = args.getString(ARG_TITLE);
        if (savedInstanceState != null) {
            pendingDownloadFileId = savedInstanceState.getLong("pendingDownloadFileId", -1L);
        }
        registerLaunchers();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("pendingDownloadFileId", pendingDownloadFileId);
    }

    private void registerLaunchers() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        promptSave(uri, null, 1, FileSource.PHOTO);
                    }
                });

        openDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        promptSave(uri, null, 1, FileSource.IMPORT);
                    }
                });

        scanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> DocumentScanLauncher.handleResult(result, this::promptSaveScan));

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                saved -> {
                    if (Boolean.TRUE.equals(saved) && pendingCameraUri != null) {
                        promptSave(pendingCameraUri, "image/jpeg", 1, FileSource.SCAN);
                    }
                    pendingCameraUri = null;
                });

        createDocumentLauncher = registerForActivityResult(
                new VaultFileSharing.CreateNamedDocument(),
                uri -> {
                    long fileId = pendingDownloadFileId;
                    pendingDownloadFileId = -1L;
                    if (uri == null || fileId < 0 || !isAdded()) {
                        return;
                    }
                    ImmigrantVaultApplication application =
                            (ImmigrantVaultApplication) requireActivity().getApplication();
                    application.getVaultFileRepository().exportTo(fileId, uri, new RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            toast(R.string.files_downloaded);
                        }

                        @Override
                        public void onError(Exception error) {
                            toast(R.string.files_download_failed);
                        }
                    });
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFileBrowserBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        app = (ImmigrantVaultApplication) requireActivity().getApplication();
        if (isFolderMode()) {
            setUpFolderMode();
        } else {
            setUpFileMode();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        applyTitle();
    }

    private boolean isFolderMode() {
        return folderId < 0;
    }

    private Long currentParentId() {
        return isFolderMode() ? null : folderId;
    }

    // region Folder mode

    private void setUpFolderMode() {
        folderAdapter = new VaultRowAdapter();
        binding.browserRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.browserRecycler.setAdapter(folderAdapter);

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyIcon.setImageResource(R.drawable.ic_folder);
        empty.emptyTitle.setText(R.string.files_empty_folders);
        empty.emptySubtitle.setText(R.string.files_empty_folders_subtitle);

        bindFolderAdapter();

        binding.fabAdd.setOnClickListener(v -> FolderFormBottomSheet.newInstance(personId)
                .show(getParentFragmentManager(), "folder_form"));

        app.getVaultFolderRepository().ensureDefaultFolders(personId, defaultFolderNames());
        observeFoldersAndCounts();
    }

    private void bindFolderAdapter() {
        folderAdapter.setOnRowClickListener(position -> openFolder(folders.get(position)));
        folderAdapter.setOnRowOverflowListener(
                (position, anchor) -> showFolderMenu(folders.get(position), anchor));
    }

    private void observeFoldersAndCounts() {
        app.getVaultFolderRepository().getFileCounts(personId)
                .observe(getViewLifecycleOwner(), counts -> {
                    folderCounts.clear();
                    if (counts != null) {
                        for (VaultFileDao.FolderFileCount count : counts) {
                            folderCounts.put(count.folderId, count.fileCount);
                        }
                    }
                    renderFolders();
                });

        app.getVaultFolderRepository().getByPerson(personId)
                .observe(getViewLifecycleOwner(), list -> {
                    allFolders = list != null ? list : new ArrayList<>();
                    renderFolders();
                    if (!isFolderMode()) {
                        renderFiles();
                    }
                });
    }

    private void renderFolders() {
        if (binding == null || folderAdapter == null) {
            return;
        }
        folders = filterFolders(FolderTree.childrenOf(allFolders, currentParentId()));
        List<VaultRowAdapter.Row> rows = new ArrayList<>();
        for (VaultFolder folder : folders) {
            int count = FolderTree.inclusiveFileCount(folder.id, allFolders, folderCounts);
            rows.add(new VaultRowAdapter.Row(
                    folder.name,
                    FileFormat.fileCountLabel(requireContext(), count),
                    R.drawable.ic_folder,
                    true));
        }
        folderAdapter.setRows(rows);
        if (isFolderMode()) {
            toggleEmptyState(folders.isEmpty());
        }
    }

    private List<VaultFolder> filterFolders(List<VaultFolder> source) {
        if (isFolderMode() || searchQuery.isEmpty()) {
            return source;
        }
        String needle = searchQuery.toLowerCase(Locale.getDefault());
        List<VaultFolder> matches = new ArrayList<>();
        for (VaultFolder folder : source) {
            if (folder.name != null
                    && folder.name.toLowerCase(Locale.getDefault()).contains(needle)) {
                matches.add(folder);
            }
        }
        return matches;
    }

    private void openFolder(VaultFolder folder) {
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        args.putLong(ARG_FOLDER_ID, folder.id);
        args.putString(ARG_TITLE, folder.name);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_browser_to_browser, args);
    }

    private void showFolderMenu(VaultFolder folder, View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.inflate(R.menu.menu_folder_actions);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_rename_folder) {
                FolderFormBottomSheet.forRename(personId, folder.id, folder.name)
                        .show(getParentFragmentManager(), "folder_form");
                return true;
            }
            if (id == R.id.action_delete_folder) {
                confirmDeleteFolder(folder);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void confirmDeleteFolder(VaultFolder folder) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.files_delete_folder_title)
                .setMessage(R.string.files_delete_folder_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        app.getVaultFolderRepository().delete(folder, new RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                toast(R.string.files_deleted);
                            }

                            @Override
                            public void onError(Exception error) {
                                toast(R.string.files_save_failed);
                            }
                        }))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private String[] defaultFolderNames() {
        return new String[]{
                getString(R.string.files_default_folder_passport),
                getString(R.string.files_default_folder_visas),
                getString(R.string.files_default_folder_travel),
                getString(R.string.files_default_folder_education),
                getString(R.string.files_default_folder_employment),
                getString(R.string.files_default_folder_taxes),
                getString(R.string.files_default_folder_petitions),
                getString(R.string.files_default_folder_other)
        };
    }

    // endregion

    // region File mode

    private void setUpFileMode() {
        folderAdapter = new VaultRowAdapter();
        fileAdapter = new VaultFileAdapter(app.getVaultFileStorage());
        ConcatAdapter concatAdapter = new ConcatAdapter(folderAdapter, fileAdapter);
        GridLayoutManager grid = new GridLayoutManager(requireContext(), 2);
        grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position < folderAdapter.getItemCount() ? 2 : 1;
            }
        });
        binding.browserRecycler.setLayoutManager(grid);
        binding.browserRecycler.setAdapter(concatAdapter);

        ViewEmptyStateBinding empty = binding.emptyState;
        empty.emptyIcon.setImageResource(R.drawable.ic_file);
        empty.emptyTitle.setText(R.string.files_empty_folder);
        empty.emptySubtitle.setText(R.string.files_empty_folder_subtitle);

        bindFolderAdapter();
        fileAdapter.setOnFileClickListener(this::openFile);
        fileAdapter.setOnFileOverflowListener(this::showFileMenu);

        binding.fabAdd.setOnClickListener(v -> AddFileBottomSheet
                .newInstance()
                .show(getParentFragmentManager(), AddFileBottomSheet.TAG));

        setUpSearch();

        AddFileBottomSheet.setResultListener(this, choice -> {
            switch (choice) {
                case FOLDER:
                    FolderFormBottomSheet.newInstance(personId, folderId)
                            .show(getParentFragmentManager(), "folder_form");
                    break;
                case SCAN:
                    startScan();
                    break;
                case PHOTO:
                    pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build());
                    break;
                case UPLOAD:
                default:
                    openDocumentLauncher.launch(new String[]{"application/pdf", "image/*"});
                    break;
            }
        });

        observeFoldersAndCounts();

        app.getVaultFileRepository().getByFolder(folderId)
                .observe(getViewLifecycleOwner(), files -> {
                    folderFiles = files != null ? files : new ArrayList<>();
                    renderFiles();
                });
    }

    private void setUpSearch() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                searchQuery = editable.toString().trim();
                renderFolders();
                renderFiles();
            }
        });
        binding.searchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void renderFiles() {
        if (binding == null || fileAdapter == null) {
            return;
        }
        List<VaultFile> visible = filterFiles();
        fileAdapter.setFiles(visible);

        boolean hasChildren = !FolderTree.childrenOf(allFolders, currentParentId()).isEmpty();
        boolean searchable = !folderFiles.isEmpty() || hasChildren || !searchQuery.isEmpty();
        binding.searchLayout.setVisibility(searchable ? View.VISIBLE : View.GONE);

        ViewEmptyStateBinding empty = binding.emptyState;
        boolean nothingVisible = visible.isEmpty() && folders.isEmpty();
        if (nothingVisible && !searchQuery.isEmpty()) {
            empty.emptyIcon.setImageResource(R.drawable.ic_search);
            empty.emptyTitle.setText(R.string.files_search_no_results);
            empty.emptySubtitle.setText(getString(R.string.files_search_no_results_subtitle, searchQuery));
        } else {
            empty.emptyIcon.setImageResource(R.drawable.ic_file);
            empty.emptyTitle.setText(R.string.files_empty_folder);
            empty.emptySubtitle.setText(R.string.files_empty_folder_subtitle);
        }
        toggleEmptyState(nothingVisible);
    }

    private List<VaultFile> filterFiles() {
        if (searchQuery.isEmpty()) {
            return folderFiles;
        }
        String needle = searchQuery.toLowerCase(Locale.getDefault());
        List<VaultFile> matches = new ArrayList<>();
        for (VaultFile file : folderFiles) {
            if (file.displayName != null
                    && file.displayName.toLowerCase(Locale.getDefault()).contains(needle)) {
                matches.add(file);
            }
        }
        return matches;
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(binding.searchInput.getWindowToken(), 0);
        }
    }

    private void startScan() {
        DocumentScanLauncher.start(requireActivity(), scanLauncher, () -> {
            try {
                java.io.File temp = app.getVaultFileStorage().createCameraTempFile();
                pendingCameraUri = VaultFileSharing.uriFor(requireContext(), temp);
                takePictureLauncher.launch(pendingCameraUri);
            } catch (Exception e) {
                toast(R.string.files_scan_unavailable);
            }
        });
    }

    private void promptSaveScan(Uri uri, String mimeType, int pageCount) {
        promptSave(uri, mimeType, pageCount, FileSource.SCAN);
    }

    private void promptSave(Uri uri, String mimeType, int pageCount, FileSource source) {
        FileSaveBottomSheet.newInstance(personId, folderId, uri, mimeType, pageCount, source)
                .show(getParentFragmentManager(), "file_save");
    }

    private void openFile(VaultFile file) {
        if (FileFormat.isImage(file.mimeType)) {
            Bundle args = new Bundle();
            args.putLong("fileId", file.id);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_browser_to_viewer, args);
            return;
        }
        if (!VaultFileSharing.view(requireContext(), app.getVaultFileStorage(), file)) {
            toast(R.string.files_no_viewer);
        }
    }

    private void showFileMenu(VaultFile file, View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.inflate(R.menu.menu_file_actions);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_rename_file) {
                FileRenameBottomSheet.newInstance(file.id, file.displayName)
                        .show(getParentFragmentManager(), "file_rename");
                return true;
            }
            if (id == R.id.action_copy_file) {
                showCopyDialog(file);
                return true;
            }
            if (id == R.id.action_move_file) {
                showMoveDialog(file);
                return true;
            }
            if (id == R.id.action_download_file) {
                startDownload(file);
                return true;
            }
            if (id == R.id.action_share_file) {
                if (!VaultFileSharing.share(requireContext(), app.getVaultFileStorage(), file)) {
                    toast(R.string.files_no_viewer);
                }
                return true;
            }
            if (id == R.id.action_delete_file) {
                confirmDeleteFile(file);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showMoveDialog(VaultFile file) {
        pickFolder(R.string.files_move_file, file.folderId, target ->
                app.getVaultFileRepository().move(file.id, target.id, new RepositoryCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        toast(R.string.files_moved);
                    }

                    @Override
                    public void onError(Exception error) {
                        toast(R.string.files_save_failed);
                    }
                }));
    }

    private void showCopyDialog(VaultFile file) {
        pickFolder(R.string.files_copy_file, -1L, target -> {
            String displayName = file.displayName;
            if (target.id == file.folderId) {
                displayName = getString(R.string.files_copy_name,
                        file.displayName != null ? file.displayName : "");
            }
            app.getVaultFileRepository().copyToFolder(file.id, target.id, displayName,
                    new RepositoryCallback<Long>() {
                        @Override
                        public void onSuccess(Long result) {
                            toast(R.string.files_copied);
                        }

                        @Override
                        public void onError(Exception error) {
                            toast(R.string.files_save_failed);
                        }
                    });
        });
    }

    private void startDownload(VaultFile file) {
        if (!app.getVaultFileStorage().exists(file.personId, file.storedName)) {
            toast(R.string.files_download_failed);
            return;
        }
        pendingDownloadFileId = file.id;
        createDocumentLauncher.launch(new VaultFileSharing.CreateNamedDocument.Request(
                file.mimeType,
                FileFormat.downloadFileName(file)));
    }

    private interface FolderPickListener {
        void onFolderPicked(VaultFolder folder);
    }

    private void pickFolder(int titleRes, long excludeFolderId, FolderPickListener listener) {
        app.getExecutor().execute(() -> {
            List<VaultFolder> all = new ArrayList<>(
                    app.getDatabase().vaultFolderDao().getByPersonSync(personId));
            List<VaultFolder> targets = new ArrayList<>(all);
            if (excludeFolderId >= 0) {
                targets.removeIf(folder -> folder.id == excludeFolderId);
            }
            targets.sort((left, right) -> FolderTree.path(left, all)
                    .compareToIgnoreCase(FolderTree.path(right, all)));
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) {
                    return;
                }
                if (targets.isEmpty()) {
                    toast(R.string.files_empty_folders);
                    return;
                }
                String[] names = new String[targets.size()];
                for (int i = 0; i < targets.size(); i++) {
                    names[i] = FolderTree.path(targets.get(i), all);
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle(titleRes)
                        .setItems(names, (dialog, which) -> listener.onFolderPicked(targets.get(which)))
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            });
        });
    }

    private void confirmDeleteFile(VaultFile file) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        app.getVaultFileRepository().delete(file, new RepositoryCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                toast(R.string.files_deleted);
                            }

                            @Override
                            public void onError(Exception error) {
                                toast(R.string.files_save_failed);
                            }
                        }))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // endregion

    private void toggleEmptyState(boolean isEmpty) {
        binding.browserRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.emptyState.getRoot().setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void applyTitle() {
        if (title == null || title.isEmpty()) {
            return;
        }
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(title);
        }
    }

    private void toast(int messageRes) {
        if (isAdded()) {
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        folderAdapter = null;
        fileAdapter = null;
    }
}
