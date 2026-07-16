package com.document.immigrantvault.ui.lock;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.document.immigrantvault.MainActivity;
import com.document.immigrantvault.R;
import com.document.immigrantvault.databinding.ActivityLockBinding;
import com.document.immigrantvault.util.SecurePrefs;
import com.google.android.material.textfield.TextInputEditText;

public class LockActivity extends AppCompatActivity {

    private ActivityLockBinding binding;
    private SecurePrefs securePrefs;
    private boolean isSetupMode;
    private String firstPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLockBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        securePrefs = new SecurePrefs(this);
        isSetupMode = !securePrefs.isPinSet();

        if (isSetupMode) {
            binding.lockSubtitle.setText(R.string.lock_setup_title);
            binding.unlockButton.setText(R.string.action_save);
            binding.biometricButton.setVisibility(View.GONE);
        } else {
            binding.lockSubtitle.setText(R.string.lock_enter_pin);
            binding.unlockButton.setText(R.string.lock_enter_pin);
            if (securePrefs.isBiometricEnabled() && canUseBiometric()) {
                binding.biometricButton.setVisibility(View.VISIBLE);
                showBiometricPrompt();
            } else {
                binding.biometricButton.setVisibility(View.GONE);
            }
        }

        binding.unlockButton.setOnClickListener(v -> handleUnlock());
        binding.biometricButton.setOnClickListener(v -> showBiometricPrompt());
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
                goToMain();
            }
        } else {
            if (securePrefs.verifyPin(pin)) {
                goToMain();
            } else {
                showError(getString(R.string.lock_auth_failed));
            }
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

    private void showError(String message) {
        TextView errorText = binding.errorText;
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        binding.errorText.setVisibility(View.GONE);
    }
}
