// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.base;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.*;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CARD_BACKGROUND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CARD_BORDER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DARK_MODE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_FADE_TRANSITIONS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_GENERAL_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_THEME;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.BackupAndRestoreUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Base activity that ensures consistent theme, accent color, and locale settings
 * across all activities in the app.
 * <p>
 * This class handles:
 * <ul>
 *     <li>Applying the correct theme and accent color on creation</li>
 *     <li>Applying the selected locale/language</li>
 *     <li>Registering a shared preferences listener to dynamically recreate the activity
 *         when user settings change (e.g., dark mode, accent color, language, etc.)</li>
 *     <li>Unregistering the listener on destruction to prevent memory leaks</li>
 * </ul>
 * <p>
 */
public class BaseActivity extends AppCompatActivity {

    /**
     * List of supported preference keys for theme and UI settings management.
     * <p>
     * This list is used to monitor only relevant keys within
     * {@link #registerThemeListener()} to optimize change handling.</p>
     */
    private static final List<String> SUPPORTED_PREF_KEYS = List.of(
        KEY_THEME, KEY_DARK_MODE, KEY_GENERAL_FONT, KEY_ACCENT_COLOR, KEY_AUTO_NIGHT_ACCENT_COLOR, KEY_NIGHT_ACCENT_COLOR,
        KEY_CARD_BACKGROUND, KEY_CARD_BORDER, KEY_FADE_TRANSITIONS
    );

    /**
     * Map to store listeners by SharedPreferences so they can be removed cleanly
     */
    private static final Map<Activity, SharedPreferences.OnSharedPreferenceChangeListener> mListenerMap = new WeakHashMap<>();

    private DataModel mDataModel;
    private UiDataModel mUiDataModel;
    private SharedPreferences mPrefs;
    private DisplayMetrics mDisplayMetrics;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mDataModel = DataModel.getDataModel();
        mUiDataModel = UiDataModel.getUiDataModel();
        mPrefs = getDefaultSharedPreferences(Utils.getSafeStorageContext(this));
        mDisplayMetrics = getResources().getDisplayMetrics();

        applyThemeAndAccentColor();

        super.onCreate(savedInstanceState);

        registerThemeListener();
    }

    @Override
    protected void onDestroy() {
        unregisterThemeListener();

        super.onDestroy();
    }

    /**
     * Apply the theme and the accent color to the activities.
     */
    private void applyThemeAndAccentColor() {
        final String theme = SettingsDAO.getTheme(mPrefs);
        final String darkMode = SettingsDAO.getDarkMode(mPrefs);
        final String accentColor = SettingsDAO.getAccentColor(mPrefs);
        final boolean isAutoNightAccentColorEnabled = SettingsDAO.isAutoNightAccentColorEnabled(mPrefs);
        final String nightAccentColor = SettingsDAO.getNightAccentColor(mPrefs);

        applyAmoledTheme(theme, darkMode);

        applyAccentColor(isAutoNightAccentColorEnabled, accentColor, nightAccentColor, darkMode);

        applyNavigationBarColor(darkMode);
    }

    /**
     * Apply the AMOLED theme to the activities if the device is set to night mode.
     */
    private void applyAmoledTheme(String theme, String darkMode) {
        if (darkMode.equals(AMOLED_DARK_MODE) && !theme.equals(SYSTEM_THEME) && !theme.equals(LIGHT_THEME)) {
            setTheme(R.style.AmoledTheme);
        }
    }

    /**
     * Sets the accent color theme for the specified activity based on user preferences.
     * <p>
     * Chooses between the regular and night accent color depending on the current theme
     * and whether auto-night accent color is enabled.</p>
     *
     * @param isAutoNightAccentColorEnabled True if automatic night accent color is enabled.
     * @param accentColor                   The regular accent color value.
     * @param nightAccentColor              The night accent color value.
     */
    private void applyAccentColor(boolean isAutoNightAccentColorEnabled, String accentColor,
                                  String nightAccentColor, String darkMode) {

        String color = isAutoNightAccentColorEnabled
            ? accentColor
            : (ThemeUtils.isNight(getResources()) ? nightAccentColor : accentColor);

        switch (color) {
            case BLACK_ACCENT_COLOR -> setTheme(R.style.BlackAccentColor);
            case BLUE_ACCENT_COLOR -> setTheme(R.style.BlueAccentColor);
            case BLUE_GRAY_ACCENT_COLOR -> setTheme(R.style.BlueGrayAccentColor);
            case BROWN_ACCENT_COLOR -> setTheme(R.style.BrownAccentColor);
            case GREEN_ACCENT_COLOR -> setTheme(R.style.GreenAccentColor);
            case INDIGO_ACCENT_COLOR -> setTheme(R.style.IndigoAccentColor);
            case ORANGE_ACCENT_COLOR -> setTheme(R.style.OrangeAccentColor);
            case PINK_ACCENT_COLOR -> setTheme(R.style.PinkAccentColor);
            case PURPLE_ACCENT_COLOR -> setTheme(R.style.PurpleAccentColor);
            case RED_ACCENT_COLOR -> setTheme(R.style.RedAccentColor);
            case YELLOW_ACCENT_COLOR -> setTheme(R.style.YellowAccentColor);
        }

        if (ThemeUtils.isNight(getResources()) && darkMode.equals(AMOLED_DARK_MODE)) {
            getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        }
    }

    /**
     * Applies a color to the navigation bar for activities.
     */
    @SuppressWarnings("deprecation")
    private void applyNavigationBarColor(String darkMode) {
        if (SdkUtils.isAtLeastAndroid10()) {
            if (this instanceof DeskClock) {
                EdgeToEdge.enable(this);
                getWindow().setNavigationBarContrastEnforced(false);
            }
        } else {
            boolean isPhoneInLandscapeMode = !ThemeUtils.isTablet() && ThemeUtils.isLandscape();
            boolean isCardBackgroundDisplayed = SettingsDAO.isCardBackgroundDisplayed(mPrefs);

            if (ThemeUtils.isNight(getResources()) && darkMode.equals(AMOLED_DARK_MODE)) {
                getWindow().setNavigationBarColor(Color.BLACK);
            } else if (this instanceof DeskClock) {
                getWindow().setNavigationBarColor(MaterialColors.getColor(this, isPhoneInLandscapeMode || !isCardBackgroundDisplayed
                    ? android.R.attr.colorBackground
                    : com.google.android.material.R.attr.colorSurface, Color.BLACK));
            } else {
                getWindow().setNavigationBarColor(MaterialColors.getColor(this, android.R.attr.colorBackground, Color.BLACK));
            }
        }
    }

    /**
     * Sets the night mode of AppCompatDelegate according to the selected theme.
     * <ul>
     *     <li>SYSTEM_THEME: follows the Android system setting.</li>
     *     <li>LIGHT_THEME: forces light mode.</li>
     *     <li>DARK_THEME: forces dark mode.</li>
     * </ul>
     *
     * @param theme The theme value (corresponding to {@code KEY_THEME}) to apply.
     */
    private void applySystemNightMode(String theme) {
        switch (theme) {
            case SYSTEM_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            case LIGHT_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            case DARK_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    /**
     * Registers a SharedPreferences listener on the given activity to monitor UI-related settings.
     * Triggers appropriate actions (theme update, locale change, etc.) when preferences change.
     * Automatically recreates the activity when needed (e.g., on accent color, language, or theme change).
     *
     */
    private void registerThemeListener() {
        // Avoid registering the listener multiple times for the same prefs
        if (mListenerMap.containsKey(this)) {
            return;
        }

        // Important: we use a cached map of preference values to avoid unnecessary activity recreation.
        // Without this check, any preference change (even with the same value) triggers recreate(),
        // which causes significant slowdown when opening the settings screen,
        // especially on low-end devices due to how Preferences are initialized.
        Map<String, Object> cachedValues = Utils.initCachedValues(SUPPORTED_PREF_KEYS, this::getPreferenceValue);

        SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
            if (BackupAndRestoreUtils.isRestoringBackupOrIsResettingApp
                || key == null
                || !cachedValues.containsKey(key)) {
                return;
            }

            Object oldValue = cachedValues.get(key);
            Object newValue = getPreferenceValue(key);

            boolean changed = (newValue == null && oldValue != null) || (newValue != null && !newValue.equals(oldValue));

            // If the value has not changed, do nothing
            if (!changed) {
                return;
            }

            cachedValues.put(key, newValue);

            switch (key) {
                case KEY_THEME -> {
                    String getTheme = SettingsDAO.getTheme(sharedPreferences);
                    applySystemNightMode(getTheme);
                }

                case KEY_GENERAL_FONT, KEY_ACCENT_COLOR -> recreate();

                case KEY_DARK_MODE, KEY_NIGHT_ACCENT_COLOR -> {
                    if (ThemeUtils.isNight(getResources())) {
                        recreate();
                    }
                }

                // Add a short delay to have a smooth animation when the setting is a switch button
                default -> AppExecutors.getMainThread().postDelayed(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        recreate();
                    }
                }, 300);
            }
        };

        mPrefs.registerOnSharedPreferenceChangeListener(listener);
        mListenerMap.put(this, listener);
    }

    /**
     * Unregisters the internal listener to avoid memory leaks.
     */
    private void unregisterThemeListener() {
        SharedPreferences.OnSharedPreferenceChangeListener registeredListener = mListenerMap.remove(this);
        if (registeredListener != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(registeredListener);
        }
    }

    /**
     * Retrieves the value of the preference associated with the given key from SharedPreferences,
     * returning a suitable default value based on the key.
     *
     * @param key The preference key to retrieve.
     */
    private Object getPreferenceValue(String key) {
        return switch (key) {
            case KEY_THEME -> SettingsDAO.getTheme(mPrefs);
            case KEY_DARK_MODE -> SettingsDAO.getDarkMode(mPrefs);
            case KEY_GENERAL_FONT -> SettingsDAO.getGeneralFont(mPrefs);
            case KEY_ACCENT_COLOR -> SettingsDAO.getAccentColor(mPrefs);
            case KEY_AUTO_NIGHT_ACCENT_COLOR -> SettingsDAO.isAutoNightAccentColorEnabled(mPrefs);
            case KEY_NIGHT_ACCENT_COLOR -> SettingsDAO.getNightAccentColor(mPrefs);
            case KEY_CARD_BACKGROUND -> SettingsDAO.isCardBackgroundDisplayed(mPrefs);
            case KEY_CARD_BORDER -> SettingsDAO.isCardBorderDisplayed(mPrefs);
            case KEY_FADE_TRANSITIONS -> SettingsDAO.isFadeTransitionsEnabled(mPrefs);
            default -> null;
        };
    }

    protected final DataModel getDataModel() {
        return mDataModel;
    }

    protected final UiDataModel getUiDataModel() {
        return mUiDataModel;
    }

    protected final SharedPreferences getPrefs() {
        return mPrefs;
    }

    protected final DisplayMetrics getDisplayMetrics() {
        return mDisplayMetrics;
    }

}
