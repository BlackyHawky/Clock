/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.screensaver;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.utils.AlarmUtils.ACTION_NEXT_ALARM_CHANGED_BY_CLOCK;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.service.dreams.DreamService;
import android.view.LayoutInflater;
import android.view.ViewTreeObserver.OnPreDrawListener;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.DeskClockSaverBinding;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.ScreensaverUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;

public final class Screensaver extends DreamService {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("Screensaver");

    private DeskClockSaverBinding mBinding;

    private SharedPreferences mPrefs;
    private final ScreensaverSettings mSettings = new ScreensaverSettings();
    private UiDataModel mUiDataModel;
    private final OnPreDrawListener mStartPositionUpdater = new StartPositionUpdater();
    private MoveScreensaverRunnable mPositionUpdater;
    private PulseScreensaverBackgroundRunnable mBackgroundAnimator;

    /**
     * Runs every midnight or when the time changes and refreshes the date.
     */
    private final Runnable mMidnightUpdater = () -> {
        if (mBinding != null) {
            ScreensaverUtils.refreshAlarmAndDate(
                mBinding, mSettings.isUppercase, mSettings.isNextAlarmDisplayed, mSettings.isDateItalic, mSettings.isNextAlarmItalic);
        }
    };

    /**
     * Receiver to alarm clock changes.
     */
    private final BroadcastReceiver mAlarmChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(@NonNull Context context, @NonNull Intent intent) {
            if (mBinding != null) {
                ScreensaverUtils.refreshAlarmAndDate(
                    mBinding, mSettings.isUppercase, mSettings.isNextAlarmDisplayed, mSettings.isDateItalic, mSettings.isNextAlarmItalic);
            }
        }
    };

    /**
     * Receiver for battery level changes.
     */
    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(@NonNull Context context, @NonNull Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction()) && mBinding != null) {
                ScreensaverUtils.updateBatteryText(
                    mBinding.saverContainer, intent, mSettings.brightnessPercentage, mSettings.batteryColor, mSettings.isBatteryItalic);
            }
        }
    };

    @Override
    public void onCreate() {
        LOGGER.v("Screensaver created");
        super.onCreate();

        mPrefs = getDefaultSharedPreferences(this);
        mUiDataModel = UiDataModel.getUiDataModel();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onAttachedToWindow() {
        LOGGER.v("Screensaver attached to window");
        super.onAttachedToWindow();

        refreshSettings();

        mBinding = DeskClockSaverBinding.inflate(LayoutInflater.from(this));

        // To manually manage insets
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Display within the cutout area
        ThemeUtils.allowDisplayCutout(getWindow());

        setContentView(mBinding.getRoot());

        ThemeUtils.hideSystemBars(getWindow(), mBinding.saverContainer);

        ScreensaverUtils.setupScreensaverView(
            mBinding.saverContainer, mSettings, getResources().getDisplayMetrics(), ThemeUtils.isLandscape(), () -> {
                mBackgroundAnimator = new PulseScreensaverBackgroundRunnable(mBinding.screensaverBackgroundImage, mUiDataModel);
                mBackgroundAnimator.start();
            }
        );

        mPositionUpdater = new MoveScreensaverRunnable(mBinding.saverContainer, mBinding.mainClock, mUiDataModel);

        applyWindowInsets();

        // We want the screen saver to exit upon user interaction.
        setInteractive(false);
        setFullscreen(true);

        // Setup handlers for time reference changes and date updates.
        final IntentFilter filter = new IntentFilter(ACTION_NEXT_ALARM_CHANGED_BY_CLOCK);
        if (SdkUtils.isAtLeastAndroid13()) {
            registerReceiver(mAlarmChangedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mAlarmChangedReceiver, filter);
        }

        ScreensaverUtils.refreshAlarmAndDate(
            mBinding, mSettings.isUppercase, mSettings.isNextAlarmDisplayed, mSettings.isDateItalic, mSettings.isNextAlarmItalic);

        startPositionUpdater();
        mUiDataModel.addMidnightCallback(mMidnightUpdater, 100);
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();

        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

        if (SdkUtils.isAtLeastAndroid13()) {
            registerReceiver(mBatteryReceiver, batteryFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mBatteryReceiver, batteryFilter);
        }

        final Intent intent = SdkUtils.isAtLeastAndroid13()
            ? registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
            : registerReceiver(null, new IntentFilter(ACTION_BATTERY_CHANGED));

        if (intent != null) {
            ScreensaverUtils.updateBatteryText(
                mBinding.saverContainer, intent, mSettings.brightnessPercentage, mSettings.batteryColor, mSettings.isBatteryItalic);
        }
    }

    @Override
    public void onDreamingStopped() {
        super.onDreamingStopped();
        unregisterReceiver(mBatteryReceiver);
    }

    @Override
    public void onDetachedFromWindow() {
        LOGGER.v("Screensaver detached from window");

        mUiDataModel.removePeriodicCallback(mMidnightUpdater);

        stopPositionUpdater();

        if (mBackgroundAnimator != null) {
            mBackgroundAnimator.stop();
        }

        // Tear down handlers for time reference changes and date updates.
        unregisterReceiver(mAlarmChangedReceiver);

        mBinding = null;

        super.onDetachedFromWindow();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        LOGGER.v("Screensaver configuration changed");
        super.onConfigurationChanged(newConfig);

        startPositionUpdater();
        if (mBackgroundAnimator != null) {
            mBackgroundAnimator.start();
        }
    }

    /**
     * This method adjusts the space occupied by system elements (such as the status bar,
     * navigation bar or screen notch) and adjust the display of the application interface
     * accordingly.
     */
    private void applyWindowInsets() {
        InsetsUtils.doOnApplyWindowInsets(mBinding.mainClock, (v, insets) -> {
            // Get the notch insets
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.displayCutout());

            v.setPadding(bars.left, bars.top, bars.right, 0);
        });
    }

    /**
     * The screensaver container will be drawn shortly. When that draw occurs, the position updater
     * callback will also be executed to choose a random position for the time display as well as
     * schedule future callbacks to move the time display each minute.
     */
    private void startPositionUpdater() {
        mBinding.saverContainer.getViewTreeObserver().addOnPreDrawListener(mStartPositionUpdater);
    }

    /**
     * This activity is no longer in the foreground; position callbacks should be removed.
     */
    private void stopPositionUpdater() {
        mBinding.saverContainer.getViewTreeObserver().removeOnPreDrawListener(mStartPositionUpdater);
        mPositionUpdater.stop();
    }

    private void refreshSettings() {
        mSettings.backgroundImagePath = SettingsDAO.getScreensaverBackgroundImage(mPrefs);
        mSettings.blurIntensity = SettingsDAO.getScreensaverBlurIntensity(mPrefs);
        mSettings.clockStyle = SettingsDAO.getScreensaverClockStyle(mPrefs);
        mSettings.areClockSecondsEnabled = SettingsDAO.areScreensaverClockSecondsDisplayed(mPrefs);
        mSettings.brightnessPercentage = SettingsDAO.getScreensaverBrightness(mPrefs);
        mSettings.isUppercase = SettingsDAO.isScreensaverTextUppercaseDisplayed(mPrefs);
        mSettings.activeAccentColor = ThemeUtils.getActiveAccentColor(this,
            SettingsDAO.isAutoNightAccentColorEnabled(mPrefs),
            SettingsDAO.getNightAccentColor(mPrefs),
            SettingsDAO.getAccentColor(mPrefs)
        );

        mSettings.clockDial = SettingsDAO.getScreensaverClockDial(mPrefs);
        mSettings.clockDialMaterial = SettingsDAO.getScreensaverClockDialMaterial(mPrefs);
        mSettings.clockSecondHand = SettingsDAO.getScreensaverClockSecondHand(mPrefs);
        mSettings.analogClockSize = SettingsDAO.getScreensaverAnalogClockSize(mPrefs);

        mSettings.isDigitalBold = SettingsDAO.isScreensaverDigitalClockInBold(mPrefs);
        mSettings.isDigitalItalic = SettingsDAO.isScreensaverDigitalClockInItalic(mPrefs);
        mSettings.screensaverTypeface = ScreensaverUtils.getScreensaverClockTypeface(ThemeUtils.loadFont(
            SettingsDAO.getScreensaverDigitalClockFont(mPrefs)), mSettings.isDigitalBold, mSettings.isDigitalItalic);
        mSettings.digitalFontSize = SettingsDAO.getScreensaverDigitalClockFontSize(mPrefs);

        mSettings.isDateBold = SettingsDAO.isScreensaverDateInBold(mPrefs);
        mSettings.isDateItalic = SettingsDAO.isScreensaverDateInItalic(mPrefs);

        mSettings.isNextAlarmDisplayed = SettingsDAO.isScreensaverNextAlarmDisplayed(mPrefs);
        mSettings.isNextAlarmBold = SettingsDAO.isScreensaverNextAlarmInBold(mPrefs);
        mSettings.isNextAlarmItalic = SettingsDAO.isScreensaverNextAlarmInItalic(mPrefs);

        mSettings.isBatteryDisplayed = SettingsDAO.isScreensaverBatteryDisplayed(mPrefs);
        mSettings.isBatteryBold = SettingsDAO.isScreensaverBatteryInBold(mPrefs);
        mSettings.isBatteryItalic = SettingsDAO.isScreensaverBatteryInItalic(mPrefs);

        boolean isDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(mPrefs);
        boolean isMaterialAnalogClock = mSettings.clockStyle == DataModel.ClockStyle.ANALOG_MATERIAL;
        int inversePrimaryColor = ContextCompat.getColor(this, R.color.md_theme_inversePrimary);

        mSettings.clockColor = isDynamicColors
            ? inversePrimaryColor : SettingsDAO.getScreensaverClockColorPicker(mPrefs);

        mSettings.dateColor = (isDynamicColors && !isMaterialAnalogClock)
            ? inversePrimaryColor : SettingsDAO.getScreensaverDateColorPicker(mPrefs);

        mSettings.nextAlarmColor = (isDynamicColors && !isMaterialAnalogClock)
            ? inversePrimaryColor : SettingsDAO.getScreensaverNextAlarmColorPicker(mPrefs);

        mSettings.batteryColor = (isDynamicColors && !isMaterialAnalogClock)
            ? inversePrimaryColor : SettingsDAO.getScreensaverBatteryColorPicker(mPrefs);
    }

    private final class StartPositionUpdater implements OnPreDrawListener {
        /**
         * This callback occurs after initial layout has completed. It is an appropriate place to
         * select a random position for the main clock and schedule future callbacks to update
         * its position.
         *
         * @return {@code true} to continue with the drawing pass
         */
        @Override
        public boolean onPreDraw() {
            if (mBinding.saverContainer.getViewTreeObserver().isAlive()) {
                // (Re)start the periodic position updater.
                mPositionUpdater.start();

                // This listener must now be removed to avoid starting the position updater again.
                mBinding.saverContainer.getViewTreeObserver().removeOnPreDrawListener(mStartPositionUpdater);
            }
            return true;
        }
    }
}
