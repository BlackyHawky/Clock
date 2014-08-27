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
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroupOverlay;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.Log;
import com.android.deskclock.R;
import com.android.deskclock.SettingsActivity;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.AlarmInstance;

public class AlarmActivity extends Activity {
    // AlarmActivity listens for this broadcast intent, so that other applications
    // can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_SNOOZE_ACTION = "com.android.deskclock.ALARM_SNOOZE";

    // AlarmActivity listens for this broadcast intent, so that other applications
    // can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";

    private static final float DIM_ALPHA = 0.3f;
    private static final float NORMAL_ALPHA = 0.65f;
    private static final float HIGHLIGHT_ALPHA = 1.0f;
    private static final float SCALE_SLOPE = 0.3f;

    private static final int RIPPLE_DELAY_MS = 500;
    private static final int FINISH_ACTIVITY_DELAY_MS = 2000;

    private View mCenterButton;
    private View mContentView;
    private View mSnoozeButton;
    private View mDismissButton;
    private View mSnoozeCircle;
    private View mDismissCircle;
    private AlarmRipple mCenterRipple;

    private boolean mShowingSnoozeCircle;
    private boolean mShowingDismissCircle;

    private AlarmInstance mInstance;
    private Resources mResource;

    private boolean mIsClickingCenterButton;
    private int mVolumeBehavior;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("AlarmActivity - Broadcast Receiver - " + action);
            if (action.equals(ALARM_SNOOZE_ACTION)) {
                snooze();
            } else if (action.equals(ALARM_DISMISS_ACTION)) {
                dismiss();
            } else if (action.equals(AlarmService.ALARM_DONE_ACTION)) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, FINISH_ACTIVITY_DELAY_MS /* Delay to make sure the animation is finished now */);
            } else {
                Log.i("Unknown broadcast in AlarmActivity: " + action);
            }
        }
    };

    private void snooze() {
        AlarmStateManager.setSnoozeState(this, mInstance, false /* showToast */);
    }

    private void dismiss() {
        AlarmStateManager.setDismissState(this, mInstance);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long instanceId = AlarmInstance.getId(getIntent().getData());
        mInstance = AlarmInstance.getInstance(this.getContentResolver(), instanceId);
        if (mInstance != null) {
            Log.v("Displaying alarm for instance: " + mInstance);
        } else {
            // The alarm got deleted before the activity got created, so just finish()
            Log.v("Error displaying alarm for intent: " + getIntent());
            finish();
            return;
        }

        // Get the volume/camera button behavior setting
        final String vol = PreferenceManager.getDefaultSharedPreferences(this).
                getString(SettingsActivity.KEY_VOLUME_BEHAVIOR,
                        SettingsActivity.DEFAULT_VOLUME_BEHAVIOR);
        mVolumeBehavior = Integer.parseInt(vol);

        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        // In order to allow tablets to freely rotate and phones to stick
        // with "nosensor" (use default device orientation) we have to have
        // the manifest start with an orientation of unspecified" and only limit
        // to "nosensor" for phones. Otherwise we get behavior like in b/8728671
        // where tablets start off in their default orientation and then are
        // able to freely rotate.
        if (!getResources().getBoolean(R.bool.config_rotateAlarmAlert)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        final LayoutInflater inflater = LayoutInflater.from(this);
        mContentView = inflater.inflate(R.layout.alarm_alert, null);
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        setContentView(mContentView);

        mResource = getResources();
        mSnoozeButton = findViewById(R.id.snooze_button);
        mCenterButton = findViewById(R.id.center_button);
        mCenterButton.setContentDescription(
                mResource.getString(R.string.description_direction_right) +
                        mResource.getString(R.string.description_direction_left));
        mDismissButton = findViewById(R.id.dismiss_button);
        mCenterRipple = (AlarmRipple) findViewById(R.id.center_ripple);
        mSnoozeCircle = findViewById(R.id.snooze_circle);
        mDismissCircle = findViewById(R.id.dismiss_circle);

        // Color the main view instead of content view, because this view is stacked on top of
        // the reveal view, which has a solid color. We don't want that solid color to show up here.
        final int currentHourColor = Utils.getCurrentHourColor();
        findViewById(R.id.main_layout).setBackgroundColor(currentHourColor);
        initializeButtonListeners(currentHourColor);
        updateTimeAndTitle();

        // Register to get the alarm done/snooze/dismiss intent.
        IntentFilter filter = new IntentFilter(AlarmService.ALARM_DONE_ACTION);
        filter.addAction(ALARM_SNOOZE_ACTION);
        filter.addAction(ALARM_DISMISS_ACTION);
        registerReceiver(mReceiver, filter);

        // Delay to make sure view is initialized before playing the ripple animation
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                drawCenterRipple();
            }
        }, RIPPLE_DELAY_MS /* Delay to make sure view is rendered before drawing ripple */);
    }

    private void bounceAnimation(boolean towardsSnooze) {
        final float distance = mCenterButton.getWidth();
        ObjectAnimator push = ObjectAnimator.ofFloat(mCenterButton, View.TRANSLATION_X,
                towardsSnooze ? 0 - distance : distance);
        push.setInterpolator(new DecelerateInterpolator());
        ObjectAnimator pull = ObjectAnimator.ofFloat(mCenterButton, View.TRANSLATION_X,
                0);
        pull.setInterpolator(new AccelerateInterpolator());
        AnimatorSet set = new AnimatorSet();
        set.play(push).before(pull);
        set.setDuration(mResource.getInteger(android.R.integer.config_shortAnimTime));
        set.start();
    }

    private void initializeButtonListeners(final int currentHourColor) {
        mSnoozeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                bounceAnimation(true /* towardsSnooze */);
            }
        });

        mDismissButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                bounceAnimation(false /* towardsDismiss */);
            }
        });

        mCenterButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    view.startDrag(null, new DragShadowBuilder(), null, 0);
                }
                return false;
            }
        });

        final View ripplePad = findViewById(R.id.ripple_pad);
        ripplePad.setOnDragListener(new OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                final int action = dragEvent.getAction();
                final float x = dragEvent.getX();
                final float centerX = ripplePad.getLeft() + ripplePad.getWidth() / 2;
                final float snoozeRightBoundary = mSnoozeButton.getRight() + mSnoozeButton
                        .getWidth() / 2;
                final float dismissLeftBoundary = mDismissButton.getLeft() - mDismissButton
                        .getWidth() / 2;
                switch (action) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        mIsClickingCenterButton = true;
                        // Once user click center button, stop ripple and highlight both button
                        mCenterRipple.setVisibility(View.INVISIBLE);
                        mCenterButton.setVisibility(View.INVISIBLE);
                        mSnoozeButton.setAlpha(HIGHLIGHT_ALPHA);
                        mDismissButton.setAlpha(HIGHLIGHT_ALPHA);
                        return true;
                    case DragEvent.ACTION_DRAG_LOCATION:
                        mIsClickingCenterButton = false;
                        // Make one button stand out and dim the other as long as user moves a
                        // little bit to either direction
                        mSnoozeButton.setAlpha(x < centerX ? HIGHLIGHT_ALPHA : DIM_ALPHA);
                        mDismissButton.setAlpha(x < centerX ? DIM_ALPHA : HIGHLIGHT_ALPHA);

                        // Scale icon in x-axis linear to finger location
                        if (x < centerX && x > snoozeRightBoundary) {
                            scaleButton(mSnoozeButton, centerX - x, centerX - snoozeRightBoundary);
                        } else if (x > centerX && x < dismissLeftBoundary) {
                            scaleButton(mDismissButton, x - centerX, dismissLeftBoundary - centerX);
                        }

                        // Expand background circle if finger enters certain boundary
                        if (x < snoozeRightBoundary) {
                            if (!mShowingSnoozeCircle) {
                                expandCircle(mSnoozeCircle, mSnoozeButton);
                                mShowingSnoozeCircle = true;
                            }
                            mShowingDismissCircle = false;
                            mSnoozeCircle.setVisibility(View.VISIBLE);
                            mDismissCircle.setVisibility(View.INVISIBLE);
                            mDismissButton.setVisibility(View.VISIBLE);
                        } else if (x > dismissLeftBoundary) {
                            if (!mShowingDismissCircle) {
                                // Fade out the icon to reduce jump when swapping to circle icon
                                final Animator alphaAnim = ObjectAnimator.ofFloat(mDismissButton,
                                        "alpha", 1, 0);
                                alphaAnim.setDuration(mResource.getInteger(
                                        android.R.integer.config_shortAnimTime));
                                alphaAnim.start();
                                alphaAnim.addListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        mDismissButton.setVisibility(View.INVISIBLE);
                                    }
                                });
                                expandCircle(mDismissCircle, mDismissButton);
                                mShowingDismissCircle = true;
                            }
                            mShowingSnoozeCircle = false;
                            mDismissCircle.setVisibility(View.VISIBLE);
                            mSnoozeCircle.setVisibility(View.INVISIBLE);
                        } else {
                            // finger is pressed down but not entering any button zone yet
                            mSnoozeCircle.setVisibility(View.INVISIBLE);
                            mDismissCircle.setVisibility(View.INVISIBLE);
                            mDismissButton.setVisibility(View.VISIBLE);
                            mShowingSnoozeCircle = false;
                            mShowingDismissCircle = false;
                            mSnoozeButton.setAlpha(HIGHLIGHT_ALPHA);
                            mDismissButton.setAlpha(HIGHLIGHT_ALPHA);
                        }
                        return true;
                    case DragEvent.ACTION_DRAG_ENDED:
                        scaleButton(mSnoozeButton, 0, 1);
                        scaleButton(mDismissButton, 0, 1);
                        if (mIsClickingCenterButton) {
                            // If user just click the center button, make it bounce towards dismiss
                            bounceAnimation(false /* towardsSnooze */);
                        }
                        if (mShowingSnoozeCircle) {
                            final int accentColor = mResource.getColor(R.color.hot_pink);
                            reveal(mSnoozeButton, accentColor, accentColor,
                                    R.string.alarm_alert_snoozed_text,
                                    AlarmStateManager.getSnoozedMinutes(AlarmActivity.this));
                            snooze();
                        } else if (mShowingDismissCircle) {
                            reveal(mDismissButton, mResource.getColor(R.color.white),
                                    currentHourColor, R.string.alarm_alert_off_text, null);
                            dismiss();
                        } else {
                            mSnoozeButton.setAlpha(NORMAL_ALPHA);
                            mDismissButton.setAlpha(NORMAL_ALPHA);
                            mDismissButton.setVisibility(View.VISIBLE);
                            mCenterButton.setVisibility(View.VISIBLE);
                            mCenterRipple.setVisibility(View.VISIBLE);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void scaleButton(View button, float a, float b) {
        final float delta = SCALE_SLOPE * a / b + 1 - SCALE_SLOPE;
        button.setScaleX(delta);
        button.setScaleY(delta);
    }

    private void expandCircle(View circle, View button) {
        circle.setX(button.getLeft() + button.getWidth() / 2 - circle.getWidth() / 2);
        circle.setY(button.getTop() + button.getHeight() / 2 - circle.getHeight() / 2);
        final ObjectAnimator xAnim = ObjectAnimator.ofFloat(circle, "scaleX", 0, 1);
        final ObjectAnimator yAnim = ObjectAnimator.ofFloat(circle, "scaleY", 0, 1);
        final AnimatorSet set = new AnimatorSet();
        set.setDuration(mResource.getInteger(android.R.integer.config_shortAnimTime));
        set.play(xAnim).with(yAnim);
        set.start();
    }

    private void updateTimeAndTitle() {
        updateTitle();
        Utils.setTimeFormat((TextClock) (findViewById(R.id.digitalClock)),
                (int) getResources().getDimension(R.dimen.bottom_text_size));
    }

    private void drawCenterRipple() {
        final View parent = findViewById(R.id.ripple_pad);
        mCenterRipple.setCenterX(parent.getWidth() / 2);
        mCenterRipple.setCenterY(parent.getHeight() / 2);
        mCenterRipple.setAlphaFactor(1.0f);
        mCenterRipple.setRadiusGravity(0.0f);
        final ObjectAnimator radiusAnim = ObjectAnimator.ofFloat(mCenterRipple, "radiusGravity", 0,
                1);
        final ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(mCenterRipple, "alphaFactor", 1,
                0);
        radiusAnim.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnim.setRepeatCount(ValueAnimator.INFINITE);

        AnimatorSet set = new AnimatorSet();
        set.play(radiusAnim).with(alphaAnim);
        set.setInterpolator(new DecelerateInterpolator());
        set.setDuration(DateUtils.SECOND_IN_MILLIS);
        set.start();
    }

    private void updateTitle() {
        final String titleText = mInstance.getLabelOrDefault(this);
        TextView tv = (TextView) findViewById(R.id.alertTitle);
        tv.setText(titleText);
        super.setTitle(titleText);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateTimeAndTitle();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // onCreate may finish this activity before registering the Receiver
        try {
            unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {}
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss.
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down to handle a few of the system keys.
        Log.v("AlarmActivity - dispatchKeyEvent - " + event.getKeyCode());
        switch (event.getKeyCode()) {
            // Volume keys and camera keys dismiss the alarm
            case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    switch (mVolumeBehavior) {
                        case 1:
                            snooze();
                            break;

                        case 2:
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

    private void reveal(View centerView, int revealColor, int finalColor, int finalText,
            String additionalText) {

        final Rect displayRect = new Rect();
        getWindow().getDecorView().getGlobalVisibleRect(displayRect);

        final View revealView = new View(this);
        revealView.setBottom(displayRect.bottom);
        revealView.setLeft(displayRect.left);
        revealView.setRight(displayRect.right);
        revealView.setBackgroundColor(revealColor);

        final int[] clearLocation = new int[2];
        centerView.getLocationInWindow(clearLocation);
        clearLocation[0] += centerView.getWidth() / 2;

        final int revealCenterX = clearLocation[0] - revealView.getLeft();
        final int revealCenterY = clearLocation[1] - revealView.getTop();

        final double x1_2 = Math.pow(revealView.getLeft() - revealCenterX, 2);
        final double x2_2 = Math.pow(revealView.getRight() - revealCenterX, 2);
        final double y_2 = Math.pow(revealView.getTop() - revealCenterY, 2);
        final float revealRadius = (float) Math.max(Math.sqrt(x1_2 + y_2), Math.sqrt(x2_2 + y_2));

        final ViewGroupOverlay groupOverlay = (ViewGroupOverlay) mContentView.getOverlay();
        final Animator revealAnimator = ViewAnimationUtils.createCircularReveal(revealView,
                        revealCenterX, revealCenterY, 0.0f, revealRadius);
        revealAnimator.setDuration(DateUtils.SECOND_IN_MILLIS / 2);
        revealAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                groupOverlay.add(revealView);
            }
        });

        final View stayView = findViewById(R.id.final_reveal_screen);
        stayView.setBackgroundColor(finalColor);
        ((TextView) findViewById(R.id.final_text)).setText(finalText);
        if (additionalText != null) {
            ((TextView) findViewById(R.id.additional_text)).setText(additionalText);
        }
        final ValueAnimator fadeInAnimator = ObjectAnimator.ofFloat(stayView, View.ALPHA, 0.0f,
                1.0f);
        fadeInAnimator.setDuration(mResource.getInteger(android.R.integer.config_longAnimTime));
        fadeInAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                groupOverlay.add(stayView);
            }
        });
        final ValueAnimator stayAnimator = ObjectAnimator.ofFloat(stayView, View.ALPHA, 1.0f,
                1.0f);
        stayAnimator.setDuration(2 * DateUtils.SECOND_IN_MILLIS);
        stayAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                groupOverlay.remove(stayView);
                groupOverlay.remove(revealView);
            }
        });

        final AnimatorSet animatorSet = new AnimatorSet();
        // First ripple cover the entire view, then fade in text, lastly make the view stay for a
        // short period of time
        animatorSet.playSequentially(revealAnimator, fadeInAnimator, stayAnimator);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }
}
