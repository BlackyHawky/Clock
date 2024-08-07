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
package com.best.music;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.best.deskclock.NotificationUtils;
import com.best.deskclock.R;

public abstract class AbstractPlayerService extends Service {

    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_PAUSE = "PAUSE";
    public static final String ACTION_STOP = "STOP";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:
                    if (getKlaxon().getmPlayer() != null) {
                        getKlaxon().play();
                    } else {
                        getKlaxon().start(this, getMusic());
                    }
                    break;
                case ACTION_PAUSE:
                    getKlaxon().pause();
                    break;
                case ACTION_STOP:
                    getKlaxon().stop();
                    stopForeground(true);
                    stopSelf();
                    break;
            }
        }

        // Show the notification
        startForeground(1, buildNotification());

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


    }

    private Notification buildNotification() {
        // Create a notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createChannel(this, NotificationUtils.BEDTIME_NOTIFICATION_CHANNEL_ID);
        }

        // Create a notification builder
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NotificationUtils.BEDTIME_NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setSmallIcon(R.drawable.ic_media_song);
        notificationBuilder.setContentTitle(getString(notificationTitleString()));

        if (notificationTextString() != -1) {
            notificationBuilder.setContentText(getString(notificationTextString()));
        }

        Intent stopIntent = new Intent(this, getClass());
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);
        notificationBuilder.addAction(R.drawable.ic_fab_stop, getString(stopString()), stopPendingIntent);
        return notificationBuilder.build();
    }

    protected abstract AbstractMediaKlaxon getKlaxon();
    protected abstract Uri getMusic();
    @StringRes
    protected abstract int stopString();
    @StringRes
    protected abstract int notificationTitleString();

    /**
     * return -1 if no text should be shown
     * @return string res
     */
    protected abstract int notificationTextString();
}