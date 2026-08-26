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
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A read-only domain object representing a countdown timer.
 *
 * @param mId                     A unique identifier for the timer.
 * @param mState                  The current state of the timer.
 * @param mLength                 The original length of the timer in milliseconds when it was created.
 * @param mTotalLength            The length of the timer in milliseconds including additional time added by the user.
 * @param mLastStartTime          The time at which the timer was last started; {@link #UNUSED} when not running.
 * @param mLastStartWallClockTime The time since epoch at which the timer was last started.
 * @param mRemainingTime          The time at which the timer is scheduled to expire; negative if it is already expired.
 * @param mLabel                  A message describing the meaning of the timer.
 * @param mButtonTime             The time indicated in the add time button of the timer.
 * @param mRingtoneUri            The timer ringtone.
 * @param mAutoSilence            The auto silence duration.
 * @param mCrescendoDuration      The volume crescendo duration.
 * @param mVibrate                {@code true} to enable vibration upon expiration, {@code false} otherwise.
 * @param mVibrationPattern       The vibration pattern
 * @param mFlashOn                {@code true} to turn on flash upon expiration, {@code false} otherwise.
 * @param mTurnOffMedia           {@code true} to turn off media upon expiration, {@code false} otherwise.
 * @param mDeleteAfterUse         A flag indicating the timer should be deleted when it is reset.
 */
public record Timer(int mId, @NonNull State mState, long mLength, long mTotalLength, long mLastStartTime, long mLastStartWallClockTime,
                    long mRemainingTime, @Nullable String mLabel, @NonNull  String mButtonTime, @Nullable Uri mRingtoneUri,
                    int mAutoSilence, int mCrescendoDuration, boolean mVibrate, @NonNull String mVibrationPattern, boolean mFlashOn,
                    boolean mTurnOffMedia, boolean mDeleteAfterUse) {

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
     * <p>
     * For reset timers, sorting is based on the setting selected in timer settings.
     */
    @NonNull
    public static Comparator<Timer> createTimerStateComparator(@NonNull Context context) {
        return new Comparator<>() {
            private final List<State> sortingStatus = Arrays.asList(MISSED, EXPIRED, RUNNING, PAUSED, RESET);

            @Override
            public int compare(@NonNull Timer timer1, @NonNull Timer timer2) {
                final int stateIndex1 = sortingStatus.indexOf(timer1.getState());
                final int stateIndex2 = sortingStatus.indexOf(timer2.getState());
                int sorting = Integer.compare(stateIndex1, stateIndex2);

                if (sorting == 0) {
                    final State state = timer1.getState();
                    final String timerSortingPreference = SettingsDAO.getTimerSortingPreference(getDefaultSharedPreferences(context));

                    if (state == RESET) {
                        switch (timerSortingPreference) {
                            case SORT_TIMER_BY_ASCENDING_DURATION -> sorting = Long.compare(-timer2.getLength(), -timer1.getLength());
                            case SORT_TIMER_BY_DESCENDING_DURATION -> sorting = Long.compare(timer2.getLength(), timer1.getLength());
                            case SORT_TIMER_BY_NAME -> {
                                final String label1 = timer1.getLabel() != null ? timer1.getLabel() : "";
                                final String label2 = timer2.getLabel() != null ? timer2.getLabel() : "";
                                sorting = CharSequence.compare(label1.toLowerCase(Locale.ROOT), label2.toLowerCase(Locale.ROOT));
                            }
                        }
                    } else {
                        sorting = Long.compare(timer1.getRemainingTime(), timer2.getRemainingTime());
                    }
                }

                return sorting;
            }
        };
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

    public String getVibrationPattern() {
        return mVibrationPattern;
    }

    public Uri getRingtoneUri() {
        return mRingtoneUri;
    }

    public int getAutoSilence() {
        return mAutoSilence;
    }

    public int getVolumeCrescendoDuration() {
        return mCrescendoDuration;
    }

    /**
     * @return a copy of this timer with the given {@code label}
     */
    Timer setLabel(@Nullable String label) {
        if (TextUtils.equals(mLabel, label)) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime, mLastStartWallClockTime, mRemainingTime, label, mButtonTime,
            mRingtoneUri, mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given {@code newLength}
     */
    Timer setNewDuration(long newLength) {
        if (mLength == newLength) {
            return this;
        }

        return new Timer(mId, RESET, newLength, newLength, UNUSED, UNUSED, newLength, mLabel, mButtonTime, mRingtoneUri, mAutoSilence,
            mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given button time
     */
    Timer setButtonTime(@NonNull String buttonTime) {
        if (TextUtils.equals(mButtonTime, buttonTime)) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime, mLastStartWallClockTime, mRemainingTime, mLabel, buttonTime,
            mRingtoneUri, mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given ringtone
     */
    Timer setRingtone(@Nullable Uri ringtone) {
        if (Objects.equals(ringtone, mRingtoneUri)) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime, mLastStartWallClockTime, mRemainingTime, mLabel, mButtonTime,
            ringtone, mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given auto silence duration
     */
    Timer setAutoSilence(int autoSilence) {
        if (mAutoSilence == autoSilence) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime, mLastStartWallClockTime, mRemainingTime, mLabel, mButtonTime,
            mRingtoneUri, autoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given volume crescendo duration
     */
    Timer setCrescendoDuration(int crescendoDuration) {
        if (mCrescendoDuration == crescendoDuration) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime, mLastStartWallClockTime, mRemainingTime, mLabel, mButtonTime,
            mRingtoneUri, mAutoSilence, crescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given {@code isVibrate}
     */
    Timer setIsVibrate(boolean isVibrate) {
        if (mVibrate == isVibrate) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime, mLastStartWallClockTime, mRemainingTime, mLabel, mButtonTime,
            mRingtoneUri, mAutoSilence, mCrescendoDuration, isVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given {@code vibrationPattern}
     */
    Timer setVibrationPattern(@NonNull String vibrationPattern) {
        if (TextUtils.equals(mVibrationPattern, vibrationPattern)) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime, mLastStartWallClockTime, mRemainingTime, mLabel, mButtonTime,
            mRingtoneUri, mAutoSilence, mCrescendoDuration, mVibrate, vibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given {@code isFlashOn}
     */
    Timer setFlashOn(boolean isFlashOn) {
        if (mFlashOn == isFlashOn) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime, mLastStartWallClockTime, mRemainingTime, mLabel, mButtonTime,
            mRingtoneUri, mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, isFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given {@code deleteAfterUse}
     */
    Timer setDeleteAfterUse(boolean deleteAfterUse) {
        if (mDeleteAfterUse == deleteAfterUse) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime, mLastStartWallClockTime, mRemainingTime, mLabel, mButtonTime,
            mRingtoneUri, mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, deleteAfterUse);
    }

    /**
     * @return a copy of this timer with the given {@code turnOffMedia}
     */
    Timer setTurnOffMedia(boolean turnOffMedia) {
        if (mTurnOffMedia == turnOffMedia) {
            return this;
        }

        return new Timer(mId, mState, mLength, mTotalLength, mLastStartTime, mLastStartWallClockTime, mRemainingTime, mLabel, mButtonTime,
            mRingtoneUri, mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, turnOffMedia, mDeleteAfterUse);
    }

    public long getLength() {
        return mLength;
    }

    public long getTotalLength() {
        return mTotalLength;
    }

    public boolean isVibrate() {
        return mVibrate;
    }

    public boolean isFlashOn() {
        return mFlashOn;
    }

    public boolean getTurnOffMedia() {
        return mTurnOffMedia;
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

        return new Timer(mId, state, mLength, totalLength, lastStartTime, lastWallClockTime, remainingTime, mLabel, mButtonTime,
            mRingtoneUri, mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
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

        return new Timer(mId, RUNNING, mLength, mTotalLength, now(), wallClock(), mRemainingTime, mLabel, mButtonTime, mRingtoneUri,
            mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
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
        return new Timer(mId, PAUSED, mLength, mTotalLength, UNUSED, UNUSED, remainingTime, mLabel, mButtonTime, mRingtoneUri, mAutoSilence,
            mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that is expired, missed or reset
     */
    Timer expire() {
        if (mState == EXPIRED || mState == RESET || mState == MISSED) {
            return this;
        }

        final long remainingTime = Math.min(0L, getRemainingTime());
        return new Timer(mId, EXPIRED, mLength, 0L, now(), wallClock(), remainingTime, mLabel, mButtonTime, mRingtoneUri,
            mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that is missed or reset
     */
    Timer miss() {
        if (mState == RESET || mState == MISSED) {
            return this;
        }

        final long remainingTime = Math.min(0L, getRemainingTime());
        return new Timer(mId, MISSED, mLength, 0L, now(), wallClock(), remainingTime, mLabel, mButtonTime, mRingtoneUri,
            mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
    }

    /**
     * @return a copy of this timer that is reset
     */
    Timer reset() {
        if (mState == RESET) {
            return this;
        }

        return new Timer(mId, RESET, mLength, mLength, UNUSED, UNUSED, mLength, mLabel, mButtonTime, mRingtoneUri, mAutoSilence,
            mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
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

        return new Timer(mId, mState, mLength, mTotalLength, timeSinceBoot, wallClockTime, remainingTime, mLabel, mButtonTime, mRingtoneUri,
            mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
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

        return new Timer(mId, mState, mLength, mTotalLength, timeSinceBoot, wallClockTime, remainingTime, mLabel, mButtonTime, mRingtoneUri,
            mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse);
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
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Timer timer = (Timer) o;

        if (mId != timer.mId) return false;
        if (mLength != timer.mLength) return false;
        if (mTotalLength != timer.mTotalLength) return false;
        if (mLastStartTime != timer.mLastStartTime) return false;
        if (mLastStartWallClockTime != timer.mLastStartWallClockTime) return false;
        if (mRemainingTime != timer.mRemainingTime) return false;
        if (!Objects.equals(mRingtoneUri, timer.mRingtoneUri)) return false;
        if (mAutoSilence != timer.mAutoSilence) return false;
        if (mCrescendoDuration != timer.mCrescendoDuration) return false;
        if (mVibrate != timer.mVibrate) return false;
        if (mFlashOn != timer.mFlashOn) return false;
        if (mTurnOffMedia != timer.mTurnOffMedia) return false;
        if (mDeleteAfterUse != timer.mDeleteAfterUse) return false;
        if (mState != timer.mState) return false;
        if (!TextUtils.equals(mLabel, timer.mLabel)) return false;
        if (!TextUtils.equals(mVibrationPattern, timer.mVibrationPattern)) return false;

        return TextUtils.equals(mButtonTime, timer.mButtonTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            mId, mState, mLength, mTotalLength, mLastStartTime, mLastStartWallClockTime, mRemainingTime, mLabel, mButtonTime, mRingtoneUri,
            mAutoSilence, mCrescendoDuration, mVibrate, mVibrationPattern, mFlashOn, mTurnOffMedia, mDeleteAfterUse
        );
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
        @Nullable
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
