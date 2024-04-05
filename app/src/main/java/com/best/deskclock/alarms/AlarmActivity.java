/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.best.deskclock.alarms;

import static android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC;

import static com.best.deskclock.settings.SettingsActivity.KEY_AMOLED_DARK_MODE;
import static com.best.deskclock.settings.SettingsActivity.LIGHT_THEME;

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
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.animation.PathInterpolatorCompat;

import com.best.deskclock.AnalogClock;
import com.best.deskclock.AnimatorUtils;
import com.best.deskclock.LogUtils;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.DataModel.AlarmVolumeButtonBehavior;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.widget.CircleView;

import java.util.List;

public class AlarmActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnTouchListener {
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
    private static final int BUTTON_DRAWABLE_ALPHA_DEFAULT = 165;
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
    private AlarmVolumeButtonBehavior mVolumeBehavior;
    private AlarmVolumeButtonBehavior mPowerBehavior;
    private int mCurrentHourColor;
    private int mTextColor;
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
    private ValueAnimator mAlarmAnimator;
    private ValueAnimator mSnoozeAnimator;
    private ValueAnimator mDismissAnimator;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LOGGER.v("Received broadcast: %s", action);

            if (!mAlarmHandled) {
                switch (action) {
                    case AlarmService.ALARM_SNOOZE_ACTION -> snooze();
                    case AlarmService.ALARM_DISMISS_ACTION -> dismiss();
                    case AlarmService.ALARM_DONE_ACTION -> finish();
                    default -> LOGGER.i("Unknown broadcast: %s", action);
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
                        if (mPowerBehavior == AlarmVolumeButtonBehavior.SNOOZE) {
                            snooze();
                        } else if (mPowerBehavior == AlarmVolumeButtonBehavior.DISMISS) {
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

        // Apply dark colors only for this activity.
        // We don't want to be woken up with bright colors if the device is set to light mode.
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        // Hide navigation bar to minimize accidental tap on Home key
        hideNavigationBar();

        // Register Power button (screen off) intent receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(PowerBtnReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
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

        mAccessibilityManager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);

        setContentView(R.layout.alarm_activity);

        View alarmActivityLayout = findViewById(R.id.alarmActivityLayout);
        final String getDarkMode = DataModel.getDataModel().getDarkMode();
        final String getTheme = DataModel.getDataModel().getTheme();
        if (getDarkMode.equals(KEY_AMOLED_DARK_MODE) && !getTheme.equals(LIGHT_THEME)) {
            alarmActivityLayout.setBackgroundColor(Color.BLACK);
        }

        mTextColor = getColor(R.color.md_theme_outline);

        mAlertView = findViewById(R.id.alert);
        mAlertTitleView = mAlertView.findViewById(R.id.alert_title);
        mAlertInfoView = mAlertView.findViewById(R.id.alert_info);

        mContentView = findViewById(R.id.content);
        mAlarmButton = mContentView.findViewById(R.id.alarm);
        mSnoozeButton = mContentView.findViewById(R.id.snooze);
        mDismissButton = mContentView.findViewById(R.id.dismiss);
        mHintView = mContentView.findViewById(R.id.hint);

        mAlarmButton.setImageDrawable(Utils.toScaledBitmapDrawable(
                mAlarmButton.getContext(), R.drawable.ic_tab_alarm_static, 2.5f)
        );
        mAlarmButton.setColorFilter(mTextColor);

        mDismissButton.setImageDrawable(Utils.toScaledBitmapDrawable(
                mDismissButton.getContext(), R.drawable.ic_alarm_off, 2f)
        );
        mDismissButton.setColorFilter(mTextColor);

        mSnoozeButton.setImageDrawable(Utils.toScaledBitmapDrawable(
                mSnoozeButton.getContext(), R.drawable.ic_snooze, 2f)
        );
        mSnoozeButton.setColorFilter(mTextColor);

        final TextView titleView = mContentView.findViewById(R.id.title);
        final AnalogClock analogClock = findViewById(R.id.analog_clock);
        final TextClock digitalClock = mContentView.findViewById(R.id.digital_clock);
        final CircleView pulseView = mContentView.findViewById(R.id.pulse);

        Utils.setClockStyle(digitalClock, analogClock);
        Utils.setClockSecondsEnabled(digitalClock, analogClock);

        titleView.setText(mAlarmInstance.getLabelOrDefault(this));
        titleView.setTextColor(mTextColor);
        Utils.setTimeFormat(digitalClock, false);
        digitalClock.setTextColor(mTextColor);

        mCurrentHourColor = getColor(R.color.md_theme_background);
        getWindow().setBackgroundDrawable(new ColorDrawable(mCurrentHourColor));

        mAlarmButton.setOnTouchListener(this);
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
                registerReceiver(mReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
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
                        case SNOOZE -> {
                            if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                                snooze();
                            }
                            return true;
                        }
                        case DISMISS -> {
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

        // If in accessibility mode, allow snooze/dismiss by double tapping on respective icons.
        if (isAccessibilityEnabled()) {
            if (view == mSnoozeButton) {
                snooze();
            } else if (view == mDismissButton) {
                dismiss();
            }
            return;
        }

        if (view == mSnoozeButton) {
            hintSnooze();
        } else if (view == mDismissButton) {
            hintDismiss();
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
                    // User touched the alarm button, hint the dismiss action.
                    hintDismiss();
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
        final float translationX = Math.max(mSnoozeButton.getLeft() - alarmRight, 0)
                + Math.min(mSnoozeButton.getRight() - alarmLeft, 0);
        getAlarmBounceAnimator(translationX, translationX < 0.0f
                ? R.string.description_direction_left
                : R.string.description_direction_right).start();
    }

    private void hintDismiss() {
        final int alarmLeft = mAlarmButton.getLeft() + mAlarmButton.getPaddingLeft();
        final int alarmRight = mAlarmButton.getRight() - mAlarmButton.getPaddingRight();
        final float translationX = Math.max(mDismissButton.getLeft() - alarmRight, 0)
                + Math.min(mDismissButton.getRight() - alarmLeft, 0);

        getAlarmBounceAnimator(translationX, translationX < 0.0f
                ? R.string.description_direction_left
                : R.string.description_direction_right).start();
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
     * Perform snooze animation and send snooze intent.
     */
    private void snooze() {
        mAlarmHandled = true;
        LOGGER.v("Snoozed: %s", mAlarmInstance);

        setAnimatedFractions(1.0f, 0.0f);

        final int snoozeMinutes = DataModel.getDataModel().getSnoozeLength();
        final String infoText = getResources().getQuantityString(
                R.plurals.alarm_alert_snooze_duration, snoozeMinutes, snoozeMinutes);
        final String accessibilityText = getResources().getQuantityString(
                R.plurals.alarm_alert_snooze_set, snoozeMinutes, snoozeMinutes);

        getAlertAnimator(mSnoozeButton, R.string.alarm_alert_snoozed_text, infoText, accessibilityText, mCurrentHourColor).start();

        AlarmStateManager.setSnoozeState(this, mAlarmInstance, false);

        Events.sendAlarmEvent(R.string.action_snooze, R.string.label_deskclock);

        // Unbind here, otherwise alarm will keep ringing until activity finishes.
        unbindAlarmService();
    }

    /**
     * Perform dismiss animation and send dismiss intent.
     */
    private void dismiss() {
        mAlarmHandled = true;
        LOGGER.v("Dismissed: %s", mAlarmInstance);

        setAnimatedFractions(0.0f, 1.0f);

        getAlertAnimator(mDismissButton, R.string.alarm_alert_off_text, null, getString(R.string.alarm_alert_off_text), mCurrentHourColor).start();

        AlarmStateManager.deleteInstanceAndUpdateParent(this, mAlarmInstance);

        Events.sendAlarmEvent(R.string.action_dismiss, R.string.label_deskclock);

        // Unbind here, otherwise alarm will keep ringing until activity finishes.
        unbindAlarmService();
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
                if (mHintView.getVisibility() != View.VISIBLE) {
                    mHintView.setVisibility(View.VISIBLE);

                    ObjectAnimator.ofFloat(mHintView, View.ALPHA, 0.0f, 1.0f).start();
                }
            }
        });

        return bounceAnimator;
    }

    private Animator getAlertAnimator(final View source, final int titleResId, final String infoText,
                                      final String accessibilityText, final int backgroundColor) {

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
                mAlertTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
                mAlertTitleView.setTextColor(mTextColor);
                mAlertTitleView.setTypeface(Typeface.DEFAULT_BOLD);

                if (infoText != null) {
                    mAlertInfoView.setVisibility(View.VISIBLE);
                    mAlertInfoView.setText(infoText);
                    mAlertInfoView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
                    mAlertInfoView.setTextColor(mTextColor);
                    mAlertInfoView.setTypeface(Typeface.DEFAULT_BOLD);
                }
                mContentView.setVisibility(View.GONE);

                getWindow().setBackgroundDrawable(new ColorDrawable(backgroundColor));
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
