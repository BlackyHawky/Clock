/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.best.deskclock.controller.Controller;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.LogEventTracker;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.SdkUtils;

import java.io.File;
import java.util.Objects;

public class DeskClockApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static DeskClockApplication sInstance;

    private int mStartedActivities = 0;
    private boolean mIsChangingConfiguration = false;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        DataModel.getDataModel().init();
        UiDataModel.getUiDataModel().init();
        Controller.getController().init();
        Controller.getController().addEventTracker(new LogEventTracker());
        Controller.getController().updateShortcuts();

        if (SdkUtils.isAtLeastAndroid8()) {
            NotificationUtils.updateNotificationChannels(this);
        }

        registerActivityLifecycleCallbacks(this);
    }

    public static Context getAppContext() {
        return sInstance;
    }

    /**
     * Returns the default {@link SharedPreferences} instance from the underlying storage context.
     */
    public static SharedPreferences getDefaultSharedPreferences(Context context) {
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

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (mStartedActivities == 0 && !mIsChangingConfiguration) {
            DataModel.getDataModel().setApplicationInForeground(true);
        }

        mIsChangingConfiguration = false;
        mStartedActivities++;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        mStartedActivities--;

        if (mStartedActivities == 0) {
            if (!activity.isChangingConfigurations()) {
                DataModel.getDataModel().setApplicationInForeground(false);
            } else {
                mIsChangingConfiguration = true;
            }
        }
    }

    @Override public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityResumed(@NonNull Activity activity) {}
    @Override public void onActivityPaused(@NonNull Activity activity) {}
    @Override public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
    @Override public void onActivityDestroyed(@NonNull Activity activity) {}

}
