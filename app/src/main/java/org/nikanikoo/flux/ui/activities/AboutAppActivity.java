package org.nikanikoo.flux.ui.activities;

import android.os.Bundle;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.ui.fragments.settings.AboutAppFragment;

public class AboutAppActivity extends BaseSettingsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_container);
        
        setupToolbar(getString(R.string.settings_app_version));
        
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_container, new AboutAppFragment())
                .commit();
        }
    }
}
