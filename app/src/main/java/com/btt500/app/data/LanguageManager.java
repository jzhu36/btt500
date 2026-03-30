package com.btt500.app.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages the user's language preference for question display.
 * Supports "zh" (Chinese) and "en" (English).
 */
public class LanguageManager {

    private static final String PREF_NAME = "btt500_prefs";
    private static final String KEY_LANGUAGE = "display_language";
    public static final String LANG_ZH = "zh";
    public static final String LANG_EN = "en";

    private static LanguageManager instance;
    private final SharedPreferences prefs;
    private String currentLanguage;

    private LanguageManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentLanguage = prefs.getString(KEY_LANGUAGE, LANG_ZH);
    }

    public static synchronized LanguageManager getInstance(Context context) {
        if (instance == null) {
            instance = new LanguageManager(context);
        }
        return instance;
    }

    public String getLanguage() {
        return currentLanguage;
    }

    public boolean isChinese() {
        return LANG_ZH.equals(currentLanguage);
    }

    public boolean isEnglish() {
        return LANG_EN.equals(currentLanguage);
    }

    public void setLanguage(String lang) {
        currentLanguage = lang;
        prefs.edit().putString(KEY_LANGUAGE, lang).apply();
    }

    public void toggleLanguage() {
        if (isChinese()) {
            setLanguage(LANG_EN);
        } else {
            setLanguage(LANG_ZH);
        }
    }
}
