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

import android.os.SystemClock;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.android.deskclock.data.Timer.State.EXPIRED;
import static com.android.deskclock.data.Timer.State.PAUSED;
import static com.android.deskclock.data.Timer.State.RESET;
import static com.android.deskclock.data.Timer.State.RUNNING;

/**
 * A read-only domain object representing a countdown timer.
 */
public final class Timer {

    public enum State {
        RUNNING(1), PAUSED(2), EXPIRED(3), RESET(4);

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

    /** The maximum duration of a timer. */
    public static final long MAX_LENGTH =
            99 * HOUR_IN_MILLIS + 99 * MINUTE_IN_MILLIS + 99 * SECOND_IN_MILLIS;

    /** A unique identifier for the city. */
    private final int mId;

    /** The current state of the timer. */
    private final State mState;

    /** The original length of the timer in milliseconds when it was created. */
    private final long mLength;

    /** The length of the timer in milliseconds including additional time added by the user. */
    private final long mTotalLength;

    /** The time at which the timer was last started; {@link Long#MIN_VALUE} when not running. */
    private final long mLastStartTime;

    /** The time at which the timer is scheduled to expire; negative if it is already expired. */
    private final long mRemainingTime;

    /** A message describing the meaning of the timer. */
    private final String mLabel;

    /** A flag indicating the timer should be deleted when it is reset. */
    private final boolean mDeleteAfterUse;

    Timer(int id, State state, long length, long totalLength, long lastStartTime,
            long remainingTime, String label, boolean deleteAfterUse) {
        mId = id;
        mState = state;
        mLength = length;
        mTotalLength = totalLength;
        mLastStartTime = lastStartTime;
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

    /**
     * @return the total amount of time remaining up to this moment; expired timers will return a
     *      negative amount
     */
    public long getRemainingTime() {
        if (mState == RUNNING || mState == EXPIRED) {
            return mRemainingTime - (now() - mLastStartTime);
        }

        return mRemainingTime;
    }

    /**
     * @return the time at which this timer will or did expire
     */
    public long getExpirationTime() {
        if (mState != RUNNING && mState != EXPIRED) {
            throw new IllegalStateException("cannot compute expiration time in state " + mState);
        }

        return mLastStartTime + mRemainingTime;
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

    /**
     * @return a copy of this timer that is running or expired
     */
    Timer start() {
        if (mState == RUNNING || mState == EXPIRED) {
            return this;
        }

        return new Timer(mId, RUNNING, mLength, mTotalLength, now(), mRemainingTime, mLabel,
                mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that is paused or reset
     */
    Timer pause() {
        if (mState == PAUSED || mState == RESET) {
            return this;
        } else if (mState == EXPIRED) {
            return reset();
        }

        final long remainingTime = getRemainingTime();
        return new Timer(mId, PAUSED, mLength, mTotalLength, Long.MIN_VALUE, remainingTime, mLabel,
                mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that is expired or reset
     */
    Timer expire() {
        if (mState == EXPIRED || mState == RESET) {
            return this;
        }

        return new Timer(mId, EXPIRED, mLength, mTotalLength, mLastStartTime, mRemainingTime,
                mLabel, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that is reset
     */
    Timer reset() {
        if (mState == RESET) {
            return this;
        }

        return new Timer(mId, RESET, mLength, mLength, Long.MIN_VALUE, mLength, mLabel,
                mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given {@code label}
     */
    Timer setLabel(String label) {
        if (TextUtils.equals(mLabel, label)) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime, mRemainingTime, label,
                mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with an additional minute added to the remaining time and total
     *      length, or this Timer if adding a minute would exceed the maximum timer duration
     */
    Timer addMinute() {
        final long lastStartTime;
        final long remainingTime;
        final long totalLength;
        final State state;
        if (mState == EXPIRED) {
            state = RUNNING;
            lastStartTime = now();
            totalLength = MINUTE_IN_MILLIS;
            remainingTime = MINUTE_IN_MILLIS;
        } else {
            state = mState;
            lastStartTime = mLastStartTime;
            totalLength = mRemainingTime + MINUTE_IN_MILLIS;
            remainingTime = mRemainingTime + MINUTE_IN_MILLIS;
        }

        // Do not allow the remaining time to exceed the maximum.
        if (remainingTime > MAX_LENGTH) {
            return this;
        }

        return new Timer(mId, state, mLength, totalLength, lastStartTime, remainingTime, mLabel,
                mDeleteAfterUse);
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

    private static long now() {
        return SystemClock.elapsedRealtime();
    }

    /**
     * Orders timers by their IDs. Oldest timers are at the bottom. Newest timers are at the top.
     */
    public static Comparator<Timer> ID_COMPARATOR = new Comparator<Timer>() {
        @Override
        public int compare(Timer timer1, Timer timer2) {
            return Integer.compare(timer2.getId(), timer1.getId());
        }
    };

    /**
     * Orders timers by their expected/actual expiration time. The general order is:
     *
     * <ol>
     *     <li>{@link State#EXPIRED EXPIRED} timers; ties broken by {@link #getRemainingTime()}</li>
     *     <li>{@link State#RUNNING RUNNING} timers; ties broken by {@link #getRemainingTime()}</li>
     *     <li>{@link State#PAUSED PAUSED} timers; ties broken by {@link #getRemainingTime()}</li>
     *     <li>{@link State#RESET RESET} timers; ties broken by {@link #getLength()}</li>
     * </ol>
     */
    public static Comparator<Timer> EXPIRY_COMPARATOR = new Comparator<Timer>() {

        private final List<State> stateExpiryOrder = Arrays.asList(EXPIRED, RUNNING, PAUSED, RESET);

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