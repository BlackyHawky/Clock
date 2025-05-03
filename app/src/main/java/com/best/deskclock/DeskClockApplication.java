/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.preference.PreferenceManager;

import com.best.deskclock.controller.Controller;
import com.best.deskclock.controller.ThemeController;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.LogEventTracker;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;

import java.io.File;
import java.util.Objects;

public class DeskClockApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context applicationContext;

    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = getApplicationContext();
        final SharedPreferences prefs = getDefaultSharedPreferences(applicationContext);

        ThemeController.initialize(this);
        DataModel.getDataModel().init(applicationContext, prefs);
        UiDataModel.getUiDataModel().init(applicationContext, prefs);
        Controller.getController().setContext(applicationContext);
        Controller.getController().addEventTracker(new LogEventTracker(applicationContext));
        Controller.getController().updateShortcuts();
    }

    public static Context getContext() {
        return applicationContext;
    }

    /**
     * Returns the default {@link SharedPreferences} instance from the underlying storage context.
     */
    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        final Context storageContext;

        if (SdkUtils.isAtLeastAndroid7()) {
            // All N devices have split storage areas. Migrate the existing preferences into the new
            // device encrypted storage area if that has not yet occurred.
            storageContext = context.createDeviceProtectedStorageContext();
            final String name = context.getPackageName() + "_preferences";
            final String prefsFilename = storageContext.getDataDir() + "/shared_prefs/" + name + ".xml";
            final File prefs = new File(Objects.requireNonNull(Uri.parse(prefsFilename).getPath()));

            if (!prefs.exists()) {
                if (!storageContext.moveSharedPreferencesFrom(context, name)) {
                    LogUtils.wtf("Failed to migrate shared preferences");
                }
            }
        } else {
            storageContext = context;
        }
        return PreferenceManager.getDefaultSharedPreferences(storageContext);
    }

}
