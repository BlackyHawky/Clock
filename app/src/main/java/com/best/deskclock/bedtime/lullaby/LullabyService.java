/*
 *  Copyright (C) 2024 The OmniROM Project
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
package com.best.deskclock.bedtime.lullaby;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;

import com.best.deskclock.LogUtils;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.bedtime.beddata.DataSaver;
import com.best.music.AbstractMediaKlaxon;
import com.best.music.AbstractPlayerService;

import java.util.Calendar;

public class LullabyService extends AbstractPlayerService {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (action.equals(AbstractPlayerService.ACTION_PLAY) && getKlaxon().getmPlayer() == null) {
            DataSaver saver = DataSaver.getInstance(this);
            // this is saver saves in minutes
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, saver.sleepLength);
            alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), getStopPendingIntent());
        } else if (action.equals(AbstractPlayerService.ACTION_STOP)) {
            alarmManager.cancel(getStopPendingIntent());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private PendingIntent getStopPendingIntent() {
        Intent stopIntent = new Intent(this, getClass());
        stopIntent.setAction(AbstractPlayerService.ACTION_STOP);
        return Utils.pendingServiceIntent(this, stopIntent);
    }

    @Override
    protected AbstractMediaKlaxon getKlaxon() {
        return LullabyKlaxon.getKlaxon();
    }

    @Override
    protected Uri getMusic() {
        DataSaver saver = DataSaver.getInstance(this);
        saver.restore();
        return saver.sleepUri;
    }

    @Override
    protected int stopString() {
        return R.string.stop_lullaby;
    }

    @Override
    protected int notificationTitleString() {
        return R.string.lullaby_playing;
    }

    @Override
    protected int notificationTextString() {
        return -1;
    }
}
