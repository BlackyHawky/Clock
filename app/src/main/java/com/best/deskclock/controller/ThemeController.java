// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.controller;

import static com.best.deskclock.settings.InterfaceCustomizationFragment.BLACK_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.BLUE_GRAY_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.BROWN_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.DARK_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.GREEN_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.INDIGO_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.KEY_AMOLED_DARK_MODE;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.KEY_DEFAULT_DARK_MODE;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.LIGHT_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.ORANGE_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.PINK_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.PURPLE_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.RED_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.SYSTEM_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationFragment.YELLOW_ACCENT_COLOR;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.collection.ArrayMap;
import androidx.core.app.ActivityCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
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
     * Allow all activities to be recreated if a setting has been changed
     */
    public static void setNewSetting() {
        for (WeakReference<Activity> activityRef : activities.values()) {
            Activity activity = activityRef.get();
            if (activity != null) {
                ActivityCompat.recreate(activity);
            }
        }
    }

    /**
     * Allow all activities to be recreated with a short delay if a setting has been changed.
     * Used for toggle switches so that their animations are performed correctly.
     */
    public static void setNewSettingWithDelay() {
        for (WeakReference<Activity> activityRef : activities.values()) {
            Activity activity = activityRef.get();
            if (activity != null) {
                new Handler().postDelayed(() -> ActivityCompat.recreate(activity), 300);
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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
            String customLanguageCode = DataModel.getDataModel().getCustomLanguageCode();
            Utils.applySpecificLocale(activity, customLanguageCode);
            activity.getResources().updateConfiguration(
                    activity.getResources().getConfiguration(), activity.getResources().getDisplayMetrics());
        }

        /**
         * Apply the theme and the accent color to the activities.
         */
        private void applyThemeAndAccentColor(final Activity activity) {
            final String theme = DataModel.getDataModel().getTheme();
            final String darkMode = DataModel.getDataModel().getDarkMode();
            final String accentColor = DataModel.getDataModel().getAccentColor();
            final boolean isAutoNightAccentColorEnabled = DataModel.getDataModel().isAutoNightAccentColorEnabled();
            final String nightAccentColor = DataModel.getDataModel().getNightAccentColor();

            applyDarkMode(activity, theme, darkMode);

            applyAccentColor(activity, isAutoNightAccentColorEnabled, accentColor, nightAccentColor);

            if (activity instanceof CollapsingToolbarBaseActivity) {
                applyNavigationBarColorForCollapsingToolbar(activity, darkMode);
            } else {
                applyNavigationBarColorForRegularActivity(activity, darkMode);
            }
        }

        private void applyDarkMode(Activity activity, String theme, String darkMode) {
            if (darkMode.equals(KEY_DEFAULT_DARK_MODE)) {
                switch (theme) {
                    case SYSTEM_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                    case LIGHT_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    case DARK_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                }
            } else if (darkMode.equals(KEY_AMOLED_DARK_MODE) && !theme.equals(SYSTEM_THEME) && !theme.equals(LIGHT_THEME)) {
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

        private void applyNavigationBarColorForCollapsingToolbar(Activity activity, String darkMode) {
            if (ThemeUtils.isNight(activity.getResources()) && darkMode.equals(KEY_AMOLED_DARK_MODE)) {
                activity.getWindow().setNavigationBarColor(Color.BLACK);
                activity.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
            } else {
                activity.getWindow().setNavigationBarColor(
                        MaterialColors.getColor(activity, android.R.attr.colorBackground, Color.BLACK));
            }
        }

        private void applyNavigationBarColorForRegularActivity(Activity activity, String darkMode) {
            if (ThemeUtils.isNight(activity.getResources()) && darkMode.equals(KEY_AMOLED_DARK_MODE)) {
                activity.getWindow().setNavigationBarColor(Color.BLACK);
                activity.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
            }
        }

    }

}
