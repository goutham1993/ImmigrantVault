package com.document.immigrantvault.data.cloud;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.document.immigrantvault.R;
import com.document.immigrantvault.data.backup.ExportImportException;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public final class DriveAuthHelper {

    public static final Scope DRIVE_FILE_SCOPE =
            new Scope("https://www.googleapis.com/auth/drive.file");

    private static final String TAG = "DriveAuthHelper";
    private static final int SIGN_IN_CANCELLED = 12501;
    private static final long SILENT_AUTH_TIMEOUT_SECONDS = 30L;

    private DriveAuthHelper() {
    }

    public static AuthorizationRequest authorizationRequest(Context context) {
        // Drive file access only. Extra identity scopes (email/openid) can make the
        // consent UI close immediately if they are not enabled on the OAuth client.
        AuthorizationRequest.Builder builder = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(DRIVE_FILE_SCOPE));
        String clientId = configuredWebClientId(context);
        if (clientId != null) {
            builder.requestOfflineAccess(clientId);
        }
        return builder.build();
    }

    @Nullable
    public static String configuredWebClientId(Context context) {
        String clientId = context.getString(R.string.google_drive_web_client_id);
        if (clientId == null || clientId.isEmpty()) {
            return null;
        }
        if (clientId.startsWith("YOUR_") || !clientId.contains(".apps.googleusercontent.com")) {
            return null;
        }
        return clientId;
    }

    public static Task<AuthorizationResult> authorize(Context context) {
        return Identity.getAuthorizationClient(context).authorize(authorizationRequest(context));
    }

    public static String silentAccessToken(Context context) throws Exception {
        AuthorizationResult result = Tasks.await(
                authorize(context), SILENT_AUTH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return accessTokenOrThrow(result);
    }

    public static String accessTokenOrThrow(AuthorizationResult result) throws ExportImportException {
        if (result == null) {
            throw new ExportImportException("Could not get a Google Drive access token.");
        }
        if (result.hasResolution()) {
            throw new ExportImportException(
                    "Google Drive access needs to be granted in Settings.");
        }
        String token = result.getAccessToken();
        if (token == null || token.isEmpty()) {
            throw new ExportImportException("Could not get a Google Drive access token.");
        }
        return token;
    }

    @Nullable
    public static String emailFrom(AuthorizationResult result) {
        if (result == null) {
            return null;
        }
        GoogleSignInAccount account = result.toGoogleSignInAccount();
        if (account == null) {
            return null;
        }
        return account.getEmail();
    }

    public static boolean isRegistrationError(Throwable error) {
        if (error instanceof ApiException) {
            int code = ((ApiException) error).getStatusCode();
            if (code == CommonStatusCodes.DEVELOPER_ERROR) {
                return true;
            }
        }
        String message = error != null && error.getMessage() != null
                ? error.getMessage().toLowerCase()
                : "";
        return message.contains("unregistered")
                || message.contains("invalid_client")
                || message.contains("invalid_request");
    }

    public static boolean isUserCancelled(Throwable error) {
        if (!(error instanceof ApiException)) {
            return false;
        }
        int code = ((ApiException) error).getStatusCode();
        return code == CommonStatusCodes.CANCELED || code == SIGN_IN_CANCELLED;
    }

    public static String messageForAuthFailure(Throwable error) {
        if (error instanceof ApiException) {
            int code = ((ApiException) error).getStatusCode();
            Log.w(TAG, "Google Drive authorization failed with status " + code, error);
            if (code == CommonStatusCodes.DEVELOPER_ERROR || isRegistrationError(error)) {
                return "Google Drive is not registered for this build. Add an Android OAuth client "
                        + "with this app's package name and SHA-1 in Google Cloud Console.";
            }
            if (code == CommonStatusCodes.NETWORK_ERROR) {
                return "No network connection. Try again when you are online.";
            }
            if (isUserCancelled(error)) {
                return "Google Drive sign-in was cancelled.";
            }
        } else if (error != null) {
            Log.w(TAG, "Google Drive authorization failed", error);
        }
        if (error != null && error.getMessage() != null && !error.getMessage().isEmpty()) {
            return error.getMessage();
        }
        return "Google Drive sign-in failed.";
    }
}
