package org.nikanikoo.flux.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public class BuildInfo {
    
    private static String cachedVersion = null;

    public static String getAppVersion(Context context) {
        if (cachedVersion != null) {
            return cachedVersion;
        }
        
        try {
            String packageName = context.getPackageName();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
                cachedVersion = "1.0." + packageInfo.getLongVersionCode();
            } else {
                var packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
                cachedVersion = "1.0." + packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            cachedVersion = "1.0.?";
        }
        
        return cachedVersion;
    }

    public static String getCommitHash() {
        return org.nikanikoo.flux.BuildConfig.GIT_COMMIT_HASH;
    }

    public static String getCommitTime() {
        return org.nikanikoo.flux.BuildConfig.GIT_COMMIT_TIME;
    }
}
