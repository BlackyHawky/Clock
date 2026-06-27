// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

/**
 * A {@link BroadcastReceiver} to safely launch {@link TimerService} when a timer expires.
 *
 * <p>Starting with Android 12 (API 31), the system prohibits background applications
 * from starting foreground services directly from an {@link android.app.AlarmManager} service intent.
 * This receiver intercepts the broadcast alarm event, inherits a temporary exemption window
 * from the operating system, and safely forwards the intent to the foreground service.</p>
 */
public class TimerAlertReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(intent);
        serviceIntent.setClass(context, TimerService.class);

        ContextCompat.startForegroundService(context, serviceIntent);
    }
}
