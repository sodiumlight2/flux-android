package org.nikanikoo.flux.data.coordinators;

import android.content.Context;

import org.nikanikoo.flux.Constants;
import org.nikanikoo.flux.data.managers.ProfileManager;
import org.nikanikoo.flux.data.managers.api.OpenVKApi;
import org.nikanikoo.flux.services.LongPollManager;
import org.nikanikoo.flux.services.MessageNotificationManager;
import org.nikanikoo.flux.ui.adapters.posts.PostAdapter;
import org.nikanikoo.flux.utils.CacheManager;
import org.nikanikoo.flux.utils.Logger;

import java.io.File;

public class CacheCoordinator {
    private static final String TAG = "CacheCoordinator";

    private final Context appContext;

    public CacheCoordinator(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public void clearAllCaches() {
        Logger.d(TAG, "Clearing all caches...");

        clearHttpCache();
        clearProfileCache();
        clearInMemoryCaches();
        clearImageCache();
        clearTempFiles();

        Logger.d(TAG, "All caches cleared");
    }

    public CacheStats getCacheStats() {
        CacheStats stats = new CacheStats();

        try {
            File httpCacheDir = new File(appContext.getCacheDir(), "http_cache");
            stats.httpCacheSizeBytes = getDirSize(httpCacheDir);
            stats.httpCacheMaxSizeBytes = Constants.Cache.HTTP_CACHE_SIZE_MB * 1024 * 1024L;
        } catch (Exception e) {
            Logger.w(TAG, "Error getting HTTP cache stats", e);
        }

        try {
            File imageCacheDir = new File(appContext.getCacheDir(), "picasso_cache");
            stats.imageCacheSizeBytes = getDirSize(imageCacheDir);
            stats.imageCacheMaxSizeBytes = Constants.Cache.IMAGE_CACHE_SIZE_MB * 1024 * 1024L;
        } catch (Exception e) {
            Logger.w(TAG, "Error getting image cache stats", e);
        }

        try {
            var profileManager = ProfileManager.getInstance(appContext);
            var cachedProfile = profileManager.getCachedProfileSync();
            stats.hasCachedProfile = cachedProfile != null;
        } catch (Exception e) {
            Logger.w(TAG, "Error getting profile cache stats", e);
        }

        try {
            stats.totalCacheDirSizeBytes = getDirSize(appContext.getCacheDir());
        } catch (Exception e) {
            Logger.w(TAG, "Error getting total cache dir size", e);
        }

        return stats;
    }

    public void evictExpired() {
        Logger.d(TAG, "Evicting expired cache entries...");
    }

    private void clearHttpCache() {
        try {
            OpenVKApi api = OpenVKApi.getInstance(appContext);
            if (api.getCacheManager() != null) {
                api.getCacheManager().evictAll();
                Logger.d(TAG, "HTTP cache evicted");
            }
        } catch (Exception e) {
            Logger.w(TAG, "Error evicting HTTP cache", e);
        }
    }

    private void clearProfileCache() {
        try {
            ProfileManager.getInstance(appContext).clearCache();
            Logger.d(TAG, "Profile cache cleared");
        } catch (Exception e) {
            Logger.w(TAG, "Error clearing profile cache", e);
        }
    }

    private void clearInMemoryCaches() {
        try {
            MessageNotificationManager.getInstance(appContext).clearCache();
            Logger.d(TAG, "Message notification cache cleared");
        } catch (Exception e) {
            Logger.w(TAG, "Error clearing message notification cache", e);
        }

        try {
            LongPollManager.getInstance(appContext).clearProcessedEventsCache();
            Logger.d(TAG, "LongPoll cache cleared");
        } catch (Exception e) {
            Logger.w(TAG, "Error clearing LongPoll cache", e);
        }
    }

    private void clearImageCache() {
        try {
            File imageCacheDir = new File(appContext.getCacheDir(), "picasso_cache");
            deleteDirectory(imageCacheDir);
            Logger.d(TAG, "Image disk cache cleared");
        } catch (Exception e) {
            Logger.w(TAG, "Error clearing image cache", e);
        }
    }

    private void clearTempFiles() {
        try {
            File cacheDir = appContext.getCacheDir();
            File[] files = cacheDir.listFiles();
            if (files != null) {
                int deletedCount = 0;
                for (File file : files) {
                    String name = file.getName();
                    if (name.startsWith("upload_image") || name.startsWith("audio_") ||
                        name.endsWith(".tmp") || name.endsWith(".jpg")) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }
                if (deletedCount > 0) {
                    Logger.d(TAG, "Deleted " + deletedCount + " temp files");
                }
            }
        } catch (Exception e) {
            Logger.w(TAG, "Error clearing temp files", e);
        }
    }

    private static long getDirSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += getDirSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    private static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
    public static class CacheStats {
        public long httpCacheSizeBytes = 0;
        public long httpCacheMaxSizeBytes = 0;
        public long imageCacheSizeBytes = 0;
        public long imageCacheMaxSizeBytes = 0;
        public boolean hasCachedProfile = false;
        public long totalCacheDirSizeBytes = 0;

        public float getHttpCacheUsagePercent() {
            return httpCacheMaxSizeBytes > 0 ? (httpCacheSizeBytes * 100f / httpCacheMaxSizeBytes) : 0;
        }

        public float getImageCacheUsagePercent() {
            return imageCacheMaxSizeBytes > 0 ? (imageCacheSizeBytes * 100f / imageCacheMaxSizeBytes) : 0;
        }

        public static String formatSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024f);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024f * 1024f));
            return String.format("%.2f GB", bytes / (1024f * 1024f * 1024f));
        }

        @Override
        public String toString() {
            return "CacheStats{" +
                    "HTTP=" + formatSize(httpCacheSizeBytes) + "/" + formatSize(httpCacheMaxSizeBytes) +
                    ", Image=" + formatSize(imageCacheSizeBytes) + "/" + formatSize(imageCacheMaxSizeBytes) +
                    ", Profile=" + (hasCachedProfile ? "yes" : "no") +
                    ", Total=" + formatSize(totalCacheDirSizeBytes) +
                    '}';
        }
    }
}
