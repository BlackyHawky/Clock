/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_AMOLED_DARK_MODE;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.animation.PathInterpolatorCompat;

import com.best.deskclock.AnalogClock;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.DataModel.PowerButtonBehavior;
import com.best.deskclock.data.DataModel.VolumeButtonBehavior;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.CircleView;

import java.util.List;

public class AlarmActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("AlarmActivity");
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

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LOGGER.i("Finished binding to AlarmService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LOGGER.i("Disconnected from AlarmService");
        }
    };

    private AlarmInstance mAlarmInstance;
    private boolean mAlarmHandled;
    private VolumeButtonBehavior mVolumeBehavior;
    private PowerButtonBehavior mPowerBehavior;
    private float mAlarmTitleFontSize;
    private int mAlarmTitleColor;
    private int mSnoozeMinutes;
    private boolean isSwipeActionEnabled;
    private boolean mReceiverRegistered;
    /**
     * Whether the AlarmService is currently bound
     */
    private boolean mServiceBound;
    private AccessibilityManager mAccessibilityManager;
    private ViewGroup mAlertView;
    private TextView mAlertTitleView;
    private TextView mAlertInfoView;
    private ViewGroup mContentView;
    private ImageView mAlarmButton;
    private ImageView mSnoozeButton;
    private ImageView mDismissButton;
    private TextView mHintView;
    private TextView mRingtoneTitle;
    private ValueAnimator mAlarmAnimator;
    private ValueAnimator mSnoozeAnimator;
    private ValueAnimator mDismissAnimator;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LOGGER.v("Received broadcast: %s", action);

            if (!mAlarmHandled) {
                if (action != null) {
                    switch (action) {
                        case AlarmService.ALARM_SNOOZE_ACTION -> snooze();
                        case AlarmService.ALARM_DISMISS_ACTION -> dismiss();
                        case AlarmService.ALARM_DONE_ACTION -> finish();
                        default -> LOGGER.i("Unknown broadcast: %s", action);
                    }
                }
            } else {
                LOGGER.v("Ignored broadcast: %s", action);
            }
        }
    };

    private final BroadcastReceiver PowerBtnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)
                        || intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    // Power keys dismiss the alarm.
                    if (!mAlarmHandled) {
                        if (mPowerBehavior == PowerButtonBehavior.SNOOZE) {
                            snooze();
                        } else if (mPowerBehavior == PowerButtonBehavior.DISMISS) {
                            dismiss();
                        }
                    }
                }
            }
        }
    };

    private ValueAnimator mPulseAnimator;
    private int mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register Power button (screen off) intent receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(PowerBtnReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(PowerBtnReceiver, filter);
        }

        setVolumeControlStream(AudioManager.STREAM_ALARM);
        final long instanceId = AlarmInstance.getId(getIntent().getData());
        mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), instanceId);
        if (mAlarmInstance == null) {
            // The alarm was deleted before the activity got created, so just finish()
            LOGGER.e("Error displaying alarm for intent: %s", getIntent());
            finish();
            return;
        } else if (mAlarmInstance.mAlarmState != AlarmInstance.FIRED_STATE) {
            LOGGER.i("Skip displaying alarm for instance: %s", mAlarmInstance);
            finish();
            return;
        }

        LOGGER.i("Displaying alarm for instance: %s", mAlarmInstance);

        // Get the volume/camera button behavior setting
        mVolumeBehavior = DataModel.getDataModel().getAlarmVolumeButtonBehavior();

        // Get the power button behavior setting
        mPowerBehavior = DataModel.getDataModel().getAlarmPowerButtonBehavior();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        // Honor rotation on tablets; fix the orientation on phones.
        if (!Utils.isLandscape(getApplicationContext())) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        // Hide navigation bar to minimize accidental tap on Home key
        hideNavigationBar();

        mAccessibilityManager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);

        setContentView(R.layout.alarm_activity);

        final String darkMode = DataModel.getDataModel().getDarkMode();
        final boolean isAmoledMode = Utils.isNight(getResources()) && darkMode.equals(KEY_AMOLED_DARK_MODE);
        int alarmBackgroundColor = isAmoledMode
                ? DataModel.getDataModel().getAlarmBackgroundAmoledColor()
                : DataModel.getDataModel().getAlarmBackgroundColor();
        int alarmClockColor = DataModel.getDataModel().getAlarmClockColor();
        float alarmClockFontSize = Float.parseFloat(DataModel.getDataModel().getAlarmClockFontSize());
        mAlarmTitleFontSize = Float.parseFloat(DataModel.getDataModel().getAlarmTitleFontSize());
        mAlarmTitleColor = DataModel.getDataModel().getAlarmTitleColor();
        int snoozeButtonColor = DataModel.getDataModel().getSnoozeButtonColor();
        int dismissButtonColor = DataModel.getDataModel().getDismissButtonColor();
        int alarmButtonColor = DataModel.getDataModel().getAlarmButtonColor();
        int pulseColor = DataModel.getDataModel().getPulseColor();

        getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));

        mAlertView = findViewById(R.id.alert);
        mAlertTitleView = mAlertView.findViewById(R.id.alert_title);
        mAlertInfoView = mAlertView.findViewById(R.id.alert_info);

        mContentView = findViewById(R.id.content);
        mAlarmButton = mContentView.findViewById(R.id.alarm);
        mSnoozeButton = mContentView.findViewById(R.id.snooze);
        mDismissButton = mContentView.findViewById(R.id.dismiss);
        mHintView = mContentView.findViewById(R.id.hint);
        mRingtoneTitle = mContentView.findViewById(R.id.ringtone_title);

        boolean isRingtoneTitleDisplayed = DataModel.getDataModel().isRingtoneTitleDisplayed();
        if (isRingtoneTitleDisplayed) {
            displayRingtoneTitle();
        }

        mAlarmButton.setImageDrawable(Utils.toScaledBitmapDrawable(
                mAlarmButton.getContext(), R.drawable.ic_tab_alarm_static, 2.5f)
        );
        mAlarmButton.setColorFilter(alarmButtonColor);

        mDismissButton.setImageDrawable(Utils.toScaledBitmapDrawable(
                mDismissButton.getContext(), R.drawable.ic_alarm_off, 2f)
        );
        mDismissButton.setColorFilter(dismissButtonColor);
        mDismissButton.setBackgroundTintList(ColorStateList.valueOf(pulseColor));

        mSnoozeMinutes = DataModel.getDataModel().getSnoozeLength();
        mSnoozeButton.setImageDrawable(mSnoozeMinutes == -1 || !mAlarmInstance.mAlarmSnoozeActions
                ? Utils.toScaledBitmapDrawable(mSnoozeButton.getContext(), R.drawable.ic_alarm_off, 2f)
                : Utils.toScaledBitmapDrawable(mSnoozeButton.getContext(), R.drawable.ic_snooze, 2f)
        );
        mSnoozeButton.setColorFilter(mSnoozeMinutes == -1 || !mAlarmInstance.mAlarmSnoozeActions
                ? dismissButtonColor
                : snoozeButtonColor
        );
        mSnoozeButton.setBackgroundTintList(ColorStateList.valueOf(pulseColor));

        final TextView titleView = mContentView.findViewById(R.id.title);
        final AnalogClock analogClock = findViewById(R.id.analog_clock);
        final TextClock digitalClock = mContentView.findViewById(R.id.digital_clock);
        final CircleView pulseView = mContentView.findViewById(R.id.pulse);
        pulseView.setFillColor(pulseColor);

        final DataModel.ClockStyle alarmClockStyle = DataModel.getDataModel().getAlarmClockStyle();
        final boolean isAlarmSecondsHandDisplayed = DataModel.getDataModel().isAlarmSecondsHandDisplayed();
        ClockUtils.setClockStyle(alarmClockStyle, digitalClock, analogClock);
        ClockUtils.setClockSecondsEnabled(alarmClockStyle, digitalClock, analogClock, isAlarmSecondsHandDisplayed);

        titleView.setText(mAlarmInstance.getLabelOrDefault(this));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
        titleView.setTextColor(mAlarmTitleColor);
        ClockUtils.setTimeFormat(digitalClock, false);
        digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, alarmClockFontSize);
        digitalClock.setTextColor(alarmClockColor);

        isSwipeActionEnabled = DataModel.getDataModel().isSwipeActionEnabled();
        if (isSwipeActionEnabled) {
            mAlarmButton.setOnTouchListener(this);
        } else {
            mAlarmButton.setOnClickListener(this);
        }
        mSnoozeButton.setOnClickListener(this);
        mDismissButton.setOnClickListener(this);

        mAlarmAnimator = AnimatorUtils.getScaleAnimator(mAlarmButton, 1.0f, 0.0f);
        mSnoozeAnimator = getButtonAnimator(mSnoozeButton, R.color.md_theme_onSurfaceVariant);
        mDismissAnimator = getButtonAnimator(mDismissButton, R.color.md_theme_onSurfaceVariant);
        mPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(pulseView,
                PropertyValuesHolder.ofFloat(CircleView.RADIUS, 0.0f, pulseView.getRadius()),
                PropertyValuesHolder.ofObject(CircleView.FILL_COLOR, AnimatorUtils.ARGB_EVALUATOR,
                        ColorUtils.setAlphaComponent(pulseView.getFillColor(), 0)));
        mPulseAnimator.setDuration(PULSE_DURATION_MILLIS);
        mPulseAnimator.setInterpolator(PULSE_INTERPOLATOR);
        mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mPulseAnimator.start();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();

        // Re-query for AlarmInstance in case the state has changed externally
        final long instanceId = AlarmInstance.getId(getIntent().getData());
        mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), instanceId);

        if (mAlarmInstance == null) {
            LOGGER.i("No alarm instance for instanceId: %d", instanceId);
            finish();
            return;
        }

        // Verify that the alarm is still firing before showing the activity
        if (mAlarmInstance.mAlarmState != AlarmInstance.FIRED_STATE) {
            LOGGER.i("Skip displaying alarm for instance: %s", mAlarmInstance);
            finish();
            return;
        }

        if (!mReceiverRegistered) {
            // Register to get the alarm done/snooze/dismiss intent.
            final IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
            filter.addAction(AlarmService.ALARM_SNOOZE_ACTION);
            filter.addAction(AlarmService.ALARM_DISMISS_ACTION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(mReceiver, filter);
            }
            mReceiverRegistered = true;
        }

        bindAlarmService();

        resetAnimations();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unbindAlarmService();

        // Skip if register didn't happen to avoid IllegalArgumentException
        if (mReceiverRegistered) {
            unregisterReceiver(mReceiver);
            mReceiverRegistered = false;
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent keyEvent) {
        // Do this in dispatch to intercept a few of the system keys.
        LOGGER.v("dispatchKeyEvent: %s", keyEvent);

        final int keyCode = keyEvent.getKeyCode();
        switch (keyCode) {
            // Volume keys and camera keys dismiss the alarm.
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (!mAlarmHandled) {
                    switch (mVolumeBehavior) {
                        case DO_NOTHING -> {
                            return keyEvent.getAction() != KeyEvent.ACTION_UP;
                        }
                        case SNOOZE_ALARM -> {
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                snooze();
                            }
                            return true;
                        }
                        case DISMISS_ALARM -> {
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                dismiss();
                            }
                            return true;
                        }
                    }
                }
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    @Override
    public void onClick(View view) {
        if (mAlarmHandled) {
            LOGGER.v("onClick ignored: %s", view);
            return;
        }
        LOGGER.v("onClick: %s", view);

        // If in accessibility mode or if alarm swiping is disabled in settings,
        // allow snooze/dismiss by tapping on respective icons.
        if (isAccessibilityEnabled() || !isSwipeActionEnabled) {
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
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (mAlarmHandled) {
            LOGGER.v("onTouch ignored: %s", event);
            return false;
        }

        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            LOGGER.v("onTouch started: %s", event);

            // Track the pointer that initiated the touch sequence.
            mInitialPointerIndex = event.getPointerId(event.getActionIndex());

            // Stop the pulse, allowing the last pulse to finish.
            mPulseAnimator.setRepeatCount(0);
        } else if (action == MotionEvent.ACTION_CANCEL) {
            LOGGER.v("onTouch canceled: %s", event);

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
            LOGGER.v("onTouch ended: %s", event);

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

    private void hideNavigationBar() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * Returns {@code true} if accessibility is enabled, to enable alternate behavior for click
     * handling, etc.
     */
    private boolean isAccessibilityEnabled() {
        if (mAccessibilityManager == null || !mAccessibilityManager.isEnabled()) {
            // Accessibility is unavailable or disabled.
            return false;
        } else if (mAccessibilityManager.isTouchExplorationEnabled()) {
            // TalkBack's touch exploration mode is enabled.
            return true;
        }

        // Check if "Switch Access" is enabled.
        final List<AccessibilityServiceInfo> enabledAccessibilityServices =
                mAccessibilityManager.getEnabledAccessibilityServiceList(FEEDBACK_GENERIC);
        return !enabledAccessibilityServices.isEmpty();
    }

    private void hintSnooze() {
        final int alarmLeft = mAlarmButton.getLeft() + mAlarmButton.getPaddingLeft();
        final int alarmRight = mAlarmButton.getRight() - mAlarmButton.getPaddingRight();
        final int hintLeftResId;
        if (mSnoozeMinutes == -1 || !mAlarmInstance.mAlarmSnoozeActions) {
            if (isOccasionalAlarmDeletedAfterUse()) {
                hintLeftResId = R.string.description_direction_left_for_occasional_non_repeatable_alarm;
            } else {
                hintLeftResId = R.string.description_direction_left_for_non_repeatable_alarm;
            }
        } else {
            hintLeftResId = R.string.description_direction_left;
        }

        final int hintRightResId;
        if (isOccasionalAlarmDeletedAfterUse()) {
            hintRightResId = R.string.description_direction_right_for_occasional_alarm;
        } else {
            hintRightResId = R.string.description_direction_right;
        }

        final float translationX = Math.max(mSnoozeButton.getLeft() - alarmRight, 0)
                + Math.min(mSnoozeButton.getRight() - alarmLeft, 0);
        getAlarmBounceAnimator(translationX, translationX < 0.0f
                ? hintLeftResId
                : hintRightResId).start();
    }

    private void hintDismiss() {
        final int alarmLeft = mAlarmButton.getLeft() + mAlarmButton.getPaddingLeft();
        final int alarmRight = mAlarmButton.getRight() - mAlarmButton.getPaddingRight();
        final int hintLeftResId;
        if (mSnoozeMinutes == -1 || !mAlarmInstance.mAlarmSnoozeActions) {
            if (isOccasionalAlarmDeletedAfterUse()) {
                hintLeftResId = R.string.description_direction_left_for_occasional_non_repeatable_alarm;
            } else {
                hintLeftResId = R.string.description_direction_left_for_non_repeatable_alarm;
            }
        } else {
            hintLeftResId = R.string.description_direction_left;
        }

        final int hintRightResId;
        if (isOccasionalAlarmDeletedAfterUse()) {
            hintRightResId = R.string.description_direction_right_for_occasional_alarm;
        } else {
            hintRightResId = R.string.description_direction_right;
        }

        final float translationX = Math.max(mDismissButton.getLeft() - alarmRight, 0)
                + Math.min(mDismissButton.getRight() - alarmLeft, 0);

        getAlarmBounceAnimator(translationX, translationX < 0.0f
                ? hintLeftResId
                : hintRightResId).start();
    }

    private void hintAlarmAction() {
        final int hintAlarmButtonResId;
        if (isSwipeActionEnabled) {
            if (mSnoozeMinutes == -1 || !mAlarmInstance.mAlarmSnoozeActions) {
                if (isOccasionalAlarmDeletedAfterUse()) {
                    hintAlarmButtonResId = R.string.description_direction_both_for_occasional_non_repeatable_alarm;
                } else {
                    hintAlarmButtonResId = R.string.description_direction_both_for_non_repeatable_alarm;
                }
            } else {
                if (isOccasionalAlarmDeletedAfterUse()) {
                    hintAlarmButtonResId = R.string.description_direction_both_for_occasional_alarm;
                } else {
                    hintAlarmButtonResId = R.string.description_direction_both;
                }
            }
        } else {
            if (mSnoozeMinutes == -1 || !mAlarmInstance.mAlarmSnoozeActions) {
                if (isOccasionalAlarmDeletedAfterUse()) {
                    hintAlarmButtonResId = R.string.description_direction_both_for_occasional_non_repeatable_alarm_clicked;
                } else {
                    hintAlarmButtonResId = R.string.description_direction_both_for_non_repeatable_alarm_clicked;
                }
            } else {
                if (isOccasionalAlarmDeletedAfterUse()) {
                    hintAlarmButtonResId = R.string.description_direction_both_clicked_for_occasional_alarm;
                } else {
                    hintAlarmButtonResId = R.string.description_direction_both_clicked;
                }
            }
        }

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
        final String title = DataModel.getDataModel().getRingtoneTitle(mAlarmInstance.mRingtone);
        mRingtoneTitle.setText(title);
        mRingtoneTitle.setTextColor(mAlarmTitleColor);

        final boolean silent = Utils.RINGTONE_SILENT.equals(mAlarmInstance.mRingtone);
        final Drawable iconRingtone = silent
                ? AppCompatResources.getDrawable(this, R.drawable.ic_ringtone_silent)
                : AppCompatResources.getDrawable(this, R.drawable.ic_music_note);
        assert iconRingtone != null;
        iconRingtone.setTint(mAlarmTitleColor);
        mRingtoneTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRingtone, null, null, null);
    }

    /**
     * Perform snooze animation and send dismiss intent if snooze duration has been set to "None";
     * otherwise, send snooze intent.
     */
    private void snooze() {
        mAlarmHandled = true;
        LOGGER.v("Snoozed: %s", mAlarmInstance);

        setAnimatedFractions(1.0f, 0.0f);

        // If snooze duration has been set to "None", simply dismiss the alarm.
        if (mSnoozeMinutes == -1 || !mAlarmInstance.mAlarmSnoozeActions) {
            int titleResId;
            int action;

            if (isOccasionalAlarmDeletedAfterUse()) {
                titleResId = R.string.alarm_alert_off_and_deleted_text;
                action = R.string.action_delete_alarm_after_use;
            } else {
                titleResId = R.string.alarm_alert_off_text;
                action = R.string.action_dismiss;
            }

            getAlertAnimator(mSnoozeButton, titleResId, null, getString(titleResId)).start();

            AlarmStateManager.deleteInstanceAndUpdateParent(this, mAlarmInstance);

            Events.sendAlarmEvent(action, R.string.label_deskclock);
        } else {
            final String infoText = getResources().getQuantityString(
                    R.plurals.alarm_alert_snooze_duration, mSnoozeMinutes, mSnoozeMinutes);
            final String accessibilityText = getResources().getQuantityString(
                    R.plurals.alarm_alert_snooze_set, mSnoozeMinutes, mSnoozeMinutes);

            getAlertAnimator(mSnoozeButton, R.string.alarm_alert_snoozed_text, infoText, accessibilityText).start();

            AlarmStateManager.setSnoozeState(this, mAlarmInstance, false);

            Events.sendAlarmEvent(R.string.action_snooze, R.string.label_deskclock);
        }

        // Unbind here, otherwise alarm will keep ringing until activity finishes.
        unbindAlarmService();
    }

    /**
     * Perform dismiss animation and send dismiss intent.
     */
    private void dismiss() {
        mAlarmHandled = true;
        LOGGER.v("Dismissed: %s", mAlarmInstance);

        int titleResId;
        int action;

        if (isOccasionalAlarmDeletedAfterUse()) {
            titleResId = R.string.alarm_alert_off_and_deleted_text;
            action = R.string.action_delete_alarm_after_use;
        } else {
            titleResId = R.string.alarm_alert_off_text;
            action = R.string.action_dismiss;
        }

        setAnimatedFractions(0.0f, 1.0f);

        getAlertAnimator(mDismissButton, titleResId, null, getString(titleResId)).start();

        AlarmStateManager.deleteInstanceAndUpdateParent(this, mAlarmInstance);

        Events.sendAlarmEvent(action, R.string.label_deskclock);

        // Unbind here, otherwise alarm will keep ringing until activity finishes.
        unbindAlarmService();
    }

    /**
     * @return {@code true} if the "Delete alarm once dismissed" button is ticked or if no day of the week is selected;
     * {@code false} otherwise.
     */
    private boolean isOccasionalAlarmDeletedAfterUse() {
        final Alarm alarm = Alarm.getAlarm(getContentResolver(), mAlarmInstance.mAlarmId);
        assert alarm != null;
        if (alarm.daysOfWeek.isRepeating()) {
            return false;
        }

        return alarm.deleteAfterUse;
    }

    /**
     * Bind AlarmService if not yet bound.
     */
    private void bindAlarmService() {
        if (!mServiceBound) {
            final Intent intent = new Intent(this, AlarmService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            mServiceBound = true;
        }
    }

    /**
     * Unbind AlarmService if bound.
     */
    private void unbindAlarmService() {
        if (mServiceBound) {
            unbindService(mConnection);
            mServiceBound = false;
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
                PropertyValuesHolder.ofInt(AnimatorUtils.DRAWABLE_ALPHA, BUTTON_DRAWABLE_ALPHA_DEFAULT, 255),
                PropertyValuesHolder.ofObject(AnimatorUtils.DRAWABLE_TINT, AnimatorUtils.ARGB_EVALUATOR, Color.WHITE, tintColor));
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
                    mRingtoneTitle.setVisibility(GONE);
                    mHintView.setVisibility(VISIBLE);

                    ObjectAnimator.ofFloat(mHintView, View.ALPHA, 0.0f, 1.0f).start();
                }
            }
        });

        return bounceAnimator;
    }

    private Animator getAlertAnimator(final View source, final int titleResId, final String infoText,
                                      final String accessibilityText) {

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
                mAlertView.setVisibility(VISIBLE);
                mAlertTitleView.setText(titleResId);
                mAlertTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
                mAlertTitleView.setTextColor(mAlarmTitleColor);
                mAlertTitleView.setTypeface(Typeface.DEFAULT_BOLD);

                if (infoText != null) {
                    mAlertInfoView.setVisibility(VISIBLE);
                    mAlertInfoView.setText(infoText);
                    mAlertInfoView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
                    mAlertInfoView.setTextColor(mAlarmTitleColor);
                    mAlertInfoView.setTypeface(Typeface.DEFAULT_BOLD);
                }
                mContentView.setVisibility(GONE);
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
                mAlertView.announceForAccessibility(accessibilityText);
                mHandler.postDelayed(() -> finish(), ALERT_DISMISS_DELAY_MILLIS);
            }
        });

        return alertAnimator;
    }
}
