package com.document.immigrantvault.ui.settings;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.backup.ExportFormat;
import com.document.immigrantvault.data.backup.ExportImportException;
import com.document.immigrantvault.data.repository.ExportImportRepository;
import com.document.immigrantvault.databinding.FragmentSettingsBinding;
import com.document.immigrantvault.util.LinkConstants;
import com.document.immigrantvault.util.SecurePrefs;
import com.document.immigrantvault.util.ThemePreferences;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private ExportImportRepository exportImportRepository;
    private ExportFormat pendingExportFormat;
    private Future<?> activeTask;
    private Future<byte[]> exportTask;

    private final ActivityResultLauncher<String> createDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument(), this::handleExportResult);

    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportSelection);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        SecurePrefs prefs = new SecurePrefs(requireContext());
        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        exportImportRepository = app.getExportImportRepository();

        setupThemeToggle();

        binding.switchBiometric.setChecked(prefs.isBiometricEnabled());
        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.setBiometricEnabled(isChecked));

        binding.linkI94.setOnClickListener(v ->
                UiUtils.openUrl(requireContext(), LinkConstants.I94_URL));
        binding.linkUscis.setOnClickListener(v ->
                UiUtils.openUrl(requireContext(), LinkConstants.USCIS_CASE_STATUS_URL));
        binding.linkProcessing.setOnClickListener(v ->
                UiUtils.openUrl(requireContext(), LinkConstants.USCIS_PROCESSING_TIMES_URL));

        binding.buttonExport.setOnClickListener(v -> showExportFormatDialog());
        binding.buttonImport.setOnClickListener(v -> startImport());

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        if (activeTask != null) {
            activeTask.cancel(true);
            activeTask = null;
        }
        binding = null;
        super.onDestroyView();
    }

    private void setupThemeToggle() {
        String currentMode = ThemePreferences.getThemeMode(requireContext());
        int checkedId = themeButtonId(currentMode);
        binding.themeToggle.check(checkedId);
        binding.themeToggle.addOnButtonCheckedListener(
                (MaterialButtonToggleGroup group, int checkedButtonId, boolean isChecked) -> {
                    if (!isChecked || binding == null) {
                        return;
                    }
                    String mode = themeModeForButton(checkedButtonId);
                    if (!mode.equals(ThemePreferences.getThemeMode(requireContext()))) {
                        ThemePreferences.setThemeMode(requireContext(), mode);
                    }
                });
    }

    private static int themeButtonId(String mode) {
        if (ThemePreferences.MODE_LIGHT.equals(mode)) {
            return R.id.theme_light;
        }
        if (ThemePreferences.MODE_DARK.equals(mode)) {
            return R.id.theme_dark;
        }
        return R.id.theme_system;
    }

    private static String themeModeForButton(int buttonId) {
        if (buttonId == R.id.theme_light) {
            return ThemePreferences.MODE_LIGHT;
        }
        if (buttonId == R.id.theme_dark) {
            return ThemePreferences.MODE_DARK;
        }
        return ThemePreferences.MODE_SYSTEM;
    }

    private void showExportFormatDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_export_format_title)
                .setItems(new CharSequence[]{
                        getString(R.string.settings_export_json),
                        getString(R.string.settings_export_csv)
                }, (dialog, which) -> {
                    pendingExportFormat = which == 0 ? ExportFormat.JSON : ExportFormat.CSV;
                    createDocumentLauncher.launch(pendingExportFormat.buildFileName());
                })
                .show();
    }

    private void startImport() {
        openDocumentLauncher.launch(new String[]{
                "application/json",
                "application/zip",
                "text/csv",
                "application/octet-stream"
        });
    }

    private void handleExportResult(Uri uri) {
        if (uri == null || pendingExportFormat == null || binding == null) {
            return;
        }
        setBackupBusy(true);
        exportTask = exportImportRepository.exportAsync(pendingExportFormat);
        activeTask = exportTask;
        pendingExportFormat = null;

        new Thread(() -> {
            try {
                byte[] data = exportTask.get();
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    try (java.io.OutputStream out =
                                 requireContext().getContentResolver().openOutputStream(uri)) {
                        if (out == null) {
                            throw new ExportImportException("Could not write to the selected file.");
                        }
                        out.write(data);
                        Toast.makeText(requireContext(), R.string.settings_export_success, Toast.LENGTH_SHORT)
                                .show();
                    } catch (Exception e) {
                        showError(R.string.settings_export_failed, e);
                    } finally {
                        setBackupBusy(false);
                    }
                });
            } catch (ExecutionException e) {
                requireActivity().runOnUiThread(() -> {
                    showError(R.string.settings_export_failed, e.getCause());
                    setBackupBusy(false);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    showError(R.string.settings_export_failed, e);
                    setBackupBusy(false);
                });
            }
        }).start();
    }

    private void handleImportSelection(Uri uri) {
        if (uri == null || binding == null) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_import_confirm_title)
                .setMessage(R.string.settings_import_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.settings_import_confirm, (dialog, which) -> performImport(uri))
                .show();
    }

    private void performImport(Uri uri) {
        if (binding == null) {
            return;
        }
        setBackupBusy(true);
        new Thread(() -> {
            try {
                byte[] data = readAllBytes(uri);
                String mimeType = requireContext().getContentResolver().getType(uri);
                Future<Void> importTask = exportImportRepository.importAsync(data, mimeType);
                activeTask = importTask;
                importTask.get();
                requireActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        ((ImmigrantVaultApplication) requireActivity().getApplication())
                                .getPersonRepository().ensureSelfExists();
                        Toast.makeText(requireContext(), R.string.settings_import_success, Toast.LENGTH_SHORT)
                                .show();
                        setBackupBusy(false);
                    }
                });
            } catch (ExecutionException e) {
                requireActivity().runOnUiThread(() -> {
                    showError(R.string.settings_import_failed, e.getCause());
                    setBackupBusy(false);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    showError(R.string.settings_import_failed, e);
                    setBackupBusy(false);
                });
            }
        }).start();
    }

    private byte[] readAllBytes(Uri uri) throws Exception {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new ExportImportException("Could not read the selected file.");
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }

    private void setBackupBusy(boolean busy) {
        if (binding == null) {
            return;
        }
        binding.progressBackup.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.buttonExport.setEnabled(!busy);
        binding.buttonImport.setEnabled(!busy);
    }

    private void showError(int messageResId, Throwable error) {
        if (binding == null) {
            return;
        }
        String message = error instanceof ExportImportException
                ? error.getMessage()
                : getString(R.string.settings_import_failed, error != null ? error.getMessage() : "Unknown error");
        if (messageResId == R.string.settings_export_failed) {
            message = getString(R.string.settings_export_failed, error != null ? error.getMessage() : "Unknown error");
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }
}
