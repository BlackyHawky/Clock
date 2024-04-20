/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uidata;

import static android.content.Intent.ACTION_DATE_CHANGED;
import static android.content.Intent.ACTION_TIMEZONE_CHANGED;
import static android.content.Intent.ACTION_TIME_CHANGED;
import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.best.deskclock.Utils.enforceMainLooper;
import static java.util.Calendar.DATE;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.SECOND;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.VisibleForTesting;

import com.best.deskclock.LogUtils;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * All callbacks to be delivered at requested times on the main thread if the application is in the
 * foreground when the callback time passes.
 */
final class PeriodicCallbackModel {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("Periodic");
    private static final long QUARTER_HOUR_IN_MILLIS = 15 * MINUTE_IN_MILLIS;
    private static Handler sHandler;
    private final List<PeriodicRunnable> mPeriodicRunnable = new CopyOnWriteArrayList<>();

    PeriodicCallbackModel(Context context) {
        // Reschedules callbacks when the device time changes.
        final IntentFilter timeChangedBroadcastFilter = new IntentFilter();
        timeChangedBroadcastFilter.addAction(ACTION_TIME_CHANGED);
        timeChangedBroadcastFilter.addAction(ACTION_DATE_CHANGED);
        timeChangedBroadcastFilter.addAction(ACTION_TIMEZONE_CHANGED);

        // Reschedules callbacks when the device time changes.
        BroadcastReceiver mTimeChangedReceiver = new TimeChangedReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(mTimeChangedReceiver, timeChangedBroadcastFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(mTimeChangedReceiver, timeChangedBroadcastFilter);
        }
    }

    /**
     * Return the delay until the given {@code period} elapses adjusted by the given {@code offset}.
     *
     * @param now    the current time
     * @param period the frequency with which callbacks should be given
     * @param offset an offset to add to the normal period; allows the callback to be made relative
     *               to the normally scheduled period end
     * @return the time delay from {@code now} to schedule the callback
     */
    @VisibleForTesting
    static long getDelay(long now, Period period, long offset) {
        final long periodStart = now - offset;

        switch (period) {
            case HALF_MINUTE -> {
                final long lastHalfMinute = periodStart - (periodStart % 30000L);
                final long nextHalfMinute = lastHalfMinute + 30000L;
                return nextHalfMinute - now + offset;
            }
            case QUARTER_HOUR -> {
                final long lastQuarterHour = periodStart - (periodStart % QUARTER_HOUR_IN_MILLIS);
                final long nextQuarterHour = lastQuarterHour + QUARTER_HOUR_IN_MILLIS;
                return nextQuarterHour - now + offset;
            }
            case HOUR -> {
                final long lastHour = periodStart - (periodStart % HOUR_IN_MILLIS);
                final long nextHour = lastHour + HOUR_IN_MILLIS;
                return nextHour - now + offset;
            }
            case MIDNIGHT -> {
                final Calendar nextMidnight = Calendar.getInstance();
                nextMidnight.setTimeInMillis(periodStart);
                nextMidnight.add(DATE, 1);
                nextMidnight.set(HOUR_OF_DAY, 0);
                nextMidnight.set(MINUTE, 0);
                nextMidnight.set(SECOND, 0);
                nextMidnight.set(MILLISECOND, 0);
                return nextMidnight.getTimeInMillis() - now + offset;
            }
            default -> throw new IllegalArgumentException("unexpected period: " + period);
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
     * @param runnable to be called every 30 seconds
     * @param offset   an offset applied to the minute to control when the callback occurs
     */
    void addHalfMinuteCallback(Runnable runnable, long offset) {
        addPeriodicCallback(runnable, Period.HALF_MINUTE, offset);
    }

    /**
     * @param runnable to be called every quarter-hour
     * @param offset   an offset applied to the quarter-hour to control when the callback occurs
     */
    void addQuarterHourCallback(Runnable runnable, long offset) {
        addPeriodicCallback(runnable, Period.QUARTER_HOUR, offset);
    }

    /**
     * @param runnable to be called every midnight
     * @param offset   an offset applied to the midnight to control when the callback occurs
     */
    void addMidnightCallback(Runnable runnable, long offset) {
        addPeriodicCallback(runnable, Period.MIDNIGHT, offset);
    }

    /**
     * @param runnable to be called periodically
     */
    private void addPeriodicCallback(Runnable runnable, Period period, long offset) {
        final PeriodicRunnable periodicRunnable = new PeriodicRunnable(runnable, period, offset);
        mPeriodicRunnable.add(periodicRunnable);
        periodicRunnable.schedule();
    }

    /**
     * @param runnable to no longer be called periodically
     */
    void removePeriodicCallback(Runnable runnable) {
        for (PeriodicRunnable periodicRunnable : mPeriodicRunnable) {
            if (periodicRunnable.mDelegate == runnable) {
                periodicRunnable.unSchedule();
                mPeriodicRunnable.remove(periodicRunnable);
                return;
            }
        }
    }

    @VisibleForTesting
    enum Period {HALF_MINUTE, QUARTER_HOUR, HOUR, MIDNIGHT}

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
            for (PeriodicRunnable periodicRunnable : mPeriodicRunnable) {
                periodicRunnable.runAndReschedule();
            }
        }
    }
}
