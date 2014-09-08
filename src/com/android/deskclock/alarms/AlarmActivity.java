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
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageButton;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.AnimatorUtils;
import com.android.deskclock.Log;
import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.AlarmInstance;

public class AlarmActivity extends Activity {

    /**
     * AlarmActivity listens for this broadcast intent, so that other applicationscan snooze the
     * alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
     */
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";
    /**
     * AlarmActivity listens for this broadcast intent, so that other applications can dismiss
     * the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
     */
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";

    private static final View.DragShadowBuilder NO_DRAG_SHADOW = new View.DragShadowBuilder();

    private static final Interpolator PULSE_INTERPOLATOR =
            new PathInterpolator(0.4f, 0.0f, 0.2f, 1.0f);
    private static final Interpolator REVEAL_INTERPOLATOR =
            new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);

    private static final int PULSE_DURATION_MILLIS = 1000;
    private static final int ALARM_BOUNCE_DURATION_MILLIS = 500;
    private static final int ALERT_SOURCE_DURATION_MILLIS = 250;
    private static final int ALERT_REVEAL_DURATION_MILLIS = 500;
    private static final int ALERT_FADE_DURATION_MILLIS = 500;
    private static final int ALERT_DISMISS_DELAY_MILLIS = 2000;

    private static final float BUTTON_SCALE_DEFAULT = 0.7f;
    private static final int BUTTON_DRAWABLE_ALPHA_DEFAULT = 165;

    private final View.OnTouchListener mAlarmOnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            return !mAlarmHandled && motionEvent.getAction() == MotionEvent.ACTION_DOWN
                    && view.startDrag(null, NO_DRAG_SHADOW, null, 0);
        }
    };

    private final View.OnClickListener mSnoozeOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final float translationX;
            if (mContentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                translationX = mSnoozeButton.getLeft() - mAlarmButton.getRight();
            } else {
                translationX = mSnoozeButton.getRight() - mAlarmButton.getLeft();
            }
            getAlarmBounceAnimator(translationX, R.string.swipe_snooze_instruction).start();
        }
    };

    private final View.OnClickListener mDismissOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final float translationX;
            if (mContentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                translationX = mDismissButton.getRight() - mAlarmButton.getLeft();
            } else {
                translationX = mDismissButton.getLeft() - mAlarmButton.getRight();
            }
            getAlarmBounceAnimator(translationX, R.string.swipe_dismiss_instruction).start();
        }
    };

    private final View.OnDragListener mContentOnDragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DROP:
                    // content view does not accept drops
                    return false;
                case DragEvent.ACTION_DRAG_LOCATION:
                    final float snoozeFraction, dismissFraction;
                    if (mContentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                        snoozeFraction = getFraction(mAlarmButton.getRight(),
                                mSnoozeButton.getLeft(), dragEvent.getX());
                        dismissFraction = getFraction(mAlarmButton.getLeft(),
                                mDismissButton.getRight(), dragEvent.getX());
                    } else {
                        snoozeFraction = getFraction(mAlarmButton.getLeft(),
                                mSnoozeButton.getRight(), dragEvent.getX());
                        dismissFraction = getFraction(mAlarmButton.getRight(),
                                mDismissButton.getLeft(), dragEvent.getX());
                    }
                    setAnimatedFractions(snoozeFraction, dismissFraction);
                    return true;
                default:
                    return true;
            }
        }

        private float getFraction(float x0, float x1, float x) {
            return Math.max(Math.min((x - x0) / (x1 - x0), 1.0f), 0.0f);
        }
    };

    private final View.OnDragListener mAlarmOnDragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    mPulseAnimator.setRepeatCount(0);
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    setAnimatedFractions(0.0f /* snoozeFraction */, 0.0f /* dismissFraction */);
                    return true;
                case DragEvent.ACTION_DROP:
                    mDismissButton.performClick();
                    return false;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (!dragEvent.getResult()) {
                        AnimatorUtils.reverse(mAlarmAnimator);
                        mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
                        if (!mPulseAnimator.isStarted()) {
                            mPulseAnimator.start();
                        }
                    }
                    return true;
                default:
                    return true;
            }
        }
    };

    private final View.OnDragListener mSnoozeOnDragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    setAnimatedFractions(1.0f /* snoozeFraction */, 0.0f /* dismissFraction */);
                    return true;
                case DragEvent.ACTION_DROP:
                    snooze();
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (!dragEvent.getResult()) {
                        AnimatorUtils.reverse(mSnoozeAnimator);
                    }
                    return true;
                default:
                    return true;
            }
        }
    };

    private final View.OnDragListener mDismissOnDragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {
            switch (dragEvent.getAction()) {
                case DragEvent.ACTION_DRAG_ENTERED:
                    setAnimatedFractions(0.0f /* snoozeFraction */, 1.0f /* dismissFraction */);
                    return true;
                case DragEvent.ACTION_DROP:
                    dismiss();
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    if (!dragEvent.getResult()) {
                        AnimatorUtils.reverse(mDismissAnimator);
                    }
                    return true;
                default:
                    return true;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Log.LOGV) {
                Log.v("AlarmActivity - Broadcast Receiver - " + action);
            }

            if (!mAlarmHandled) {
                switch (action) {
                    case ALARM_SNOOZE_ACTION:
                        snooze();
                        break;
                    case ALARM_DISMISS_ACTION:
                        dismiss();
                        break;
                    case AlarmService.ALARM_DONE_ACTION:
                        finish();
                        break;
                    default:
                        Log.i("Unknown broadcast in AlarmActivity: " + action);
                }
            }
        }
    };

    private final Handler mHandler = new Handler();

    private AlarmInstance mAlarmInstance;
    private boolean mAlarmHandled;
    private String mVolumeBehavior;
    private int mCurrentHourColor;

    private ViewGroup mContainerView;

    private ViewGroup mAlertView;
    private TextView mAlertTitleView;
    private TextView mAlertInfoView;

    private ViewGroup mContentView;
    private ImageButton mAlarmButton;
    private ImageButton mSnoozeButton;
    private ImageButton mDismissButton;
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
        if (mAlarmInstance != null) {
            Log.v("Displaying alarm for instance: " + mAlarmInstance);
        } else {
            // The alarm got deleted before the activity got created, so just finish()
            Log.v("Error displaying alarm for intent: " + getIntent());
            finish();
            return;
        }

        // Get the volume/camera button behavior setting
        mVolumeBehavior = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
                        SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        // In order to allow tablets to freely rotate and phones to stick
        // with "nosensor" (use default device orientation) we have to have
        // the manifest start with an orientation of unspecified" and only limit
        // to "nosensor" for phones. Otherwise we get behavior like in b/8728671
        // where tablets start off in their default orientation and then are
        // able to freely rotate.
        if (!getResources().getBoolean(R.bool.config_rotateAlarmAlert)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        setContentView(R.layout.alarm_activity);

        mContainerView = (ViewGroup) findViewById(android.R.id.content);

        mAlertView = (ViewGroup) mContainerView.findViewById(R.id.alert);
        mAlertTitleView = (TextView) mAlertView.findViewById(R.id.alert_title);
        mAlertInfoView = (TextView) mAlertView.findViewById(R.id.alert_info);

        mContentView = (ViewGroup) mContainerView.findViewById(R.id.content);
        mAlarmButton = (ImageButton) mContentView.findViewById(R.id.alarm);
        mSnoozeButton = (ImageButton) mContentView.findViewById(R.id.snooze);
        mDismissButton = (ImageButton) mContentView.findViewById(R.id.dismiss);
        mHintView = (TextView) mContentView.findViewById(R.id.hint);

        final TextView titleView = (TextView) mContentView.findViewById(R.id.title);
        final TextClock digitalClock = (TextClock) mContentView.findViewById(R.id.digital_clock);
        final View pulseView = mContentView.findViewById(R.id.pulse);

        titleView.setText(mAlarmInstance.getLabelOrDefault(this));
        Utils.setTimeFormat(digitalClock,
                getResources().getDimensionPixelSize(R.dimen.main_ampm_font_size));

        mCurrentHourColor = Utils.getCurrentHourColor();
        mContainerView.setBackgroundColor(mCurrentHourColor);

        mAlarmButton.setOnTouchListener(mAlarmOnTouchListener);
        mSnoozeButton.setOnClickListener(mSnoozeOnClickListener);
        mDismissButton.setOnClickListener(mDismissOnClickListener);

        mContentView.setOnDragListener(mContentOnDragListener);
        mAlarmButton.setOnDragListener(mAlarmOnDragListener);
        mSnoozeButton.setOnDragListener(mSnoozeOnDragListener);
        mDismissButton.setOnDragListener(mDismissOnDragListener);

        mAlarmAnimator = AnimatorUtils.getScaleAnimator(mAlarmButton, 1.0f, 0.0f);
        mSnoozeAnimator = getButtonAnimator(mSnoozeButton, Color.WHITE);
        mDismissAnimator = getButtonAnimator(mDismissButton, mCurrentHourColor);
        mPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(pulseView,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.0f, 1.0f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.0f, 1.0f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.0f));
        mPulseAnimator.setDuration(PULSE_DURATION_MILLIS);
        mPulseAnimator.setInterpolator(PULSE_INTERPOLATOR);
        mPulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mPulseAnimator.start();

        // Set the animators to their initial values.
        setAnimatedFractions(0.0f /* snoozeFraction */, 0.0f /* dismissFraction */);

        // Register to get the alarm done/snooze/dismiss intent.
        final IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
        filter.addAction(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // If the alarm instance is null the receiver was never registered and calling
        // unregisterReceiver will throw an exception.
        if (mAlarmInstance != null) {
            unregisterReceiver(mReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss.
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        // Do this on key down to handle a few of the system keys.
        if (Log.LOGV) {
            Log.v("AlarmActivity - dispatchKeyEvent - " + event.getKeyCode());
        }

        switch (event.getKeyCode()) {
            // Volume keys and camera keys dismiss the alarm.
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (!mAlarmHandled && event.getAction() == KeyEvent.ACTION_UP) {
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
                break;
        }

        return super.dispatchKeyEvent(event);
    }

    private void snooze() {
        mAlarmHandled = true;

        final int alertColor = getResources().getColor(R.color.hot_pink);
        setAnimatedFractions(1.0f /* snoozeFraction */, 0.0f /* dismissFraction */);
        getAlertAnimator(mSnoozeButton, R.string.alarm_alert_snoozed_text,
                AlarmStateManager.getSnoozedMinutes(this), alertColor, alertColor).start();
        AlarmStateManager.setSnoozeState(this, mAlarmInstance, false /* showToast */);
    }

    private void dismiss() {
        mAlarmHandled = true;

        setAnimatedFractions(0.0f /* snoozeFraction */, 1.0f /* dismissFraction */);
        getAlertAnimator(mDismissButton, R.string.alarm_alert_off_text, null /* infoText */,
                Color.WHITE, mCurrentHourColor).start();
        AlarmStateManager.setDismissState(this, mAlarmInstance);
    }

    private void setAnimatedFractions(float snoozeFraction, float dismissFraction) {
        final float alarmFraction = Math.max(snoozeFraction, dismissFraction);
        AnimatorUtils.setAnimatedFraction(mAlarmAnimator, alarmFraction);
        AnimatorUtils.setAnimatedFraction(mSnoozeAnimator, snoozeFraction);
        AnimatorUtils.setAnimatedFraction(mDismissAnimator, dismissFraction);
    }

    private ValueAnimator getButtonAnimator(ImageButton button, int tintColor) {
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
            final String infoText, final int revealColor, final int backgroundColor) {
        final ViewGroupOverlay overlay = mContainerView.getOverlay();

        // Create a transient view for performing the reveal animation.
        final View revealView = new View(this);
        revealView.setRight(mContainerView.getWidth());
        revealView.setBottom(mContainerView.getHeight());
        revealView.setBackgroundColor(revealColor);
        overlay.add(revealView);

        // Add the source to the containerView's overlay so that the animation can occur under the
        // status bar, the source view will be automatically positioned in the overlay so that
        // it maintains the same relative position on screen.
        overlay.add(source);

        final int centerX = Math.round((source.getLeft() + source.getRight()) / 2.0f);
        final int centerY = Math.round((source.getTop() + source.getBottom()) / 2.0f);
        final float startRadius = Math.max(source.getWidth(), source.getHeight()) / 2.0f;

        final int xMax = Math.max(centerX, mContainerView.getWidth() - centerX);
        final int yMax = Math.max(centerY, mContainerView.getHeight() - centerY);
        final float endRadius = (float) Math.sqrt(Math.pow(xMax, 2.0) + Math.pow(yMax, 2.0));

        final ValueAnimator sourceAnimator = ObjectAnimator.ofFloat(source, View.ALPHA, 0.0f);
        sourceAnimator.setDuration(ALERT_SOURCE_DURATION_MILLIS);
        sourceAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlay.remove(source);
            }
        });

        final Animator revealAnimator = ViewAnimationUtils.createCircularReveal(
                revealView, centerX, centerY, startRadius, endRadius);
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
                mContainerView.setBackgroundColor(backgroundColor);
            }
        });

        final ValueAnimator fadeAnimator = ObjectAnimator.ofFloat(revealView, View.ALPHA, 0.0f);
        fadeAnimator.setDuration(ALERT_FADE_DURATION_MILLIS);
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                overlay.remove(revealView);
            }
        });

        final AnimatorSet alertAnimator = new AnimatorSet();
        alertAnimator.play(revealAnimator).with(sourceAnimator).before(fadeAnimator);
        alertAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
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
