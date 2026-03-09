package org.nikanikoo.flux.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.nikanikoo.flux.R;
import org.nikanikoo.flux.ui.activities.MainActivity;
import org.nikanikoo.flux.utils.LocaleManager;

public class LanguageSettingsActivity extends BaseSettingsActivity {

    private LocaleManager localeManager;
    private View langSystem, langRussian, langEnglish;
    private RadioButton radioSystem, radioRussian, radioEnglish;
    private TextView titleSystem, titleRussian, titleEnglish;
    private TextView translatorRu, translatorEn;
    private TextView translatorRuInfo, translatorEnInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_settings);

        setupToolbar(getString(R.string.settings_language));

        localeManager = LocaleManager.getInstance(this);

        initViews();
        updateSelection();
        setupClickListeners();
    }

    private void initViews() {
        langSystem = findViewById(R.id.lang_system);
        langRussian = findViewById(R.id.lang_russian);
        langEnglish = findViewById(R.id.lang_english);

        radioSystem = findViewById(R.id.radio_system);
        radioRussian = findViewById(R.id.radio_russian);
        radioEnglish = findViewById(R.id.radio_english);

        titleSystem = findViewById(R.id.title_system);
        titleRussian = findViewById(R.id.title_russian);
        titleEnglish = findViewById(R.id.title_english);

        translatorRu = findViewById(R.id.translator_ru);
        translatorEn = findViewById(R.id.translator_en);

        // Set language titles with flags
        titleSystem.setText(getString(R.string.language_system));
        titleRussian.setText(getString(R.string.language_russian));
        titleEnglish.setText(getString(R.string.language_english));

        // Set translator names
        String translatorRuText = getString(R.string.language_translator_ru);
        String translatorEnText = getString(R.string.language_translator_en);
        
        translatorRu.setText(translatorRuText);
        translatorEn.setText(translatorEnText);
    }

    private void updateSelection() {
        int currentLanguage = localeManager.getLanguage();

        radioSystem.setChecked(currentLanguage == LocaleManager.LANGUAGE_SYSTEM);
        radioRussian.setChecked(currentLanguage == LocaleManager.LANGUAGE_RUSSIAN);
        radioEnglish.setChecked(currentLanguage == LocaleManager.LANGUAGE_ENGLISH);
    }

    private void setupClickListeners() {
        langSystem.setOnClickListener(v -> selectLanguage(LocaleManager.LANGUAGE_SYSTEM));
        langRussian.setOnClickListener(v -> selectLanguage(LocaleManager.LANGUAGE_RUSSIAN));
        langEnglish.setOnClickListener(v -> selectLanguage(LocaleManager.LANGUAGE_ENGLISH));
    }

    private void selectLanguage(int language) {
        localeManager.setLanguage(language);
        updateSelection();
        Toast.makeText(this, getString(R.string.language_applied), Toast.LENGTH_SHORT).show();
        restartApp();
    }

    private void restartApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
