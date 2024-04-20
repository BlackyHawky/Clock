/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.bedtime;

import static com.best.deskclock.NotificationUtils.BEDTIME_NOTIFICATION_CHANNEL_ID;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;

import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.best.deskclock.AlarmUtils;
import com.best.deskclock.DeskClock;
import com.best.deskclock.LogUtils;
import com.best.deskclock.NotificationUtils;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.bedtime.beddata.DataSaver;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;


public final class BedtimeService extends Service {

    private static final String ACTION_PREFIX = "com.best.deskclock.action.";

    public static final String ACTION_SHOW_BEDTIME = ACTION_PREFIX + "SHOW_BEDTIME";

    // Notification
    public static final String ACTION_BED_REMIND_NOTIF = ACTION_PREFIX + "BED_REMIND_NOTIF";

    // starts everything
    public static final String ACTION_LAUNCH_BEDTIME = ACTION_PREFIX + "LAUNCH_BEDTIME";
    private static final String ACTION_BEDTIME_CANCEL = ACTION_PREFIX + "BEDTIME_STOP";
    private static final String ACTION_BEDTIME_PAUSE = ACTION_PREFIX + "BEDTIME_PAUSE";

    private static final int notifId = 1237449874; // we may need a proper id

    Bitmap originalBitmap;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
        DataSaver saver = DataSaver.getInstance(context);
        String action = intent.getAction();
        switch (action) {
            case ACTION_BED_REMIND_NOTIF -> showRemindNotification(context);
            case ACTION_LAUNCH_BEDTIME -> startBed(context, saver);
            case ACTION_BEDTIME_CANCEL -> stopBed(context, saver);
            case ACTION_BEDTIME_PAUSE -> {
                stopBed(context, saver);
                AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
                Calendar c = Calendar.getInstance();
                c.setTime(new Date());
                c.add(Calendar.MINUTE, 30);
                am.setExact(AlarmManager.RTC, c.getTimeInMillis(), getPendingIntent(context, ACTION_LAUNCH_BEDTIME));
                String txt = AlarmUtils.getFormattedTime(context, c.getTimeInMillis());
                txt = context.getString(R.string.bedtime_notification_resume, txt);
                showPausedNotification(context, txt);
            }
        }
        return START_NOT_STICKY;
    }

    public static void scheduleBed(Context context, DataSaver saver, String action) {
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        cancelBed(context, action);
        am.setExact(AlarmManager.RTC, getNextBedtime(saver, action), getPendingIntent(context, action));
    }

    public static void cancelBed(Context context, String action) {
        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        am.cancel(getPendingIntent(context, action));
    }

    private static PendingIntent getPendingIntent(Context context, String action) {
        Intent i = new Intent(context, BedtimeService.class);
        i.setAction(action);
        return PendingIntent.getService(context, 0, i,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    //TODO: what if someone goes to bed after 12 am
    private static long getNextBedtime(DataSaver saver, String action) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, saver.hour);
        calendar.set(Calendar.MINUTE, saver.minutes);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // Check if the calculated time is in the past
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            // If it's in the past, add one day to the calendar to get the next occurrence
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // there currently are only two actions in which it makes sense to use this: remind notif and bed launch
        if (action.equals(ACTION_BED_REMIND_NOTIF)) {
            calendar.add(Calendar.MINUTE, -saver.notifShowTime);
        }

        // handle weekdays
        for (int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); !saver.daysOfWeek.isBitOn(dayOfWeek);) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        }
        return calendar.getTimeInMillis();
    }

    private static void showRemindNotification(Context context) {
        LogUtils.v("Displaying upcomming notif:" + "Bed");

        DataSaver saver = DataSaver.getInstance(context);
        String h = Integer.toString(saver.hour);
        String ending = "";
        if (!DataModel.getDataModel().is24HourFormat()) {
            h = saver.hour > 12 ? Integer.toString(saver.hour - 12) : Integer.toString(saver.hour);
            if (saver.hour > 11) {
                ending = " PM";
            } else {
                ending = " AM";
            }
        }
        String bedtime = h + ":" + saver.minutes + ending;

        Alarm alarm = Alarm.getAlarmByLabel(context.getApplicationContext().getContentResolver(), BedtimeFragment.BEDLABEL);
        if (alarm == null) {
            return;
        }
        int minDiff = alarm.minutes - saver.minutes;
        int hDiff = alarm.hour + 24 - saver.hour;
        if (minDiff < 0) {
            hDiff = hDiff - 1;
            minDiff = 60 + minDiff;
        }
        String diff;
        if (minDiff == 0) {
            diff = Utils.getNumberFormattedQuantityString(context, R.plurals.hours, hDiff);
        } else {
            diff = context.getString(R.string.bedtime_and, Utils.getNumberFormattedQuantityString(context, R.plurals.hours, hDiff), Utils.getNumberFormattedQuantityString(context, R.plurals.minutes, minDiff));
        }

        String wake;
        if (!DataModel.getDataModel().is24HourFormat()) {
            h = alarm.hour > 12 ? Integer.toString(alarm.hour - 12) : Integer.toString(alarm.hour);
            if (alarm.hour > 11) {
                ending = " PM";
            } else {
                ending = " AM";
            }
        }
        wake = h + ":" + alarm.minutes + ending;

        // Intent to load the app when the notification is tapped.
        @StringRes final int eventLabel = R.string.label_notification;
        final Intent showApp = new Intent(context, DeskClock.class)
                .setAction(ACTION_SHOW_BEDTIME)
                .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, BEDTIME_NOTIFICATION_CHANNEL_ID)
                .setShowWhen(false)
                .setContentTitle(context.getString(R.string.bedtime_reminder_notification_title, bedtime))
                .setContentText(context.getString(R.string.bedtime_reminder_notification_text, wake, diff))
                .setContentIntent(pendingShowApp)
                .setColor(context.getColor(R.color.md_theme_primary))
                .setSmallIcon(R.drawable.ic_moon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createChannel(context, BEDTIME_NOTIFICATION_CHANNEL_ID);
        }
        final Notification notification = builder.build();
        nm.cancel(notifId);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Always false, because notification activation is always checked when the application is started.
            return;
        }

        nm.notify(notifId, notification);
    }

    private static void showLaunchNotification(Context context, String text) {
        LogUtils.v("Displaying upcomming notif:" + "Bed");

        // Intent to load the app when the notification is tapped.
        @StringRes final int eventLabel = R.string.label_notification;
        final Intent showApp = new Intent(context, DeskClock.class)
                .setAction(ACTION_SHOW_BEDTIME)
                .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, BEDTIME_NOTIFICATION_CHANNEL_ID)
                .setShowWhen(false)
                .setContentTitle(context.getString(R.string.bedtime_notification_title))
                .setContentText(text)
                .setContentIntent(pendingShowApp)
                .setColor(context.getColor(R.color.md_theme_primary))
                .setSmallIcon(R.drawable.ic_tab_bedtime)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        Intent pause = new Intent(context, BedtimeService.class);
        pause.setAction(ACTION_BEDTIME_PAUSE);
        builder.addAction(R.drawable.ic_fab_pause, context.getString(R.string.bedtime_notification_action_pause),
                PendingIntent.getService(context, 0, pause, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent off = new Intent(context, BedtimeService.class);
        off.setAction(ACTION_BEDTIME_CANCEL);
        //TODO: we need a proper icon
        builder.addAction(R.drawable.ic_reset, context.getString(R.string.bedtime_notification_action_turn_off), PendingIntent.getService(context, 0,
                off, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createChannel(context, BEDTIME_NOTIFICATION_CHANNEL_ID);
        }
        final Notification notification = builder.build();
        nm.cancel(notifId);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Always false, because notification activation is always checked when the application is started.
            return;
        }

        nm.notify(notifId, notification);
    }

    private static void showPausedNotification(Context context, String text) {
        LogUtils.v("Displaying upcomming notif:" + "Bed");

        // Intent to load the app when the notification is tapped.
        @StringRes final int eventLabel = R.string.label_notification;
        final Intent showApp = new Intent(context, DeskClock.class)
                .setAction(ACTION_SHOW_BEDTIME)
                .putExtra(Events.EXTRA_EVENT_LABEL, eventLabel);

        final PendingIntent pendingShowApp = Utils.pendingActivityIntent(context, showApp);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, BEDTIME_NOTIFICATION_CHANNEL_ID)
                .setShowWhen(false)
                .setContentTitle(context.getString(R.string.bedtime_paused_notification_title))
                .setContentText(text)
                .setContentIntent(pendingShowApp)
                .setColor(context.getColor(R.color.md_theme_primary))
                .setSmallIcon(R.drawable.ic_moon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(true);

        Intent it = new Intent(context, BedtimeService.class);
        it.setAction(ACTION_LAUNCH_BEDTIME);
        builder.addAction(R.drawable.ic_fab_play, context.getString(R.string.bedtime_notification_resume_action), PendingIntent.getService(context, notifId,
                it, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createChannel(context, BEDTIME_NOTIFICATION_CHANNEL_ID);
        }
        final Notification notification = builder.build();
        nm.cancel(notifId);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            // Always false, because notification activation is always checked when the application is started.
            return;
        }

        nm.notify(notifId, notification);
    }

    public static void cancelNotification(Context context) {
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        nm.cancel(notifId);
    }

    private void startBed(Context context, DataSaver saver) {
        cancelBed(context, ACTION_LAUNCH_BEDTIME);
        cancelBed(context, ACTION_BED_REMIND_NOTIF);
        cancelNotification(context);
        if (saver.notifShowTime != -1) {
            scheduleBed(context, saver, ACTION_BED_REMIND_NOTIF);
        }
        // hope activating needs at least a millisecond
        scheduleBed(context, saver, ACTION_LAUNCH_BEDTIME);

        String txt = "";
        if (saver.dimWall && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            boolean success = false;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                // Get the WallpaperManager
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);

                // Get the current wallpaper as a Bitmap
                originalBitmap = BitmapFactory.decodeFileDescriptor(wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM).getFileDescriptor());

                // Apply the dim effect to the current wallpaper
                int width = originalBitmap.getWidth();
                int height = originalBitmap.getHeight();

                // Create a new Bitmap with the same width and height
                Bitmap dimmedBitmap = Bitmap.createBitmap(width, height, originalBitmap.getConfig());

                // Create a Canvas with the new Bitmap to apply the effect
                Canvas canvas = new Canvas(dimmedBitmap);

                // Create a Paint object with a ColorMatrixColorFilter to apply the dim effect
                Paint paint = new Paint();
                ColorMatrix colorMatrix = new ColorMatrix();
                colorMatrix.setSaturation(0); // Set saturation to 0 to make the image grayscale
                colorMatrix.setScale(0.7f, 0.7f, 0.7f, 1f); // Reduce RGB channels to dim the image
                paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));

                // Draw the original Bitmap onto the Canvas with the Paint
                canvas.drawBitmap(originalBitmap, 0, 0, paint);

                // Save the dimmed wallpaper to app storage
                String fileName = "dimmed_wallpaper.png";
                File directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (directory != null) {
                    File file = new File(directory, fileName);

                    try {
                        FileOutputStream fos = new FileOutputStream(file);
                        dimmedBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.close();
                        wallpaperManager.setBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
                        success = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (success) {
                txt = context.getString(R.string.bedtime_screen_option_description);
            }
        }
        if (saver.doNotDisturb) {
            NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            if (nm.isNotificationPolicyAccessGranted()) {
                LogUtils.d("has permissions");
            } else {
                LogUtils.d("does not have permissions");
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
            }
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            if (txt.isEmpty()) {
                String INPUT = context.getString(R.string.bedtime_notification_dnd_description);
                txt = INPUT.substring(0, 1).toUpperCase() + INPUT.substring(1);
            } else {
                txt = context.getString(R.string.bedtime_and, txt, context.getString(R.string.bedtime_notification_dnd_description));
            }
        }
        if (saver.doNotDisturb || saver.dimWall) {

            Alarm alarm = Alarm.getAlarmByLabel(context.getApplicationContext().getContentResolver(), BedtimeFragment.BEDLABEL);
            String ending = "";
            if (alarm == null) {
                return;
            }
            if (!DataModel.getDataModel().is24HourFormat()) {
                if (alarm.hour > 11) {
                    ending = " PM";
                } else {
                    ending = " AM";
                }
            }
            String wake = alarm.hour > 12 ? Integer.toString(alarm.hour - 12) : alarm.hour + ":" + alarm.minutes + ending;
            txt = context.getString(R.string.bedtime_notification_until, txt, wake);
            showLaunchNotification(context, txt);

            AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            Intent off = new Intent(context, BedtimeService.class);
            off.setAction(ACTION_BEDTIME_CANCEL);

            am.setExact(AlarmManager.RTC,
                    Objects.requireNonNull(AlarmInstance.getNextUpcomingInstanceByAlarmId(context.getContentResolver(),
                    alarm.id)).getAlarmTime().getTimeInMillis(),
                    PendingIntent.getService(context, 0, off,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
    }

    private void stopBed(Context context, DataSaver saver) {
        if (saver.dimWall && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {

            // Get the WallpaperManager
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
            try {
                wallpaperManager.setBitmap(originalBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (saver.doNotDisturb) {
            NotificationManager nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            if (nm.isNotificationPolicyAccessGranted()) {
                LogUtils.d("has permissions");
            } else {
                LogUtils.d("does not have permissions");
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
            }
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
        }
        cancelNotification(context);
    }

}