package com.document.immigrantvault.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import androidx.annotation.Nullable;

import java.security.MessageDigest;
import java.util.Locale;

/** SHA-1 of the certificate that signed this installed build (what Google Cloud needs). */
public final class AppSigningFingerprint {

    private AppSigningFingerprint() {
    }

    @Nullable
    public static String sha1ColonSeparated(Context context) {
        Signature[] signatures = signatures(context);
        if (signatures == null || signatures.length == 0) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(signatures[0].toByteArray());
            StringBuilder formatted = new StringBuilder(hash.length * 3);
            for (int i = 0; i < hash.length; i++) {
                if (i > 0) {
                    formatted.append(':');
                }
                formatted.append(String.format(Locale.US, "%02X", hash[i]));
            }
            return formatted.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private static Signature[] signatures(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
                SigningInfo signingInfo = info.signingInfo;
                if (signingInfo == null) {
                    return null;
                }
                return signingInfo.hasMultipleSigners()
                        ? signingInfo.getApkContentsSigners()
                        : signingInfo.getSigningCertificateHistory();
            }
            PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            return info.signatures;
        } catch (Exception e) {
            return null;
        }
    }
}
