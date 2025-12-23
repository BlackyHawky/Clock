/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.best.deskclock.BaseActivity;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerListener;
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

    private SharedPreferences mPrefs;

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

    /**
     * Displays the expired timers.
     */
    private ViewGroup mExpiredTimersView;

    private ImageView mRingtoneIcon;
    private TextView mRingtoneTitle;

    private final BroadcastReceiver PowerBtnReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getAction() != null) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)
                        || intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    final boolean isExpiredTimerResetWithPowerButton =
                            SettingsDAO.isExpiredTimerResetWithPowerButton(mPrefs);
                    if (isExpiredTimerResetWithPowerButton) {
                        DataModel.getDataModel().resetOrDeleteExpiredTimers(R.string.label_hardware_button);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getDefaultSharedPreferences(this);

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
        if (ThemeUtils.isPortrait()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        }

        setContentView(R.layout.expired_timers_activity);

        mExpiredTimersScrollView = findViewById(R.id.expired_timers_scroll);
        mExpiredTimersView = findViewById(R.id.expired_timers_list);
        final ImageView timerBackgroundImage = findViewById(R.id.timer_background_image);
        final String imagePath = SettingsDAO.getTimerBackgroundImage(mPrefs);

        if (SettingsDAO.isTimerBackgroundTransparent(mPrefs)) {
            timerBackgroundImage.setVisibility(View.GONE);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        } else {
            // Apply a background image and a blur effect.
            if (imagePath != null) {
                timerBackgroundImage.setVisibility(View.VISIBLE);

                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    if (bitmap != null) {
                        timerBackgroundImage.setImageBitmap(bitmap);

                        if (SdkUtils.isAtLeastAndroid12() && SettingsDAO.isTimerBlurEffectEnabled(mPrefs)) {
                            float intensity = SettingsDAO.getTimerBlurIntensity(mPrefs);
                            RenderEffect blur = RenderEffect.createBlurEffect(intensity, intensity, Shader.TileMode.CLAMP);
                            timerBackgroundImage.setRenderEffect(blur);
                        }
                    } else {
                        LogUtils.e("Bitmap null for path: " + imagePath);
                        timerBackgroundImage.setVisibility(View.GONE);
                    }
                } else {
                    LogUtils.e("Image file not found: " + imagePath);
                    timerBackgroundImage.setVisibility(View.GONE);
                }
            } else {
                timerBackgroundImage.setVisibility(View.GONE);
            }
        }

        // Create views for each of the expired timers.
        for (Timer timer : expiredTimers) {
            addTimer(timer);
        }

        if (SettingsDAO.isTimerRingtoneTitleDisplayed(mPrefs)) {
            mRingtoneTitle = findViewById(R.id.ringtone_title);
            mRingtoneIcon = findViewById(R.id.ringtone_icon);
            displayRingtoneTitle();
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
        super.onDestroy();
        DataModel.getDataModel().removeTimerListener(mTimerChangeWatcher);
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
                    KeyEvent.KEYCODE_CAMERA, KeyEvent.KEYCODE_FOCUS, KeyEvent.KEYCODE_HEADSETHOOK -> {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    final boolean isExpiredTimerResetWithVolumeButtons =
                            SettingsDAO.isExpiredTimerResetWithVolumeButtons(mPrefs);
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
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() |
                    WindowInsetsCompat.Type.displayCutout());

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
        int iconRingtoneSize = (int) dpToPx(24, getResources().getDisplayMetrics());
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
                mRingtoneIcon.setImageBitmap(finalBitmap);

                mRingtoneTitle.setShadowLayer(shadowRadius, shadowOffset, shadowOffset, shadowColor);
            } else {
                mRingtoneIcon.setImageDrawable(iconRingtone);
            }
        }

        mRingtoneTitle.setText(DataModel.getDataModel().getTimerRingtoneTitle());
        mRingtoneTitle.setTypeface(ThemeUtils.boldTypeface(SettingsDAO.getGeneralFont(mPrefs)));
        mRingtoneTitle.setTextColor(ringtoneTitleColor);
        // Allow text scrolling (all other attributes are indicated in the "expired_timers_activity.xml" file)
        mRingtoneTitle.setSelected(true);
    }

    /**
     * Post the first runnable to update times within the UI. It will reschedule itself as needed.
     */
    private void startUpdatingTime() {
        // Ensure only one copy of the runnable is ever scheduled by first stopping updates.
        stopUpdatingTime();
        mExpiredTimersView.post(mTimeUpdateRunnable);
    }

    /**
     * Remove the runnable that updates times within the UI.
     */
    private void stopUpdatingTime() {
        mExpiredTimersView.removeCallbacks(mTimeUpdateRunnable);
    }

    /**
     * Create and add a new view that corresponds with the given {@code timer}.
     */
    private void addTimer(Timer timer) {
        TransitionManager.beginDelayedTransition(mExpiredTimersScrollView, new AutoTransition());

        final int timerId = timer.getId();
        final TimerItem timerItem = (TimerItem)
                getLayoutInflater().inflate(R.layout.timer_item, mExpiredTimersView, false);
        // Store the timer id as a tag on the view so it can be located on delete.
        timerItem.setId(timerId);
        timerItem.bindTimer(timer);
        mExpiredTimersView.addView(timerItem);

        // Hide the label hint for expired timers.
        final TextView labelView = timerItem.findViewById(R.id.timer_label);
        labelView.setVisibility(TextUtils.isEmpty(timer.getLabel()) ? View.GONE : View.VISIBLE);

        // Add logic to the "Add Minute Or Hour" button.
        final View addTimeButton = timerItem.findViewById(R.id.timer_add_time_button);
        addTimeButton.setOnClickListener(v -> {
            final Timer timer12 = DataModel.getDataModel().getTimer(timerId);
            DataModel.getDataModel().addCustomTimeToTimer(timer12);
        });

        // Add logic to hide the 'X' and reset buttons
        final View deleteButton = timerItem.findViewById(R.id.delete_timer);
        deleteButton.setVisibility(View.GONE);
        final View resetButton = timerItem.findViewById(R.id.reset);
        resetButton.setVisibility(View.GONE);

        // Add logic to the "Stop" button
        final View stopButton = timerItem.findViewById(R.id.play_pause);
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
    }

    /**
     * Remove an existing view that corresponds with the given {@code timer}.
     */
    private void removeTimer(Timer timer) {
        TransitionManager.beginDelayedTransition(mExpiredTimersScrollView, new AutoTransition());

        final int timerId = timer.getId();
        final int count = mExpiredTimersView.getChildCount();
        for (int i = 0; i < count; ++i) {
            final View timerView = mExpiredTimersView.getChildAt(i);
            if (timerView.getId() == timerId) {
                mExpiredTimersView.removeView(timerView);
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
    }

    /**
     * Center the single timer.
     */
    private void centerFirstTimer() {
        final FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) mExpiredTimersView.getLayoutParams();
        lp.gravity = Gravity.CENTER;
        mExpiredTimersView.requestLayout();
    }

    /**
     * Display the multiple timers as a scrollable list.
     */
    private void uncenterFirstTimer() {
        final FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) mExpiredTimersView.getLayoutParams();
        lp.gravity = Gravity.NO_GRAVITY;
        mExpiredTimersView.requestLayout();
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
            final int count = mExpiredTimersView.getChildCount();
            for (int i = 0; i < count; ++i) {
                final TimerItem timerItem = (TimerItem) mExpiredTimersView.getChildAt(i);
                final Timer timer = DataModel.getDataModel().getTimer(timerItem.getId());
                if (timer != null) {
                    timerItem.updateTimeDisplay(timer);
                }
            }

            // Try to maintain a consistent period of time between redraws.
            mExpiredTimersView.postDelayed(this, 500);
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
