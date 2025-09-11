// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.controller;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLACK_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLUE_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLUE_GRAY_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.BROWN_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.DARK_THEME;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_DARK_MODE;
import static com.best.deskclock.settings.PreferencesDefaultValues.GREEN_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.INDIGO_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.best.deskclock.settings.PreferencesDefaultValues.LIGHT_THEME;
import static com.best.deskclock.settings.PreferencesDefaultValues.ORANGE_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.PINK_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.PURPLE_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.RED_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.SYSTEM_THEME;
import static com.best.deskclock.settings.PreferencesDefaultValues.YELLOW_ACCENT_COLOR;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.collection.ArrayMap;
import androidx.core.app.ActivityCompat;

import com.best.deskclock.FirstLaunch;
import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.color.MaterialColors;

import java.lang.ref.WeakReference;

/**
 * This class registers ActivityLifecycleCallbacks to collect all activities to a single set.
 * This allows to change settings at runtime.
 */
public class ThemeController {

    private static final ArrayMap<Integer, WeakReference<Activity>> activities = new ArrayMap<>();

    /**
     * To initialize this class in the application class.
     */
    public static void initialize(Application application) {
        // Registering the callback allows us to listen and react to the lifecycle of every app activity.
        application.registerActivityLifecycleCallbacks(new ActivityCallbacks());
    }

    /**
     * Allow all activities to be recreated with a short delay to have a smooth animation
     * when a setting has been changed.
     * <p>
     * This applies to settings that need to be applied immediately (eg: changing the accent color).
     */
    public static void setNewSettingWithDelay() {
        for (WeakReference<Activity> activityRef : activities.values()) {
            Activity activity = activityRef.get();
            if (activity != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> ActivityCompat.recreate(activity), 300);
            }
        }
    }

    private static class ActivityCallbacks implements Application.ActivityLifecycleCallbacks {

        @Override
        public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            applyThemeAndAccentColor(activity);
            setLocale(activity);
        }

        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
            if (SdkUtils.isAtLeastAndroid10()) {
                // Specifying "activity instanceof AppCompatActivity" is necessary for the
                // screensaver to launch.
                if (activity instanceof AppCompatActivity) {
                    EdgeToEdge.enable((ComponentActivity) activity);
                    activity.getWindow().setNavigationBarContrastEnforced(false);
                }
            } else {
                onActivityPreCreated(activity, savedInstanceState);
                onActivityPostCreated(activity, savedInstanceState);
            }
        }

        @Override
        public void onActivityPostCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            activities.put(activity.hashCode(), new WeakReference<>(activity));
        }

        @Override
        public void onActivityPreDestroyed(@NonNull Activity activity) {
            activities.remove(activity.hashCode());
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            if (SdkUtils.isBeforeAndroid10()) {
                onActivityPreDestroyed(activity);
            }
        }

        // Methods implemented by default
        @Override
        public void onActivityStarted(@NonNull Activity activity) {}

        @Override
        public void onActivityResumed(@NonNull Activity activity) {}

        @Override
        public void onActivityPaused(@NonNull Activity activity) {}

        @Override
        public void onActivityStopped(@NonNull Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

        /**
         * Sets the locale of the given activity based on the custom language stored in the data model.
         * <p>
         * This method applies the selected locale and updates the activity's configuration accordingly.
         * It ensures that the activity is localized to the specified language and the UI is refreshed
         * with the new locale settings.
         */
        private void setLocale(Activity activity) {
            String customLanguageCode = SettingsDAO.getCustomLanguageCode(getDefaultSharedPreferences(activity));
            Utils.applySpecificLocale(activity, customLanguageCode);
            activity.getResources().updateConfiguration(
                    activity.getResources().getConfiguration(), activity.getResources().getDisplayMetrics());
        }

        /**
         * Apply the theme and the accent color to the activities.
         */
        private void applyThemeAndAccentColor(final Activity activity) {
            final SharedPreferences prefs = getDefaultSharedPreferences(activity);
            final String theme = SettingsDAO.getTheme(prefs);
            final String darkMode = SettingsDAO.getDarkMode(prefs);
            final String accentColor = SettingsDAO.getAccentColor(prefs);
            final boolean isAutoNightAccentColorEnabled = SettingsDAO.isAutoNightAccentColorEnabled(prefs);
            final String nightAccentColor = SettingsDAO.getNightAccentColor(prefs);

            applyDarkMode(activity, theme, darkMode);

            applyAccentColor(activity, isAutoNightAccentColorEnabled, accentColor, nightAccentColor);

            if (activity instanceof CollapsingToolbarBaseActivity) {
                applyNavBarAndBackgroundColorsForCollapsingToolbarActivity(activity, darkMode);
            } else if (activity instanceof AppCompatActivity) {
                applyNavBarAndBackgroundColorsForRegularActivity(activity, darkMode);
            }
        }

        private void applyDarkMode(Activity activity, String theme, String darkMode) {
            if (darkMode.equals(DEFAULT_DARK_MODE)) {
                switch (theme) {
                    case SYSTEM_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    case LIGHT_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    case DARK_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
            } else if (darkMode.equals(AMOLED_DARK_MODE) && !theme.equals(SYSTEM_THEME) && !theme.equals(LIGHT_THEME)) {
                activity.setTheme(R.style.AmoledTheme);
            }
        }

        private void applyAccentColor(Activity activity, boolean isAutoNightAccentColorEnabled,
                                      String accentColor, String nightAccentColor) {

            String color = isAutoNightAccentColorEnabled
                    ? accentColor
                    : (ThemeUtils.isNight(activity.getResources()) ? nightAccentColor : accentColor);

            switch (color) {
                case BLACK_ACCENT_COLOR -> activity.setTheme(R.style.BlackAccentColor);
                case BLUE_ACCENT_COLOR -> activity.setTheme(R.style.BlueAccentColor);
                case BLUE_GRAY_ACCENT_COLOR -> activity.setTheme(R.style.BlueGrayAccentColor);
                case BROWN_ACCENT_COLOR -> activity.setTheme(R.style.BrownAccentColor);
                case GREEN_ACCENT_COLOR -> activity.setTheme(R.style.GreenAccentColor);
                case INDIGO_ACCENT_COLOR -> activity.setTheme(R.style.IndigoAccentColor);
                case ORANGE_ACCENT_COLOR -> activity.setTheme(R.style.OrangeAccentColor);
                case PINK_ACCENT_COLOR -> activity.setTheme(R.style.PinkAccentColor);
                case PURPLE_ACCENT_COLOR -> activity.setTheme(R.style.PurpleAccentColor);
                case RED_ACCENT_COLOR -> activity.setTheme(R.style.RedAccentColor);
                case YELLOW_ACCENT_COLOR -> activity.setTheme(R.style.YellowAccentColor);
            }
        }

        /**
         * Applies a color to windows and the navigation bar for activities that extend "CollapsingToolbarBaseActivity".
         * <p>
         * Note: For Android 10+, the color of the navigation bar is ensured by {@systemProperty setNavigationBarContrastEnforced(false)}
         * and by the insets defined in the activities.
         */
        private void applyNavBarAndBackgroundColorsForCollapsingToolbarActivity(Activity activity, String darkMode) {
            if (SdkUtils.isAtLeastAndroid10()) {
                activity.getWindow().setNavigationBarContrastEnforced(false);

                if (ThemeUtils.isNight(activity.getResources()) && darkMode.equals(AMOLED_DARK_MODE)) {
                    activity.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
                }
            } else {
                if (ThemeUtils.isNight(activity.getResources()) && darkMode.equals(AMOLED_DARK_MODE)) {
                    activity.getWindow().setNavigationBarColor(Color.BLACK);
                    activity.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
                } else {
                    activity.getWindow().setNavigationBarColor(
                            MaterialColors.getColor(activity, android.R.attr.colorBackground, Color.BLACK));
                }
            }
        }

        /**
         * Applies a color to windows and the navigation bar for activities that extend "AppCompatActivity".
         * <p>
         * Note: For Android 10+, the color of the navigation bar is ensured by the insets defined
         * in the activities.
         */
        private void applyNavBarAndBackgroundColorsForRegularActivity(Activity activity, String darkMode) {
            if (SdkUtils.isAtLeastAndroid10()) {
                if (ThemeUtils.isNight(activity.getResources()) && darkMode.equals(AMOLED_DARK_MODE)) {
                    activity.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
                }
            } else {
                if (ThemeUtils.isNight(activity.getResources()) && darkMode.equals(AMOLED_DARK_MODE)) {
                    activity.getWindow().setNavigationBarColor(Color.BLACK);
                    activity.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
                } else if (activity instanceof FirstLaunch) {
                    activity.getWindow().setNavigationBarColor(
                            MaterialColors.getColor(activity, android.R.attr.colorBackground, Color.BLACK));
                } else {
                    boolean isPhoneInLandscapeMode = !ThemeUtils.isTablet() && ThemeUtils.isLandscape();
                    activity.getWindow().setNavigationBarColor(
                            MaterialColors.getColor(activity, isPhoneInLandscapeMode
                                    ? android.R.attr.colorBackground
                                    : com.google.android.material.R.attr.colorSurface, Color.BLACK));
                }
            }
        }

    }

}
