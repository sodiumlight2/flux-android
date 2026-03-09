package org.nikanikoo.flux.ui.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.utils.LocaleManager;
import org.nikanikoo.flux.utils.ThemeManager;

public abstract class BaseSettingsActivity extends AppCompatActivity {

    private LocaleManager localeManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        localeManager = LocaleManager.getInstance(newBase);
        Context context = localeManager.updateContext(newBase);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme before super.onCreate()
        ThemeManager themeManager = ThemeManager.getInstance(this);
        themeManager.applySavedTheme();
        themeManager.applyThemeToActivity(this);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ThemeManager.applySystemBarsAppearance(this);
    }

    protected void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
