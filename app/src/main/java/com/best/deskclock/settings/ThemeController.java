// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLUE_GRAY_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BROWN_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.DARK_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.DEFAULT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.GREEN_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.INDIGO_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.LIGHT_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.ORANGE_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PINK_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.RED_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.SYSTEM_THEME;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This class registers ActivityLifecycleCallbacks to collect all activities to a single set.
 * This allows to change the dark mode and the accent color at runtime.
 */
public class ThemeController {
    private static final Set<Activity> activities = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean initialized = false;
    private static DarkMode darkMode = DarkMode.DEFAULT_DARK_MODE;
    private static AccentColor accentColor = AccentColor.DEFAULT;

    /**
     * To initialize this class in the application class.
     */
    public static void initialize(Application application) {
        if (!initialized) {
            initialized = true;
            // Registering the callback allows us to listen and react to the lifecycle of every app activity.
            application.registerActivityLifecycleCallbacks(new ActivityCallbacks());
        }
    }

    /**
     * Store a selected dark mode in the static field and trigger recreation for all the activities.
     * @param darkMode Dark mode to use.
     */
    public static void applyDarkMode(DarkMode darkMode) {
        ThemeController.darkMode = darkMode;
        for (Activity activity : activities) {
            ActivityCompat.recreate(activity);
        }
    }

    /**
     * Store a selected accent color mode in the static field and trigger recreation for all the activities.
     * @param accentColor Accent color to use.
     */
    public static void applyAccentColor(AccentColor accentColor) {
        ThemeController.accentColor = accentColor;
        for (Activity activity : activities) {
            ActivityCompat.recreate(activity);
        }
    }

    private static class ActivityCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            final String getTheme = DataModel.getDataModel().getTheme();
            final String getColor = DataModel.getDataModel().getAccentColor();
            if (Utils.isNight(activity.getResources())) {
                switch (darkMode) {
                    case DEFAULT_DARK_MODE -> {
                        switch (getTheme) {
                            case SYSTEM_THEME ->
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            case LIGHT_THEME ->
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            case DARK_THEME ->
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        }
                    }

                    case AMOLED -> {
                        if (!getTheme.equals(SYSTEM_THEME)) {
                            activity.setTheme(R.style.AmoledTheme);
                        }
                    }
                }
            }

            switch (accentColor) {
                case DEFAULT -> {
                    if (getColor.equals(DEFAULT_ACCENT_COLOR))
                        activity.setTheme(R.style.DefaultColor);
                }
                case BLUE_GRAY -> {
                    if (getColor.equals(BLUE_GRAY_ACCENT_COLOR))
                        activity.setTheme(R.style.BlueGrayAccentColor);
                }
                case BROWN -> {
                    if (getColor.equals(BROWN_ACCENT_COLOR))
                        activity.setTheme(R.style.BrownAccentColor);
                }
                case GREEN -> {
                    if (getColor.equals(GREEN_ACCENT_COLOR))
                        activity.setTheme(R.style.GreenAccentColor);
                }
                case INDIGO -> {
                    if (getColor.equals(INDIGO_ACCENT_COLOR)) {
                        activity.setTheme(R.style.IndigoAccentColor);
                    }
                }
                case ORANGE -> {
                    if (getColor.equals(ORANGE_ACCENT_COLOR))
                        activity.setTheme(R.style.OrangeAccentColor);
                }
                case PINK -> {
                    if (getColor.equals(PINK_ACCENT_COLOR))
                        activity.setTheme(R.style.PinkAccentColor);
                }
                case RED -> {
                    if (getColor.equals(RED_ACCENT_COLOR))
                        activity.setTheme(R.style.RedAccentColor);
                }
            }

            activities.add(activity);
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            activities.remove(activity);
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

    }

    public enum DarkMode {DEFAULT_DARK_MODE, AMOLED}
    public enum AccentColor {DEFAULT, BLUE_GRAY, BROWN, GREEN, INDIGO, ORANGE, PINK, RED}
}
