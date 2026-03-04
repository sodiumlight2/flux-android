package org.nikanikoo.flux.ui.activities;

import android.os.Bundle;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.ui.fragments.settings.DataSettingsFragment;

public class DataSettingsActivity extends BaseSettingsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_container);
        
        setupToolbar(getString(R.string.settings_data));
        
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, new DataSettingsFragment())
                .commit();
        }
    }
}
