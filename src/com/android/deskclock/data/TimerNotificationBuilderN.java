/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.data;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.android.deskclock.HandleDeskClockApiCalls;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.timer.ExpiredTimersActivity;
import com.android.deskclock.timer.TimerService;

import java.util.ArrayList;
import java.util.List;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

/**
 * Builds N-style notification to reflect the latest state of the unexpired timers.
 */
@TargetApi(Build.VERSION_CODES.N)
class TimerNotificationBuilderN implements TimerModel.NotificationBuilder {

    @Override
    public Notification build(Context context, NotificationModel nm, List<Timer> unexpired) {
        final Timer timer = unexpired.get(0);
        final int count = unexpired.size();

        // Compute some values required below.
        final boolean running = timer.isRunning();
        final Resources res = context.getResources();

        final long base = getChronometerBase(timer);
        final String pname = context.getPackageName();
        final RemoteViews content = new RemoteViews(pname, R.layout.chronometer_notif_content);
        content.setChronometerCountDown(R.id.chronometer, true);
        content.setChronometer(R.id.chronometer, base, null, running);

        final List<Notification.Action> actions = new ArrayList<>(2);

        final CharSequence stateText;
        if (count == 1) {
            if (running) {
                // Single timer is running.
                if (TextUtils.isEmpty(timer.getLabel())) {
                    stateText = res.getString(R.string.timer_notification_label);
                } else {
                    stateText = timer.getLabel();
                }

                // Left button: Pause
                final Intent pause = new Intent(context, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_PAUSE_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());

                final Icon icon1 = Icon.createWithResource(context, R.drawable.ic_pause_24dp);
                final CharSequence title1 = res.getText(R.string.timer_pause);
                final PendingIntent intent1 = Utils.pendingServiceIntent(context, pause);
                actions.add(new Notification.Action.Builder(icon1, title1, intent1).build());

                // Right Button: +1 Minute
                final Intent addMinute = new Intent(context, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_ADD_MINUTE_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());

                final Icon icon2 = Icon.createWithResource(context, R.drawable.ic_add_24dp);
                final CharSequence title2 = res.getText(R.string.timer_plus_1_min);
                final PendingIntent intent2 = Utils.pendingServiceIntent(context, addMinute);
                actions.add(new Notification.Action.Builder(icon2, title2, intent2).build());

            } else {
                // Single timer is paused.
                stateText = res.getString(R.string.timer_paused);

                // Left button: Start
                final Intent start = new Intent(context, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_START_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());

                final Icon icon1 = Icon.createWithResource(context, R.drawable.ic_start_24dp);
                final CharSequence title1 = res.getText(R.string.sw_resume_button);
                final PendingIntent intent1 = Utils.pendingServiceIntent(context, start);
                actions.add(new Notification.Action.Builder(icon1, title1, intent1).build());

                // Right Button: Reset
                final Intent reset = new Intent(context, TimerService.class)
                        .setAction(HandleDeskClockApiCalls.ACTION_RESET_TIMER)
                        .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId());

                final Icon icon2 = Icon.createWithResource(context, R.drawable.ic_reset_24dp);
                final CharSequence title2 = res.getText(R.string.sw_reset_button);
                final PendingIntent intent2 = Utils.pendingServiceIntent(context, reset);
                actions.add(new Notification.Action.Builder(icon2, title2, intent2).build());
            }
        } else {
            if (running) {
                // At least one timer is running.
                stateText = res.getString(R.string.timers_in_use, count);
            } else {
                // All timers are paused.
                stateText = res.getString(R.string.timers_stopped, count);
            }

            final Intent reset = TimerService.createResetUnexpiredTimersIntent(context);

            final Icon icon1 = Icon.createWithResource(context, R.drawable.ic_reset_24dp);
            final CharSequence title1 = res.getText(R.string.timer_reset_all);
            final PendingIntent intent1 = Utils.pendingServiceIntent(context, reset);
            actions.add(new Notification.Action.Builder(icon1, title1, intent1).build());
        }

        content.setTextViewText(R.id.state, stateText);

        // Intent to load the app and show the timer when the notification is tapped.
        final Intent showApp = new Intent(context, HandleDeskClockApiCalls.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setAction(HandleDeskClockApiCalls.ACTION_SHOW_TIMERS)
                .putExtra(HandleDeskClockApiCalls.EXTRA_TIMER_ID, timer.getId())
                .putExtra(HandleDeskClockApiCalls.EXTRA_EVENT_LABEL, R.string.label_notification);

        final PendingIntent pendingShowApp = PendingIntent.getActivity(context, 0, showApp,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Builder(context)
                .setOngoing(true)
                .setLocalOnly(true)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setCustomContentView(content)
                .setContentIntent(pendingShowApp)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_ALARM)
                .setSmallIcon(R.drawable.stat_notify_timer)
                .setGroup(nm.getTimerNotificationGroupKey())
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .setActions(actions.toArray(new Notification.Action[actions.size()]))
                .setColor(ContextCompat.getColor(context, R.color.default_background))
                .build();
    }

    @Override
    public Notification buildHeadsUp(Context context, List<Timer> expired) {
        final Timer timer = expired.get(0);

        // First action intent is to reset all timers.
        final Icon icon1 = Icon.createWithResource(context, R.drawable.ic_stop_24dp);
        final Intent reset = TimerService.createResetExpiredTimersIntent(context);
        final PendingIntent intent1 = Utils.pendingServiceIntent(context, reset);

        // Generate some descriptive text, a title, and an action name based on the timer count.
        final CharSequence stateText;
        final int count = expired.size();
        final List<Notification.Action> actions = new ArrayList<>(2);
        if (count == 1) {
            final String label = timer.getLabel();
            if (TextUtils.isEmpty(label)) {
                stateText = context.getString(R.string.timer_times_up);
            } else {
                stateText = label;
            }

            // Left button: Reset single timer
            final CharSequence title1 = context.getString(R.string.timer_stop);
            actions.add(new Notification.Action.Builder(icon1, title1, intent1).build());

            // Right button: Add minute
            final Intent addTime = TimerService.createAddMinuteTimerIntent(context, timer.getId());
            final PendingIntent intent2 = Utils.pendingServiceIntent(context, addTime);
            final Icon icon2 = Icon.createWithResource(context, R.drawable.ic_add_24dp);
            final CharSequence title2 = context.getString(R.string.timer_plus_1_min);
            actions.add(new Notification.Action.Builder(icon2, title2, intent2).build());

        } else {
            stateText = context.getString(R.string.timer_multi_times_up, count);

            // Left button: Reset all timers
            final CharSequence title1 = context.getString(R.string.timer_stop_all);
            actions.add(new Notification.Action.Builder(icon1, title1, intent1).build());
        }

        final long base = getChronometerBase(timer);

        final String pname = context.getPackageName();
        final RemoteViews contentView = new RemoteViews(pname, R.layout.chronometer_notif_content);
        contentView.setChronometerCountDown(R.id.chronometer, true);
        contentView.setChronometer(R.id.chronometer, base, null, true);
        contentView.setTextViewText(R.id.state, stateText);

        // Content intent shows the timer full screen when clicked.
        final Intent content = new Intent(context, ExpiredTimersActivity.class);
        final PendingIntent contentIntent = Utils.pendingActivityIntent(context, content);

        // Full screen intent has flags so it is different than the content intent.
        final Intent fullScreen = new Intent(context, ExpiredTimersActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        final PendingIntent pendingFullScreen = Utils.pendingActivityIntent(context, fullScreen);

        return new Notification.Builder(context)
                .setOngoing(true)
                .setLocalOnly(true)
                .setShowWhen(false)
                .setAutoCancel(false)
                .setContentIntent(contentIntent)
                .setCustomContentView(contentView)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setColor(ContextCompat.getColor(context, R.color.default_background))
                .setSmallIcon(R.drawable.stat_notify_timer)
                .setFullScreenIntent(pendingFullScreen, true)
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .setActions(actions.toArray(new Notification.Action[actions.size()]))
                .build();
    }

    /**
     * @param timer the timer on which to base the chronometer display
     * @return the time at which the chronometer will/did reach 0:00 in realtime
     */
    private static long getChronometerBase(Timer timer) {
        // The in-app timer display rounds *up* to the next second for positive timer values. Mirror
        // that behavior in the notification's Chronometer by padding in an extra second as needed.
        final long remaining = timer.getRemainingTime();
        final long adjustedRemaining = remaining < 0 ? remaining : remaining + SECOND_IN_MILLIS;

        // Chronometer will/did reach 0:00 adjustedRemaining milliseconds from now.
        return SystemClock.elapsedRealtime() + adjustedRemaining;
    }
}
