package org.nikanikoo.flux.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

public class LocaleManager {
    private static final String PREFS_NAME = "locale_prefs";
    private static final String KEY_LANGUAGE = "app_language";

    public static final int LANGUAGE_SYSTEM = 0;
    public static final int LANGUAGE_RUSSIAN = 1;
    public static final int LANGUAGE_ENGLISH = 2;
    public static final int LANGUAGE_UKRAINIAN = 3;

    private static LocaleManager instance;
    private final SharedPreferences prefs;
    private final Context context;

    private LocaleManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized LocaleManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocaleManager(context);
        }
        return instance;
    }

    public void setLanguage(int language) {
        prefs.edit().putInt(KEY_LANGUAGE, language).apply();
        updateResources(language);
    }

    public int getLanguage() {
        return prefs.getInt(KEY_LANGUAGE, LANGUAGE_SYSTEM);
    }

    public Context updateContext(Context context) {
        int language = getLanguage();
        return updateContext(context, language);
    }

    public Context updateContext(Context context, int language) {
        Locale locale = getLocale(language);
        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);
        } else {
            configuration.locale = locale;
        }

        return context.createConfigurationContext(configuration);
    }

    private Locale getLocale(int language) {
        switch (language) {
            case LANGUAGE_RUSSIAN:
                return new Locale("ru", "RU");
            case LANGUAGE_ENGLISH:
                return new Locale("en", "US");
            case LANGUAGE_UKRAINIAN:
                return new Locale("uk", "UA");
            case LANGUAGE_SYSTEM:
            default:
                // For system language, get the current system locale
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    return Resources.getSystem().getConfiguration().getLocales().get(0);
                } else {
                    return Resources.getSystem().getConfiguration().locale;
                }
        }
    }

    private void updateResources(int language) {
        Locale locale = getLocale(language);
        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);
        } else {
            configuration.locale = locale;
        }

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    public String getLanguageName(int language) {
        switch (language) {
            case LANGUAGE_RUSSIAN:
                return context.getString(org.nikanikoo.flux.R.string.language_russian);
            case LANGUAGE_ENGLISH:
                return context.getString(org.nikanikoo.flux.R.string.language_english);
            case LANGUAGE_UKRAINIAN:
                return context.getString(org.nikanikoo.flux.R.string.language_ukrainian);
            case LANGUAGE_SYSTEM:
            default:
                return context.getString(org.nikanikoo.flux.R.string.language_system);
        }
    }

    public boolean isRussian() {
        return getLanguage() == LANGUAGE_RUSSIAN;
    }

    public boolean isEnglish() {
        return getLanguage() == LANGUAGE_ENGLISH;
    }

    public boolean isUkrainian() {
        return getLanguage() == LANGUAGE_UKRAINIAN;
    }

    public boolean isSystem() {
        return getLanguage() == LANGUAGE_SYSTEM;
    }
}
