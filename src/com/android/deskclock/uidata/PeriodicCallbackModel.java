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

package com.android.deskclock.uidata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;

import com.android.deskclock.LogUtils;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static android.content.Intent.ACTION_DATE_CHANGED;
import static android.content.Intent.ACTION_TIMEZONE_CHANGED;
import static android.content.Intent.ACTION_TIME_CHANGED;
import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.android.deskclock.Utils.enforceMainLooper;
import static java.util.Calendar.DATE;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

/**
 * All callbacks to be delivered at requested times on the main thread if the application is in the
 * foreground when the callback time passes.
 */
final class PeriodicCallbackModel {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("Periodic");

    @VisibleForTesting
    enum Period {MINUTE, QUARTER_HOUR, HOUR, MIDNIGHT}

    private static final long QUARTER_HOUR_IN_MILLIS = 15 * MINUTE_IN_MILLIS;

    private static Handler sHandler;

    /** Reschedules callbacks when the device time changes. */
    @SuppressWarnings("FieldCanBeLocal")
    private final BroadcastReceiver mTimeChangedReceiver = new TimeChangedReceiver();

    private final List<PeriodicRunnable> mPeriodicRunnables = new CopyOnWriteArrayList<>();

    PeriodicCallbackModel(Context context) {
        // Reschedules callbacks when the device time changes.
        final IntentFilter timeChangedBroadcastFilter = new IntentFilter();
        timeChangedBroadcastFilter.addAction(ACTION_TIME_CHANGED);
        timeChangedBroadcastFilter.addAction(ACTION_DATE_CHANGED);
        timeChangedBroadcastFilter.addAction(ACTION_TIMEZONE_CHANGED);
        context.registerReceiver(mTimeChangedReceiver, timeChangedBroadcastFilter);
    }

    /**
     * @param runnable to be called every minute
     * @param offset an offset applied to the minute to control when the callback occurs
     */
    void addMinuteCallback(Runnable runnable, long offset) {
        addPeriodicCallback(runnable, Period.MINUTE, offset);
    }

    /**
     * @param runnable to be called every quarter-hour
     * @param offset an offset applied to the quarter-hour to control when the callback occurs
     */
    void addQuarterHourCallback(Runnable runnable, long offset) {
        addPeriodicCallback(runnable, Period.QUARTER_HOUR, offset);
    }

    /**
     * @param runnable to be called every hour
     * @param offset an offset applied to the hour to control when the callback occurs
     */
    void addHourCallback(Runnable runnable, long offset) {
        addPeriodicCallback(runnable, Period.HOUR, offset);
    }

    /**
     * @param runnable to be called every midnight
     * @param offset an offset applied to the midnight to control when the callback occurs
     */
    void addMidnightCallback(Runnable runnable, long offset) {
        addPeriodicCallback(runnable, Period.MIDNIGHT, offset);
    }

    /**
     * @param runnable to be called periodically
     */
    private void addPeriodicCallback(Runnable runnable, Period period, long offset) {
        final PeriodicRunnable periodicRunnable = new PeriodicRunnable(runnable, period, offset);
        mPeriodicRunnables.add(periodicRunnable);
        periodicRunnable.schedule();
    }

    /**
     * @param runnable to no longer be called periodically
     */
    void removePeriodicCallback(Runnable runnable) {
        for (PeriodicRunnable periodicRunnable : mPeriodicRunnables) {
            if (periodicRunnable.mDelegate == runnable) {
                periodicRunnable.unSchedule();
                mPeriodicRunnables.remove(periodicRunnable);
                return;
            }
        }
    }

    /**
     * Return the delay until the given {@code period} elapses adjusted by the given {@code offset}.
     *
     * @param now the current time
     * @param period the frequency with which callbacks should be given
     * @param offset an offset to add to the normal period; allows the callback to be made relative
     *      to the normally scheduled period end
     * @return the time delay from {@code now} to schedule the callback
     */
    @VisibleForTesting
    static long getDelay(long now, Period period, long offset) {
        final long periodStart = now - offset;

        switch (period) {
            case MINUTE:
                final long lastMinute = periodStart - (periodStart % MINUTE_IN_MILLIS);
                final long nextMinute = lastMinute + MINUTE_IN_MILLIS;
                return nextMinute - now + offset;

            case QUARTER_HOUR:
                final long lastQuarterHour = periodStart - (periodStart % QUARTER_HOUR_IN_MILLIS);
                final long nextQuarterHour = lastQuarterHour + QUARTER_HOUR_IN_MILLIS;
                return nextQuarterHour - now + offset;

            case HOUR:
                final long lastHour = periodStart - (periodStart % HOUR_IN_MILLIS);
                final long nextHour = lastHour + HOUR_IN_MILLIS;
                return nextHour - now + offset;

            case MIDNIGHT:
                final Calendar nextMidnight = Calendar.getInstance();
                nextMidnight.setTimeInMillis(periodStart);
                nextMidnight.add(DATE, 1);
                nextMidnight.set(HOUR_OF_DAY, 0);
                nextMidnight.set(MINUTE, 0);
                nextMidnight.set(SECOND, 0);
                nextMidnight.set(MILLISECOND, 0);
                return nextMidnight.getTimeInMillis() - now + offset;

            default:
                throw new IllegalArgumentException("unexpected period: " + period);
        }
    }

    private static Handler getHandler() {
        enforceMainLooper();
        if (sHandler == null) {
            sHandler = new Handler();
        }
        return sHandler;
    }

    /**
     * Schedules the execution of the given delegate Runnable at the next callback time.
     */
    private static final class PeriodicRunnable implements Runnable {

        private final Runnable mDelegate;
        private final Period mPeriod;
        private final long mOffset;

        public PeriodicRunnable(Runnable delegate, Period period, long offset) {
            mDelegate = delegate;
            mPeriod = period;
            mOffset = offset;
        }

        @Override
        public void run() {
            LOGGER.i("Executing periodic callback for %s because the period ended", mPeriod);
            mDelegate.run();
            schedule();
        }

        private void runAndReschedule() {
            LOGGER.i("Executing periodic callback for %s because the time changed", mPeriod);
            unSchedule();
            mDelegate.run();
            schedule();
        }

        private void schedule() {
            final long delay = getDelay(System.currentTimeMillis(), mPeriod, mOffset);
            getHandler().postDelayed(this, delay);
        }

        private void unSchedule() {
            getHandler().removeCallbacks(this);
        }
    }

    /**
     * Reschedules callbacks when the device time changes.
     */
    private final class TimeChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            for (PeriodicRunnable periodicRunnable : mPeriodicRunnables) {
                periodicRunnable.runAndReschedule();
            }
        }
    }
}