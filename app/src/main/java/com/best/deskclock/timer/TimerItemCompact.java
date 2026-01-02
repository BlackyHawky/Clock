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
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.Locale;

/**
 * This view is a visual representation of a compact {@link Timer}.
 */
public class TimerItemCompact extends ConstraintLayout {

    Context mContext;
    SharedPreferences mPrefs;
    String mGeneralFontPath;
    Typeface mTimerTimeTypeface;
    DisplayMetrics mDisplayMetrics;

    /** Displays timer progress as a horizontal bar. */
    private TimerBarView mTimerBar;

    /** Formats and displays the text in the timer. */
    private TimerTextController mTimerTextController;

    /** Displays the remaining time or time since expiration. */
    private TextView mTimerText;

    /** A button that resets the timer. */
    private ImageButton mResetButton;

    /** A button that adds time to the timer. */
    private MaterialButton mAddTimeButton;

    /** Displays the label associated with the timer. Tapping it presents an edit dialog. */
    private TextView mLabelView;

    /** A button to start / stop the timer */
    private MaterialButton mPlayPauseButton;

    /** View representing the visual indicator of the timer state (running, paused, expired). */
    private View mIndicatorState;

    /** Drawable used to style the timer state indicator as a circle with dynamic fill. */
    private GradientDrawable mGradientDrawable;

    /** The last state of the timer that was rendered; used to avoid expensive operations. */
    private Timer.State mLastState;

    /** The timer duration text that appears when the timer is reset */
    private TextView mTimerTotalDurationText;

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

        mContext = getContext();
        mPrefs = getDefaultSharedPreferences(mContext);
        mGeneralFontPath = SettingsDAO.getGeneralFont(mPrefs);
        mTimerTimeTypeface = ThemeUtils.loadFont(SettingsDAO.getTimerDurationFont(mPrefs));
        mDisplayMetrics = getResources().getDisplayMetrics();

        setBackground(ThemeUtils.cardBackground(mContext));

        mTimerBar = findViewById(R.id.timer_bar);
        mLabelView = findViewById(R.id.timer_label);
        mResetButton = findViewById(R.id.reset);
        mAddTimeButton = findViewById(R.id.timer_add_time_button);
        mPlayPauseButton = findViewById(R.id.play_pause);

        // Displays the remaining time or time since expiration.
        // Timer text serves as a virtual start/stop button.
        mTimerText = findViewById(R.id.timer_time_text);
        final int colorAccent = MaterialColors.getColor(
                mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = mTimerText.getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
                new int[][]{{-state_activated, -state_pressed}, {}},
                new int[]{textColorPrimary, colorAccent});
        mTimerText.setTextColor(timeTextColor);
        mTimerTextController = new TimerTextController(mTimerText);

        mTimerTotalDurationText = findViewById(R.id.timer_total_duration);

        mIndicatorState = findViewById(R.id.indicator_state);
        final Drawable drawable = ThemeUtils.circleDrawable();
        mIndicatorState.setBackground(drawable);
        mGradientDrawable = (GradientDrawable) mIndicatorState.getBackground();
    }

    /**
     * Dynamically updates the {@code timer} display based on its current state.
     */
    void updateTimeDisplay(Timer timer) {
        final boolean blinkOff = SystemClock.elapsedRealtime() % 1000 < 500;

        mTimerTextController.setTimeString(timer.getRemainingTime());

        if (mTimerBar != null) {
            final boolean isBlinking = timer.isExpired() || timer.isMissed();
            final float targetAlpha = isBlinking
                    ? (blinkOff ? 0f : 1f)
                    : 1f;

            // Apply circle blinking
            mTimerBar.animate()
                    .alpha(targetAlpha)
                    .setDuration(300)
                    .start();

            // Update circle only if visible
            if (!isBlinking || !blinkOff) {
                mTimerBar.update(timer);
            }
        }

        final float textTargetAlpha = (!timer.isPaused() || !blinkOff || mTimerText.isPressed()) ? 1f : 0f;
        mTimerText.animate()
                .alpha(textTargetAlpha)
                .setDuration(200)
                .start();
    }

    /**
     * Initializes the {@code timer} static visual elements when binding to a ViewHolder.
     */
    public void bindTimer(Timer timer) {
        // Initialize the time.
        mTimerTextController.setTimeString(timer.getRemainingTime());

        // Initialize text for timer total duration
        if (mTimerTotalDurationText != null) {
            mTimerTotalDurationText.setText(timer.getTotalDuration());
            mTimerTotalDurationText.setTypeface(mTimerTimeTypeface);
        }

        // Initialize the label
        final String label = timer.getLabel();
        final boolean isLabelEmpty = label.isEmpty();

        if (isLabelEmpty) {
            mLabelView.setText(null);
            mLabelView.setTypeface(ThemeUtils.loadFont(mGeneralFontPath));
        } else {
            mLabelView.setText(label);
            mLabelView.setTypeface(ThemeUtils.boldTypeface(mGeneralFontPath));
            mLabelView.setAlpha(1f);
        }

        // Initialize the timer bar
        mTimerBar.setAlpha(1f);
        mTimerBar.update(timer);

        // Initialize the alpha value of the time text color
        mTimerText.setAlpha(1f);
        mTimerText.setTypeface(mTimerTimeTypeface);

        final boolean isIndicatorStateDisplayed = SettingsDAO.isTimerStateIndicatorDisplayed(mPrefs);
        if (isIndicatorStateDisplayed && !timer.isReset()) {
            mIndicatorState.setVisibility(VISIBLE);
        } else {
            mIndicatorState.setVisibility(GONE);
        }

        // Initialize the time value to add to timer in the "Add time" button
        String buttonTime = timer.getButtonTime();
        long totalSeconds = Long.parseLong(buttonTime);
        long buttonTimeMinutes = (totalSeconds) / 60;
        long buttonTimeSeconds = totalSeconds % 60;
        String buttonTimeFormatted = String.format(
                Locale.getDefault(),
                buttonTimeMinutes < 10 ? "%d:%02d" : "%02d:%02d",
                buttonTimeMinutes,
                buttonTimeSeconds);

        mAddTimeButton.setText(mContext.getString(R.string.timer_add_custom_time, buttonTimeFormatted));
        mAddTimeButton.setTypeface(ThemeUtils.boldTypeface(mGeneralFontPath));

        String buttonContentDescription = buttonTimeSeconds == 0
                ? mContext.getString(R.string.timer_add_custom_time_description, String.valueOf(buttonTimeMinutes))
                : mContext.getString(R.string.timer_add_custom_time_with_seconds_description,
                String.valueOf(buttonTimeMinutes),
                String.valueOf(buttonTimeSeconds));
        mAddTimeButton.setContentDescription(buttonContentDescription);

        if (timer.getState() != mLastState) {
            final String resetDesc = mContext.getString(R.string.reset);

            mResetButton.setVisibility(VISIBLE);
            mResetButton.setContentDescription(resetDesc);
            mAddTimeButton.setVisibility(VISIBLE);
            mLastState = timer.getState();

            switch (mLastState) {
                case RESET -> {
                    mResetButton.setVisibility(GONE);
                    mResetButton.setContentDescription(null);
                    mAddTimeButton.setVisibility(INVISIBLE);
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(mContext, R.drawable.ic_fab_play));

                    mIndicatorState.setVisibility(GONE);

                    mTimerText.setVisibility(GONE);
                    mTimerBar.setVisibility(GONE);
                    mTimerTotalDurationText.setVisibility(VISIBLE);

                }

                case PAUSED -> {
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(mContext, R.drawable.ic_fab_play));

                    if (isIndicatorStateDisplayed) {
                        mIndicatorState.setVisibility(VISIBLE);
                        int indicatorColor = SettingsDAO.getPausedTimerIndicatorColor(mPrefs);
                        mGradientDrawable.setColor(indicatorColor);
                    }

                    mTimerText.setVisibility(VISIBLE);
                    mTimerBar.setVisibility(VISIBLE);
                    mTimerTotalDurationText.setVisibility(GONE);

                }

                case RUNNING -> {
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(mContext, R.drawable.ic_fab_pause));

                    if (isIndicatorStateDisplayed) {
                        mIndicatorState.setVisibility(VISIBLE);
                        int indicatorColor = SettingsDAO.getRunningTimerIndicatorColor(mPrefs);
                        mGradientDrawable.setColor(indicatorColor);
                    }

                    mTimerText.setVisibility(VISIBLE);
                    mTimerBar.setVisibility(VISIBLE);
                    mTimerTotalDurationText.setVisibility(GONE);

                }

                case EXPIRED, MISSED -> {
                    mPlayPauseButton.setIcon(AppCompatResources.getDrawable(mContext, R.drawable.ic_fab_stop));

                    if (isIndicatorStateDisplayed) {
                        mIndicatorState.setVisibility(VISIBLE);
                        int indicatorColor = SettingsDAO.getExpiredTimerIndicatorColor(mPrefs);
                        mGradientDrawable.setColor(indicatorColor);
                    }

                    mTimerText.setVisibility(VISIBLE);
                    mTimerBar.setVisibility(VISIBLE);
                    mTimerTotalDurationText.setVisibility(GONE);

                    mResetButton.setVisibility(GONE);
                }
            }
        }
    }

}
