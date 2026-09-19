// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.best.deskclock.databinding.TimerItemCompactBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.BaseProgressIndicator;

public class TimerItemCompact extends BaseTimerItem {

    private TimerItemCompactBinding mBinding;

    public TimerItemCompact(@NonNull Context context) {
        this(context, null);
    }

    public TimerItemCompact(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (isInEditMode()) {
            // Skip logic during Android Studio preview
            return;
        }

        mBinding = TimerItemCompactBinding.bind(this);

        onFinishInflateShared();
    }

    public void setButtonPosition(boolean areTimerButtonPositionsInverted, boolean isRtl) {
        if (areTimerButtonPositionsInverted) {
            mBinding.timerControlsContainer.setLayoutDirection(isRtl
                ? LAYOUT_DIRECTION_LTR
                : LAYOUT_DIRECTION_RTL
            );
        } else {
            mBinding.timerControlsContainer.setLayoutDirection(LAYOUT_DIRECTION_LOCALE);
        }
    }

    @Override
    protected void onTimerTopUpdated() {
        if (mIsIndicatorStateDisplayed || !mLastLabel.isEmpty()) {
            mBinding.timerTop.setVisibility(VISIBLE);
        } else {
            mBinding.timerTop.setVisibility(GONE);
        }
    }

    @Override protected TextView getTimeText() { return mBinding.timerTimeText; }
    @Override protected TextView getLabelText() { return mBinding.timerLabel; }
    @Override protected TextView getEndTimeText() { return mBinding.timerEndTime; }
    @Override protected TextView getAddTimeButton() { return mBinding.timerAddTimeButton; }
    @Override protected View getIndicatorState() { return mBinding.timerIndicatorState; }
    @Override protected BaseProgressIndicator<?> getProgressIndicator() { return mBinding.linearProgressIndicator; }
    @Override protected View getResetButton() { return mBinding.resetButton; }
    @Override protected MaterialButton getPlayPauseButton() { return mBinding.playPauseButton; }
    @Override protected int getAddTimeHiddenVisibility() { return GONE; }

}
