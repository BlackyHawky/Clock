/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.text.TextUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.android.deskclock.Utils.now;
import static com.android.deskclock.Utils.wallClock;
import static com.android.deskclock.data.Timer.State.EXPIRED;
import static com.android.deskclock.data.Timer.State.MISSED;
import static com.android.deskclock.data.Timer.State.PAUSED;
import static com.android.deskclock.data.Timer.State.RESET;
import static com.android.deskclock.data.Timer.State.RUNNING;

/**
 * A read-only domain object representing a countdown timer.
 */
public final class Timer {

    public enum State {
        RUNNING(1), PAUSED(2), EXPIRED(3), RESET(4), MISSED(5);

        /** The value assigned to this State in prior releases. */
        private final int mValue;

        State(int value) {
            mValue = value;
        }

        /**
         * @return the numeric value assigned to this state
         */
        public int getValue() {
            return mValue;
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
    }

    /** The minimum duration of a timer. */
    public static final long MIN_LENGTH = SECOND_IN_MILLIS;

    /** The maximum duration of a new timer created via the user interface. */
    static final long MAX_LENGTH =
            99 * HOUR_IN_MILLIS + 99 * MINUTE_IN_MILLIS + 99 * SECOND_IN_MILLIS;

    static final long UNUSED = Long.MIN_VALUE;

    /** A unique identifier for the timer. */
    private final int mId;

    /** The current state of the timer. */
    private final State mState;

    /** The original length of the timer in milliseconds when it was created. */
    private final long mLength;

    /** The length of the timer in milliseconds including additional time added by the user. */
    private final long mTotalLength;

    /** The time at which the timer was last started; {@link #UNUSED} when not running. */
    private final long mLastStartTime;

    /** The time since epoch at which the timer was last started. */
    private final long mLastStartWallClockTime;

    /** The time at which the timer is scheduled to expire; negative if it is already expired. */
    private final long mRemainingTime;

    /** A message describing the meaning of the timer. */
    private final String mLabel;

    /** A flag indicating the timer should be deleted when it is reset. */
    private final boolean mDeleteAfterUse;

    Timer(int id, State state, long length, long totalLength, long lastStartTime,
          long lastWallClockTime, long remainingTime, String label, boolean deleteAfterUse) {
        mId = id;
        mState = state;
        mLength = length;
        mTotalLength = totalLength;
        mLastStartTime = lastStartTime;
        mLastStartWallClockTime = lastWallClockTime;
        mRemainingTime = remainingTime;
        mLabel = label;
        mDeleteAfterUse = deleteAfterUse;
    }

    public int getId() { return mId; }
    public State getState() { return mState; }
    public String getLabel() { return mLabel; }
    public long getLength() { return mLength; }
    public long getTotalLength() { return mTotalLength; }
    public boolean getDeleteAfterUse() { return mDeleteAfterUse; }
    public boolean isReset() { return mState == RESET; }
    public boolean isRunning() { return mState == RUNNING; }
    public boolean isPaused() { return mState == PAUSED; }
    public boolean isExpired() { return mState == EXPIRED; }
    public boolean isMissed() { return mState == MISSED; }

    /**
     * @return the amount of remaining time when the timer was last started or paused.
     */
    public long getLastRemainingTime() {
        return mRemainingTime;
    }

    /**
     * @return the total amount of time remaining up to this moment; expired and missed timers will
     *      return a negative amount
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
     *
     * @return the total amount of time elapsed up to this moment; expired timers will report more
     *      than the {@link #getTotalLength() total length}
     */
    public long getElapsedTime() {
        return getTotalLength() - getRemainingTime();
    }

    long getLastStartTime() { return mLastStartTime; }
    long getLastWallClockTime() { return mLastStartWallClockTime; }

    /**
     * @return a copy of this timer that is running, expired or missed
     */
    Timer start() {
        if (mState == RUNNING || mState == EXPIRED || mState == MISSED) {
            return this;
        }

        return new Timer(mId, RUNNING, mLength, mTotalLength, now(), wallClock(), mRemainingTime,
                mLabel, mDeleteAfterUse);
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
                mDeleteAfterUse);
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
                mDeleteAfterUse);
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
                mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that is reset
     */
    Timer reset() {
        if (mState == RESET) {
            return this;
        }

        return new Timer(mId, RESET, mLength, mLength, UNUSED, UNUSED, mLength, mLabel,
                mDeleteAfterUse);
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
                remainingTime, mLabel, mDeleteAfterUse);
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
                remainingTime, mLabel, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given {@code label}
     */
    Timer setLabel(String label) {
        if (TextUtils.equals(mLabel, label)) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime,
                mLastStartWallClockTime, mRemainingTime, label, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given {@code length} or this timer if the length could
     *      not be legally adjusted
     */
    Timer setLength(long length) {
        if (mLength == length || length <= Timer.MIN_LENGTH) {
            return this;
        }

        final long totalLength;
        final long remainingTime;
        if (mState == RESET) {
            totalLength = length;
            remainingTime = length;
        } else {
            totalLength = mTotalLength;
            remainingTime = mRemainingTime;
        }

        return new Timer(mId, mState, length, totalLength, mLastStartTime,
                mLastStartWallClockTime, remainingTime, mLabel, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given {@code remainingTime} or this timer if the
     *      remaining time could not be legally adjusted
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
                lastWallClockTime, remainingTime, mLabel, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with an additional minute added to the remaining time and total
     *      length, or this Timer if the minute could not be added
     */
    Timer addMinute() {
        // Expired and missed timers restart with 60 seconds of remaining time.
        if (mState == EXPIRED || mState == MISSED) {
            return setRemainingTime(MINUTE_IN_MILLIS);
        }

        // Otherwise try to add a minute to the remaining time.
        return setRemainingTime(mRemainingTime + MINUTE_IN_MILLIS);
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

    /**
     * Orders timers by their IDs. Oldest timers are at the bottom. Newest timers are at the top.
     */
    static Comparator<Timer> ID_COMPARATOR = new Comparator<Timer>() {
        @Override
        public int compare(Timer timer1, Timer timer2) {
            return Integer.compare(timer2.getId(), timer1.getId());
        }
    };

    /**
     * Orders timers by their expected/actual expiration time. The general order is:
     *
     * <ol>
     *     <li>{@link State#MISSED MISSED} timers; ties broken by {@link #getRemainingTime()}</li>
     *     <li>{@link State#EXPIRED EXPIRED} timers; ties broken by {@link #getRemainingTime()}</li>
     *     <li>{@link State#RUNNING RUNNING} timers; ties broken by {@link #getRemainingTime()}</li>
     *     <li>{@link State#PAUSED PAUSED} timers; ties broken by {@link #getRemainingTime()}</li>
     *     <li>{@link State#RESET RESET} timers; ties broken by {@link #getLength()}</li>
     * </ol>
     */
    static Comparator<Timer> EXPIRY_COMPARATOR = new Comparator<Timer>() {

        private final List<State> stateExpiryOrder = Arrays.asList(MISSED, EXPIRED, RUNNING, PAUSED,
                RESET);

        @Override
        public int compare(Timer timer1, Timer timer2) {
            final int stateIndex1 = stateExpiryOrder.indexOf(timer1.getState());
            final int stateIndex2 = stateExpiryOrder.indexOf(timer2.getState());

            int order = Integer.compare(stateIndex1, stateIndex2);
            if (order == 0) {
                final State state = timer1.getState();
                if (state == RESET) {
                    order = Long.compare(timer1.getLength(), timer2.getLength());
                } else {
                    order = Long.compare(timer1.getRemainingTime(), timer2.getRemainingTime());
                }
            }

            return order;
        }
    };
}
