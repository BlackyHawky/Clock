// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.utils.AlarmUtils;

/**
 * Transparent activity that allows a web task to be launched from the notification before dismissing/snoozing the alarm.
 */
public class AlarmActionRoutingActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        Intent serviceIntent = new Intent(intent);
        serviceIntent.setClass(this, AlarmService.class);
        startService(serviceIntent);

        if (intent.getData() != null) {
            long instanceId = AlarmInstance.getId(intent.getData());
            AlarmInstance instance = AlarmInstance.getInstance(getContentResolver(), instanceId);

            if (instance != null) {
                Alarm alarm = Alarm.getAlarm(getContentResolver(), instance.mAlarmId);
                if (alarm != null) {
                    AlarmUtils.launchTask(this, alarm);
                }
            }
        }

        finish();
    }
}
