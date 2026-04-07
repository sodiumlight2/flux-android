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
import org.nikanikoo.flux.data.coordinators.CacheCoordinator;

public class DataSettingsFragment extends Fragment {

    private TextView cacheSizeValue;
    private View settingsAutoDownload;
    private View settingsClearCache;
    private CacheCoordinator cacheCoordinator;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_data_settings, container, false);

        cacheCoordinator = new CacheCoordinator(requireContext());

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
                CacheCoordinator.CacheStats stats = cacheCoordinator.getCacheStats();
                String sizeStr = CacheCoordinator.CacheStats.formatSize(stats.totalCacheDirSizeBytes);

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

    private void clearCache() {
        new Thread(() -> {
            try {
                cacheCoordinator.clearAllCaches();

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
}
