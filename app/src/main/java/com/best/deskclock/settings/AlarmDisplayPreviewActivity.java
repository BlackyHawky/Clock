/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.animation.PathInterpolatorCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.widget.AnalogClock;
import com.best.deskclock.widget.CircleView;

public class AlarmDisplayPreviewActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnTouchListener {

    private static final TimeInterpolator PULSE_INTERPOLATOR =
            PathInterpolatorCompat.create(0.4f, 0.0f, 0.2f, 1.0f);
    private static final TimeInterpolator REVEAL_INTERPOLATOR =
            PathInterpolatorCompat.create(0.0f, 0.0f, 0.2f, 1.0f);
    private static final int PULSE_DURATION_MILLIS = 1000;
    private static final int ALARM_BOUNCE_DURATION_MILLIS = 500;
    private static final int ALERT_REVEAL_DURATION_MILLIS = 500;
    private static final int ALERT_FADE_DURATION_MILLIS = 500;
    private static final int ALERT_DISMISS_DELAY_MILLIS = 2000;
    private static final float BUTTON_SCALE_DEFAULT = 0.7f;
    private static final int BUTTON_DRAWABLE_ALPHA_DEFAULT = 255;
    private final Handler mHandler = new Handler();
    private float mAlarmTitleFontSize;
    private int mAlarmTitleColor;
    private int mSnoozeMinutes;
    private boolean isSwipeActionEnabled;
    private ViewGroup mAlertView;
    private TextView mAlertTitleView;
    private TextView mAlertInfoView;
    private LinearLayout mRingtoneLayout;
    private TextView mRingtoneTitle;
    private ImageView mRingtoneIcon;
    private ViewGroup mContentView;
    private ImageView mAlarmButton;
    private ImageView mSnoozeButton;
    private ImageView mDismissButton;
    private TextView mHintView;
    private ValueAnimator mAlarmAnimator;
    private ValueAnimator mSnoozeAnimator;
    private ValueAnimator mDismissAnimator;
    private ValueAnimator mPulseAnimator;
    private int mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;
    private Vibrator mVibrator;
    private boolean mAreSnoozedOrDismissedAlarmVibrationsEnabled;
    private boolean mIsFadeTransitionsEnabled;
    private boolean mIsRingtoneTitleDisplayed;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences prefs = getDefaultSharedPreferences(this);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mAreSnoozedOrDismissedAlarmVibrationsEnabled = SettingsDAO.areSnoozedOrDismissedAlarmVibrationsEnabled(prefs);

        // Honor rotation on tablets; fix the orientation on phones.
        if (ThemeUtils.isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        AlarmUtils.hideSystemBarsOfTriggeredAlarms(getWindow(), getWindow().getDecorView());

        final String getDarkMode = SettingsDAO.getDarkMode(prefs);
        final boolean isAmoledMode = ThemeUtils.isNight(getResources()) && getDarkMode.equals(AMOLED_DARK_MODE);
        int alarmBackgroundColor = isAmoledMode
                ? SettingsDAO.getAlarmBackgroundAmoledColor(prefs)
                : SettingsDAO.getAlarmBackgroundColor(prefs);
        int alarmClockColor = SettingsDAO.getAlarmClockColor(prefs);
        float alarmDigitalClockFontSize = SettingsDAO.getAlarmDigitalClockFontSize(prefs);
        mAlarmTitleFontSize = SettingsDAO.getAlarmTitleFontSize(prefs);
        mAlarmTitleColor = SettingsDAO.getAlarmTitleColor(prefs);
        int snoozeButtonColor = SettingsDAO.getSnoozeButtonColor(prefs);
        int dismissButtonColor = SettingsDAO.getDismissButtonColor(prefs);
        int alarmButtonColor = SettingsDAO.getAlarmButtonColor(prefs);
        int pulseColor = SettingsDAO.getPulseColor(prefs);

        setContentView(R.layout.alarm_activity);

        getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));

        mAlertView = findViewById(R.id.alert);
        mAlertTitleView = mAlertView.findViewById(R.id.alert_title);
        mAlertInfoView = mAlertView.findViewById(R.id.alert_info);

        mContentView = findViewById(R.id.content);
        mAlarmButton = mContentView.findViewById(R.id.alarm);
        mSnoozeButton = mContentView.findViewById(R.id.snooze);
        mDismissButton = mContentView.findViewById(R.id.dismiss);
        mHintView = mContentView.findViewById(R.id.hint);

        mIsRingtoneTitleDisplayed = SettingsDAO.isRingtoneTitleDisplayed(prefs);
        if (mIsRingtoneTitleDisplayed) {
            mRingtoneLayout = mContentView.findViewById(R.id.ringtone_layout);
            mRingtoneTitle = mContentView.findViewById(R.id.ringtone_title);
            mRingtoneIcon = mContentView.findViewById(R.id.ringtone_icon);
            displayRingtoneTitle();
            mContentView.setOnClickListener(this);
        }

        mAlarmButton.setImageDrawable(ThemeUtils.toScaledBitmapDrawable(
                mAlarmButton.getContext(), R.drawable.ic_tab_alarm_static, 2.5f)
        );
        mAlarmButton.setColorFilter(alarmButtonColor);

        mDismissButton.setImageDrawable(ThemeUtils.toScaledBitmapDrawable(
                mDismissButton.getContext(), R.drawable.ic_alarm_off, 2f)
        );
        mDismissButton.setColorFilter(dismissButtonColor);
        mDismissButton.setBackgroundTintList(ColorStateList.valueOf(pulseColor));

        mSnoozeMinutes = SettingsDAO.getSnoozeLength(prefs);
        mSnoozeButton.setImageDrawable(ThemeUtils.toScaledBitmapDrawable(
                mSnoozeButton.getContext(), R.drawable.ic_snooze, 2f)
        );
        mSnoozeButton.setColorFilter(snoozeButtonColor);
        mSnoozeButton.setBackgroundTintList(ColorStateList.valueOf(pulseColor));

        final TextView titleView = mContentView.findViewById(R.id.alarm_title);
        titleView.setText(R.string.app_label);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
        titleView.setTextColor(mAlarmTitleColor);
        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        titleView.setSelected(true);

        final AnalogClock analogClock = findViewById(R.id.analog_clock);
        final TextClock digitalClock = mContentView.findViewById(R.id.digital_clock);
        final DataModel.ClockStyle alarmClockStyle = SettingsDAO.getAlarmClockStyle(prefs);
        final boolean isAlarmSecondsHandDisplayed = SettingsDAO.isAlarmSecondsHandDisplayed(prefs);
        ClockUtils.setClockStyle(alarmClockStyle, digitalClock, analogClock);
        ClockUtils.setClockSecondsEnabled(alarmClockStyle, digitalClock, analogClock, isAlarmSecondsHandDisplayed);
        ClockUtils.setTimeFormat(digitalClock, false);
        digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, alarmDigitalClockFontSize);
        digitalClock.setTextColor(alarmClockColor);

        final CircleView pulseView = mContentView.findViewById(R.id.pulse);
        pulseView.setFillColor(pulseColor);

        isSwipeActionEnabled = SettingsDAO.isSwipeActionEnabled(prefs);
        if (isSwipeActionEnabled) {
            mAlarmButton.setOnTouchListener(this);
        } else {
            mAlarmButton.setOnClickListener(this);
        }
        mSnoozeButton.setOnClickListener(this);
        mDismissButton.setOnClickListener(this);

        mAlarmAnimator = AnimatorUtils.getScaleAnimator(mAlarmButton, 1.0f, 0.0f);
        mSnoozeAnimator = getButtonAnimator(mSnoozeButton, snoozeButtonColor);
        mDismissAnimator = getButtonAnimator(mDismissButton, dismissButtonColor);
        mPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(pulseView,
                PropertyValuesHolder.ofFloat(CircleView.RADIUS, 0.0f, pulseView.getRadius()),
                PropertyValuesHolder.ofObject(CircleView.FILL_COLOR, AnimatorUtils.ARGB_EVALUATOR,
                        ColorUtils.setAlphaComponent(pulseView.getFillColor(), 0)));
        mPulseAnimator.setDuration(PULSE_DURATION_MILLIS);
        mPulseAnimator.setInterpolator(PULSE_INTERPOLATOR);
        mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mPulseAnimator.start();

        mIsFadeTransitionsEnabled = SettingsDAO.isFadeTransitionsEnabled(prefs);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                if (mIsFadeTransitionsEnabled) {
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                } else {
                    overridePendingTransition(R.anim.activity_slide_from_left, R.anim.activity_slide_to_right);
                }
            }
        });
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();

        resetAnimations();
    }

    @Override
    public void onClick(View view) {
        // If alarm swiping is disabled in settings, allow snooze/dismiss by tapping on respective icons.
        if (!isSwipeActionEnabled) {
            if (view == mSnoozeButton) {
                snooze();
            } else if (view == mDismissButton) {
                dismiss();
            } else if (view == mAlarmButton) {
                hintAlarmAction();
            }
            return;
        }

        if (view == mSnoozeButton) {
            hintSnooze();
        } else if (view == mDismissButton) {
            hintDismiss();
        } else if (view == mAlarmButton) {
            hintAlarmAction();
        } else if (view == mContentView && mIsRingtoneTitleDisplayed && mHintView.getVisibility() != GONE) {
            mHintView.setVisibility(GONE);
            mRingtoneLayout.setVisibility(VISIBLE);
            ObjectAnimator.ofFloat(mRingtoneLayout, View.ALPHA, 0f, 1f).start();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            // Track the pointer that initiated the touch sequence.
            mInitialPointerIndex = event.getPointerId(event.getActionIndex());

            // Stop the pulse, allowing the last pulse to finish.
            mPulseAnimator.setRepeatCount(0);
        } else if (action == MotionEvent.ACTION_CANCEL) {
            // Clear the pointer index.
            mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;

            // Reset everything.
            resetAnimations();
        }

        final int actionIndex = event.getActionIndex();
        if (mInitialPointerIndex == MotionEvent.INVALID_POINTER_ID
                || mInitialPointerIndex != event.getPointerId(actionIndex)) {
            // Ignore any pointers other than the initial one, bail early.
            return true;
        }

        final int[] contentLocation = {0, 0};
        mContentView.getLocationOnScreen(contentLocation);

        final float x = event.getRawX() - contentLocation[0];
        final float y = event.getRawY() - contentLocation[1];

        final int alarmLeft = mAlarmButton.getLeft() + mAlarmButton.getPaddingLeft();
        final int alarmRight = mAlarmButton.getRight() - mAlarmButton.getPaddingRight();

        final float snoozeFraction, dismissFraction;
        if (mContentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            snoozeFraction = getFraction(alarmRight, mSnoozeButton.getLeft(), x);
            dismissFraction = getFraction(alarmLeft, mDismissButton.getRight(), x);
        } else {
            snoozeFraction = getFraction(alarmLeft, mSnoozeButton.getRight(), x);
            dismissFraction = getFraction(alarmRight, mDismissButton.getLeft(), x);
        }
        setAnimatedFractions(snoozeFraction, dismissFraction);

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;
            if (snoozeFraction == 1.0f) {
                snooze();
            } else if (dismissFraction == 1.0f) {
                dismiss();
            } else {
                if (snoozeFraction > 0.0f || dismissFraction > 0.0f) {
                    // Animate back to the initial state.
                    AnimatorUtils.reverse(mAlarmAnimator, mSnoozeAnimator, mDismissAnimator);
                } else if (mAlarmButton.getTop() <= y && y <= mAlarmButton.getBottom()) {
                    // The alarm action hint is displayed after the user touches the alarm button.
                    hintAlarmAction();
                }

                // Restart the pulse.
                mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                if (!mPulseAnimator.isStarted()) {
                    mPulseAnimator.start();
                }
            }
        }

        return true;
    }

    private void hintSnooze() {
        final int alarmLeft = mAlarmButton.getLeft() + mAlarmButton.getPaddingLeft();
        final int alarmRight = mAlarmButton.getRight() - mAlarmButton.getPaddingRight();
        final int hintLeftResId = R.string.description_direction_left;
        final float translationX = Math.max(mSnoozeButton.getLeft() - alarmRight, 0)
                + Math.min(mSnoozeButton.getRight() - alarmLeft, 0);
        getAlarmBounceAnimator(translationX, hintLeftResId).start();
    }

    private void hintDismiss() {
        final int alarmLeft = mAlarmButton.getLeft() + mAlarmButton.getPaddingLeft();
        final int alarmRight = mAlarmButton.getRight() - mAlarmButton.getPaddingRight();
        final int hintRightResId = R.string.description_direction_right;
        final float translationX = Math.max(mDismissButton.getLeft() - alarmRight, 0)
                + Math.min(mDismissButton.getRight() - alarmLeft, 0);
        getAlarmBounceAnimator(translationX, hintRightResId).start();
    }

    private void hintAlarmAction() {
        final int hintAlarmButtonResId;
        hintAlarmButtonResId = R.string.description_direction_both;
        getAlarmBounceAnimator(0, hintAlarmButtonResId).start();
    }

    /**
     * Set animators to initial values and restart pulse on alarm button.
     */
    private void resetAnimations() {
        // Set the animators to their initial values.
        setAnimatedFractions(0.0f, 0.0f);
        // Restart the pulse.
        mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        if (!mPulseAnimator.isStarted()) {
            mPulseAnimator.start();
        }
    }

    /**
     * Display ringtone title if enabled in <i>"Customize alarm display"</i> settings.
     */
    private void displayRingtoneTitle() {
        final Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        final Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        final Drawable musicIcon = AppCompatResources.getDrawable(this, R.drawable.ic_music_note);
        if (musicIcon != null) {
            musicIcon.setTint(mAlarmTitleColor);
        }
        mRingtoneIcon.setImageDrawable(musicIcon);
        mRingtoneTitle.setText(ringtone.getTitle(this));
        mRingtoneTitle.setTextColor(mAlarmTitleColor);
        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        mRingtoneTitle.setSelected(true);
    }

    /**
     * Perform snooze animation.
     */
    private void snooze() {
        if (mAreSnoozedOrDismissedAlarmVibrationsEnabled) {
            performDoubleVibration();
        }
        setAnimatedFractions(1.0f, 0.0f);

        final String infoText = getResources().getQuantityString(
                    R.plurals.alarm_alert_snooze_duration, mSnoozeMinutes, mSnoozeMinutes);
        getAlertAnimator(mSnoozeButton, R.string.alarm_alert_snoozed_text, infoText).start();
    }

    /**
     * Perform dismiss animation.
     */
    private void dismiss() {
        if (mAreSnoozedOrDismissedAlarmVibrationsEnabled) {
            performSingleVibration();
        }
        setAnimatedFractions(0.0f, 1.0f);
        getAlertAnimator(mDismissButton, R.string.alarm_alert_off_text, null).start();
    }

    /**
     * Perform single vibration if alarm is dismissed.
     */
    private void performSingleVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrator.vibrate(VibrationEffect.createWaveform(
                    new long[]{700, 500}, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            mVibrator.vibrate(new long[]{700, 500}, -1);
        }
    }

    /**
     * Perform double vibration if alarm is snoozed.
     */
    private void performDoubleVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrator.vibrate(VibrationEffect.createWaveform(
                    new long[]{700, 200, 100, 500}, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            mVibrator.vibrate(new long[]{700, 200, 100, 500}, -1);
        }
    }

    private void setAnimatedFractions(float snoozeFraction, float dismissFraction) {
        final float alarmFraction = Math.max(snoozeFraction, dismissFraction);
        mAlarmAnimator.setCurrentFraction(alarmFraction);
        mSnoozeAnimator.setCurrentFraction(snoozeFraction);
        mDismissAnimator.setCurrentFraction(dismissFraction);
    }

    private float getFraction(float x0, float x1, float x) {
        return Math.max(Math.min((x - x0) / (x1 - x0), 1.0f), 0.0f);
    }

    private ValueAnimator getButtonAnimator(ImageView button, int tintColor) {
        return ObjectAnimator.ofPropertyValuesHolder(button,
                PropertyValuesHolder.ofFloat(View.SCALE_X, BUTTON_SCALE_DEFAULT, 1.0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, BUTTON_SCALE_DEFAULT, 1.0f),
                PropertyValuesHolder.ofInt(AnimatorUtils.BACKGROUND_ALPHA, 0, 255),
                PropertyValuesHolder.ofInt(AnimatorUtils.DRAWABLE_ALPHA,
                        BUTTON_DRAWABLE_ALPHA_DEFAULT, 255),
                PropertyValuesHolder.ofObject(AnimatorUtils.DRAWABLE_TINT,
                        AnimatorUtils.ARGB_EVALUATOR, Color.WHITE, tintColor)
        );
    }

    private ValueAnimator getAlarmBounceAnimator(float translationX, final int hintResId) {
        final ValueAnimator bounceAnimator = ObjectAnimator.ofFloat(mAlarmButton,
                View.TRANSLATION_X, mAlarmButton.getTranslationX(), translationX, 0.0f);
        bounceAnimator.setInterpolator(AnimatorUtils.DECELERATE_ACCELERATE_INTERPOLATOR);
        bounceAnimator.setDuration(ALARM_BOUNCE_DURATION_MILLIS);
        bounceAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                mHintView.setText(hintResId);
                mHintView.setTextColor(mAlarmTitleColor);
                if (mHintView.getVisibility() != VISIBLE) {
                    if (mRingtoneLayout != null) {
                        mRingtoneLayout.setVisibility(GONE);
                    }
                    mHintView.setVisibility(VISIBLE);

                    ObjectAnimator.ofFloat(mHintView, View.ALPHA, 0f, 1f).start();
                }
            }
        });

        return bounceAnimator;
    }

    private Animator getAlertAnimator(final View source, final int titleResId, final String infoText) {

        final ViewGroup containerView = findViewById(android.R.id.content);

        final Rect sourceBounds = new Rect(0, 0, source.getHeight(), source.getWidth());
        containerView.offsetDescendantRectToMyCoords(source, sourceBounds);

        final int centerX = sourceBounds.centerX();
        final int centerY = sourceBounds.centerY();

        final int xMax = Math.max(centerX, containerView.getWidth() - centerX);
        final int yMax = Math.max(centerY, containerView.getHeight() - centerY);

        final float startRadius = Math.max(sourceBounds.width(), sourceBounds.height()) / 2.0f;
        final float endRadius = (float) Math.sqrt(xMax * xMax + yMax * yMax);

        final CircleView revealView = new CircleView(this)
                .setCenterX(centerX)
                .setCenterY(centerY)
                .setFillColor(Color.TRANSPARENT);
        containerView.addView(revealView);

        // TODO: Fade out source icon over the reveal (like LOLLIPOP version).

        final Animator revealAnimator = ObjectAnimator.ofFloat(revealView, CircleView.RADIUS, startRadius, endRadius);
        revealAnimator.setDuration(ALERT_REVEAL_DURATION_MILLIS);
        revealAnimator.setInterpolator(REVEAL_INTERPOLATOR);
        revealAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mAlertView.setVisibility(View.VISIBLE);
                mAlertTitleView.setText(titleResId);
                mAlertTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
                mAlertTitleView.setTextColor(mAlarmTitleColor);
                mAlertTitleView.setTypeface(Typeface.DEFAULT_BOLD);

                if (infoText != null) {
                    mAlertInfoView.setVisibility(View.VISIBLE);
                    mAlertInfoView.setText(infoText);
                    mAlertInfoView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
                    mAlertInfoView.setTextColor(mAlarmTitleColor);
                    mAlertInfoView.setTypeface(Typeface.DEFAULT_BOLD);
                }
                mContentView.setVisibility(View.GONE);
            }
        });

        final ValueAnimator fadeAnimator = ObjectAnimator.ofFloat(revealView, View.ALPHA, 0.0f);
        fadeAnimator.setDuration(ALERT_FADE_DURATION_MILLIS);

        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                containerView.removeView(revealView);
            }
        });

        final AnimatorSet alertAnimator = new AnimatorSet();
        alertAnimator.play(revealAnimator).before(fadeAnimator);

        alertAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mHandler.postDelayed(() -> finishActivity(), ALERT_DISMISS_DELAY_MILLIS);
            }
        });

        return alertAnimator;
    }

    private void finishActivity() {
        finish();
        if (mIsFadeTransitionsEnabled) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }

}
