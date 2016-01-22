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
package com.android.deskclock.alarms;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.animation.PathInterpolatorCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.events.Events;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.settings.SettingsActivity;
import com.android.deskclock.widget.CircleView;

public class AlarmActivity extends AppCompatActivity
        implements View.OnClickListener, View.OnTouchListener {

    private static final String LOGTAG = AlarmActivity.class.getSimpleName();

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
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LogUtils.v(LOGTAG, "Received broadcast: %s", action);

            if (!mAlarmHandled) {
                switch (action) {
                    case AlarmService.ALARM_SNOOZE_ACTION:
                        snooze();
                        break;
                    case AlarmService.ALARM_DISMISS_ACTION:
                        dismiss();
                        break;
                    case AlarmService.ALARM_DONE_ACTION:
                        finish();
                        break;
                    default:
                        LogUtils.i(LOGTAG, "Unknown broadcast: %s", action);
                        break;
                }
            } else {
                LogUtils.v(LOGTAG, "Ignored broadcast: %s", action);
            }
        }
    };

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtils.i("Finished binding to AlarmService");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtils.i("Disconnected from AlarmService");
        }
    };

    private AlarmInstance mAlarmInstance;
    private boolean mAlarmHandled;
    private String mVolumeBehavior;
    private int mCurrentHourColor;
    private boolean mReceiverRegistered;
    /** Whether the AlarmService is currently bound */
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
    private ValueAnimator mPulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final long instanceId = AlarmInstance.getId(getIntent().getData());
        mAlarmInstance = AlarmInstance.getInstance(getContentResolver(), instanceId);
        if (mAlarmInstance == null) {
            // The alarm was deleted before the activity got created, so just finish()
            LogUtils.e(LOGTAG, "Error displaying alarm for intent: %s", getIntent());
            finish();
            return;
        } else if (mAlarmInstance.mAlarmState != AlarmInstance.FIRED_STATE) {
            LogUtils.i(LOGTAG, "Skip displaying alarm for instance: %s", mAlarmInstance);
            finish();
            return;
        }

        LogUtils.i(LOGTAG, "Displaying alarm for instance: %s", mAlarmInstance);

        // Get the volume/camera button behavior setting
        mVolumeBehavior = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.KEY_VOLUME_BUTTONS,
                        SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        // Hide navigation bar to minimize accidental tap on Home key
        hideNavigationBar();

        // Close dialogs and window shade, so this is fully visible
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

        // In order to allow tablets to freely rotate and phones to stick
        // with "nosensor" (use default device orientation) we have to have
        // the manifest start with an orientation of unspecified" and only limit
        // to "nosensor" for phones. Otherwise we get behavior like in b/8728671
        // where tablets start off in their default orientation and then are
        // able to freely rotate.
        if (!getResources().getBoolean(R.bool.config_rotateAlarmAlert)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        mAccessibilityManager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);

        setContentView(R.layout.alarm_activity);

        mAlertView = (ViewGroup) findViewById(R.id.alert);
        mAlertTitleView = (TextView) mAlertView.findViewById(R.id.alert_title);
        mAlertInfoView = (TextView) mAlertView.findViewById(R.id.alert_info);

        mContentView = (ViewGroup) findViewById(R.id.content);
        mAlarmButton = (ImageView) mContentView.findViewById(R.id.alarm);
        mSnoozeButton = (ImageView) mContentView.findViewById(R.id.snooze);
        mDismissButton = (ImageView) mContentView.findViewById(R.id.dismiss);
        mHintView = (TextView) mContentView.findViewById(R.id.hint);

        final TextView titleView = (TextView) mContentView.findViewById(R.id.title);
        final TextClock digitalClock = (TextClock) mContentView.findViewById(R.id.digital_clock);
        final CircleView pulseView = (CircleView) mContentView.findViewById(R.id.pulse);

        titleView.setText(mAlarmInstance.getLabelOrDefault(this));
        Utils.setTimeFormat(this, digitalClock);

        mCurrentHourColor = Utils.getCurrentHourColor();
        getWindow().setBackgroundDrawable(new ColorDrawable(mCurrentHourColor));

        mAlarmButton.setOnTouchListener(this);
        mSnoozeButton.setOnClickListener(this);
        mDismissButton.setOnClickListener(this);

        mAlarmAnimator = AnimatorUtils.getScaleAnimator(mAlarmButton, 1.0f, 0.0f);
        mSnoozeAnimator = getButtonAnimator(mSnoozeButton, Color.WHITE);
        mDismissAnimator = getButtonAnimator(mDismissButton, mCurrentHourColor);
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
            LogUtils.i(LOGTAG, "No alarm instance for instanceId: %d", instanceId);
            finish();
            return;
        }

        // Verify that the alarm is still firing before showing the activity
        if (mAlarmInstance.mAlarmState != AlarmInstance.FIRED_STATE) {
            LogUtils.i(LOGTAG, "Skip displaying alarm for instance: %s", mAlarmInstance);
            finish();
            return;
        }

        if (!mReceiverRegistered) {
            // Register to get the alarm done/snooze/dismiss intent.
            final IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
            filter.addAction(AlarmService.ALARM_SNOOZE_ACTION);
            filter.addAction(AlarmService.ALARM_DISMISS_ACTION);
            registerReceiver(mReceiver, filter);
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
        LogUtils.v(LOGTAG, "dispatchKeyEvent: %s", keyEvent);

        switch (keyEvent.getKeyCode()) {
            // Volume keys and camera keys dismiss the alarm.
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (!mAlarmHandled && keyEvent.getAction() == KeyEvent.ACTION_UP) {
                    switch (mVolumeBehavior) {
                        case SettingsActivity.VOLUME_BEHAVIOR_SNOOZE:
                            snooze();
                            break;
                        case SettingsActivity.VOLUME_BEHAVIOR_DISMISS:
                            dismiss();
                            break;
                        default:
                            break;
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(keyEvent);
        }
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss.
    }

    @Override
    public void onClick(View view) {
        if (mAlarmHandled) {
            LogUtils.v(LOGTAG, "onClick ignored: %s", view);
            return;
        }
        LogUtils.v(LOGTAG, "onClick: %s", view);

        // If in accessibility mode, allow snooze/dismiss by double tapping on respective icons.
        if (mAccessibilityManager != null && mAccessibilityManager.isTouchExplorationEnabled()) {
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

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mAlarmHandled) {
            LogUtils.v(LOGTAG, "onTouch ignored: %s", motionEvent);
            return false;
        }

        final int[] contentLocation = {0, 0};
        mContentView.getLocationOnScreen(contentLocation);

        final float x = motionEvent.getRawX() - contentLocation[0];
        final float y = motionEvent.getRawY() - contentLocation[1];

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

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                LogUtils.v(LOGTAG, "onTouch started: %s", motionEvent);

                // Stop the pulse, allowing the last pulse to finish.
                mPulseAnimator.setRepeatCount(0);
                break;
            case MotionEvent.ACTION_UP:
                LogUtils.v(LOGTAG, "onTouch ended: %s", motionEvent);

                if (snoozeFraction == 1.0f) {
                    snooze();
                } else if (dismissFraction == 1.0f) {
                    dismiss();
                } else {
                    if (snoozeFraction > 0.0f || dismissFraction > 0.0f) {
                        // Animate back to the initial state.
                        AnimatorUtils.reverse(mAlarmAnimator, mSnoozeAnimator, mDismissAnimator);
                    } else if (mAlarmButton.getTop() <= y && y <= mAlarmButton.getBottom()) {
                        // User touched the alarm button, hint the dismiss action
                        hintDismiss();
                    }

                    // Restart the pulse.
                    mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                    if (!mPulseAnimator.isStarted()) {
                        mPulseAnimator.start();
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                resetAnimations();
                break;
            default:
                break;
        }

        return true;
    }

    private void hideNavigationBar() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void hintSnooze() {
        final int alarmLeft = mAlarmButton.getLeft() + mAlarmButton.getPaddingLeft();
        final int alarmRight = mAlarmButton.getRight() - mAlarmButton.getPaddingRight();
        final float translationX = Math.max(mSnoozeButton.getLeft() - alarmRight, 0)
                + Math.min(mSnoozeButton.getRight() - alarmLeft, 0);
        getAlarmBounceAnimator(translationX, translationX < 0.0f ?
                R.string.description_direction_left : R.string.description_direction_right).start();
    }

    private void hintDismiss() {
        final int alarmLeft = mAlarmButton.getLeft() + mAlarmButton.getPaddingLeft();
        final int alarmRight = mAlarmButton.getRight() - mAlarmButton.getPaddingRight();
        final float translationX = Math.max(mDismissButton.getLeft() - alarmRight, 0)
                + Math.min(mDismissButton.getRight() - alarmLeft, 0);
        getAlarmBounceAnimator(translationX, translationX < 0.0f ?
                R.string.description_direction_left : R.string.description_direction_right).start();
    }

    /**
     * Set animators to initial values and restart pulse on alarm button.
     */
    private void resetAnimations() {
        // Set the animators to their initial values.
        setAnimatedFractions(0.0f /* snoozeFraction */, 0.0f /* dismissFraction */);
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
        LogUtils.v(LOGTAG, "Snoozed: %s", mAlarmInstance);

        final int accentColor = Utils.obtainStyledColor(this, R.attr.colorAccent, Color.RED);
        setAnimatedFractions(1.0f /* snoozeFraction */, 0.0f /* dismissFraction */);

        final int snoozeMinutes = AlarmStateManager.getSnoozedMinutes(this);
        final String infoText = getResources().getQuantityString(
                R.plurals.alarm_alert_snooze_duration, snoozeMinutes, snoozeMinutes);
        final String accessibilityText = getResources().getQuantityString(
                R.plurals.alarm_alert_snooze_set, snoozeMinutes, snoozeMinutes);

        getAlertAnimator(mSnoozeButton, R.string.alarm_alert_snoozed_text, infoText,
                accessibilityText, accentColor, accentColor).start();

        AlarmStateManager.setSnoozeState(this, mAlarmInstance, false /* showToast */);

        Events.sendAlarmEvent(R.string.action_snooze, R.string.label_deskclock);

        // Unbind here, otherwise alarm will keep ringing until activity finishes.
        unbindAlarmService();
    }

    /**
     * Perform dismiss animation and send dismiss intent.
     */
    private void dismiss() {
        mAlarmHandled = true;
        LogUtils.v(LOGTAG, "Dismissed: %s", mAlarmInstance);

        setAnimatedFractions(0.0f /* snoozeFraction */, 1.0f /* dismissFraction */);

        getAlertAnimator(mDismissButton, R.string.alarm_alert_off_text, null /* infoText */,
                getString(R.string.alarm_alert_off_text) /* accessibilityText */,
                Color.WHITE, mCurrentHourColor).start();

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
        AnimatorUtils.setAnimatedFraction(mAlarmAnimator, alarmFraction);
        AnimatorUtils.setAnimatedFraction(mSnoozeAnimator, snoozeFraction);
        AnimatorUtils.setAnimatedFraction(mDismissAnimator, dismissFraction);
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
                        AnimatorUtils.ARGB_EVALUATOR, Color.WHITE, tintColor));
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

    private Animator getAlertAnimator(final View source, final int titleResId,
            final String infoText, final String accessibilityText, final int revealColor,
            final int backgroundColor) {
        final ViewGroup containerView = (ViewGroup) findViewById(android.R.id.content);

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
                .setFillColor(revealColor);
        containerView.addView(revealView);

        // TODO: Fade out source icon over the reveal (like LOLLIPOP version).

        final Animator revealAnimator = ObjectAnimator.ofFloat(
                revealView, CircleView.RADIUS, startRadius, endRadius);
        revealAnimator.setDuration(ALERT_REVEAL_DURATION_MILLIS);
        revealAnimator.setInterpolator(REVEAL_INTERPOLATOR);
        revealAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                mAlertView.setVisibility(View.VISIBLE);
                mAlertTitleView.setText(titleResId);

                if (infoText != null) {
                    mAlertInfoView.setText(infoText);
                    mAlertInfoView.setVisibility(View.VISIBLE);
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
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, ALERT_DISMISS_DELAY_MILLIS);
            }
        });

        return alertAnimator;
    }
}
