// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.best.deskclock.alarms.AlarmStateManager;
import com.best.deskclock.utils.LogUtils;

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
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            return;
        }

        LogUtils.i("MY_PACKAGE_REPLACED received");

        final PendingResult result = goAsync();
        final PowerManager.WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();

        AsyncHandler.post(() -> {
            try {
                // Update all the alarm instances
                AlarmStateManager.fixAlarmInstances(context);
            } finally {
                result.finish();
                wl.release();
                LogUtils.v("PackageReplacedReceiver finished");
            }
        });
    }
}
