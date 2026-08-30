/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static com.best.deskclock.settings.PreferencesDefaultValues.DARK_THEME;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEBUG_LANGUAGE_CODE;
import static com.best.deskclock.settings.PreferencesDefaultValues.LIGHT_THEME;
import static com.best.deskclock.settings.PreferencesDefaultValues.PURPLE_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.RED_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.SYSTEM_THEME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_LANGUAGE_CODE;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;

import com.best.deskclock.controller.Controller;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.LogEventTracker;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.SdkUtils;

import java.io.File;
import java.util.Objects;

public class DeskClockApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static DeskClockApplication sInstance;
    private DataModel mDataModel;

    private int mStartedActivities = 0;
    private boolean mIsChangingConfiguration = false;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;
        mDataModel = DataModel.getDataModel();
        Controller controller = Controller.getController();
        SharedPreferences prefs = getDefaultSharedPreferences(this);

        initDebugAndNightlyDefaults(prefs);

        String theme = SettingsDAO.getTheme(prefs);
        applySystemNightMode(theme);

        mDataModel.init(prefs);
        UiDataModel.getUiDataModel().init(prefs);
        controller.init(this, prefs);
        controller.addEventTracker(new LogEventTracker());
        controller.updateShortcuts();

        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.updateNotificationChannels(this);
        }

        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (mStartedActivities == 0 && !mIsChangingConfiguration) {
            mDataModel.setApplicationInForeground(true);
        }

        mIsChangingConfiguration = false;
        mStartedActivities++;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        mStartedActivities--;

        if (mStartedActivities == 0) {
            if (!activity.isChangingConfigurations()) {
                mDataModel.setApplicationInForeground(false);
            } else {
                mIsChangingConfiguration = true;
            }
        }
    }

    @Override public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override public void onActivityResumed(@NonNull Activity activity) {}
    @Override public void onActivityPaused(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}

    private void initDebugAndNightlyDefaults(@NonNull SharedPreferences prefs) {
        if (!prefs.contains(KEY_ACCENT_COLOR)) {
            if (BuildConfig.IS_DEBUG_BUILD) {
                prefs.edit().putString(KEY_ACCENT_COLOR, RED_ACCENT_COLOR).apply();
            } else if (BuildConfig.IS_NIGHTLY_BUILD) {
                prefs.edit().putString(KEY_ACCENT_COLOR, PURPLE_ACCENT_COLOR).apply();
            }
        }

        if (!prefs.contains(KEY_LANGUAGE_CODE)) {
            if (BuildConfig.IS_DEBUG_BUILD || BuildConfig.IS_NIGHTLY_BUILD) {
                prefs.edit().putString(KEY_LANGUAGE_CODE, DEBUG_LANGUAGE_CODE).apply();
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(DEBUG_LANGUAGE_CODE));
            }
        }
    }

    private void applySystemNightMode(@NonNull String theme) {
        switch (theme) {
            case SYSTEM_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            case LIGHT_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            case DARK_THEME -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    public static Context getAppContext() {
        return sInstance;
    }

    /**
     * Returns the default {@link SharedPreferences} instance from the underlying storage context.
     */
    public static SharedPreferences getDefaultSharedPreferences(@NonNull Context context) {
        final Context appContext = context.getApplicationContext();
        final Context storageContext;

        if (SdkUtils.isAtLeastAndroid7()) {
            // All N devices have split storage areas. Migrate the existing preferences into the new
            // device encrypted storage area if that has not yet occurred.
            storageContext = appContext.createDeviceProtectedStorageContext();
            final String name = appContext.getPackageName() + "_preferences";
            final String prefsFilename = storageContext.getDataDir() + "/shared_prefs/" + name + ".xml";
            final File prefs = new File(Objects.requireNonNull(Uri.parse(prefsFilename).getPath()));

            if (!prefs.exists()) {
                if (!storageContext.moveSharedPreferencesFrom(appContext, name)) {
                    LogUtils.wtf("Failed to migrate shared preferences");
                }
            }
        } else {
            storageContext = appContext;
        }

        return PreferenceManager.getDefaultSharedPreferences(storageContext);
    }

}
