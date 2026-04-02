package com.btt500.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the user's language preference for question display.
 * Supports "zh" (Chinese) and "en" (English).
 * Provides listener mechanism for immediate UI refresh on language change.
 */
public class LanguageManager {

    private static final String PREF_NAME = "btt500_prefs";
    private static final String KEY_LANGUAGE = "display_language";
    public static final String LANG_ZH = "zh";
    public static final String LANG_EN = "en";

    private static LanguageManager instance;
    private final SharedPreferences prefs;
    private String currentLanguage;

    private final List<OnLanguageChangedListener> listeners = new ArrayList<>();

    public interface OnLanguageChangedListener {
        void onLanguageChanged(String newLanguage);
    }

    private LanguageManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentLanguage = prefs.getString(KEY_LANGUAGE, LANG_EN);
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
        notifyListeners();
    }

    public void toggleLanguage() {
        setLanguage(isChinese() ? LANG_EN : LANG_ZH);
    }

    public void addListener(OnLanguageChangedListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(OnLanguageChangedListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (OnLanguageChangedListener l : listeners) {
            l.onLanguageChanged(currentLanguage);
        }
    }
}
