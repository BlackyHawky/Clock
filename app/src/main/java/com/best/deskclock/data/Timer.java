/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.data.Timer.State.EXPIRED;
import static com.best.deskclock.data.Timer.State.MISSED;
import static com.best.deskclock.data.Timer.State.PAUSED;
import static com.best.deskclock.data.Timer.State.RESET;
import static com.best.deskclock.data.Timer.State.RUNNING;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_TIMER_BY_ASCENDING_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_TIMER_BY_DESCENDING_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_TIMER_BY_NAME;
import static com.best.deskclock.utils.Utils.now;
import static com.best.deskclock.utils.Utils.wallClock;

import android.content.Context;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * A read-only domain object representing a countdown timer.
 */
public final class Timer {

    /**
     * The minimum duration of a timer.
     */
    public static final long MIN_LENGTH = SECOND_IN_MILLIS;
    static final long UNUSED = Long.MIN_VALUE;

    /**
     * Sorts timers by their expected/actual expiration time. The general sorting is:
     *
     * <ol>
     *     <li>{@link State#MISSED MISSED} timers; ties broken by {@link #getRemainingTime()}</li>
     *     <li>{@link State#EXPIRED EXPIRED} timers; ties broken by {@link #getRemainingTime()}</li>
     *     <li>{@link State#RUNNING RUNNING} timers; ties broken by {@link #getRemainingTime()}</li>
     *     <li>{@link State#PAUSED PAUSED} timers; ties broken by {@link #getRemainingTime()}</li>
     *     <li>{@link State#RESET RESET} timers; ties broken by {@link #getLength()}</li>
     * </ol>
     *
     * For reset timers, sorting is based on the setting selected in timer settings.
     */
    public static Comparator<Timer> createTimerStateComparator(final Context context) {
        return new Comparator<>() {
            private final List<State> sortingStatus = Arrays.asList(MISSED, EXPIRED, RUNNING, PAUSED, RESET);

            @Override
            public int compare(Timer timer1, Timer timer2) {
                final int stateIndex1 = sortingStatus.indexOf(timer1.getState());
                final int stateIndex2 = sortingStatus.indexOf(timer2.getState());
                int sorting = Integer.compare(stateIndex1, stateIndex2);

                if (sorting == 0) {
                    final State state = timer1.getState();
                    final String timerSortingPreference = SettingsDAO.getTimerSortingPreference(getDefaultSharedPreferences(context));

                    if (state == RESET) {
                        switch (timerSortingPreference) {
                            case SORT_TIMER_BY_ASCENDING_DURATION ->
                                    sorting = Long.compare(-timer2.getLength(), -timer1.getLength());
                            case SORT_TIMER_BY_DESCENDING_DURATION ->
                                    sorting = Long.compare(timer2.getLength(), timer1.getLength());
                            case SORT_TIMER_BY_NAME -> sorting =
                                    CharSequence.compare(timer1.getLabel().toLowerCase(Locale.ROOT), timer2.getLabel().toLowerCase(Locale.ROOT));
                        }
                    } else {
                        sorting = Long.compare(timer1.getRemainingTime(), timer2.getRemainingTime());
                    }
                }

                return sorting;
            }
        };
    }

    /**
     * A unique identifier for the timer.
     */
    private final int mId;

    /**
     * The current state of the timer.
     */
    private final State mState;

    /**
     * The original length of the timer in milliseconds when it was created.
     */
    private final long mLength;

    /**
     * The length of the timer in milliseconds including additional time added by the user.
     */
    private final long mTotalLength;

    /**
     * The time at which the timer was last started; {@link #UNUSED} when not running.
     */
    private final long mLastStartTime;

    /**
     * The time since epoch at which the timer was last started.
     */
    private final long mLastStartWallClockTime;

    /**
     * The time at which the timer is scheduled to expire; negative if it is already expired.
     */
    private final long mRemainingTime;

    /**
     * A message describing the meaning of the timer.
     */
    private final String mLabel;

    /**
     * The time indicated in the add time button of the timer.
     */
    private final String mButtonTime;

    /**
     * A flag indicating the timer should be deleted when it is reset.
     */
    private final boolean mDeleteAfterUse;

    Timer(int id, State state, long length, long totalLength, long lastStartTime,
          long lastWallClockTime, long remainingTime, String label, String buttonTime, boolean deleteAfterUse) {
        mId = id;
        mState = state;
        mLength = length;
        mTotalLength = totalLength;
        mLastStartTime = lastStartTime;
        mLastStartWallClockTime = lastWallClockTime;
        mRemainingTime = remainingTime;
        mLabel = label;
        mButtonTime = buttonTime;
        mDeleteAfterUse = deleteAfterUse;
    }

    public int getId() {
        return mId;
    }

    public State getState() {
        return mState;
    }

    public String getLabel() {
        return mLabel;
    }

    public String getButtonTime() {
        return mButtonTime;
    }

    /**
     * @return a copy of this timer with the given {@code label}
     */
    Timer setLabel(String label) {
        if (TextUtils.equals(mLabel, label)) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime,
                mLastStartWallClockTime, mRemainingTime, label, mButtonTime, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given {@code newLength}
     */
    Timer setNewDuration(long newLength) {
        if (mState != State.RESET) {
            return this;
        }

        if (mLength == newLength) {
            return this;
        }

        return new Timer(mId, mState, newLength, newLength, mLastStartTime, mLastStartWallClockTime,
                newLength, mLabel, mButtonTime, mDeleteAfterUse
        );
    }

    /**
     * @return a copy of this timer with the given button time
     */
    Timer setButtonTime(String buttonTime) {
        if (TextUtils.equals(mButtonTime, buttonTime)) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime,
                mLastStartWallClockTime, mRemainingTime, mLabel, buttonTime, mDeleteAfterUse);
    }

    public long getLength() {
        return mLength;
    }

    public long getTotalLength() {
        return mTotalLength;
    }

    public boolean getDeleteAfterUse() {
        return mDeleteAfterUse;
    }

    public boolean isReset() {
        return mState == RESET;
    }

    public boolean isRunning() {
        return mState == RUNNING;
    }

    public boolean isPaused() {
        return mState == PAUSED;
    }

    public boolean isExpired() {
        return mState == EXPIRED;
    }

    public boolean isMissed() {
        return mState == MISSED;
    }

    /**
     * @return the total amount of time remaining up to this moment; expired and missed timers will
     * return a negative amount
     */
    public long getRemainingTime() {
        if (mState == PAUSED || mState == RESET) {
            return mRemainingTime;
        }

        // In practice, "now" can be any value due to device reboots. When the real-time clock
        // is reset, there is no more guarantee that "now" falls after the last start time. To
        // ensure the timer is monotonically decreasing, normalize negative time segments to 0,
        final long timeSinceStart = now() - mLastStartTime;
        return mRemainingTime - Math.max(0, timeSinceStart);
    }

    /**
     * Returns the total duration of the timer as a formatted string.
     *
     * <ul>
     *     <li>If the duration is less than one hour, the format is {@code MM:SS}.</li>
     *     <li>If the duration is one hour or more, the format is {@code HH:MM:SS}.</li>
     * </ul>
     *
     * @return the formatted duration string
     */
    public String getTotalDuration() {
        long length = getLength();

        long HH = TimeUnit.MILLISECONDS.toHours(length);
        long MM = TimeUnit.MILLISECONDS.toMinutes(length) % 60;
        long SS = TimeUnit.MILLISECONDS.toSeconds(length) % 60;

        if (HH == 0) {
            return String.format(Locale.US, "%02d:%02d", MM, SS);
        } else {
            return String.format(Locale.US, "%02d:%02d:%02d", HH, MM, SS);
        }
    }

    /**
     * @return a copy of this timer with the given {@code remainingTime} or this timer if the
     * remaining time could not be legally adjusted
     */
    Timer setRemainingTime(long remainingTime) {
        // Do not change the remaining time of a reset timer.
        if (mRemainingTime == remainingTime || mState == RESET) {
            return this;
        }

        final long delta = remainingTime - mRemainingTime;
        final long totalLength = mTotalLength + delta;

        final long lastStartTime;
        final long lastWallClockTime;
        final State state;
        if (remainingTime > 0 && (mState == EXPIRED || mState == MISSED)) {
            state = RUNNING;
            lastStartTime = now();
            lastWallClockTime = wallClock();
        } else {
            state = mState;
            lastStartTime = mLastStartTime;
            lastWallClockTime = mLastStartWallClockTime;
        }

        return new Timer(mId, state, mLength, totalLength, lastStartTime,
                lastWallClockTime, remainingTime, mLabel, mButtonTime, mDeleteAfterUse);
    }

    /**
     * @return the elapsed realtime at which this timer will or did expire
     */
    public long getExpirationTime() {
        if (mState != RUNNING && mState != EXPIRED && mState != MISSED) {
            throw new IllegalStateException("cannot compute expiration time in state " + mState);
        }

        return mLastStartTime + mRemainingTime;
    }

    /**
     * @return the wall clock time at which this timer will or did expire
     */
    public long getWallClockExpirationTime() {
        if (mState != RUNNING && mState != EXPIRED && mState != MISSED) {
            throw new IllegalStateException("cannot compute expiration time in state " + mState);
        }

        return mLastStartWallClockTime + mRemainingTime;
    }

    /**
     * @return the total amount of time elapsed up to this moment; expired timers will report more
     * than the {@link #getTotalLength() total length}
     */
    public long getElapsedTime() {
        return getTotalLength() - getRemainingTime();
    }

    long getLastStartTime() {
        return mLastStartTime;
    }

    long getLastWallClockTime() {
        return mLastStartWallClockTime;
    }

    /**
     * @return a copy of this timer that is running, expired or missed
     */
    Timer start() {
        if (mState == RUNNING || mState == EXPIRED || mState == MISSED) {
            return this;
        }

        return new Timer(mId, RUNNING, mLength, mTotalLength, now(), wallClock(), mRemainingTime,
                mLabel, mButtonTime, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that is paused or reset
     */
    Timer pause() {
        if (mState == PAUSED || mState == RESET) {
            return this;
        } else if (mState == EXPIRED || mState == MISSED) {
            return reset();
        }

        final long remainingTime = getRemainingTime();
        return new Timer(mId, PAUSED, mLength, mTotalLength, UNUSED, UNUSED, remainingTime, mLabel,
                mButtonTime, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that is expired, missed or reset
     */
    Timer expire() {
        if (mState == EXPIRED || mState == RESET || mState == MISSED) {
            return this;
        }

        final long remainingTime = Math.min(0L, getRemainingTime());
        return new Timer(mId, EXPIRED, mLength, 0L, now(), wallClock(), remainingTime, mLabel,
                mButtonTime, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that is missed or reset
     */
    Timer miss() {
        if (mState == RESET || mState == MISSED) {
            return this;
        }

        final long remainingTime = Math.min(0L, getRemainingTime());
        return new Timer(mId, MISSED, mLength, 0L, now(), wallClock(), remainingTime, mLabel,
                mButtonTime, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that is reset
     */
    Timer reset() {
        if (mState == RESET) {
            return this;
        }

        return new Timer(mId, RESET, mLength, mLength, UNUSED, UNUSED, mLength, mLabel,
                mButtonTime, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that has its times adjusted after a reboot
     */
    Timer updateAfterReboot() {
        if (mState == RESET || mState == PAUSED) {
            return this;
        }

        final long timeSinceBoot = now();
        final long wallClockTime = wallClock();
        // Avoid negative time deltas. They can happen in practice, but they can't be used. Simply
        // update the recorded times and proceed with no change in accumulated time.
        final long delta = Math.max(0, wallClockTime - mLastStartWallClockTime);
        final long remainingTime = mRemainingTime - delta;
        return new Timer(mId, mState, mLength, mTotalLength, timeSinceBoot, wallClockTime,
                remainingTime, mLabel, mButtonTime, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that has its times adjusted after time has been set
     */
    Timer updateAfterTimeSet() {
        if (mState == RESET || mState == PAUSED) {
            return this;
        }

        final long timeSinceBoot = now();
        final long wallClockTime = wallClock();
        final long delta = timeSinceBoot - mLastStartTime;
        final long remainingTime = mRemainingTime - delta;
        if (delta < 0) {
            // Avoid negative time deltas. They typically happen following reboots when TIME_SET is
            // broadcast before BOOT_COMPLETED. Simply ignore the time update and hope
            // updateAfterReboot() can successfully correct the data at a later time.
            return this;
        }
        return new Timer(mId, mState, mLength, mTotalLength, timeSinceBoot, wallClockTime,
                remainingTime, mLabel, mButtonTime, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with additional minutes added to the remaining time and total
     * length, or this Timer if the minutes could not be added
     */
    Timer addCustomTime() {
        // Expired and missed timers restart with the time indicated on the add time button.
        if (mState == EXPIRED || mState == MISSED) {
            return setRemainingTime(Integer.parseInt(mButtonTime) * SECOND_IN_MILLIS);
        }

        // Otherwise try to add time indicated on the add time button to the remaining time.
        return setRemainingTime(mRemainingTime + Integer.parseInt(mButtonTime) * SECOND_IN_MILLIS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Timer timer = (Timer) o;

        return mId == timer.mId;
    }

    @Override
    public int hashCode() {
        return mId;
    }

    public enum State {
        RUNNING(1), PAUSED(2), EXPIRED(3), RESET(4), MISSED(5);

        /**
         * The value assigned to this State in prior releases.
         */
        private final int mValue;

        State(int value) {
            mValue = value;
        }

        /**
         * @return the state corresponding to the given {@code value}
         */
        public static State fromValue(int value) {
            for (State state : values()) {
                if (state.getValue() == value) {
                    return state;
                }
            }

            return null;
        }

        /**
         * @return the numeric value assigned to this state
         */
        public int getValue() {
            return mValue;
        }
    }
}
