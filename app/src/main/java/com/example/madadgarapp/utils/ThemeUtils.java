package com.example.madadgarapp.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Utility class to manage dark mode preference and apply theme changes.
 */
public class ThemeUtils {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    /**
     * Call this as early as possible (e.g., in Activity.onCreate before setContentView)
     * so that theme is applied before views are drawn.
     */
    public static void applyTheme(Context context) {
        boolean enabled = isDarkModeEnabled(context);
        AppCompatDelegate.setDefaultNightMode(enabled ?
                AppCompatDelegate.MODE_NIGHT_YES :
                AppCompatDelegate.MODE_NIGHT_NO);
    }

    /**
     * Persist preference and immediately switch theme.
     */
    public static void setDarkModeEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
        AppCompatDelegate.setDefaultNightMode(enabled ?
                AppCompatDelegate.MODE_NIGHT_YES :
                AppCompatDelegate.MODE_NIGHT_NO);
    }

    /**
     * Returns stored preference value. Default = false (light mode).
     */
    public static boolean isDarkModeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }
}
