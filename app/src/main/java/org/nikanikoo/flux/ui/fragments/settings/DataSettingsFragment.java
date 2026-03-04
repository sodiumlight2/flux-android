package org.nikanikoo.flux.ui.fragments.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.nikanikoo.flux.R;

import java.io.File;

public class DataSettingsFragment extends Fragment {

    private TextView cacheSizeValue;
    private View settingsAutoDownload;
    private View settingsClearCache;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data_settings, container, false);
        
        initViews(view);
        calculateCacheSize();
        setupClickListeners();
        
        return view;
    }
    
    private void initViews(View view) {
        cacheSizeValue = view.findViewById(R.id.cache_size_value);
        settingsClearCache = view.findViewById(R.id.settings_clear_cache);
    }
    
    private void setupClickListeners() {
        
        settingsClearCache.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.data_clear_cache))
                .setMessage(getString(R.string.data_clear_cache_message))
                .setPositiveButton(getString(R.string.data_clear_btn_clear), (dialog, which) -> clearCache())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        });
    }
    
    private void calculateCacheSize() {
        new Thread(() -> {
            try {
                File cacheDir = requireContext().getCacheDir();
                long size = getDirSize(cacheDir);
                String sizeStr = formatSize(size);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> cacheSizeValue.setText(sizeStr));
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> cacheSizeValue.setText("N/A"));
                }
            }
        }).start();
    }
    
    private long getDirSize(File dir) {
        long size = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else {
                        size += getDirSize(file);
                    }
                }
            }
        }
        return size;
    }
    
    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
    
    private void clearCache() {
        new Thread(() -> {
            try {
                File cacheDir = requireContext().getCacheDir();
                deleteDir(cacheDir);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), getString(R.string.data_cache_cleared), Toast.LENGTH_SHORT).show();
                        calculateCacheSize();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), getString(R.string.data_cache_clear_error), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }
    
    private void deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDir(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
    }
}
