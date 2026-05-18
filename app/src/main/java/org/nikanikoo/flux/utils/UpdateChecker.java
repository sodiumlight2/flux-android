package org.nikanikoo.flux.utils;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nikanikoo.flux.BuildConfig;
import org.nikanikoo.flux.R;

import java.io.File;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateChecker {
    private static final String TAG = "UpdateChecker";
    private static final String RELEASES_URL = org.nikanikoo.flux.Constants.Links.GITHUB_RELEASES_API;
    private static final OkHttpClient client = new OkHttpClient();

    public interface UpdateCheckCallback {
        void onResult(UpdateInfo info, boolean isNewer);
    }

    public static void checkForUpdates(Activity activity) {
        checkUpdateStatus(new UpdateCheckCallback() {
            @Override
            public void onResult(UpdateInfo result, boolean isNewer) {
                if (result != null && isNewer && !activity.isFinishing()) {
                    android.content.SharedPreferences prefs = activity.getSharedPreferences("updates", android.content.Context.MODE_PRIVATE);
                    int skippedVersion = prefs.getInt("skipped_version", -1);
                    
                    if (result.versionCode > skippedVersion) {
                        showUpdateDialog(activity, result);
                    }
                }
            }
        });
    }

    public static void checkUpdateStatus(UpdateCheckCallback callback) {
        AsyncTaskHelper.executeAsync(() -> {
            Request request = new Request.Builder()
                    .url(RELEASES_URL)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }
                
                String jsonStr = response.body().string();
                JSONArray releases = new JSONArray(jsonStr);
                if (releases.length() == 0) return null;
                
                JSONObject latest = releases.getJSONObject(0);
                String tagName = latest.optString("tag_name", "");
                int latestVersion = parseVersionCode(tagName);
                
                JSONArray assets = latest.optJSONArray("assets");
                if (assets != null && assets.length() > 0) {
                    JSONObject asset = assets.getJSONObject(0);
                    String downloadUrl = asset.optString("browser_download_url");
                    String releaseName = latest.optString("name", "New Update");
                    String body = latest.optString("body", "");
                    
                    return new UpdateInfo(latestVersion, releaseName, body, downloadUrl);
                }
            } catch (Exception e) {
                Logger.e(TAG, "Failed to check for updates", e);
            }
            return null;
        }, new AsyncTaskHelper.AsyncCallback<UpdateInfo>() {
            @Override
            public void onSuccess(UpdateInfo result) {
                if (result != null) {
                    boolean isNewer = result.versionCode > BuildConfig.VERSION_CODE;
                    callback.onResult(result, isNewer);
                } else {
                    callback.onResult(null, false);
                }
            }

            @Override
            public void onError(String error) {
                callback.onResult(null, false);
            }
        });
    }

    public static int parseVersionCode(String tagName) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^build-alpha-(\\d+)").matcher(tagName);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to parse version code from tag: " + tagName, e);
        }
        return -1;
    }

    public static void showUpdateDialog(Activity activity, UpdateInfo info) {
        String title = activity.getString(R.string.update_available_title, info.versionName);
        String message = activity.getString(R.string.update_available_message, info.changelog);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(activity)
                .setIcon(R.drawable.ic_info)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.update_download, (dialog, which) -> {
                    downloadAndInstall(activity, info.downloadUrl, "flux-android-update.apk");
                })
                .setNegativeButton(R.string.update_later, null)
                .setNeutralButton(R.string.update_skip, (dialog, which) -> {
                    android.content.SharedPreferences prefs = activity.getSharedPreferences("updates", android.content.Context.MODE_PRIVATE);
                    prefs.edit().putInt("skipped_version", info.versionCode).apply();
                })
                .show();
    }

    private static void downloadAndInstall(Context context, String url, String fileName) {
        Toast.makeText(context, R.string.update_downloading, Toast.LENGTH_SHORT).show();
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(context.getString(R.string.app_name));
            request.setDescription(context.getString(R.string.update_downloading));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            
            File oldFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            if (oldFile.exists()) {
                oldFile.delete();
            }

            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                long downloadId = downloadManager.enqueue(request);
                
                BroadcastReceiver onComplete = new BroadcastReceiver() {
                    public void onReceive(Context ctxt, Intent intent) {
                        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                        if (id == downloadId) {
                            installApk(ctxt, downloadManager, id);
                            ctxt.unregisterReceiver(this);
                        }
                    }
                };
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
                } else {
                    context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
                }
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to download update", e);
            // Fallback to browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    private static void installApk(Context context, DownloadManager downloadManager, long downloadId) {
        try {
            Uri contentUri = downloadManager.getUriForDownloadedFile(downloadId);
            if (contentUri != null) {
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setDataAndType(contentUri, "application/vnd.android.package-archive");
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(install);
            }
        } catch (Exception e) {
            Logger.e(TAG, "Failed to install APK", e);
        }
    }

    public static class UpdateInfo {
        public int versionCode;
        public String versionName;
        public String changelog;
        public String downloadUrl;

        public UpdateInfo(int versionCode, String versionName, String changelog, String downloadUrl) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.changelog = changelog;
            this.downloadUrl = downloadUrl;
        }
    }
}
