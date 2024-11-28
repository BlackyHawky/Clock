/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.stopwatch;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import android.content.Context;
import android.widget.TextView;

import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.Utils;

/**
 * A controller which will format a provided time in millis to display as a stopwatch.
 */
public final class StopwatchTextController {

    private final TextView mMainTextView;
    private final TextView mHundredthsTextView;

    private long mLastTime = Long.MIN_VALUE;

    public StopwatchTextController(TextView mainTextView, TextView hundredthsTextView) {
        mMainTextView = mainTextView;
        mHundredthsTextView = hundredthsTextView;
    }

    public void setTimeString(long accumulatedTime) {
        // Since time is only displayed to centiseconds, if there is a change at the milliseconds
        // level but not the centiseconds level, we can avoid unnecessary work.
        if ((mLastTime / 10) == (accumulatedTime / 10)) {
            return;
        }

        final int hours = (int) (accumulatedTime / HOUR_IN_MILLIS);
        int remainder = (int) (accumulatedTime % HOUR_IN_MILLIS);

        final int minutes = (int) (remainder / MINUTE_IN_MILLIS);
        remainder = (int) (remainder % MINUTE_IN_MILLIS);

        final int seconds = (int) (remainder / SECOND_IN_MILLIS);
        remainder = (int) (remainder % SECOND_IN_MILLIS);

        mHundredthsTextView.setText(UiDataModel.getUiDataModel().getFormattedNumber(
                remainder / 10, 2));

        // Avoid unnecessary computations and garbage creation if seconds have not changed since
        // last layout pass.
        if ((mLastTime / SECOND_IN_MILLIS) != (accumulatedTime / SECOND_IN_MILLIS)) {
            final Context context = mMainTextView.getContext();
            final String time = Utils.getTimeString(context, hours, minutes, seconds);
            mMainTextView.setText(time);
        }
        mLastTime = accumulatedTime;
    }
}
