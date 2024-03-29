// SPDX-License-Identifier: GPL-3.0-only
package com.best.deskclock.settings;

import static com.best.deskclock.settings.SettingsActivity.DARK_THEME;
import static com.best.deskclock.settings.SettingsActivity.LIGHT_THEME;
import static com.best.deskclock.settings.SettingsActivity.SYSTEM_THEME;

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
 * This allows to change the dark mode at runtime.
 */
public class DarkModeController {
    private static final Set<Activity> activities = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean initialized = false;
    private static DarkMode darkMode = DarkMode.DEFAULT_DARK_MODE;

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
        DarkModeController.darkMode = darkMode;
        for (Activity activity : activities) {
            ActivityCompat.recreate(activity);
        }
    }

    private static class ActivityCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            final String getTheme = DataModel.getDataModel().getTheme();
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
                activities.add(activity);
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
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
}
