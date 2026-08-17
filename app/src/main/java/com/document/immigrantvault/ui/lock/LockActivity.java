package com.document.immigrantvault.ui.lock;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.document.immigrantvault.ImmigrantVaultApplication;
import com.document.immigrantvault.MainActivity;
import com.document.immigrantvault.R;
import com.document.immigrantvault.data.backup.ExportFormat;
import com.document.immigrantvault.data.backup.ExportImportException;
import com.document.immigrantvault.data.repository.ExportImportRepository;
import com.document.immigrantvault.databinding.ActivityLockBinding;
import com.document.immigrantvault.util.SecurePrefs;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class LockActivity extends AppCompatActivity {

    private ActivityLockBinding binding;
    private SecurePrefs securePrefs;
    private ExportImportRepository exportImportRepository;
    private boolean isSetupMode;
    private String firstPin;
    private ExportFormat pendingExportFormat;
    private AlertDialog progressDialog;
    private Future<?> activeTask;

    private final ActivityResultLauncher<String> createDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument(), this::handleExportResult);

    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportSelection);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        securePrefs = new SecurePrefs(this);
        ImmigrantVaultApplication app = (ImmigrantVaultApplication) getApplication();
        exportImportRepository = app.getExportImportRepository();
        isSetupMode = !securePrefs.isPinSet();

        if (isSetupMode) {
            binding.lockSubtitle.setText(R.string.lock_setup_title);
            binding.unlockButton.setText(R.string.action_save);
            binding.biometricButton.setVisibility(View.GONE);
            binding.forgotPinButton.setVisibility(View.GONE);
        } else {
            binding.lockSubtitle.setText(R.string.lock_enter_pin);
            binding.unlockButton.setText(R.string.lock_enter_pin);
            binding.forgotPinButton.setVisibility(View.VISIBLE);
            if (securePrefs.isBiometricEnabled() && canUseBiometric()) {
                binding.biometricButton.setVisibility(View.VISIBLE);
                showBiometricPrompt();
            } else {
                binding.biometricButton.setVisibility(View.GONE);
            }
        }

        binding.unlockButton.setOnClickListener(v -> handleUnlock());
        binding.biometricButton.setOnClickListener(v -> showBiometricPrompt());
        binding.forgotPinButton.setOnClickListener(v -> showForgotPinDialog());
    }

    @Override
    protected void onDestroy() {
        if (activeTask != null) {
            activeTask.cancel(true);
            activeTask = null;
        }
        dismissProgress();
        super.onDestroy();
    }

    private void handleUnlock() {
        TextInputEditText pinInput = binding.pinInput;
        String pin = pinInput.getText() != null ? pinInput.getText().toString().trim() : "";

        if (pin.length() < 4 || pin.length() > 6) {
            showError(getString(R.string.lock_pin_invalid));
            return;
        }

        if (isSetupMode) {
            if (firstPin == null) {
                firstPin = pin;
                binding.lockSubtitle.setText(R.string.lock_confirm_subtitle);
                pinInput.setText("");
                hideError();
            } else if (!firstPin.equals(pin)) {
                showError(getString(R.string.lock_pin_mismatch));
                firstPin = null;
                binding.lockSubtitle.setText(R.string.lock_setup_title);
                pinInput.setText("");
            } else {
                securePrefs.setPinHash(SecurePrefs.hashPin(pin));
                if (securePrefs.consumePendingRestoreOffer()) {
                    showRestoreOfferDialog();
                } else {
                    goToMain();
                }
            }
        } else {
            if (securePrefs.verifyPin(pin)) {
                goToMain();
            } else {
                showError(getString(R.string.lock_auth_failed));
            }
        }
    }

    private void showForgotPinDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lock_forgot_title)
                .setMessage(R.string.lock_forgot_message)
                .setPositiveButton(R.string.lock_forgot_backup_then_reset,
                        (dialog, which) -> showExportFormatDialog())
                .setNeutralButton(R.string.lock_forgot_reset_without_backup,
                        (dialog, which) -> showWipeWithoutBackupConfirm())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showExportFormatDialog() {
        new MaterialAlertDialogBuilder(this)
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

    private void showWipeWithoutBackupConfirm() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lock_forgot_wipe_confirm_title)
                .setMessage(R.string.lock_forgot_wipe_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.lock_forgot_wipe_confirm,
                        (dialog, which) -> performReset())
                .show();
    }

    private void handleExportResult(Uri uri) {
        if (uri == null || pendingExportFormat == null) {
            return;
        }
        ExportFormat format = pendingExportFormat;
        pendingExportFormat = null;
        showProgress(getString(R.string.settings_backup_in_progress));
        Future<byte[]> exportTask = exportImportRepository.exportAsync(format);
        activeTask = exportTask;

        new Thread(() -> {
            try {
                byte[] data = exportTask.get();
                runOnUiThread(() -> {
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        if (out == null) {
                            throw new ExportImportException("Could not write to the selected file.");
                        }
                        out.write(data);
                        Toast.makeText(this, R.string.settings_export_success, Toast.LENGTH_SHORT).show();
                        dismissProgress();
                        performReset();
                    } catch (Exception e) {
                        dismissProgress();
                        showBackupError(R.string.settings_export_failed, e);
                    }
                });
            } catch (ExecutionException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    showBackupError(R.string.settings_export_failed, e.getCause());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    showBackupError(R.string.settings_export_failed, e);
                });
            }
        }).start();
    }

    private void performReset() {
        showProgress(getString(R.string.lock_forgot_reset_working));
        Future<Void> clearTask = exportImportRepository.clearAllAsync();
        activeTask = clearTask;
        new Thread(() -> {
            try {
                clearTask.get();
                runOnUiThread(() -> {
                    securePrefs.clearPin();
                    securePrefs.setPendingRestoreOffer(true);
                    dismissProgress();
                    recreate();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    Toast.makeText(this,
                            getString(R.string.settings_export_failed,
                                    e.getMessage() != null ? e.getMessage() : "Unknown error"),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void showRestoreOfferDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.lock_forgot_restore_title)
                .setMessage(R.string.lock_forgot_restore_message)
                .setCancelable(false)
                .setPositiveButton(R.string.lock_forgot_restore_yes,
                        (dialog, which) -> startImport())
                .setNegativeButton(R.string.lock_forgot_restore_no,
                        (dialog, which) -> goToMain())
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

    private void handleImportSelection(Uri uri) {
        if (uri == null) {
            goToMain();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_import_confirm_title)
                .setMessage(R.string.settings_import_confirm_message)
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> goToMain())
                .setPositiveButton(R.string.settings_import_confirm, (dialog, which) -> performImport(uri))
                .show();
    }

    private void performImport(Uri uri) {
        showProgress(getString(R.string.settings_backup_in_progress));
        new Thread(() -> {
            try {
                byte[] data = readAllBytes(uri);
                String mimeType = getContentResolver().getType(uri);
                Future<Void> importTask = exportImportRepository.importAsync(data, mimeType);
                activeTask = importTask;
                importTask.get();
                runOnUiThread(() -> {
                    ((ImmigrantVaultApplication) getApplication())
                            .getPersonRepository().ensureSelfExists();
                    dismissProgress();
                    Toast.makeText(this, R.string.settings_import_success, Toast.LENGTH_SHORT).show();
                    goToMain();
                });
            } catch (ExecutionException e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    showBackupError(R.string.settings_import_failed, e.getCause());
                    goToMain();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    dismissProgress();
                    showBackupError(R.string.settings_import_failed, e);
                    goToMain();
                });
            }
        }).start();
    }

    private byte[] readAllBytes(Uri uri) throws Exception {
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
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

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void showBiometricPrompt() {
        if (!canUseBiometric()) {
            return;
        }
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.lock_title))
                .setSubtitle(getString(R.string.lock_use_biometric))
                .setNegativeButtonText(getString(R.string.action_cancel))
                .build();

        BiometricPrompt prompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        goToMain();
                    }
                });
        prompt.authenticate(promptInfo);
    }

    private boolean canUseBiometric() {
        int result = BiometricManager.from(this)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showProgress(String message) {
        dismissProgress();
        progressDialog = new MaterialAlertDialogBuilder(this)
                .setMessage(message)
                .setCancelable(false)
                .create();
        progressDialog.show();
        setControlsEnabled(false);
    }

    private void dismissProgress() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
        if (binding != null) {
            setControlsEnabled(true);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        binding.unlockButton.setEnabled(enabled);
        binding.biometricButton.setEnabled(enabled);
        binding.forgotPinButton.setEnabled(enabled);
        binding.pinInput.setEnabled(enabled);
    }

    private void showBackupError(int messageResId, Throwable error) {
        String detail = error != null && error.getMessage() != null
                ? error.getMessage()
                : "Unknown error";
        String message;
        if (error instanceof ExportImportException) {
            message = error.getMessage();
        } else if (messageResId == R.string.settings_export_failed) {
            message = getString(R.string.settings_export_failed, detail);
        } else if (messageResId == R.string.settings_import_failed) {
            message = getString(R.string.settings_import_failed, detail);
        } else {
            message = detail;
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showError(String message) {
        TextView errorText = binding.errorText;
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.errorText.setVisibility(View.GONE);
    }
}
