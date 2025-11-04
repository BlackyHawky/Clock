/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.WindowCompat;

import com.best.deskclock.BaseActivity;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.DataModel.PowerButtonBehavior;
import com.best.deskclock.data.DataModel.VolumeButtonBehavior;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.uicomponents.PillView;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.AnimatorUtils;
import com.best.deskclock.utils.ClockUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.button.MaterialButton;

import java.io.File;

public class AlarmActivity extends BaseActivity implements View.OnClickListener, View.OnTouchListener {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("AlarmActivity");

    private static final float TEXT_FADE_START_THRESHOLD = 0.5f;
    private static final int TRANSLATION_DURATION_START_DELAY = 1000;
    private static final int TRANSLATION_DURATION_DELAY = 1000;
    private static final int TRANSLATION_DURATION_MILLIS = 1000;
    private static final int ALPHA_DURATION_MILLIS = 400;
    private static final int ALERT_REVEAL_DURATION_MILLIS = 500;
    private static final int ALERT_DISMISS_DELAY_MILLIS = 2500;

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

    private SharedPreferences mPrefs;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private AlarmInstance mAlarmInstance;
    private boolean mAlarmHandled;
    private VolumeButtonBehavior mVolumeBehavior;
    private PowerButtonBehavior mPowerBehavior;
    private float mAlarmTitleFontSize;
    private int mAlarmTitleColor;
    private boolean mIsTextShadowDisplayed;
    private int mShadowColor;
    private int mShadowOffset;
    private float mShadowRadius;
    private boolean mIsSwipeActionEnabled;
    private boolean mReceiverRegistered;
    /**
     * Whether the AlarmService is currently bound
     */
    private boolean mServiceBound;
    private ViewGroup mAlertView;
    private TextView mAlertTitleView;
    private TextView mAlertInfoView;
    private ViewGroup mContentView;
    private ConstraintLayout mSlideZoneLayout;
    private PillView mPillView;
    private MaterialButton mAlarmButton;
    private MaterialButton mSnoozeButton;
    private MaterialButton mDismissButton;
    private TextView mSnoozeActionText;
    private TextView mDismissActionText;
    private TextView mRingtoneTitle;
    private ImageView mRingtoneIcon;

    private Animator mTranslationAnimator;
    private int mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;
    private float mInitialTouchX = 0;

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context storageContext = Utils.getSafeStorageContext(this);

        mPrefs = getDefaultSharedPreferences(storageContext);

        // Register Power button (screen off) intent receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        if (SdkUtils.isAtLeastAndroid13()) {
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
        mVolumeBehavior = SettingsDAO.getAlarmVolumeButtonBehavior(mPrefs);

        // Get the power button behavior setting
        mPowerBehavior = SettingsDAO.getAlarmPowerButtonBehavior(mPrefs);

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        if (SdkUtils.isAtLeastAndroid81()) {
            setTurnScreenOn(true);
            setShowWhenLocked(true);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        }

        // Honor rotation on tablets; fix the orientation on phones.
        if (ThemeUtils.isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        setContentView(R.layout.alarm_activity);

        final String darkMode = SettingsDAO.getDarkMode(mPrefs);
        final boolean isAmoledMode = ThemeUtils.isNight(getResources()) && darkMode.equals(AMOLED_DARK_MODE);
        int alarmBackgroundColor = isAmoledMode
                ? SettingsDAO.getAlarmBackgroundAmoledColor(mPrefs)
                : SettingsDAO.getAlarmBackgroundColor(mPrefs);
        final String imagePath = SettingsDAO.getAlarmBackgroundImage(mPrefs);
        final ImageView alarmBackgroundImage = findViewById(R.id.alarm_image_background);

        // Apply a background image and a blur effect.
        if (SettingsDAO.isAlarmBackgroundImageEnabled(mPrefs) && imagePath != null) {
            alarmBackgroundImage.setVisibility(View.VISIBLE);

            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    alarmBackgroundImage.setImageBitmap(bitmap);
                    alarmBackgroundImage.setColorFilter(alarmBackgroundColor);

                    if (SdkUtils.isAtLeastAndroid12() && SettingsDAO.isAlarmBlurEffectEnabled(mPrefs)) {
                        float intensity = SettingsDAO.getAlarmBlurIntensity(mPrefs);
                        RenderEffect blur = RenderEffect.createBlurEffect(intensity, intensity, Shader.TileMode.CLAMP);
                        alarmBackgroundImage.setRenderEffect(blur);
                    }
                } else {
                    LogUtils.e("Bitmap null for path: " + imagePath);
                    getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
                }
            } else {
                LogUtils.e("Image file not found: " + imagePath);
                getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
            }
        } else {
            getWindow().setBackgroundDrawable(new ColorDrawable(alarmBackgroundColor));
        }

        mIsSwipeActionEnabled = SettingsDAO.isSwipeActionEnabled(mPrefs);

        int alarmClockColor = SettingsDAO.getAlarmClockColor(mPrefs);
        float alarmDigitalClockFontSize = SettingsDAO.getAlarmDigitalClockFontSize(mPrefs);
        mAlarmTitleFontSize = SettingsDAO.getAlarmTitleFontSize(mPrefs);
        mAlarmTitleColor = SettingsDAO.getAlarmTitleColor(mPrefs);
        mIsTextShadowDisplayed = SettingsDAO.isAlarmTextShadowDisplayed(mPrefs);
        mShadowColor = SettingsDAO.getAlarmShadowColor(mPrefs);
        mShadowOffset = SettingsDAO.getAlarmShadowOffset(mPrefs);
        mShadowRadius = mShadowOffset * 0.5f;

        mAlertView = findViewById(R.id.alert);
        mAlertTitleView = mAlertView.findViewById(R.id.alert_title);
        mAlertInfoView = mAlertView.findViewById(R.id.alert_info);

        mContentView = findViewById(R.id.content);
        mSnoozeButton = mContentView.findViewById(R.id.snooze_button);
        mDismissButton = mContentView.findViewById(R.id.dismiss_button);
        mSlideZoneLayout = mContentView.findViewById(R.id.slide_zone_layout);
        mAlarmButton = mSlideZoneLayout.findViewById(R.id.alarm_button);
        mSnoozeActionText = mSlideZoneLayout.findViewById(R.id.snooze_text);
        mDismissActionText = mSlideZoneLayout.findViewById(R.id.dismiss_text);
        mPillView = mSlideZoneLayout.findViewById(R.id.pill);

        final AnalogClock analogClock = findViewById(R.id.analog_clock);
        final TextClock digitalClock = mContentView.findViewById(R.id.digital_clock);
        final DataModel.ClockStyle alarmClockStyle = SettingsDAO.getAlarmClockStyle(mPrefs);
        final boolean isAlarmSecondHandDisplayed = SettingsDAO.isAlarmSecondHandDisplayed(mPrefs);
        ClockUtils.setClockStyle(alarmClockStyle, digitalClock, analogClock);
        ClockUtils.setClockSecondsEnabled(alarmClockStyle, digitalClock, analogClock, isAlarmSecondHandDisplayed);
        ClockUtils.setTimeFormat(digitalClock, false);
        digitalClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, alarmDigitalClockFontSize);
        digitalClock.setTextColor(alarmClockColor);

        final TextView titleView = mContentView.findViewById(R.id.alarm_title);
        titleView.setText(mAlarmInstance.getLabelOrDefault(this));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mAlarmTitleFontSize);
        titleView.setTextColor(mAlarmTitleColor);
        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        titleView.setSelected(true);

        // Display a shadow if enabled in the settings
        if (mIsTextShadowDisplayed) {
            digitalClock.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
            titleView.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
        }

        if (mIsSwipeActionEnabled) {
            mSlideZoneLayout.setVisibility(VISIBLE);
            mSnoozeButton.setVisibility(GONE);
            mDismissButton.setVisibility(GONE);

            int snoozeTitleColor = SettingsDAO.getSnoozeTitleColor(mPrefs);
            int dismissTitleColor = SettingsDAO.getDismissTitleColor(mPrefs);
            int alarmButtonColor = SettingsDAO.getAlarmButtonColor(mPrefs, this);
            int slideZoneColor = SettingsDAO.getSlideZoneColor(mPrefs);

            final Drawable alarmSlideZoneBackground = AppCompatResources.getDrawable(this, R.drawable.bg_alarm_slide_zone);
            if (alarmSlideZoneBackground != null) {
                DrawableCompat.setTint(alarmSlideZoneBackground, slideZoneColor);
            }
            mSlideZoneLayout.setBackground(alarmSlideZoneBackground);

            mAlarmButton.setBackgroundColor(alarmButtonColor);
            mAlarmButton.setOnTouchListener(this);
            if (mAlarmInstance.mSnoozeDuration == ALARM_SNOOZE_DURATION_DISABLED) {
                mAlarmButton.setContentDescription(getString(isOccasionalAlarmDeletedAfterUse()
                        ? R.string.description_direction_both_for_occasional_non_repeatable_alarm
                        : R.string.description_direction_both_for_non_repeatable_alarm)
                );
            } else {
                mAlarmButton.setContentDescription(getString(isOccasionalAlarmDeletedAfterUse()
                        ? R.string.description_direction_both_for_occasional_alarm
                        : R.string.description_direction_both)
                );
            }

            if (mAlarmInstance.mSnoozeDuration == ALARM_SNOOZE_DURATION_DISABLED) {
                mSnoozeActionText.setText(getString(isOccasionalAlarmDeletedAfterUse()
                        ? R.string.delete
                        : R.string.button_action_dismiss)
                );
            } else {
                mSnoozeActionText.setText(getString(R.string.button_action_snooze));
            }
            mSnoozeActionText.setTextColor(snoozeTitleColor);

            mDismissActionText.setText(getString(isOccasionalAlarmDeletedAfterUse()
                    ? R.string.delete
                    : R.string.button_action_dismiss)
            );
            mDismissActionText.setTextColor(dismissTitleColor);

            mPillView.setFillColor(ColorUtils.setAlphaComponent(alarmButtonColor, 128));
            mPillView.post(() -> {
                mPillView.setPillHeight(mAlarmButton.getHeight() - mAlarmButton.getInsetTop() - mAlarmButton.getInsetBottom());
                final float pillStretchWidth = getAvailableSlideZoneWidth() / 2f;
                final int originalFillColor = mPillView.getFillColor();

                // Move to left
                AnimatorSet toLeftAnimator = new AnimatorSet();
                toLeftAnimator.playTogether(
                        translationAnimator(mPillView, pillStretchWidth,
                                mPillView.getPillCenterX() - pillStretchWidth / 2),
                        alphaAnimator(mPillView, originalFillColor)
                );
                toLeftAnimator.setStartDelay(TRANSLATION_DURATION_START_DELAY);
                toLeftAnimator.setDuration(TRANSLATION_DURATION_MILLIS);

                // Apply alpha
                Animator alphaLeft = alphaAnimator(mPillView, ColorUtils.setAlphaComponent(originalFillColor, 0));
                alphaLeft.setDuration(ALPHA_DURATION_MILLIS);

                // Reset position and alpha
                AnimatorSet resetAndRestoreLeft = new AnimatorSet();
                resetAndRestoreLeft.playTogether(
                        translationAnimator(mPillView, 0, mPillView.getPillCenterX()),
                        alphaAnimator(mPillView, originalFillColor)
                );
                resetAndRestoreLeft.setDuration(0);

                // Move to right
                Animator toRightAnimator = translationAnimator(mPillView, pillStretchWidth,
                        mPillView.getPillCenterX() + pillStretchWidth / 2);
                toRightAnimator.setStartDelay(TRANSLATION_DURATION_DELAY);
                toRightAnimator.setDuration(TRANSLATION_DURATION_MILLIS);

                // Apply alpha
                Animator alphaRight = alphaAnimator(mPillView, ColorUtils.setAlphaComponent(originalFillColor, 0));
                alphaRight.setDuration(ALPHA_DURATION_MILLIS);

                // Reset position and alpha
                AnimatorSet resetAndRestoreRight = new AnimatorSet();
                resetAndRestoreRight.playTogether(
                        translationAnimator(mPillView, 0, mPillView.getPillCenterX()),
                        alphaAnimator(mPillView, originalFillColor)
                );
                resetAndRestoreRight.setDuration(0);

                // Sequence
                AnimatorSet translationSequence = new AnimatorSet();
                translationSequence.playSequentially(toLeftAnimator, alphaLeft, resetAndRestoreLeft,
                        toRightAnimator, alphaRight, resetAndRestoreRight);
                translationSequence.setInterpolator(new AccelerateDecelerateInterpolator());

                // Listener to repeat animation if needed
                translationSequence.addListener(new AnimatorListenerAdapter() {

                    private boolean wasCancelled = false;

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        mPillView.setFillColor(Color.TRANSPARENT);

                        wasCancelled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!wasCancelled && mTranslationAnimator == animation) {
                            mTranslationAnimator.start();
                        }

                        wasCancelled = false;
                    }
                });

                mTranslationAnimator = translationSequence;
                mTranslationAnimator.start();
            });
        } else {
            mSlideZoneLayout.setVisibility(GONE);
            mSnoozeButton.setVisibility(VISIBLE);
            mDismissButton.setVisibility(VISIBLE);
            mSnoozeButton.setOnClickListener(this);
            mDismissButton.setOnClickListener(this);
            mSnoozeButton.setBackgroundColor(SettingsDAO.getSnoozeButtonColor(mPrefs, this));
            mDismissButton.setBackgroundColor(SettingsDAO.getDismissButtonColor(mPrefs, this));

            if (mAlarmInstance.mSnoozeDuration == ALARM_SNOOZE_DURATION_DISABLED) {
                mSnoozeButton.setText(getString(isOccasionalAlarmDeletedAfterUse()
                        ? R.string.delete
                        : R.string.button_action_dismiss)
                );

                mSnoozeButton.setContentDescription(getString(isOccasionalAlarmDeletedAfterUse()
                        ? R.string.description_dismiss_button_for_occasional_alarm
                        : R.string.description_dismiss_button)
                );
            } else {
                mSnoozeButton.setText(getString(R.string.button_action_snooze));
                mSnoozeButton.setContentDescription(getString(R.string.description_snooze_button));
            }

            mDismissButton.setText(getString(isOccasionalAlarmDeletedAfterUse()
                    ? R.string.delete
                    : R.string.button_action_dismiss)
            );
            mDismissButton.setContentDescription(getString(isOccasionalAlarmDeletedAfterUse()
                    ? R.string.description_dismiss_button_for_occasional_alarm
                    : R.string.description_dismiss_button)
            );

            // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
            mSnoozeButton.setSelected(true);
            mDismissButton.setSelected(true);
        }

        boolean isRingtoneTitleDisplayed = SettingsDAO.isRingtoneTitleDisplayed(mPrefs);
        if (isRingtoneTitleDisplayed) {
            mRingtoneTitle = mContentView.findViewById(R.id.ringtone_title);
            mRingtoneIcon = mContentView.findViewById(R.id.ringtone_icon);
            displayRingtoneTitle();
        }

        AlarmUtils.hideSystemBarsOfTriggeredAlarms(getWindow(), getWindow().getDecorView());
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
            if (SdkUtils.isAtLeastAndroid13()) {
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

        // If alarm swiping is disabled in settings, allow snooze/dismiss by tapping on respective text.
        if (!mIsSwipeActionEnabled) {
            if (view == mSnoozeButton) {
                snooze();
            } else if (view == mDismissButton) {
                dismiss();
            }
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

            // Stop the translation
            if (mTranslationAnimator != null && (mTranslationAnimator.isRunning() || mTranslationAnimator.isStarted())) {
                mTranslationAnimator.cancel();
            }

            final int[] contentLocation = {0, 0};
            mContentView.getLocationOnScreen(contentLocation);

            mInitialTouchX = event.getRawX() - contentLocation[0];
        } else if (action == MotionEvent.ACTION_CANCEL) {
            LOGGER.v("onTouch canceled: %s", event);

            // Clear the pointer index.
            mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;

            // Reset everything.
            resetAnimations();
            return true;
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

        float deltaX = x - mInitialTouchX;

        // Limit movement within the parent
        float maxDeltaX = (getAvailableSlideZoneWidth() - mAlarmButton.getWidth()) / 2f;
        deltaX = Math.max(-maxDeltaX, Math.min(deltaX, maxDeltaX));
        mAlarmButton.setTranslationX(deltaX);

        if (Math.abs(deltaX) >= maxDeltaX) {
            if (mTranslationAnimator != null && (mTranslationAnimator.isRunning() || mTranslationAnimator.isStarted())) {
                mTranslationAnimator.cancel();
            }
        }

        updateTextAlpha(deltaX);

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            LOGGER.v("onTouch ended: %s", event);

            mInitialPointerIndex = MotionEvent.INVALID_POINTER_ID;

            if (mContentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                if (deltaX <= -maxDeltaX) {
                    dismiss(); // Left = Dismiss in RTL
                } else if (deltaX >= maxDeltaX) {
                    snooze(); // Right = Snooze en RTL
                } else {
                    resetAnimations();
                }
            } else {
                if (deltaX >= maxDeltaX) {
                    dismiss(); // Right = Dismiss in RTL
                } else if (deltaX <= -maxDeltaX) {
                    snooze(); // Left = Snooze in LTR
                } else {
                    resetAnimations();
                }
            }
        }

        return true;
    }

    /**
     * Returns the width available for animations or interactions in the slide area,
     * excluding the left and right paddings of the layout.
     */
    private float getAvailableSlideZoneWidth() {
        return mSlideZoneLayout.getWidth() - mSlideZoneLayout.getPaddingStart() - mSlideZoneLayout.getPaddingEnd();
    }

    /**
     * Helper method to create a translation animation.
     */
    private Animator translationAnimator(View view, float targetWidth, float targetCenterX) {
        return ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(PillView.PILL_WIDTH, targetWidth),
                PropertyValuesHolder.ofFloat(PillView.PILL_CENTER_X, targetCenterX));
    }

    /**
     * Helper method to create an alpha color change animation.
     */
    private Animator alphaAnimator(View view, int alphaColor) {
        return ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofObject(PillView.FILL_COLOR, AnimatorUtils.ARGB_EVALUATOR, alphaColor));
    }

    /**
     * Apply transparency to "Snooze" and "Dismiss" texts based on movement direction.
     */
    private void updateTextAlpha(float deltaX) {
        final View parentView = (View) mAlarmButton.getParent();
        int parentPaddingHorizontal = parentView.getPaddingStart() + parentView.getPaddingLeft();
        int parentWidth = parentView.getWidth() - (parentPaddingHorizontal);
        float maxDeltaX = parentWidth - mAlarmButton.getWidth();
        maxDeltaX /= 2f; // since the displacement is centered

        float threshold = TEXT_FADE_START_THRESHOLD * maxDeltaX;
        float absDeltaX = Math.abs(deltaX);

        if (absDeltaX <= threshold) {
            mSnoozeActionText.setAlpha(1.0f);
            mDismissActionText.setAlpha(1.0f);
            return;
        }

        float fadeFraction = (absDeltaX - threshold) / (maxDeltaX - threshold);
        fadeFraction = Math.min(fadeFraction, 1.0f);
        float alpha = 1.0f - fadeFraction;

        boolean isRTL = mContentView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

        if ((deltaX > 0 && !isRTL) || (deltaX < 0 && isRTL)) {
            // Swipe right (Dismiss right side in LTR, left in RTL)
            mDismissActionText.setAlpha(alpha);
            mSnoozeActionText.setAlpha(1.0f);
        } else {
            // Swipe left (Snooze left in LTR, right in RTL)
            mSnoozeActionText.setAlpha(alpha);
            mDismissActionText.setAlpha(1.0f);
        }
    }

    /**
     * Set animators to initial values, reset text transparency and restart translation on pill view.
     */
    private void resetAnimations() {
        mSnoozeActionText.setAlpha(1.0f);
        mDismissActionText.setAlpha(1.0f);

        mAlarmButton.animate()
                .translationX(0)
                .setDuration(200)
                .start();

        if (mTranslationAnimator != null && !mTranslationAnimator.isRunning()) {
            mTranslationAnimator.start();
        }
    }

    /**
     * Perform snooze animation and send dismiss intent if snooze duration has been set to "None";
     * otherwise, send snooze intent.
     */
    private void snooze() {
        mAlarmHandled = true;
        LOGGER.v("Snoozed: %s", mAlarmInstance);

        // If snooze duration has been set to "None", simply dismiss the alarm.
        if (mAlarmInstance.mSnoozeDuration == ALARM_SNOOZE_DURATION_DISABLED) {
            int titleResId;
            int action;

            if (isOccasionalAlarmDeletedAfterUse()) {
                titleResId = R.string.alarm_alert_off_and_deleted_text;
                action = R.string.action_delete_alarm_after_use;
            } else {
                titleResId = R.string.alarm_alert_off_text;
                action = R.string.action_dismiss;
            }

            showAlert(titleResId, null, getString(titleResId));

            AlarmStateManager.deleteInstanceAndUpdateParent(this, mAlarmInstance);

            Events.sendAlarmEvent(action, R.string.label_deskclock);
        } else {
            int snoozeDuration = mAlarmInstance.mSnoozeDuration;
            final String infoText = getResources().getQuantityString(
                    R.plurals.alarm_alert_snooze_duration, snoozeDuration, snoozeDuration);
            final String accessibilityText = getResources().getQuantityString(
                    R.plurals.alarm_alert_snooze_set, snoozeDuration, snoozeDuration);

            showAlert(R.string.alarm_alert_snoozed_text, infoText, accessibilityText);

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

        showAlert(titleResId, null, getString(titleResId));

        AlarmStateManager.deleteInstanceAndUpdateParent(this, mAlarmInstance);

        Events.sendAlarmEvent(action, R.string.label_deskclock);

        // Unbind here, otherwise alarm will keep ringing until activity finishes.
        unbindAlarmService();
    }

    /**
     * @return {@code true} if the "Delete alarm once dismissed" button is ticked;
     * {@code false} otherwise.
     */
    private boolean isOccasionalAlarmDeletedAfterUse() {
        final Alarm alarm = Alarm.getAlarm(getContentResolver(), mAlarmInstance.mAlarmId);

        if (alarm == null) {
            LogUtils.wtf("Failed to retrieve alarm with ID: %d", mAlarmInstance.mAlarmId);
            return false;
        }

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

    /**
     * Display ringtone title if enabled in <i>"Customize alarm display"</i> settings.
     */
    private void displayRingtoneTitle() {
        final boolean silent = RingtoneUtils.RINGTONE_SILENT.equals(mAlarmInstance.mRingtone);
        final String title = DataModel.getDataModel().getRingtoneTitle(mAlarmInstance.mRingtone);
        final Drawable iconRingtone = silent
                ? AppCompatResources.getDrawable(this, R.drawable.ic_ringtone_silent)
                : AppCompatResources.getDrawable(this, R.drawable.ic_music_note);
        int iconRingtoneSize = ThemeUtils.convertDpToPixels(24, this);
        final int ringtoneTitleColor = SettingsDAO.getRingtoneTitleColor(mPrefs);

        if (iconRingtone != null) {
            if (mIsTextShadowDisplayed) {
                // Convert the drawable to a bitmap
                Bitmap iconBitmap = Bitmap.createBitmap(iconRingtoneSize, iconRingtoneSize, Bitmap.Config.ARGB_8888);
                Canvas iconCanvas = new Canvas(iconBitmap);
                iconRingtone.setBounds(0, 0, iconRingtoneSize, iconRingtoneSize);
                iconRingtone.draw(iconCanvas);

                // Create the alpha mask for the shadow
                Bitmap shadowBitmap = iconBitmap.extractAlpha();
                Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                shadowPaint.setColor(mShadowColor);
                shadowPaint.setMaskFilter(new BlurMaskFilter(mShadowRadius * 1.5f, BlurMaskFilter.Blur.NORMAL));

                // Create the final bitmap with space for the shadow
                int finalWidth = iconRingtoneSize + mShadowOffset;
                int finalHeight = iconRingtoneSize + mShadowOffset;
                Bitmap finalBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888);
                Canvas finalCanvas = new Canvas(finalBitmap);

                // Draw the blurred shadow with an offset
                finalCanvas.drawBitmap(shadowBitmap, mShadowOffset, mShadowOffset, shadowPaint);

                // Draw the normal icon on top
                finalCanvas.drawBitmap(iconBitmap, 0, 0, null);

                // Apply the result to the ImageView
                mRingtoneIcon.setImageBitmap(finalBitmap);

                mRingtoneTitle.setShadowLayer(mShadowRadius, mShadowOffset, mShadowOffset, mShadowColor);
            } else {
                iconRingtone.setTint(ringtoneTitleColor);
                mRingtoneIcon.setImageDrawable(iconRingtone);
            }
        }

        mRingtoneTitle.setText(title);
        mRingtoneTitle.setTextColor(ringtoneTitleColor);
        // Allow text scrolling (all other attributes are indicated in the "alarm_activity.xml" file)
        mRingtoneTitle.setSelected(true);
    }

    /**
     * Show alert after alarm has been snoozed or dismissed.
     */
    private void showAlert(final int titleResId, final String infoText, final String accessibilityText) {
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

        mAlertView.setAlpha(0f);
        mAlertView.animate()
                .alpha(1f)
                .setDuration(ALERT_REVEAL_DURATION_MILLIS)
                .withEndAction(() -> {
                    mAlertView.announceForAccessibility(accessibilityText);
                    mHandler.postDelayed(this::finish, ALERT_DISMISS_DELAY_MILLIS);
                })
                .start();
    }

}
