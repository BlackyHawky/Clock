// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import static android.R.attr.state_activated;
import static android.R.attr.state_pressed;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.util.AttributeSet;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.databinding.TimerItemCompactBinding;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;

import java.util.Locale;

/**
 * This view is a visual representation of a compact {@link Timer}.
 */
public class TimerItemCompact extends ConstraintLayout {

    private TimerItemCompactBinding mBinding;

    SharedPreferences mPrefs;
    Typeface mRegularTypeface;
    Typeface mBoldTypeface;
    Typeface mTimerTimeTypeface;
    private boolean mIsIndicatorStateDisplay;

    private Drawable mIconPlay;
    private Drawable mIconPause;
    private Drawable mIconStop;

    private int mColorPaused;
    private int mColorRunning;
    private int mColorExpired;
    private int mColorMissed;

    private String mLastButtonTimeRaw;
    private String mCachedAddButtonText;
    private String mCachedAddButtonContentDesc;
    private String mLastLabel = null;
    private String mLastTotalDuration = null;

    /**
     * Formats and displays the text in the timer.
     */
    private TimerTextController mTimerTextController;

    /**
     * Drawable used to style the timer state indicator as a circle with dynamic fill.
     */
    private GradientDrawable mGradientDrawable;

    /**
     * The last state of the timer that was rendered; used to avoid expensive operations.
     */
    private Timer.State mLastState;

    public TimerItemCompact(Context context) {
        this(context, null);
    }

    public TimerItemCompact(Context context, AttributeSet attrs) {
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

        mPrefs = getDefaultSharedPreferences(getContext());
        String generalFontPath = SettingsDAO.getGeneralFont(mPrefs);
        mRegularTypeface = ThemeUtils.loadFont(generalFontPath);
        mBoldTypeface = ThemeUtils.boldTypeface(generalFontPath);
        mTimerTimeTypeface = ThemeUtils.loadFont(SettingsDAO.getTimerDurationFont(mPrefs));
        mIsIndicatorStateDisplay = SettingsDAO.isTimerStateIndicatorDisplayed(mPrefs);
        mTimerTextController = new TimerTextController(mBinding.timerTimeText);

        final Drawable drawable = ThemeUtils.circleDrawable();
        mBinding.timerIndicatorState.setBackground(drawable);
        mGradientDrawable = (GradientDrawable) mBinding.timerIndicatorState.getBackground();

        mIconPlay = AppCompatResources.getDrawable(getContext(), R.drawable.ic_fab_play);
        mIconPause = AppCompatResources.getDrawable(getContext(), R.drawable.ic_fab_pause);
        mIconStop = AppCompatResources.getDrawable(getContext(), R.drawable.ic_fab_stop);

        mColorPaused = SettingsDAO.getPausedTimerIndicatorColor(mPrefs);
        mColorRunning = SettingsDAO.getRunningTimerIndicatorColor(mPrefs);
        mColorExpired = SettingsDAO.getExpiredTimerIndicatorColor(mPrefs);
        mColorMissed = SettingsDAO.getMissedTimerIndicatorColor(mPrefs);

        final int colorAccent = MaterialColors.getColor(getContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = mBinding.timerTimeText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
            new int[][]{{-state_activated, -state_pressed}, {}},
            new int[]{textColorPrimary, colorAccent});
        mBinding.timerTimeText.setTextColor(timeTextColor);
    }

    /**
     * Injects preloaded typefaces into the view to optimize performance.
     * This avoids expensive disk I/O operations when the view is inflated or recycled.
     *
     * @param regular   The standard typeface used for regular text (e.g., empty labels).
     * @param bold      The bold typeface used for active labels and buttons.
     * @param timerTime The specific typeface used for the main timer countdown display.
     */
    public void setCachedFonts(Typeface regular, Typeface bold, Typeface timerTime) {
        mRegularTypeface = regular;
        mBoldTypeface = bold;
        mTimerTimeTypeface = timerTime;

        mBinding.timerTimeText.setTypeface(mTimerTimeTypeface);

        mBinding.timerTotalDurationText.setTypeface(mTimerTimeTypeface);

        mBinding.timerAddTimeButton.setTypeface(mBoldTypeface);
    }

    /**
     * Dynamically updates the {@code timer} display based on its current state.
     */
    void updateTimeDisplay(Timer timer) {
        final boolean blinkOff = SystemClock.elapsedRealtime() % 1000 < 500;

        mTimerTextController.setTimeString(timer.getRemainingTime());

        final boolean isBlinking = timer.isExpired() || timer.isMissed();
        final float targetAlpha = isBlinking
            ? (blinkOff ? 0f : 1f)
            : 1f;

        // Apply circle blinking
        if (mBinding.timerBar.getAlpha() != targetAlpha) {
            mBinding.timerBar.animate()
                .alpha(targetAlpha)
                .setDuration(300)
                .start();
        }

        // Update circle only if visible
        if (!isBlinking || !blinkOff) {
            mBinding.timerBar.update(timer);
        }

        final float textTargetAlpha = (!timer.isPaused() || !blinkOff || mBinding.timerTimeText.isPressed()) ? 1f : 0f;
        if (mBinding.timerTimeText.getAlpha() != textTargetAlpha) {
            mBinding.timerTimeText.animate()
                .alpha(textTargetAlpha)
                .setDuration(200)
                .start();
        }
    }

    /**
     * Initializes the {@code timer} static visual elements when binding to a ViewHolder.
     */
    public void bindTimer(Timer timer) {
        // Initialize text for timer total duration
        String totalDuration = timer.getTotalDuration();
        if (!totalDuration.equals(mLastTotalDuration)) {
            mLastTotalDuration = totalDuration;
            mBinding.timerTotalDurationText.setText(totalDuration);
        }

        // Initialize the label
        final String label = timer.getLabel();

        if (!label.equals(mLastLabel)) {
            mLastLabel = label;

            if (label.isEmpty()) {
                mBinding.timerLabel.setText(null);
                mBinding.timerLabel.setTypeface(mRegularTypeface);
            } else {
                mBinding.timerLabel.setText(label);
                mBinding.timerLabel.setTypeface(mBoldTypeface);
                mBinding.timerLabel.setAlpha(1f);
            }
        }

        // Initialize the timer bar
        mBinding.timerBar.animate().cancel();
        mBinding.timerBar.setAlpha(1f);

        // Initialize the alpha value of the time text color
        mBinding.timerTimeText.animate().cancel();
        mBinding.timerTimeText.setAlpha(1f);

        // Initialize the time value to add to timer in the "Add time" button
        String buttonTime = timer.getButtonTime();

        if (!buttonTime.equals(mLastButtonTimeRaw)) {
            mLastButtonTimeRaw = buttonTime;

            long totalSeconds = Long.parseLong(buttonTime);
            long buttonTimeMinutes = (totalSeconds) / 60;
            long buttonTimeSeconds = totalSeconds % 60;

            String buttonTimeFormatted = String.format(
                Locale.getDefault(),
                buttonTimeMinutes < 10 ? "%d:%02d" : "%02d:%02d",
                buttonTimeMinutes,
                buttonTimeSeconds);

            mCachedAddButtonText = getContext().getString(R.string.timer_add_custom_time, buttonTimeFormatted);

            mCachedAddButtonContentDesc = buttonTimeSeconds == 0
                ? getContext().getString(R.string.timer_add_custom_time_description, String.valueOf(buttonTimeMinutes))
                : getContext().getString(R.string.timer_add_custom_time_with_seconds_description,
                String.valueOf(buttonTimeMinutes),
                String.valueOf(buttonTimeSeconds));
        }

        mBinding.timerAddTimeButton.setText(mCachedAddButtonText);
        mBinding.timerAddTimeButton.setContentDescription(mCachedAddButtonContentDesc);

        if (timer.getState() != mLastState) {
            boolean isReset = timer.getState() == Timer.State.RESET;

            final String resetDesc = getContext().getString(R.string.reset);

            mBinding.resetButton.setVisibility(VISIBLE);
            mBinding.resetButton.setContentDescription(resetDesc);
            mBinding.timerAddTimeButton.setVisibility(VISIBLE);
            mLastState = timer.getState();

            switch (mLastState) {
                case RESET -> {
                    mBinding.resetButton.setVisibility(GONE);
                    mBinding.resetButton.setContentDescription(null);
                    mBinding.timerAddTimeButton.setVisibility(INVISIBLE);
                    mBinding.playPauseButton.setIcon(mIconPlay);
                    mBinding.timerIndicatorState.setVisibility(GONE);
                }

                case PAUSED -> {
                    mBinding.playPauseButton.setIcon(mIconPlay);
                    updateIndicatorState(mColorPaused);
                }

                case RUNNING -> {
                    mBinding.playPauseButton.setIcon(mIconPause);
                    updateIndicatorState(mColorRunning);
                }

                case EXPIRED, MISSED -> {
                    mBinding.playPauseButton.setIcon(mIconStop);
                    mBinding.resetButton.setVisibility(GONE);
                    updateIndicatorState(mLastState == Timer.State.EXPIRED ? mColorExpired : mColorMissed);
                }
            }

            mBinding.timerTimeText.setVisibility(isReset ? GONE : VISIBLE);
            mBinding.timerBar.setVisibility(isReset ? GONE : VISIBLE);
            mBinding.timerTotalDurationText.setVisibility(isReset ? VISIBLE : GONE);
        }

        updateTimeDisplay(timer);
    }

    private void updateIndicatorState(int color) {
        if (!mIsIndicatorStateDisplay) {
            mBinding.timerIndicatorState.setVisibility(GONE);
            return;
        }

        mGradientDrawable.setColor(color);
        mBinding.timerIndicatorState.setVisibility(VISIBLE);
    }

}
