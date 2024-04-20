/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import androidx.preference.PreferenceManager;

import com.best.deskclock.controller.Controller;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.LogEventTracker;
import com.best.deskclock.settings.DarkModeController;
import com.best.deskclock.uidata.UiDataModel;

import java.io.File;

public class DeskClockApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        final Context applicationContext = getApplicationContext();
        final SharedPreferences prefs = getDefaultSharedPreferences(applicationContext);

        DarkModeController.initialize(this);
        DataModel.getDataModel().init(applicationContext, prefs);
        UiDataModel.getUiDataModel().init(applicationContext, prefs);
        Controller.getController().setContext(applicationContext);
        Controller.getController().addEventTracker(new LogEventTracker(applicationContext));
    }

    /**
     * Returns the default {@link SharedPreferences} instance from the underlying storage context.
     */
    private static SharedPreferences getDefaultSharedPreferences(Context context) {
        final Context storageContext;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // All N devices have split storage areas. Migrate the existing preferences into the new
            // device encrypted storage area if that has not yet occurred.
            storageContext = context.createDeviceProtectedStorageContext();
            final String name = context.getPackageName() + "_preferences";
            final String prefsFilename = storageContext.getDataDir() + "/shared_prefs/" + name + ".xml";
            final File prefs = new File(Uri.parse(prefsFilename).getPath());

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
