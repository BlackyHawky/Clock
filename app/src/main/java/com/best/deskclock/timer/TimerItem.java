/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.databinding.TimerItemBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.BaseProgressIndicator;

public class TimerItem extends BaseTimerItem {

    private TimerItemBinding mBinding;

    public TimerItem(@NonNull Context context) {
        this(context, null);
    }

    public TimerItem(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (isInEditMode()) {
            // Skip logic during Android Studio preview
            return;
        }

        mBinding = TimerItemBinding.bind(this);

        onFinishInflateShared();
    }

    public void setButtonPosition(boolean areTimerButtonPositionsInverted, boolean isTablet, boolean isLandscape, boolean isSingleTimer,
                                  boolean isRtl) {

        if (areTimerButtonPositionsInverted) {
            mBinding.getRoot().setLayoutDirection(isRtl ? LAYOUT_DIRECTION_LTR : LAYOUT_DIRECTION_RTL);
        } else {
            mBinding.getRoot().setLayoutDirection(LAYOUT_DIRECTION_LOCALE);
        }

        if ((!isTablet && isLandscape) || isSingleTimer) {
            mBinding.timerEndTime.setGravity(Gravity.CENTER);
        } else {
            mBinding.timerEndTime.setGravity(areTimerButtonPositionsInverted
                ? Gravity.START | Gravity.CENTER_VERTICAL
                : Gravity.END | Gravity.CENTER_VERTICAL);
        }

        mBinding.timerLabel.setLayoutDirection(LAYOUT_DIRECTION_LOCALE);
        mBinding.timerIndicatorState.setLayoutDirection(LAYOUT_DIRECTION_LOCALE);
    }

    @Override protected TextView getTimeText() { return mBinding.timerTimeText; }
    @Override protected TextView getLabelText() { return mBinding.timerLabel; }
    @Override protected TextView getEndTimeText() { return mBinding.timerEndTime; }
    @Override protected TextView getAddTimeButton() { return mBinding.timerAddTimeButton; }
    @Override protected View getIndicatorState() { return mBinding.timerIndicatorState; }
    @Override protected BaseProgressIndicator<?> getProgressIndicator() { return mBinding.circularProgressIndicator; }
    @Override protected View getResetButton() { return mBinding.resetButton; }
    @Override protected MaterialButton getPlayPauseButton() { return mBinding.playPauseButton; }
    @Override protected int getAddTimeHiddenVisibility() { return INVISIBLE; }

}
