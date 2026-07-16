package com.document.immigrantvault.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurePrefs {

    private static final String PREFS_NAME = "immigrant_vault_secure_prefs";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_SET = "pin_set";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";

    private final SharedPreferences prefs;

    public SecurePrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to create secure preferences", e);
        }
    }

    public boolean isPinSet() {
        return prefs.getBoolean(KEY_PIN_SET, false);
    }

    public void setPinHash(String hash) {
        prefs.edit()
                .putString(KEY_PIN_HASH, hash)
                .putBoolean(KEY_PIN_SET, true)
                .apply();
    }

    public boolean verifyPin(String pin) {
        String stored = prefs.getString(KEY_PIN_HASH, null);
        if (stored == null) {
            return false;
        }
        return stored.equals(hashPin(pin));
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public static String hashPin(String pin) {
        return Integer.toHexString(pin.hashCode());
    }
}
