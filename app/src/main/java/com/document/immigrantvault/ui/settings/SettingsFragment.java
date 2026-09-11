package com.document.immigrantvault.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.backup.AutoBackupPolicy;
import com.document.immigrantvault.data.backup.ExportFormat;
import com.document.immigrantvault.data.backup.ExportImportException;
import com.document.immigrantvault.data.cloud.CloudBackupOperations;
import com.document.immigrantvault.data.cloud.DriveAuthHelper;
import com.document.immigrantvault.data.cloud.DriveBackupItem;
import com.document.immigrantvault.data.repository.ExportImportRepository;
import com.document.immigrantvault.databinding.FragmentSettingsBinding;
import com.document.immigrantvault.util.AppSigningFingerprint;
import com.document.immigrantvault.util.AutoBackupScheduler;
import com.document.immigrantvault.util.BackupFolderLocator;
import com.document.immigrantvault.util.BackupPreferences;
import com.document.immigrantvault.util.LinkConstants;
import com.document.immigrantvault.util.SecurePrefs;
import com.document.immigrantvault.util.ThemePreferences;
import com.document.immigrantvault.util.UiUtils;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SettingsFragment extends Fragment {

    private static final String STATE_LINKS_EXPANDED = "links_expanded";

    private FragmentSettingsBinding binding;
    private boolean linksExpanded;
    private ExportImportRepository exportImportRepository;
    private ExportFormat pendingExportFormat;
    private Future<?> activeTask;
    private Future<byte[]> exportTask;
    private boolean pendingEnableCloud;
    private boolean ignoreCloudSwitch;
    private boolean backupBusy;

    private final ActivityResultLauncher<String> createJsonLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),
                    this::handleExportResult);
    private final ActivityResultLauncher<String> createZipLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/zip"),
                    this::handleExportResult);

    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportSelection);

    private final ActivityResultLauncher<Uri> pickBackupFolderLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree() {
                @NonNull
                @Override
                public Intent createIntent(@NonNull Context context, @Nullable Uri input) {
                    Intent intent = super.createIntent(context, input);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    return intent;
                }
            }, this::handleBackupFolderPicked);

    private final ActivityResultLauncher<IntentSenderRequest> driveAuthorizeLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    this::handleDriveAuthResult);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        SecurePrefs prefs = new SecurePrefs(requireContext());
        ImmigrantVaultApplication app = (ImmigrantVaultApplication) requireActivity().getApplication();
        exportImportRepository = app.getExportImportRepository();

        setupThemeToggle();
        setupAutoBackup();
        setupCloudBackup();
        setupUsefulLinks(savedInstanceState);

        binding.switchBiometric.setChecked(prefs.isBiometricEnabled());
        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.setBiometricEnabled(isChecked));

        binding.linkI94.setOnClickListener(v ->
                UiUtils.openUrl(requireContext(), LinkConstants.I94_URL));
        binding.linkUscis.setOnClickListener(v ->
                UiUtils.openUrl(requireContext(), LinkConstants.USCIS_CASE_STATUS_URL));
        binding.linkProcessing.setOnClickListener(v ->
                UiUtils.openUrl(requireContext(), LinkConstants.USCIS_PROCESSING_TIMES_URL));
        binding.linkVisaBulletin.setOnClickListener(v ->
                UiUtils.openUrl(requireContext(), LinkConstants.VISA_BULLETIN_URL));
        binding.linkVisaBulletinDates.setOnClickListener(v ->
                UiUtils.openUrl(requireContext(), LinkConstants.VISA_BULLETIN_DATES_URL));
        binding.linkGreenCard.setOnClickListener(v ->
                UiUtils.openUrl(requireContext(), LinkConstants.USCIS_GREEN_CARD_URL));
        binding.linkN400.setOnClickListener(v ->
                UiUtils.openUrl(requireContext(), LinkConstants.USCIS_N400_URL));

        binding.buttonExport.setOnClickListener(v -> showExportFormatDialog());
        binding.buttonImport.setOnClickListener(v -> startImport());

        return binding.getRoot();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_LINKS_EXPANDED, linksExpanded);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            updateAutoBackupPathUi();
            updateCloudBackupUi();
        }
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

    private void setupUsefulLinks(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            linksExpanded = savedInstanceState.getBoolean(STATE_LINKS_EXPANDED, false);
        }
        binding.linksHeader.setOnClickListener(v -> setLinksExpanded(!linksExpanded));
        setLinksExpanded(linksExpanded);
    }

    private void setLinksExpanded(boolean expanded) {
        if (binding == null) {
            return;
        }
        linksExpanded = expanded;
        binding.linksContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
        binding.linksExpandIcon.setRotation(expanded ? 180f : 0f);
        binding.linksHeader.setContentDescription(getString(
                expanded ? R.string.settings_links_collapse : R.string.settings_links_expand));
    }

    private void setupAutoBackup() {
        binding.switchAutoBackup.setChecked(BackupPreferences.isAutoBackupEnabled(requireContext()));
        binding.switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            BackupPreferences.setAutoBackupEnabled(requireContext(), isChecked);
            AutoBackupScheduler.reschedule(requireContext());
        });
        binding.buttonChooseBackupFolder.setOnClickListener(v -> pickBackupFolderLauncher.launch(null));
        updateAutoBackupPathUi();
    }

    private void setupCloudBackup() {
        binding.switchCloudBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ignoreCloudSwitch || binding == null) {
                return;
            }
            if (isChecked) {
                setCloudSwitchChecked(false);
                confirmEnableCloudBackup();
            } else {
                BackupPreferences.setCloudBackupEnabled(requireContext(), false);
                AutoBackupScheduler.reschedule(requireContext());
                updateCloudBackupUi();
            }
        });
        binding.buttonConnectDrive.setOnClickListener(v -> {
            if (BackupPreferences.isCloudConnected(requireContext())) {
                confirmDisconnectDrive();
            } else {
                startDriveConnect(false);
            }
        });
        binding.buttonBackupToDrive.setOnClickListener(v -> backupToDriveNow());
        binding.buttonRestoreFromDrive.setOnClickListener(v -> restoreFromDrive());
        updateCloudBackupUi();
    }

    private void confirmEnableCloudBackup() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_cloud_backup_enable_title)
                .setMessage(R.string.settings_cloud_backup_enable_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.settings_cloud_backup_enable_confirm, (dialog, which) -> {
                    if (BackupPreferences.isCloudConnected(requireContext())) {
                        BackupPreferences.setCloudBackupEnabled(requireContext(), true);
                        AutoBackupScheduler.reschedule(requireContext());
                        updateCloudBackupUi();
                    } else {
                        startDriveConnect(true);
                    }
                })
                .show();
    }

    private void confirmDisconnectDrive() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_cloud_backup_disconnect_title)
                .setMessage(R.string.settings_cloud_backup_disconnect_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.settings_cloud_backup_disconnect, (dialog, which) -> {
                    BackupPreferences.disconnectCloud(requireContext());
                    AutoBackupScheduler.reschedule(requireContext());
                    updateCloudBackupUi();
                    Toast.makeText(requireContext(), R.string.settings_cloud_backup_disconnected_toast,
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void startDriveConnect(boolean enableAfter) {
        pendingEnableCloud = enableAfter;
        DriveAuthHelper.authorize(requireActivity())
                .addOnSuccessListener(requireActivity(), result -> {
                    if (binding == null) {
                        return;
                    }
                    if (result.hasResolution() && result.getPendingIntent() != null) {
                        try {
                            IntentSenderRequest request = new IntentSenderRequest.Builder(
                                    result.getPendingIntent()).build();
                            driveAuthorizeLauncher.launch(request);
                        } catch (Exception e) {
                            onDriveAuthFailed(e);
                        }
                    } else {
                        onDriveAuthSuccess(result);
                    }
                })
                .addOnFailureListener(requireActivity(), this::onDriveAuthFailed);
    }

    private void handleDriveAuthResult(ActivityResult result) {
        // AuthorizationClient often returns RESULT_CANCELED even after a successful
        // grant. Always parse the intent when data is present, matching Google's docs.
        if (result.getData() == null) {
            onDriveAuthCancelled();
            return;
        }
        try {
            AuthorizationResult auth = Identity.getAuthorizationClient(requireActivity())
                    .getAuthorizationResultFromIntent(result.getData());
            onDriveAuthSuccess(auth);
        } catch (ApiException e) {
            if (DriveAuthHelper.isUserCancelled(e)) {
                onDriveAuthCancelled();
            } else {
                onDriveAuthFailed(e);
            }
        }
    }

    private void onDriveAuthSuccess(AuthorizationResult result) {
        if (binding == null) {
            return;
        }
        try {
            DriveAuthHelper.accessTokenOrThrow(result);
        } catch (ExportImportException e) {
            onDriveAuthFailed(e);
            return;
        }
        String email = DriveAuthHelper.emailFrom(result);
        if (email == null || email.isEmpty()) {
            email = getString(R.string.settings_cloud_backup_account_connected);
        }
        BackupPreferences.setCloudAccountEmail(requireContext(), email);
        if (pendingEnableCloud) {
            BackupPreferences.setCloudBackupEnabled(requireContext(), true);
            pendingEnableCloud = false;
        }
        AutoBackupScheduler.reschedule(requireContext());
        updateCloudBackupUi();
        Toast.makeText(requireContext(), R.string.settings_cloud_backup_connected_toast, Toast.LENGTH_SHORT)
                .show();
    }

    private void onDriveAuthCancelled() {
        pendingEnableCloud = false;
        if (binding == null) {
            return;
        }
        updateCloudBackupUi();
        showDriveRegistrationHelp();
    }

    private void onDriveAuthFailed(Exception error) {
        pendingEnableCloud = false;
        if (binding == null) {
            return;
        }
        updateCloudBackupUi();
        if (DriveAuthHelper.isRegistrationError(error) || DriveAuthHelper.isUserCancelled(error)) {
            showDriveRegistrationHelp();
            return;
        }
        Toast.makeText(requireContext(), DriveAuthHelper.messageForAuthFailure(error), Toast.LENGTH_LONG)
                .show();
    }

    private void showDriveRegistrationHelp() {
        String packageName = requireContext().getPackageName();
        String sha1 = AppSigningFingerprint.sha1ColonSeparated(requireContext());
        if (sha1 == null || sha1.isEmpty()) {
            sha1 = getString(R.string.settings_cloud_backup_sha1_unavailable);
        }
        final String sha1ToCopy = sha1;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_cloud_backup_unregistered_title)
                .setMessage(getString(R.string.settings_cloud_backup_unregistered_message, packageName, sha1))
                .setNeutralButton(R.string.settings_cloud_backup_copy_sha1, (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager)
                            requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("SHA-1", sha1ToCopy));
                        Toast.makeText(requireContext(), R.string.settings_cloud_backup_sha1_copied,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void backupToDriveNow() {
        if (!BackupPreferences.isCloudConnected(requireContext())) {
            startDriveConnect(false);
            return;
        }
        setBackupBusy(true);
        new Thread(() -> {
            try {
                byte[] data = exportImportRepository.export(ExportFormat.CSV);
                String fileName = ExportFormat.CSV.buildFileName();
                CloudBackupOperations.upload(requireContext(), data, fileName, false);
                BackupPreferences.markCloudSuccess(
                        requireContext(),
                        AutoBackupPolicy.yearMonth(Calendar.getInstance()),
                        System.currentTimeMillis());
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    updateCloudBackupUi();
                    Toast.makeText(requireContext(), R.string.settings_cloud_backup_now_success,
                            Toast.LENGTH_SHORT).show();
                    setBackupBusy(false);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    Toast.makeText(requireContext(),
                            getString(R.string.settings_cloud_backup_now_failed, messageOf(e)),
                            Toast.LENGTH_LONG).show();
                    setBackupBusy(false);
                });
            }
        }).start();
    }

    private void restoreFromDrive() {
        if (!BackupPreferences.isCloudConnected(requireContext())) {
            startDriveConnect(false);
            return;
        }
        setBackupBusy(true);
        new Thread(() -> {
            try {
                List<DriveBackupItem> items = CloudBackupOperations.list(requireContext());
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    setBackupBusy(false);
                    showDriveRestorePicker(items);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    Toast.makeText(requireContext(),
                            getString(R.string.settings_cloud_backup_restore_failed, messageOf(e)),
                            Toast.LENGTH_LONG).show();
                    setBackupBusy(false);
                });
            }
        }).start();
    }

    private void showDriveRestorePicker(List<DriveBackupItem> items) {
        if (items == null || items.isEmpty()) {
            Toast.makeText(requireContext(), R.string.settings_cloud_backup_restore_empty, Toast.LENGTH_LONG)
                    .show();
            return;
        }
        CharSequence[] labels = new CharSequence[items.size()];
        for (int i = 0; i < items.size(); i++) {
            labels[i] = items.get(i).displayLabel();
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_cloud_backup_restore_title)
                .setItems(labels, (dialog, which) -> confirmDriveRestore(items.get(which)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmDriveRestore(DriveBackupItem item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_import_confirm_title)
                .setMessage(R.string.settings_import_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.settings_import_confirm, (dialog, which) ->
                        performDriveRestore(item))
                .show();
    }

    private void performDriveRestore(DriveBackupItem item) {
        setBackupBusy(true);
        new Thread(() -> {
            try {
                byte[] data = CloudBackupOperations.download(requireContext(), item.id);
                Future<Void> importTask = exportImportRepository.importAsync(data, "application/zip");
                activeTask = importTask;
                importTask.get();
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) {
                        return;
                    }
                    ((ImmigrantVaultApplication) requireActivity().getApplication())
                            .getPersonRepository().ensureSelfExists();
                    Toast.makeText(requireContext(), R.string.settings_import_success, Toast.LENGTH_SHORT)
                            .show();
                    setBackupBusy(false);
                });
            } catch (ExecutionException e) {
                requireActivity().runOnUiThread(() -> {
                    showError(R.string.settings_cloud_backup_restore_failed, e.getCause());
                    setBackupBusy(false);
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    showError(R.string.settings_cloud_backup_restore_failed, e);
                    setBackupBusy(false);
                });
            }
        }).start();
    }

    private void updateCloudBackupUi() {
        if (binding == null) {
            return;
        }
        boolean connected = BackupPreferences.isCloudConnected(requireContext());
        String email = BackupPreferences.getCloudAccountEmail(requireContext());
        if (connected) {
            binding.cloudBackupAccount.setText(getString(R.string.settings_cloud_backup_connected, email));
            binding.buttonConnectDrive.setText(R.string.settings_cloud_backup_disconnect);
        } else {
            binding.cloudBackupAccount.setText(R.string.settings_cloud_backup_not_connected);
            binding.buttonConnectDrive.setText(R.string.settings_cloud_backup_connect);
        }
        long lastSuccess = BackupPreferences.getCloudLastSuccessAtMillis(requireContext());
        if (lastSuccess <= 0L) {
            binding.cloudBackupLast.setText(R.string.settings_cloud_backup_last_never);
        } else {
            String formatted = android.text.format.DateFormat.getMediumDateFormat(requireContext())
                    .format(new Date(lastSuccess));
            binding.cloudBackupLast.setText(getString(R.string.settings_cloud_backup_last, formatted));
        }
        setCloudSwitchChecked(BackupPreferences.isCloudBackupEnabled(requireContext()));
        applyBackupBusyState();
    }

    private void setCloudSwitchChecked(boolean checked) {
        ignoreCloudSwitch = true;
        binding.switchCloudBackup.setChecked(checked);
        ignoreCloudSwitch = false;
    }

    private void handleBackupFolderPicked(Uri uri) {
        if (uri == null || binding == null) {
            return;
        }
        ContentResolver resolver = requireContext().getContentResolver();
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        try {
            resolver.takePersistableUriPermission(uri, flags);
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), R.string.settings_auto_backup_folder_failed, Toast.LENGTH_LONG)
                    .show();
            return;
        }
        String previous = BackupPreferences.getFolderUri(requireContext());
        if (previous != null && !previous.equals(uri.toString())) {
            try {
                resolver.releasePersistableUriPermission(Uri.parse(previous), flags);
            } catch (SecurityException ignored) {
            }
        }
        BackupPreferences.setFolderUri(requireContext(), uri.toString());
        updateAutoBackupPathUi();
    }

    private void updateAutoBackupPathUi() {
        if (binding == null) {
            return;
        }
        String path = BackupFolderLocator.getDisplayPath(requireContext());
        boolean custom = BackupFolderLocator.isUsingCustomFolder(requireContext());
        if (custom && !BackupFolderLocator.hasWritableFolder(requireContext())) {
            binding.autoBackupPath.setText(getString(R.string.settings_auto_backup_path_unavailable, path));
        } else if (custom) {
            binding.autoBackupPath.setText(getString(R.string.settings_auto_backup_path_selected, path));
        } else {
            binding.autoBackupPath.setText(getString(R.string.settings_auto_backup_path_default, path));
        }

        long lastSuccess = BackupPreferences.getLastSuccessAtMillis(requireContext());
        if (lastSuccess <= 0L) {
            binding.autoBackupLast.setText(R.string.settings_auto_backup_last_never);
        } else {
            String formatted = android.text.format.DateFormat.getMediumDateFormat(requireContext())
                    .format(new Date(lastSuccess));
            binding.autoBackupLast.setText(getString(R.string.settings_auto_backup_last, formatted));
        }
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
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_export_format, null, false);
        RadioGroup group = content.findViewById(R.id.export_format_group);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_export_format_title)
                .setView(content)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.settings_export_continue, (dialog, which) -> {
                    ExportFormat format = group.getCheckedRadioButtonId() == R.id.option_json
                            ? ExportFormat.JSON
                            : ExportFormat.CSV;
                    startExport(format);
                })
                .show();
    }

    private void startExport(ExportFormat format) {
        pendingExportFormat = format;
        ActivityResultLauncher<String> launcher = format == ExportFormat.JSON
                ? createJsonLauncher
                : createZipLauncher;
        launcher.launch(format.buildFileName());
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
        backupBusy = busy;
        applyBackupBusyState();
    }

    private void applyBackupBusyState() {
        if (binding == null) {
            return;
        }
        boolean busy = backupBusy;
        boolean connected = BackupPreferences.isCloudConnected(requireContext());
        binding.progressBackup.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.buttonExport.setEnabled(!busy);
        binding.buttonImport.setEnabled(!busy);
        binding.buttonChooseBackupFolder.setEnabled(!busy);
        binding.buttonConnectDrive.setEnabled(!busy);
        binding.buttonBackupToDrive.setEnabled(!busy && connected);
        binding.buttonRestoreFromDrive.setEnabled(!busy && connected);
        binding.switchCloudBackup.setEnabled(!busy);
        binding.switchAutoBackup.setEnabled(!busy);
    }

    private void showError(int messageResId, Throwable error) {
        if (binding == null) {
            return;
        }
        String detail = messageOf(error);
        String message;
        if (error instanceof ExportImportException) {
            message = error.getMessage();
        } else {
            message = getString(messageResId, detail);
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private static String messageOf(Throwable error) {
        if (error instanceof ExportImportException && error.getMessage() != null) {
            return error.getMessage();
        }
        return error != null && error.getMessage() != null ? error.getMessage() : "Unknown error";
    }
}
