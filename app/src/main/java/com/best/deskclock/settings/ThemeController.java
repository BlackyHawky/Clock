// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This class registers ActivityLifecycleCallbacks to collect all activities to a single set.
 * This allows to change settings at runtime.
 */
public class ThemeController {
    private static final Set<Activity> activities = Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean initialized = false;

    private static Setting settingChanged = Setting.CHANGED;

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
     * Allow all activities to be recreated if a setting has been changed
     * @param settingChanged the value that indicates that the setting has been changed.
     */
    public static void setNewSetting(Setting settingChanged) {
        ThemeController.settingChanged = settingChanged;
        for (Activity activity : activities) {
            ActivityCompat.recreate(activity);
        }
    }

    /**
     * Allow all activities to be recreated with a short delay if a setting has been changed.
     * Used for toggle switches so that their animations are performed correctly.
     * @param settingChanged the value that indicates that the setting has been changed.
     */
    public static void setNewSettingWithDelay(Setting settingChanged) {
        ThemeController.settingChanged = settingChanged;
        for (Activity activity : activities) {
            new Handler().postDelayed(() -> ActivityCompat.recreate(activity), 300);
        }
    }

    private static class ActivityCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
            if (Objects.requireNonNull(settingChanged) == Setting.CHANGED) {
                activities.add(activity);
            }
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

    public enum Setting {CHANGED}

}
