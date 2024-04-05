/*
 *  Copyright (C) 2024 Linus Stubbe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.best.deskclock.bedtime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.bedtime.beddata.DataSaver;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.ringtone.BaseKlaxon;
import com.best.deskclock.ringtone.ui.RingtonePickerActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class SleepActivity extends AppCompatActivity {

    private AlarmManager mAlarmManager;
    private long mRemainingTime = 0;
    private long mTrigger = 0;
    private static String STOP = "SLEEP_ACTIVITY.stop";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sleep_activity);

        final MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable title and home button by default
        final ActionBar actionBar = getSupportActionBar();
        DataSaver saver = DataSaver.getInstance(this);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(DataModel.getDataModel().getRingtoneTitle(saver.sleepUri));
        }
        FloatingActionButton toggle = findViewById(R.id.play_pause);
        toggle.setImageResource(R.drawable.ic_fab_pause);
        toggle.setOnClickListener(v -> {
            toggle.setImageResource(SleepKlaxon.toggle(SleepActivity.this, saver.sleepUri));
            toggle.setColorFilter(getColor(R.color.md_theme_surface));
            mTrigger = getTriggerTime();
            mAlarmManager.set(AlarmManager.RTC, mTrigger, getPendingIntent());
        });
        Button sleepChoose = findViewById(R.id.sleep_choose);
        sleepChoose.setOnClickListener(v -> startActivity(RingtonePickerActivity.createSleepSoundPickerIntent(SleepActivity.this)));
        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mRemainingTime = saver.sleepLength * 1000L;
    }

    @Override
    public void onPause() {
        super.onPause();
        BaseKlaxon.stop(this);
    }

    private PendingIntent getPendingIntent() {
        Intent i = new Intent(this, mReceiver.getClass());
        i.setAction(STOP);
        return PendingIntent.getService(this, 0, i,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(STOP)) {
                SleepActivity.this.finish();
            }
        }
    };

    private long getTriggerTime() {
        return Utils.now() + mRemainingTime;
    }
}
