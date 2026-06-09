/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.transition.TransitionManager;

import com.best.deskclock.R;
import com.best.deskclock.base.BaseActivity;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerListener;
import com.best.deskclock.databinding.ExpiredTimersActivityBinding;
import com.best.deskclock.databinding.TimerItemBinding;
import com.best.deskclock.databinding.TimerItemCompactBinding;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;

import java.io.File;
import java.util.List;

/**
 * This activity is designed to be shown over the lock screen. As such, it displays the expired
 * timers and a single button to reset them all. Each expired timer can also be reset to one minute
 * with a button in the user interface. All other timer operations are disabled in this activity.
 */
public class ExpiredTimersActivity extends BaseActivity {

    private static final long POWER_BUTTON_ACTIVATION_DELAY = 1500;

    private ExpiredTimersActivityBinding mBinding;
    private SharedPreferences mPrefs;
    private Typeface mRegularTypeface;
    private Typeface mBoldTypeface;
    private Typeface mTimerTimeTypeface;
    private DisplayMetrics mDisplayMetrics;
    private boolean mAreTimerButtonPositionsInverted;
    private boolean mIsIndicatorStateDisplayed;
    private int mColorPaused;
    private int mColorRunning;
    private int mColorExpired;
    private int mColorMissed;
    private boolean mIsPortrait;
    private boolean mIsTablet;
    private int mMargin10;
    private int mMargin2;
    private long mActivityStartTime;

    /**
     * Scheduled to update the timers while at least one is expired.
     */
    private final Runnable mTimeUpdateRunnable = new TimeUpdateRunnable();

    /**
     * Updates the timers displayed in this activity as the backing data changes.
     */
    private final TimerListener mTimerChangeWatcher = new TimerChangeWatcher();

    /**
     * The scene root for transitions when expired timers are added/removed from this container.
     */
    private ViewGroup mExpiredTimersScrollView;

    private final BroadcastReceiver PowerBtnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    // Ignore immediate screen-off events to prevent the proximity sensor from instantly dismissing
                    // the timer if the device wakes up in a pocket or face down.
                    if (SystemClock.elapsedRealtime() - mActivityStartTime < POWER_BUTTON_ACTIVATION_DELAY) {
                        LogUtils.v("Ignored ACTION_SCREEN_OFF due to grace period.");
                        return;
                    }

                    if (SettingsDAO.isExpiredTimerResetWithPowerButton(mPrefs)) {
                        DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_hardware_button);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivityStartTime = SystemClock.elapsedRealtime();

        mBinding = ExpiredTimersActivityBinding.inflate(getLayoutInflater());

        mPrefs = getDefaultSharedPreferences(this);
        mAreTimerButtonPositionsInverted = SettingsDAO.areTimerButtonPositionsInverted(mPrefs);
        mIsIndicatorStateDisplayed = SettingsDAO.isTimerStateIndicatorDisplayed(mPrefs);
        mColorPaused = SettingsDAO.getPausedTimerIndicatorColor(mPrefs);
        mColorRunning = SettingsDAO.getRunningTimerIndicatorColor(mPrefs);
        mColorExpired = SettingsDAO.getExpiredTimerIndicatorColor(mPrefs);
        mColorMissed = SettingsDAO.getMissedTimerIndicatorColor(mPrefs);
        String generalFontPath = SettingsDAO.getGeneralFont(mPrefs);
        mRegularTypeface = ThemeUtils.loadFont(generalFontPath);
        mBoldTypeface = ThemeUtils.boldTypeface(generalFontPath);
        mTimerTimeTypeface = ThemeUtils.loadFont(SettingsDAO.getTimerDurationFont(mPrefs));
        mDisplayMetrics = getResources().getDisplayMetrics();
        mIsPortrait = ThemeUtils.isPortrait();
        mIsTablet = ThemeUtils.isTablet();
        mMargin10 = (int) dpToPx(10, mDisplayMetrics);
        mMargin2 = (int) dpToPx(2, mDisplayMetrics);

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Register Power button (screen off) intent receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        if (SdkUtils.isAtLeastAndroid13()) {
            registerReceiver(PowerBtnReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(PowerBtnReceiver, filter);
        }

        final List<Timer> expiredTimers = getExpiredTimers();

        // If no expired timers, finish
        if (expiredTimers.isEmpty()) {
            LogUtils.i("No expired timers, skipping display.");
            finish();
            return;
        }

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
        if (mIsPortrait) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        setContentView(mBinding.getRoot());

        String activeAccentColor = ThemeUtils.isNight(getResources()) && !SettingsDAO.isAutoNightAccentColorEnabled(mPrefs)
            ? SettingsDAO.getNightAccentColor(mPrefs)
            : SettingsDAO.getAccentColor(mPrefs);

        getWindow().setBackgroundDrawable(new ColorDrawable(ThemeUtils.getNightBackgroundColor(this, activeAccentColor)));

        if (mBinding.expiredTimersScrollVertical != null) {
            mExpiredTimersScrollView = mBinding.expiredTimersScrollVertical;
        } else {
            mExpiredTimersScrollView = mBinding.expiredTimersScrollHorizontal;
        }

        final String imagePath = SettingsDAO.getTimerBackgroundImage(mPrefs);

        if (SettingsDAO.isTimerRingtoneTitleDisplayed(mPrefs)) {
            displayRingtoneTitle();
            mBinding.ringtoneLayout.setVisibility(VISIBLE);
        }

        if (SettingsDAO.isTimerBackgroundTransparent(mPrefs)) {
            mBinding.timerBackgroundImage.setVisibility(GONE);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        } else {
            // Apply a background image and a blur effect.
            if (imagePath != null) {
                mBinding.timerBackgroundImage.setVisibility(VISIBLE);

                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    if (bitmap != null) {
                        mBinding.timerBackgroundImage.setImageBitmap(bitmap);

                        if (SdkUtils.isAtLeastAndroid12() && SettingsDAO.isTimerBlurEffectEnabled(mPrefs)) {
                            float intensity = SettingsDAO.getTimerBlurIntensity(mPrefs);
                            RenderEffect blur = RenderEffect.createBlurEffect(intensity, intensity, Shader.TileMode.CLAMP);
                            mBinding.timerBackgroundImage.setRenderEffect(blur);
                        }
                    } else {
                        LogUtils.e("Bitmap null for path: " + imagePath);
                        mBinding.timerBackgroundImage.setVisibility(GONE);
                    }
                } else {
                    LogUtils.e("Image file not found: " + imagePath);
                    mBinding.timerBackgroundImage.setVisibility(GONE);
                }
            } else {
                mBinding.timerBackgroundImage.setVisibility(GONE);
            }
        }

        // Create views for each of the expired timers.
        for (Timer timer : expiredTimers) {
            addTimer(timer);
        }

        AlarmUtils.hideSystemBarsOfTriggeredAlarms(getWindow(), getWindow().getDecorView());

        applyWindowInsets();

        // Update views in response to timer data changes.
        DataModel.getDataModel().addTimerListener(mTimerChangeWatcher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUpdatingTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdatingTime();
    }

    @Override
    public void onDestroy() {
        DataModel.getDataModel().removeTimerListener(mTimerChangeWatcher);

        mRegularTypeface = null;
        mBoldTypeface = null;
        mTimerTimeTypeface = null;

        mExpiredTimersScrollView = null;

        mBinding = null;

        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
                 KeyEvent.KEYCODE_CAMERA, KeyEvent.KEYCODE_FOCUS, KeyEvent.KEYCODE_HEADSETHOOK -> {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    final boolean isExpiredTimerResetWithVolumeButtons = SettingsDAO.isExpiredTimerResetWithVolumeButtons(mPrefs);
                    if (isExpiredTimerResetWithVolumeButtons) {
                        DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_hardware_button);
                    }
                }
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    /**
     * This method adjusts the space occupied by system elements (such as the status bar,
     * navigation bar or screen notch) and adjust the display of the application interface
     * accordingly.
     */
    private void applyWindowInsets() {
        InsetsUtils.doOnApplyWindowInsets(mExpiredTimersScrollView, (v, insets) -> {
            // Get the system bar and notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            v.setPadding(bars.left, bars.top, bars.right, 0);
        });
    }

    /**
     * Display ringtone title if enabled in Timer settings.
     */
    private void displayRingtoneTitle() {
        final boolean silent = RingtoneUtils.RINGTONE_SILENT.equals(DataModel.getDataModel().getTimerRingtoneUri());
        final Drawable iconRingtone = silent
            ? AppCompatResources.getDrawable(this, R.drawable.ic_ringtone_silent)
            : AppCompatResources.getDrawable(this, R.drawable.ic_music_note);
        int iconRingtoneSize = (int) dpToPx(24, mDisplayMetrics);
        final int ringtoneTitleColor = SettingsDAO.getTimerRingtoneTitleColor(mPrefs);
        final int shadowOffset = SettingsDAO.getTimerShadowOffset(mPrefs);
        final float shadowRadius = shadowOffset * 0.5f;
        final int shadowColor = SettingsDAO.getTimerShadowColor(mPrefs);

        if (iconRingtone != null) {
            iconRingtone.setTint(ringtoneTitleColor);

            if (SettingsDAO.isTimerTextShadowDisplayed(mPrefs)) {
                // Convert the drawable to a bitmap
                Bitmap iconBitmap = Bitmap.createBitmap(iconRingtoneSize, iconRingtoneSize, Bitmap.Config.ARGB_8888);
                Canvas iconCanvas = new Canvas(iconBitmap);
                iconRingtone.setBounds(0, 0, iconRingtoneSize, iconRingtoneSize);
                iconRingtone.draw(iconCanvas);

                // Create the alpha mask for the shadow
                Bitmap shadowBitmap = iconBitmap.extractAlpha();
                Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                shadowPaint.setColor(shadowColor);
                shadowPaint.setMaskFilter(new BlurMaskFilter(shadowRadius * 1.5f, BlurMaskFilter.Blur.NORMAL));

                // Create the final bitmap with space for the shadow
                int finalWidth = iconRingtoneSize + shadowOffset;
                int finalHeight = iconRingtoneSize + shadowOffset;
                Bitmap finalBitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888);
                Canvas finalCanvas = new Canvas(finalBitmap);

                // Draw the blurred shadow with an offset
                finalCanvas.drawBitmap(shadowBitmap, shadowOffset, shadowOffset, shadowPaint);

                // Draw the normal icon on top
                finalCanvas.drawBitmap(iconBitmap, 0, 0, null);

                // Apply the result to the ImageView
                mBinding.ringtoneIcon.setImageBitmap(finalBitmap);

                mBinding.ringtoneTitle.setShadowLayer(shadowRadius, shadowOffset, shadowOffset, shadowColor);
            } else {
                mBinding.ringtoneIcon.setImageDrawable(iconRingtone);
            }
        }

        mBinding.ringtoneTitle.setText(DataModel.getDataModel().getTimerRingtoneTitle());
        mBinding.ringtoneTitle.setTypeface(ThemeUtils.boldTypeface(SettingsDAO.getGeneralFont(mPrefs)));
        mBinding.ringtoneTitle.setTextColor(ringtoneTitleColor);
        // Allow text scrolling (all other attributes are indicated in the "expired_timers_activity.xml" file)
        mBinding.ringtoneTitle.setSelected(true);
    }

    /**
     * Post the first runnable to update times within the UI. It will reschedule itself as needed.
     */
    private void startUpdatingTime() {
        // Ensure only one copy of the runnable is ever scheduled by first stopping updates.
        stopUpdatingTime();
        mBinding.expiredTimersList.post(mTimeUpdateRunnable);
    }

    /**
     * Remove the runnable that updates times within the UI.
     */
    private void stopUpdatingTime() {
        mBinding.expiredTimersList.removeCallbacks(mTimeUpdateRunnable);
    }

    /**
     * Create and add a new view that corresponds with the given {@code timer}.
     */
    private void addTimer(Timer timer) {
        TransitionManager.beginDelayedTransition(mExpiredTimersScrollView);

        final int timerId = timer.getId();
        final boolean isCompact = SettingsDAO.isCompactTimersDisplayed(mPrefs) && !SettingsDAO.isSingleTimerModeEnabled(mPrefs);
        final boolean useCompactLayout = ThemeUtils.isPortrait() && isCompact;

        final View view;
        final TextView labelView;
        final View addTimeButton;
        final View deleteButton;
        final View resetButton;
        final View stopButton;

        if (useCompactLayout) {
            TimerItemCompactBinding compactBinding = TimerItemCompactBinding.inflate(
                getLayoutInflater(), mBinding.expiredTimersList, false);

            view = compactBinding.getRoot();
            ((TimerItemCompact) view).setButtonPosition(mAreTimerButtonPositionsInverted);
            ((TimerItemCompact) view).setGeneralFonts(mRegularTypeface, mBoldTypeface);
            ((TimerItemCompact) view).setTimerTimeFont(mTimerTimeTypeface);
            ((TimerItemCompact) view).setIndicatorStateDisplay(mIsIndicatorStateDisplayed);
            ((TimerItemCompact) view).setIndicatorColors(mColorPaused, mColorRunning, mColorExpired, mColorMissed);
            ((TimerItemCompact) view).bindTimer(timer, false);

            labelView = compactBinding.timerLabel;
            addTimeButton = compactBinding.timerAddTimeButton;
            deleteButton = compactBinding.deleteTimerButton;
            resetButton = compactBinding.resetOrEditButton;
            stopButton = compactBinding.playPauseButton;
        } else {
            TimerItemBinding normalBinding = TimerItemBinding.inflate(getLayoutInflater(), mBinding.expiredTimersList, false);

            view = normalBinding.getRoot();
            ((TimerItem) view).setButtonPosition(mAreTimerButtonPositionsInverted);
            ((TimerItem) view).setGeneralFonts(mRegularTypeface, mBoldTypeface);
            ((TimerItem) view).setTimerTimeFont(mTimerTimeTypeface);
            ((TimerItem) view).setIndicatorStateDisplay(mIsIndicatorStateDisplayed);
            ((TimerItem) view).setIndicatorColors(mColorPaused, mColorRunning, mColorExpired, mColorMissed);
            ((TimerItem) view).bindTimer(timer, false);

            labelView = normalBinding.timerLabel;
            addTimeButton = normalBinding.timerAddTimeButton;
            deleteButton = normalBinding.deleteTimerButton;
            resetButton = normalBinding.resetOrEditButton;
            stopButton = normalBinding.playPauseButton;
        }

        // Store the timer id as a tag on the view so it can be located on delete.
        view.setId(timerId);

        mBinding.expiredTimersList.addView(view);

        // Hide the label hint for expired timers.
        labelView.setVisibility(TextUtils.isEmpty(timer.getLabel()) ? GONE : VISIBLE);

        // Add logic to the "Add Minute Or Hour" button.
        addTimeButton.setOnClickListener(v -> {
            final Timer timer1 = DataModel.getDataModel().getTimer(timerId);
            DataModel.getDataModel().addCustomTimeToTimer(timer1);
        });

        // Add logic to hide the "Delete" and "Reset" buttons
        deleteButton.setVisibility(GONE);
        resetButton.setVisibility(GONE);

        // Add logic to the "Stop" button
        stopButton.setOnClickListener(v -> {
            final Timer timer1 = DataModel.getDataModel().getTimer(timerId);
            DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_deskclock);
            removeTimer(timer1);
        });

        // If the first timer was just added, center it.
        final List<Timer> expiredTimers = getExpiredTimers();
        if (expiredTimers.size() == 1) {
            centerFirstTimer();
        } else if (expiredTimers.size() == 2) {
            uncenterFirstTimer();
        }

        updateAllTimerBackgrounds();
    }

    /**
     * Remove an existing view that corresponds with the given {@code timer}.
     */
    private void removeTimer(Timer timer) {
        TransitionManager.beginDelayedTransition(mExpiredTimersScrollView);

        final int timerId = timer.getId();
        final int count = mBinding.expiredTimersList.getChildCount();
        for (int i = 0; i < count; ++i) {
            final View timerView = mBinding.expiredTimersList.getChildAt(i);
            if (timerView.getId() == timerId) {
                mBinding.expiredTimersList.removeView(timerView);
                break;
            }
        }

        // If the second last timer was just removed, center the last timer.
        final List<Timer> expiredTimers = getExpiredTimers();
        if (expiredTimers.isEmpty()) {
            finish();
        } else if (expiredTimers.size() == 1) {
            centerFirstTimer();
        }

        updateAllTimerBackgrounds();
    }

    /**
     * Center the single timer.
     */
    private void centerFirstTimer() {
        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mBinding.expiredTimersList.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        mBinding.expiredTimersList.requestLayout();
    }

    /**
     * Display the multiple timers as a scrollable list.
     */
    private void uncenterFirstTimer() {
        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mBinding.expiredTimersList.getLayoutParams();
        lp.gravity = Gravity.NO_GRAVITY;
        mBinding.expiredTimersList.requestLayout();
    }

    private void updateAllTimerBackgrounds() {
        final int totalCount = mBinding.expiredTimersList.getChildCount();

        if (totalCount == 0) {
            return;
        }

        final boolean isPhoneInLandscapeMode = !mIsTablet && !mIsPortrait;
        final boolean isTabletOrPortrait = mIsTablet || mIsPortrait;

        for (int i = 0; i < totalCount; i++) {
            View child = mBinding.expiredTimersList.getChildAt(i);
            child.setBackground(isPhoneInLandscapeMode
                ? ThemeUtils.expressiveCardBackgroundForLandscape(this, i, totalCount)
                : ThemeUtils.expressiveCardBackground(this, i, totalCount));

            if (child.getLayoutParams() instanceof ViewGroup.MarginLayoutParams layoutParams) {
                if (isTabletOrPortrait) {
                    layoutParams.leftMargin = mMargin10;
                    layoutParams.rightMargin = mMargin10;
                    layoutParams.topMargin = (i > 0) ? mMargin2 : 0;
                } else {
                    layoutParams.leftMargin = (i > 0) ? mMargin2 : 0;
                    layoutParams.rightMargin = 0;
                    layoutParams.topMargin = 0;
                }

                layoutParams.bottomMargin = 0;

                child.setLayoutParams(layoutParams);
            }
        }
    }

    private List<Timer> getExpiredTimers() {
        return DataModel.getDataModel().getExpiredTimers();
    }

    /**
     * Periodically refreshes the state of each timer.
     */
    private class TimeUpdateRunnable implements Runnable {
        @Override
        public void run() {
            final int count = mBinding.expiredTimersList.getChildCount();

            for (int i = 0; i < count; ++i) {
                final View child = mBinding.expiredTimersList.getChildAt(i);

                final int timerId = child.getId();
                final Timer timer = DataModel.getDataModel().getTimer(timerId);
                if (timer == null) {
                    continue;
                }

                if (child instanceof TimerItem) {
                    ((TimerItem) child).updateTimeDisplay(timer, false);
                } else if (child instanceof TimerItemCompact) {
                    ((TimerItemCompact) child).updateTimeDisplay(timer, false);
                }
            }

            // Try to maintain a consistent period of time between redraws.
            mBinding.expiredTimersList.postDelayed(this, 500);
        }
    }

    /**
     * Adds and removes expired timers from this activity based on their state changes.
     */
    private class TimerChangeWatcher implements TimerListener {
        @Override
        public void timerAdded(Timer timer) {
            if (timer.isExpired()) {
                addTimer(timer);
            }
        }

        @Override
        public void timerUpdated(Timer before, Timer after) {
            if (!before.isExpired() && after.isExpired()) {
                addTimer(after);
            } else if (before.isExpired() && !after.isExpired()) {
                removeTimer(before);
            }
        }

        @Override
        public void timerRemoved(Timer timer) {
            if (timer.isExpired()) {
                removeTimer(timer);
            }
        }
    }
}
