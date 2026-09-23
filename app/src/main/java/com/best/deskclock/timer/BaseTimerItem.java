// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.best.deskclock.R;
import com.best.deskclock.data.Timer;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.FormattedTextUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.progressindicator.BaseProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This view is a visual representation of a {@link Timer}.
 */
public abstract class BaseTimerItem extends ConstraintLayout {

    protected Locale mLocale = Locale.getDefault();

    /** Formats and displays the text in the timer. */
    protected TimerTextController mTimerTextController;

    /** Drawable used to style the timer state indicator as a circle with dynamic fill. */
    protected GradientDrawable mGradientDrawable;

    protected Drawable mIconPlay, mIconPause, mIconStop, mIconDelete;

    protected int mColorPaused, mColorRunning, mColorExpired, mColorMissed;
    protected boolean mIsLandscapePhone, mIsTimerEndTimeDisplayed, mIsIndicatorStateDisplayed, mIsAddTimeZero;

    protected String mLastLabel = "", mLastButtonTimeRaw = "";
    protected String mCachedAddButtonText, mCachedAddButtonContentDesc;

    /** The last state of the timer that was rendered; used to avoid expensive operations. */
    protected Timer.State mLastState;
    protected boolean mLastDeleteAfterUse;

    protected CharSequence mTimerEndTimeFormatPattern;
    protected SimpleDateFormat mTimeFormat, mDayFormat;

    public BaseTimerItem(@NonNull Context context) {
        super(context);
    }

    public BaseTimerItem(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseTimerItem(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    protected abstract TextView getTimeText();
    protected abstract TextView getLabelText();
    protected abstract TextView getEndTimeText();
    protected abstract MaterialButton getAddTimeButton();
    protected abstract View getIndicatorState();
    protected abstract BaseProgressIndicator<?> getProgressIndicator();
    protected abstract View getResetButton();
    protected abstract MaterialButton getPlayPauseButton();

    protected abstract int getAddTimeHiddenVisibility();
    protected void onTimerTopUpdated() {}

    protected void onFinishInflateShared() {
        mTimerTextController = new TimerTextController(getTimeText());

        final Drawable drawable = ThemeUtils.circleDrawable();
        getIndicatorState().setBackground(drawable);
        mGradientDrawable = (GradientDrawable) getIndicatorState().getBackground();

        mIconPlay = AppCompatResources.getDrawable(getContext(), R.drawable.ic_fab_play);
        mIconPause = AppCompatResources.getDrawable(getContext(), R.drawable.ic_fab_pause);
        mIconStop = AppCompatResources.getDrawable(getContext(), R.drawable.ic_fab_stop);
        mIconDelete = AppCompatResources.getDrawable(getContext(), R.drawable.ic_delete);

        final int colorAccent = MaterialColors.getColor(getContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);
        final int textColorPrimary = getTimeText().getCurrentTextColor();
        final ColorStateList timeTextColor = new ColorStateList(
            new int[][]{{-android.R.attr.state_activated, -android.R.attr.state_pressed}, {}},
            new int[]{textColorPrimary, colorAccent});
        getTimeText().setTextColor(timeTextColor);
    }

    public void checkIsLandscapePhone(boolean isLandscapePhone) {
        mIsLandscapePhone = isLandscapePhone;
    }

    public void setGeneralFonts(@NonNull Typeface regular, @NonNull Typeface bold) {
        getLabelText().setTypeface(bold);
        if (!mIsLandscapePhone) {
            getAddTimeButton().setTypeface(bold);
        }
        getEndTimeText().setTypeface(regular, Typeface.ITALIC);
    }

    public void setTimerTimeFont(@NonNull Typeface timerTime) {
        getTimeText().setTypeface(timerTime);
    }

    public void setLocale(@NonNull Locale locale) {
        mLocale = locale;
    }

    public void setTimerEndTimeFormatPattern(@NonNull CharSequence formatPattern) {
        if (!TextUtils.equals(mTimerEndTimeFormatPattern, formatPattern)) {
            mTimerEndTimeFormatPattern = formatPattern;
            refreshFormatters();
        }
    }

    public void displayTimerEndTime(boolean isTimerEndTimeDisplayed) {
        mIsTimerEndTimeDisplayed = isTimerEndTimeDisplayed;
    }

    public void setIndicatorStateDisplay(boolean isIndicatorStateDisplayed) {
        mIsIndicatorStateDisplayed = isIndicatorStateDisplayed;
    }

    public void setIndicatorColors(int colorPaused, int colorRunning, int colorExpired, int colorMissed) {
        mColorPaused = colorPaused;
        mColorRunning = colorRunning;
        mColorExpired = colorExpired;
        mColorMissed = colorMissed;
    }

    /**
     * Dynamically updates the {@link Timer} display based on its current state.
     */
    public void updateTimeDisplay(@NonNull Timer timer, boolean animateProgress) {
        final boolean blinkOff = SystemClock.elapsedRealtime() % 1000 < 500;

        mTimerTextController.setTimeString(timer.getRemainingTime());

        if (getProgressIndicator() != null) {
            final boolean isBlinking = timer.isExpired() || timer.isMissed();
            final float targetAlpha = isBlinking
                ? (blinkOff ? 0f : 1f)
                : 1f;

            // Apply circle blinking
            if (getProgressIndicator().getAlpha() != targetAlpha) {
                getProgressIndicator().animate()
                    .alpha(targetAlpha)
                    .setDuration(AnimatorUtils.MEDIUM_ANIMATION_DURATION)
                    .start();
            }

            // Update circle only if visible
            if (!isBlinking || !blinkOff) {
                long totalLength = timer.getTotalLength();

                if (totalLength > 0) {
                    int progress = (int) ((timer.getRemainingTime() * 1000) / totalLength);

                    progress = Math.max(0, Math.min(1000, progress));

                    if (getProgressIndicator().getProgress() != progress) {
                        if (SdkUtils.isAtLeastAndroid7()) {
                            getProgressIndicator().setProgress(progress, animateProgress);
                        } else {
                            getProgressIndicator().setProgressCompat(progress, animateProgress);
                        }
                    }
                } else {
                    if (getProgressIndicator().getProgress() != 0) {
                        if (SdkUtils.isAtLeastAndroid7()) {
                            getProgressIndicator().setProgress(0, animateProgress);
                        } else {
                            getProgressIndicator().setProgressCompat(0, animateProgress);
                        }
                    }
                }
            }
        }

        final float textTargetAlpha = (!timer.isPaused() || !blinkOff || getTimeText().isPressed()) ? 1f : 0f;
        if (getTimeText().getAlpha() != textTargetAlpha) {
            getTimeText().animate()
                .alpha(textTargetAlpha)
                .setDuration(AnimatorUtils.SHORT_ANIMATION_DURATION)
                .start();
        }
    }

    /**
     * Initializes the {@link Timer} static visual elements when binding to a ViewHolder.
     */
    public void bindTimer(@NonNull Timer timer, boolean animate) {
        // Initialize the label
        final String label = timer.getLabel();

        if (!TextUtils.isEmpty(label)) {
            if (!TextUtils.equals(label, mLastLabel)) {
                getLabelText().setText(label);
            }
            getLabelText().setAlpha(1f);
            getLabelText().setVisibility(VISIBLE);
        } else {
            getLabelText().setVisibility(GONE);
        }

        mLastLabel = label;

        // Initialize the circle
        if (getProgressIndicator() != null) {
            getProgressIndicator().animate().cancel();
            getProgressIndicator().setAlpha(1f);
        }

        // Initialize the alpha value of the time text color
        getTimeText().animate().cancel();
        getTimeText().setAlpha(1f);

        // Initialize the time value to add to timer in the "Add time" button
        String buttonTime = timer.getButtonTime();

        if (!buttonTime.equals(mLastButtonTimeRaw)) {
            mLastButtonTimeRaw = buttonTime;

            long totalSeconds = Long.parseLong(buttonTime);
            mIsAddTimeZero = totalSeconds == 0;

            long buttonTimeMinutes = (totalSeconds) / 60;
            long buttonTimeSeconds = totalSeconds % 60;

            String buttonTimeFormatted = String.format(
                mLocale,
                buttonTimeMinutes < 10 ? "%d:%02d" : "%02d:%02d",
                buttonTimeMinutes,
                buttonTimeSeconds
            );

            mCachedAddButtonText = getContext().getString(R.string.timer_add_custom_time, buttonTimeFormatted);

            mCachedAddButtonContentDesc = buttonTimeSeconds == 0
                ? getContext().getString(R.string.timer_add_custom_time_description, String.valueOf(buttonTimeMinutes))
                : getContext().getString(R.string.timer_add_custom_time_with_seconds_description,
                String.valueOf(buttonTimeMinutes),
                String.valueOf(buttonTimeSeconds));
        }

        final boolean deleteAfterUse = timer.getDeleteAfterUse();

        // Initialize some potentially expensive areas of the user interface only on state changes.
        if (timer.getState() != mLastState || deleteAfterUse != mLastDeleteAfterUse) {
            getResetButton().setVisibility(VISIBLE);

            mLastState = timer.getState();
            mLastDeleteAfterUse = deleteAfterUse;

            switch (mLastState) {
                case RESET -> {
                    getResetButton().setVisibility(INVISIBLE);
                    getPlayPauseButton().setIcon(mIconPlay);
                }

                case PAUSED -> {
                    getResetButton().setVisibility(VISIBLE);
                    getPlayPauseButton().setIcon(mIconPlay);
                }

                case RUNNING -> {
                    getResetButton().setVisibility(VISIBLE);
                    getPlayPauseButton().setIcon(mIconPause);
                }

                case EXPIRED, MISSED -> {
                    getResetButton().setVisibility(INVISIBLE);
                    getPlayPauseButton().setIcon(deleteAfterUse ? mIconDelete : mIconStop);
                }
            }
        }

        updateAddTimeButtonDisplay(timer.getState());

        updateIndicator(timer.getState(), label);

        updateEndTimeDisplay(timer);

        updateTimeDisplay(timer, animate);

        onTimerTopUpdated();
    }

    protected void updateAddTimeButtonDisplay(@NonNull Timer.State state) {
        if (state == Timer.State.RESET || mIsAddTimeZero) {
            getAddTimeButton().setVisibility(getAddTimeHiddenVisibility());
            return;
        }

        if (!mIsLandscapePhone) {
            getAddTimeButton().setText(mCachedAddButtonText);
        }
        getAddTimeButton().setContentDescription(mCachedAddButtonContentDesc);
        getAddTimeButton().setVisibility(VISIBLE);
    }

    private void updateIndicator(@NonNull Timer.State state, @Nullable String label) {
        if (!mIsIndicatorStateDisplayed) {
            getIndicatorState().setVisibility(GONE);
            return;
        }

        if (state == Timer.State.RESET) {
            getIndicatorState().setVisibility(TextUtils.isEmpty(label) ? INVISIBLE : GONE);
            return;
        }

        int color = switch (state) {
            case PAUSED -> mColorPaused;
            case RUNNING -> mColorRunning;
            case EXPIRED -> mColorExpired;
            case MISSED -> mColorMissed;
            default -> Color.TRANSPARENT;
        };

        mGradientDrawable.setColor(color);
        getIndicatorState().setVisibility(VISIBLE);
    }

    private void updateEndTimeDisplay(@NonNull Timer timer) {
        if (!mIsTimerEndTimeDisplayed) {
            getEndTimeText().setVisibility(GONE);
            return;
        }

        if (timer.getState() == Timer.State.RUNNING) {
            long endTimeMillis = timer.getWallClockExpirationTime();

            CharSequence formattedTime;

            if (mTimeFormat == null || mDayFormat == null) {
                refreshFormatters();
            }

            Date endDate = new Date(endTimeMillis);
            String timeString = mTimeFormat.format(endDate);

            if (!DateUtils.isToday(endTimeMillis)) {
                String dayString = mDayFormat.format(endDate);
                String capitalizedDay = FormattedTextUtils.capitalizeFirstLetter(dayString, mLocale);

                formattedTime = TextUtils.concat(capitalizedDay, ", ", timeString);
            } else {
                formattedTime = timeString;
            }

            CharSequence expandedText = TextUtils.expandTemplate(getContext().getText(R.string.timer_end_time_label), formattedTime);

            // Add a "No-Break Space" before and after the text to center it properly and prevent it
            // from being cut off at the end due to the italic formatting.
            CharSequence finalText = TextUtils.concat("\u00A0", expandedText, "\u00A0");

            getEndTimeText().setText(finalText);
            getEndTimeText().setVisibility(VISIBLE);
        } else {
            getEndTimeText().setVisibility(INVISIBLE);
        }
    }

    private void refreshFormatters() {
        if (mTimerEndTimeFormatPattern != null) {
            mTimeFormat = new SimpleDateFormat(mTimerEndTimeFormatPattern.toString(), mLocale);
        }

        String dayPattern = getContext().getString(R.string.abbrev_wday_only);
        mDayFormat = new SimpleDateFormat(dayPattern, mLocale);
    }

}
