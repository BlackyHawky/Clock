/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLACK_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLUE_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLUE_GRAY_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.BROWN_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_CLOCK_DIAL;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_CLOCK_DIAL_MATERIAL;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_CLOCK_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesDefaultValues.GREEN_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.INDIGO_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.ORANGE_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.PINK_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.PURPLE_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.RED_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.YELLOW_ACCENT_COLOR;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.content.res.AppCompatResources;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmActivity;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.screensaver.ScreensaverActivity;
import com.best.deskclock.settings.AlarmDisplayPreviewActivity;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * This widget display an analog clock with two hands for hours and minutes.
 */
public class AnalogClock extends FrameLayout {

    // Constants used for the analog clocks
    private final String DIAL = "DIAL";
    private final String HOUR_HAND = "HOUR_HAND";
    private final String MINUTE_HAND = "MINUTE_HAND";
    private final String SECOND_HAND = "SECOND_HAND";

    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final DataModel.ClockStyle mClockStyle;
    private final ImageView mHourHand;
    private final ImageView mMinuteHand;
    private final ImageView mSecondHand;
    private final String mDescFormat;
    private Calendar mTime;
    private TimeZone mTimeZone;
    private boolean mEnableSeconds = true;
    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                final String tz = intent.getStringExtra("time-zone");
                mTime = Calendar.getInstance(TimeZone.getTimeZone(tz));
            }
            onTimeChanged();
        }
    };
    private final Runnable mClockTick = new Runnable() {
        @Override
        public void run() {
            onTimeChanged();

            if (mEnableSeconds) {
                final long now = System.currentTimeMillis();
                final long delay = SECOND_IN_MILLIS - now % SECOND_IN_MILLIS;
                postDelayed(this, delay);
            }
        }
    };

    public AnalogClock(Context context) {
        this(context, null);
    }

    public AnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;
        mPrefs = getDefaultSharedPreferences(mContext);
        mClockStyle = getClockStyleForContext();
        mTime = Calendar.getInstance();
        mDescFormat = ((SimpleDateFormat) DateFormat.getTimeFormat(mContext)).toLocalizedPattern();

        final String accentColor = SettingsDAO.getAccentColor(mPrefs);
        final int alarmClockColor = SettingsDAO.getAlarmClockColor(mPrefs);
        final int alarmSecondHandColor = SettingsDAO.getAlarmSecondHandColor(mPrefs, mContext);
        final int defaultClockColor = MaterialColors.getColor(mContext, android.R.attr.textColorPrimary, Color.BLACK);

        // Create clock dial
        final ImageView dial = createClockComponent(accentColor, DIAL, alarmClockColor, defaultClockColor);

        // Create hour hand
        mHourHand = createClockComponent(accentColor, HOUR_HAND, alarmClockColor, defaultClockColor);

        // Create minute hand
        mMinuteHand = createClockComponent(accentColor, MINUTE_HAND, alarmClockColor, defaultClockColor);

        // Create second hand
        mSecondHand = createSecondHand(accentColor, alarmSecondHandColor);

        addView(dial);
        addView(mHourHand);
        addView(mMinuteHand);
        addView(mSecondHand);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if (SdkUtils.isAtLeastAndroid13()) {
            mContext.registerReceiver(mIntentReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            mContext.registerReceiver(mIntentReceiver, filter);
        }

        // Refresh the calendar instance since the time zone may have changed while the receiver
        // wasn't registered.
        mTime = Calendar.getInstance(mTimeZone != null ? mTimeZone : TimeZone.getDefault());
        onTimeChanged();

        // Tick every second.
        if (mEnableSeconds) {
            mClockTick.run();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mContext.unregisterReceiver(mIntentReceiver);
        removeCallbacks(mClockTick);
    }

    /**
     * Helper method to determine the clock style based on the context.
     */
    private DataModel.ClockStyle getClockStyleForContext() {
        if (mContext instanceof AlarmActivity || mContext instanceof AlarmDisplayPreviewActivity) {
            return SettingsDAO.getAlarmClockStyle(mPrefs);
        } else if (mContext instanceof ScreensaverActivity) {
            return SettingsDAO.getScreensaverClockStyle(mPrefs);
        } else if (mContext instanceof DeskClock) {
            return SettingsDAO.getClockStyle(mPrefs);
        } else {
            // Default for DreamService or other unknown contexts
            return SettingsDAO.getScreensaverClockStyle(mPrefs);
        }
    }

    /**
     * Helper method to create clock components (dial, hour hand and minute hand).
     */
    private ImageView createClockComponent(String accentColor, String componentType, int alarmClockColor,
                                           int defaultClockColor) {

        ImageView component = new ImageView(mContext);

        // Handle clock style and component color configuration
        if (mClockStyle == DataModel.ClockStyle.ANALOG_MATERIAL) {
            int drawableResId = getMaterialAnalogDrawableResId(componentType);
            component.setImageDrawable(AppCompatResources.getDrawable(mContext, drawableResId));
            component.setColorFilter(getMaterialAnalogClockColor(accentColor, componentType));
        } else {
            int drawableResId = getAnalogDrawableResId(componentType);
            component.setImageDrawable(AppCompatResources.getDrawable(mContext, drawableResId));
            component.setColorFilter(isAlarmContext() ? alarmClockColor : defaultClockColor);
        }

        // Must call mutate on these instances, otherwise the drawables will blur, because they're
        // sharing their size characteristics with the (smaller) world cities analog clocks.
        component.getDrawable().mutate();

        return component;
    }

    /**
     * Helper method to create the second hand with a specific color logic.
     */
    private ImageView createSecondHand(String accentColor, int alarmSecondHandColor) {
        ImageView secondHand = new ImageView(mContext);

        if (mClockStyle == DataModel.ClockStyle.ANALOG_MATERIAL) {
            secondHand.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.material_you_analog_clock_second));
            secondHand.setColorFilter(getMaterialAnalogClockColor(accentColor, SECOND_HAND));
        } else {
            secondHand.setImageDrawable(AppCompatResources.getDrawable(mContext,
                    getAnalogSecondHandPreference().equals(DEFAULT_CLOCK_SECOND_HAND)
                            ? R.drawable.analog_clock_second
                            : R.drawable.analog_clock_second_vintage));
            boolean isAutoNightAccentColorEnabled = SettingsDAO.isAutoNightAccentColorEnabled(mPrefs);
            String nightAccentColor = SettingsDAO.getNightAccentColor(mPrefs);

            secondHand.setColorFilter(isAlarmContext()
                    ? alarmSecondHandColor
                    : getAccentColor(isAutoNightAccentColorEnabled, accentColor, nightAccentColor));
        }

        // Must call mutate on these instances, otherwise the drawables will blur, because they're
        // sharing their size characteristics with the (smaller) world cities analog clocks.
        secondHand.getDrawable().mutate();

        return secondHand;
    }

    /**
     * Helper method to get the drawable resource ID for material analog components.
     */
    private int getMaterialAnalogDrawableResId(String componentType) {
        return switch (componentType) {
            case DIAL -> getMaterialAnalogDialPreference().equals(DEFAULT_CLOCK_DIAL_MATERIAL)
                    ? R.drawable.material_you_analog_clock_dial_sun
                    : R.drawable.material_you_analog_clock_dial_flower;
            case HOUR_HAND -> R.drawable.material_you_analog_clock_hour;
            case MINUTE_HAND -> R.drawable.material_you_analog_clock_minute;
            default -> 0; // Default, should never happen
        };
    }

    /**
     * Helper method to determine the style of the clock dial Material based on the context.
     */
    private String getMaterialAnalogDialPreference() {
        if (mContext instanceof AlarmActivity || mContext instanceof AlarmDisplayPreviewActivity) {
            return SettingsDAO.getAlarmClockDialMaterial(mPrefs);
        } else if (mContext instanceof ScreensaverActivity) {
            return SettingsDAO.getScreensaverClockDialMaterial(mPrefs);
        } else if (mContext instanceof DeskClock) {
            return SettingsDAO.getClockDialMaterial(mPrefs);
        } else {
            return SettingsDAO.getScreensaverClockDialMaterial(mPrefs);
        }
    }

    /**
     * Helper method to get the drawable resource ID for analog components (non-material).
     */
    private int getAnalogDrawableResId(String componentType) {
        return switch (componentType) {
            case DIAL -> getAnalogDialPreference().equals(DEFAULT_CLOCK_DIAL)
                    ? R.drawable.analog_clock_dial_with_numbers
                    : R.drawable.analog_clock_dial_without_numbers;
            case HOUR_HAND -> R.drawable.analog_clock_hour;
            case MINUTE_HAND -> R.drawable.analog_clock_minute;
            default -> 0; // Default, should never happen
        };
    }

    /**
     * Helper method to determine the clock dial style based on the context.
     */
    private String getAnalogDialPreference() {
        if (mContext instanceof AlarmActivity || mContext instanceof AlarmDisplayPreviewActivity) {
            return SettingsDAO.getAlarmClockDial(mPrefs);
        } else if (mContext instanceof ScreensaverActivity) {
            return SettingsDAO.getScreensaverClockDial(mPrefs);
        } else if (mContext instanceof DeskClock) {
            return SettingsDAO.getClockDial(mPrefs);
        } else {
            return SettingsDAO.getScreensaverClockDial(mPrefs);
        }
    }

    /**
     * Helper method to determine the second hand style based on the context.
     */
    private String getAnalogSecondHandPreference() {
        if (mContext instanceof AlarmActivity || mContext instanceof AlarmDisplayPreviewActivity) {
            return SettingsDAO.getAlarmClockSecondHand(mPrefs);
        } else if (mContext instanceof ScreensaverActivity) {
            return SettingsDAO.getScreensaverClockSecondHand(mPrefs);
        } else if (mContext instanceof DeskClock) {
            return SettingsDAO.getClockSecondHand(mPrefs);
        } else {
            return SettingsDAO.getScreensaverClockSecondHand(mPrefs);
        }
    }

    /**
     * Helper method to determine if the context is an alarm-related activity.
     */
    private boolean isAlarmContext() {
        return mContext instanceof AlarmActivity || mContext instanceof AlarmDisplayPreviewActivity;
    }

    /**
     * Helper method to get the accent color to apply to the second hand of the analog clock.
     */
    private int getAccentColor(boolean isAutoNightAccentColorEnabled, String accentColor, String nightAccentColor) {
        String colorKey = isAutoNightAccentColorEnabled
                ? accentColor
                : (ThemeUtils.isNight(mContext.getResources()) ? nightAccentColor : accentColor);

        return switch (colorKey) {
            case BLACK_ACCENT_COLOR -> mContext.getColor(R.color.blackColorPrimary);
            case BLUE_ACCENT_COLOR -> mContext.getColor(R.color.blueColorPrimary);
            case BLUE_GRAY_ACCENT_COLOR -> mContext.getColor(R.color.blueGrayColorPrimary);
            case BROWN_ACCENT_COLOR -> mContext.getColor(R.color.brownColorPrimary);
            case GREEN_ACCENT_COLOR -> mContext.getColor(R.color.greenColorPrimary);
            case INDIGO_ACCENT_COLOR -> mContext.getColor(R.color.indigoColorPrimary);
            case ORANGE_ACCENT_COLOR -> mContext.getColor(R.color.orangeColorPrimary);
            case PINK_ACCENT_COLOR -> mContext.getColor(R.color.pinkColorPrimary);
            case PURPLE_ACCENT_COLOR -> mContext.getColor(R.color.purpleColorPrimary);
            case RED_ACCENT_COLOR -> mContext.getColor(R.color.redColorPrimary);
            case YELLOW_ACCENT_COLOR -> mContext.getColor(R.color.yellowColorPrimary);
            default -> mContext.getColor(R.color.md_theme_primary);
        };
    }

    /**
     * Helper method to get colors to apply to the Material analog clock.
     */
    private int getMaterialAnalogClockColor(String accentColor, String componentType) {
        int colorResId = switch (accentColor) {
            case BLACK_ACCENT_COLOR -> getColorResourceForComponent(
                    R.color.blackColorGray1,
                    R.color.blackColorSecondary,
                    R.color.blackColorPrimary,
                    R.color.blackColorTertiary,
                    componentType);
            case BLUE_ACCENT_COLOR -> getColorResourceForComponent(
                    R.color.blueSecondaryContainer,
                    R.color.blueColorSecondary,
                    R.color.blueColorPrimary,
                    R.color.blueColorTertiary,
                    componentType);
            case BLUE_GRAY_ACCENT_COLOR -> getColorResourceForComponent(
                    R.color.blueGraySecondaryContainer,
                    R.color.blueGrayColorSecondary,
                    R.color.blueGrayColorPrimary,
                    R.color.blueGrayColorTertiary,
                    componentType);
            case BROWN_ACCENT_COLOR -> getColorResourceForComponent(
                    R.color.brownSecondaryContainer,
                    R.color.brownColorSecondary,
                    R.color.brownColorPrimary,
                    R.color.brownColorTertiary,
                    componentType);
            case GREEN_ACCENT_COLOR -> getColorResourceForComponent(
                    R.color.greenSecondaryContainer,
                    R.color.greenColorSecondary,
                    R.color.greenColorPrimary,
                    R.color.greenColorTertiary,
                    componentType);
            case INDIGO_ACCENT_COLOR -> getColorResourceForComponent(
                    R.color.indigoSecondaryContainer,
                    R.color.indigoColorSecondary,
                    R.color.indigoColorPrimary,
                    R.color.indigoColorTertiary,
                    componentType);
            case ORANGE_ACCENT_COLOR -> getColorResourceForComponent(
                    R.color.orangeSecondaryContainer,
                    R.color.orangeColorSecondary,
                    R.color.orangeColorPrimary,
                    R.color.orangeColorTertiary,
                    componentType);
            case PINK_ACCENT_COLOR -> getColorResourceForComponent(
                    R.color.pinkSecondaryContainer,
                    R.color.pinkColorSecondary,
                    R.color.pinkColorPrimary,
                    R.color.pinkColorTertiary,
                    componentType);
            case PURPLE_ACCENT_COLOR -> getColorResourceForComponent(
                    R.color.purpleSecondaryContainer,
                    R.color.purpleColorSecondary,
                    R.color.purpleColorPrimary,
                    R.color.purpleColorTertiary,
                    componentType);
            case RED_ACCENT_COLOR -> getColorResourceForComponent(
                    R.color.redSecondaryContainer,
                    R.color.redColorSecondary,
                    R.color.redColorPrimary,
                    R.color.redColorTertiary,
                    componentType);
            case YELLOW_ACCENT_COLOR -> getColorResourceForComponent(
                    R.color.yellowSecondaryContainer,
                    R.color.yellowColorSecondary,
                    R.color.yellowColorPrimary,
                    R.color.yellowColorTertiary,
                    componentType);
            default -> getColorResourceForComponent(
                    R.color.md_theme_secondaryContainer,
                    R.color.md_theme_secondary,
                    R.color.md_theme_primary,
                    R.color.md_theme_tertiary,
                    componentType);
        };

        return mContext.getColor(colorResId);
    }

    /**
     * Helper method to centralize the logic to determine the color of each type of Material analog clock component.
     */
    private int getColorResourceForComponent(int dialColorRes, int hourHandColorRes, int minuteHandColorRes,
                                             int secondHandColorRes, String componentType) {

        return switch (componentType) {
            case DIAL -> dialColorRes;
            case HOUR_HAND -> hourHandColorRes;
            case MINUTE_HAND -> minuteHandColorRes;
            case SECOND_HAND -> secondHandColorRes;
            default -> Color.BLACK; // Fallback to black color if the type is unknown
        };
    }

    private void onTimeChanged() {
        mTime.setTimeInMillis(System.currentTimeMillis());

        // To get closer to a mechanical watch, the hour hand will move according to the minute value
        int hour = mTime.get(Calendar.HOUR);
        int minute = mTime.get(Calendar.MINUTE);
        float hourAngle = (hour % 12) * 30f;
        float minuteFraction = minute * 0.5f;
        hourAngle += minuteFraction;
        mHourHand.setRotation(hourAngle);

        final float minuteAngle = mTime.get(Calendar.MINUTE) * 6f;
        mMinuteHand.setRotation(minuteAngle);

        if (mEnableSeconds) {
            final float secondAngle = mTime.get(Calendar.SECOND) * 6f;
            mSecondHand.setRotation(secondAngle);
        }

        setContentDescription(DateFormat.format(mDescFormat, mTime));
        invalidate();
    }

    public void setTimeZone(String id) {
        mTimeZone = TimeZone.getTimeZone(id);
        mTime.setTimeZone(mTimeZone);
        onTimeChanged();
    }

    public void enableSeconds(boolean enable) {
        mEnableSeconds = enable;
        if (mEnableSeconds) {
            mSecondHand.setVisibility(VISIBLE);
            mClockTick.run();
        } else {
            mSecondHand.setVisibility(GONE);
        }
    }
}
