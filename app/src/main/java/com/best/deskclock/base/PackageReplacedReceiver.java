// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.base;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_BLUR_INTENSITY;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;

import androidx.annotation.NonNull;

import com.best.deskclock.alarms.AlarmStateManager;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.TimerDAO;
import com.best.deskclock.utils.LogUtils;

import java.util.Map;

/**
 * BroadcastReceiver triggered when the application package is replaced
 * (typically after an update). This receiver is responsible for restoring
 * all scheduled alarms by re-registering every existing AlarmInstance.
 * <p>
 * Since updating the app invalidates existing PendingIntents, this ensures
 * that all alarms continue to function normally after the update.
 * <p>
 * Note: This receiver must not be directBootAware, as
 * ACTION_MY_PACKAGE_REPLACED is only delivered to receivers running in
 * credential-protected storage.
 */
public class PackageReplacedReceiver extends BroadcastReceiver {

    @SuppressLint({"WakelockTimeout", "Wakelock"})
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            return;
        }

        LogUtils.i("MY_PACKAGE_REPLACED received");

        final PendingResult result = goAsync();
        final PowerManager.WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();

        AppExecutors.getDiskIO().execute(() -> {
            try {
                // Update all the alarm instances
                AlarmStateManager.fixAlarmInstances(context);

                // Update all the timer keys stored in SharedPreferences
                updateTimerKeys(context);

                // Update the blur setting keys stored in SharedPreferences
                migrateBlurSettings(context);
            } finally {
                result.finish();
                wl.release();
                LogUtils.v("PackageReplacedReceiver finished");
            }
        });
    }

    @SuppressLint("ApplySharedPref")
    private void updateTimerKeys(@NonNull Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> allEntries = prefs.getAll();
        boolean hasChanges = false;

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String oldKey = entry.getKey();

            if (oldKey.startsWith("delete_after_use_") && !oldKey.startsWith(TimerDAO.DELETE_AFTER_USE)) {
                String timerId = oldKey.replace("delete_after_use_", "");
                String newKey = TimerDAO.DELETE_AFTER_USE + timerId;
                boolean oldValue = prefs.getBoolean(oldKey, false);

                // Migrate values
                editor.putBoolean(newKey, oldValue);

                // Delete the old keys
                editor.remove(oldKey);
                hasChanges = true;
            } else if (oldKey.startsWith("timer_button_time") && !oldKey.startsWith(TimerDAO.BUTTON_TIME)) {
                String timerId = oldKey.replace("timer_button_time", "");
                String newKey = TimerDAO.BUTTON_TIME + timerId;
                String oldTime = prefs.getString(oldKey, String.valueOf(SettingsDAO.getDefaultTimeToAddToTimer(prefs)));

                // Migrate values
                editor.putString(newKey, oldTime);

                // Delete the old keys
                editor.remove(oldKey);
                hasChanges = true;
            }
        }

        if (hasChanges) {
            editor.commit();
            LogUtils.i("PackageReplacedReceiver - Timer keys cleaned up successfully");
        }
    }

    @SuppressLint("ApplySharedPref")
    private void migrateBlurSettings(@NonNull Context context) {
        SharedPreferences prefs = getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        boolean hasChanges = false;

        if (prefs.contains("key_enable_alarm_blur_effect")) {
            boolean wasEnabled = prefs.getBoolean("key_enable_alarm_blur_effect", false);
            if (!wasEnabled) {
                editor.putInt(KEY_ALARM_BLUR_INTENSITY, 0);
            }

            editor.remove("key_enable_alarm_blur_effect");
            hasChanges = true;
        }

        if (prefs.contains("key_enable_timer_blur_effect")) {
            boolean wasEnabled = prefs.getBoolean("key_enable_timer_blur_effect", false);
            if (!wasEnabled) {
                editor.putInt(KEY_TIMER_BLUR_INTENSITY, 0);
            }

            editor.remove("key_enable_timer_blur_effect");
            hasChanges = true;
        }

        if (prefs.contains("key_enable_screensaver_blur_effect")) {
            boolean wasEnabled = prefs.getBoolean("key_enable_screensaver_blur_effect", false);
            if (!wasEnabled) {
                editor.putInt(KEY_SCREENSAVER_BLUR_INTENSITY, 0);
            }

            editor.remove("key_enable_screensaver_blur_effect");
            hasChanges = true;
        }

        if (hasChanges) {
            editor.commit();
            LogUtils.i("PackageReplacedReceiver - Blur settings migrated successfully");
        }
    }
}
