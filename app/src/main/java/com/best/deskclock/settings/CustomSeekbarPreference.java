// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_SHADOW_OFFSET;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_TITLE_FONT_SIZE_PREF;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_BLUETOOTH_VOLUME;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SCREENSAVER_BRIGHTNESS;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_TIMER_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_WIDGETS_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SHADOW_OFFSET;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_TITLE_FONT_SIZE_PREF;
import static com.best.deskclock.settings.PreferencesKeys.KEY_BLUETOOTH_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BRIGHTNESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHAKE_INTENSITY;
import static com.best.deskclock.utils.RingtoneUtils.ALARM_PREVIEW_DURATION_MS;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.ringtone.RingtonePreviewKlaxon;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.WidgetUtils;
import com.google.android.material.color.MaterialColors;

import java.util.Locale;

public class CustomSeekbarPreference extends SeekBarPreference {

    private static final int MIN_FONT_SIZE_VALUE = 20;
    private static final int MIN_SHAKE_INTENSITY_VALUE = DEFAULT_SHAKE_INTENSITY;
    private static final int MIN_TIMER_SHAKE_INTENSITY_VALUE = DEFAULT_TIMER_SHAKE_INTENSITY;
    private static final int MIN_BRIGHTNESS_VALUE = 0;
    private static final int MIN_BLUETOOTH_VOLUME = 10;
    private static final int MIN_SHADOW_OFFSET_VALUE = 1;
    private static final int MIN_BLUR_INTENSITY_VALUE = 1;

    private Context mContext;
    private SharedPreferences mPrefs;
    private SeekBar mSeekBar;
    private ImageView mSeekBarMinus;
    private ImageView mSeekBarPlus;
    private TextView mResetSeekBar;
    private final Handler mRingtoneHandler = new Handler(Looper.getMainLooper());
    private Runnable mRingtoneStopRunnable;
    private boolean mIsPreviewPlaying = false;

    public CustomSeekbarPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Binds the preference view, initializes the SeekBar and associated UI elements,
     * and sets up listeners for user interactions.
     */
    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mContext = getContext();
        mPrefs = getDefaultSharedPreferences(mContext);

        holder.itemView.setClickable(false);

        mSeekBar = (SeekBar) holder.findViewById(R.id.seekbar);
        configureSeekBarMinValue();

        final TextView seekBarSummary = (TextView) holder.findViewById(android.R.id.summary);
        setSeekBarProgress(seekBarSummary);

        mSeekBarMinus = (ImageView) holder.findViewById(R.id.seekbar_minus_icon);
        mSeekBarPlus = (ImageView) holder.findViewById(R.id.seekbar_plus_icon);
        mResetSeekBar = (TextView) holder.findViewById(R.id.reset_seekbar_value);

        configureSeekBarButtonDrawables();
        setupSeekBarButton(mSeekBarMinus, isBluetoothVolumePreference() ? -10 : -5, seekBarSummary);
        setupSeekBarButton(mSeekBarPlus, isBluetoothVolumePreference() ? 10 : 5, seekBarSummary);
        updateSeekBarButtonStates();
        updateResetButtonStates();

        mResetSeekBar.setOnClickListener(v -> {
            resetPreference();
            setSeekBarProgress(seekBarSummary);
            startRingtonePreviewForBluetoothDevices();
            updateDigitalWidgets();
            updateSeekBarButtonStates();
            updateResetButtonStates();
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (SdkUtils.isBeforeAndroid8()) {
                        if (isShakeIntensityPreference() && progress < MIN_SHAKE_INTENSITY_VALUE) {
                            seekBar.setProgress(MIN_SHAKE_INTENSITY_VALUE);
                        } else if (isTimerShakeIntensityPreference() && progress < MIN_TIMER_SHAKE_INTENSITY_VALUE) {
                            seekBar.setProgress(MIN_TIMER_SHAKE_INTENSITY_VALUE);
                        } else if (isAlarmShadowOffsetPreference() && progress < MIN_SHADOW_OFFSET_VALUE) {
                            seekBar.setProgress(MIN_SHADOW_OFFSET_VALUE);
                        } else if (isAlarmBlurIntensityPreference() && progress < MIN_BLUR_INTENSITY_VALUE) {
                            seekBar.setProgress(MIN_BLUR_INTENSITY_VALUE);
                        } else if (isBluetoothVolumePreference() && progress < MIN_BLUETOOTH_VOLUME) {
                            seekBar.setProgress(MIN_BLUETOOTH_VOLUME);
                        } else if (!isScreensaverBrightnessPreference() && progress < MIN_FONT_SIZE_VALUE) {
                            seekBar.setProgress(MIN_FONT_SIZE_VALUE);
                        }
                    }

                    updateSeekBarSummary(seekBarSummary, progress);
                    updateSeekBarButtonStates();
                    updateResetButtonStates();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                startRingtonePreviewForBluetoothDevices();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int finalProgress = seekBar.getProgress();
                saveSeekBarValue(finalProgress);
                updateDigitalWidgets();
            }
        });
    }

    /**
     * For Android Oreo and above, sets the minimum value of the SeekBar based on the preference type.
     */
    private void configureSeekBarMinValue() {
        if (SdkUtils.isAtLeastAndroid8()) {
            if (isScreensaverBrightnessPreference()) {
                mSeekBar.setMin(MIN_BRIGHTNESS_VALUE);
            } else if (isShakeIntensityPreference()) {
                mSeekBar.setMin(MIN_SHAKE_INTENSITY_VALUE);
            } else if (isTimerShakeIntensityPreference()) {
                mSeekBar.setMin(MIN_TIMER_SHAKE_INTENSITY_VALUE);
            } else if (isAlarmShadowOffsetPreference()) {
                mSeekBar.setMin(MIN_SHADOW_OFFSET_VALUE);
            } else if (isAlarmBlurIntensityPreference()) {
                mSeekBar.setMin(MIN_BLUR_INTENSITY_VALUE);
            } else if (isBluetoothVolumePreference()) {
                mSeekBar.setMin(MIN_BLUETOOTH_VOLUME);
            } else {
                mSeekBar.setMin(MIN_FONT_SIZE_VALUE);
            }
        }
    }

    /**
     * Sets the SeekBar value base on the current preference (screensaver brightness, widget font size, etc.)
     * and updates the SeekBar summary to reflect this value.
     */
    private void setSeekBarProgress(TextView seekBarSummary) {
        int currentProgress;
        if (isScreensaverBrightnessPreference()) {
            currentProgress = getScreensaverBrightnessPreferenceValue();
        } else if (isShakeIntensityPreference()) {
            currentProgress = getShakeIntensityPreferenceValue();
        } else if (isTimerShakeIntensityPreference()) {
            currentProgress = getTimerShakeIntensityPreferenceValue();
        } else if (isAlarmDigitalClockFontSizePreference()) {
            currentProgress = getAlarmDigitalClockFontSizeValue();
        } else if (isAlarmTitleFontSizePreference()) {
            currentProgress = getAlarmTitleFontSizeValue();
        } else if (isAlarmShadowOffsetPreference()) {
            currentProgress = getAlarmShadowOffsetValue();
        } else if (isAlarmBlurIntensityPreference()) {
            currentProgress = getAlarmBlurIntensityValue();
        } else if (isBluetoothVolumePreference()) {
            currentProgress = getBluetoothVolumeValue();
        } else {
            currentProgress = getWidgetPreferenceValue();
        }

        updateSeekBarSummary(seekBarSummary, currentProgress);
        mSeekBar.setProgress(currentProgress);
    }

    /**
     * Updates the SeekBar summary.
     */
    private void updateSeekBarSummary(TextView seekBarSummary, int progress) {
        if (SdkUtils.isBeforeAndroid8() && progress < getSeekBarMinValue()) {
            return;
        }

        if (progress == getDefaultSeekBarValue()) {
            seekBarSummary.setText(R.string.label_default);
        } else if (isScreensaverBrightnessPreference() || isBluetoothVolumePreference()) {
            String formattedText = String.format(Locale.getDefault(), "%d%%", progress);
            seekBarSummary.setText(formattedText);
        } else {
            seekBarSummary.setText(String.valueOf(progress));
        }
    }

    /**
     * Sets the icons for the minus and plus buttons of the SeekBar based on the current preference type.
     */
    private void configureSeekBarButtonDrawables() {
        if (isScreensaverBrightnessPreference()) {
            mSeekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_brightness_decrease));
            mSeekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_brightness_increase));
        } else if (isShakeIntensityPreference() || isTimerShakeIntensityPreference()) {
            mSeekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_sensor_low));
            mSeekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_sensor_high));
        } else if (isAlarmShadowOffsetPreference()) {
            mSeekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_shadow_minus));
            mSeekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_shadow_add));
        } else if (isAlarmBlurIntensityPreference()) {
            mSeekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_blur_down));
            mSeekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_blur_up));
        } else if (isBluetoothVolumePreference()) {
            mSeekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_volume_down));
            mSeekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_volume_up));
        } else {
            mSeekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_text_decrease));
            mSeekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_text_increase));
        }
    }

    /**
     * Configures the buttons to increase or decrease the value of the SeekBar.
     * The interface is updated with the new value and saved in the preferences.
     * Sends a broadcast if it concerns buttons related to widget settings (widget font size).
     */
    private void setupSeekBarButton(ImageView button, final int delta, final TextView seekBarSummary) {
        button.setOnClickListener(v -> {
            int newSeekBarValue = getNewSeekBarValue(delta);
            mSeekBar.setProgress(newSeekBarValue);
            updateSeekBarSummary(seekBarSummary, newSeekBarValue);
            saveSeekBarValue(newSeekBarValue);
            startRingtonePreviewForBluetoothDevices();
            updateDigitalWidgets();
            updateSeekBarButtonStates();
            updateResetButtonStates();
        });
    }

    /**
     * Updates the enabled state of the minus and plus buttons based on the current SeekBar value
     * and the enabled state of the preference.
     *
     * <p>Disables the minus button if the current value is at the minimum allowed,
     * and disables the plus button if the value is at the maximum allowed.</p>
     */
    private void updateSeekBarButtonStates() {
        boolean isPrefEnabled = isEnabled();
        int current = mSeekBar.getProgress();
        int min = getSeekBarMinValue();
        int max = mSeekBar.getMax();

        ThemeUtils.updateSeekBarButtonEnabledState(mContext, mSeekBarMinus, isPrefEnabled && current > min);
        ThemeUtils.updateSeekBarButtonEnabledState(mContext, mSeekBarPlus, isPrefEnabled && current < max);
    }

    /**
     * Updates the enabled state of the reset button based on the current SeekBar value
     * and the enabled state of the preference.
     *
     * <p>The reset button is enabled only when the SeekBar's value differs from the default
     * value for the current preference type.</p>
     */
    private void updateResetButtonStates() {
        boolean isEnabled = isEnabled() && !isSeekBarAtDefault(mSeekBar.getProgress());
        mResetSeekBar.setEnabled(isEnabled);

        if (isEnabled) {
            int enabledColor = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

            mResetSeekBar.setTextColor(enabledColor);
            TextViewCompat.setCompoundDrawableTintList(mResetSeekBar, ColorStateList.valueOf(enabledColor));
        } else {
            int disabledColor = mContext.getColor(R.color.colorDisabled);
            mResetSeekBar.setTextColor(disabledColor);
            TextViewCompat.setCompoundDrawableTintList(mResetSeekBar, ColorStateList.valueOf(disabledColor));
        }
    }

    /**
     * @return true if the SeekBar is currently set to its default value, depending on the preference type.
     */
    private boolean isSeekBarAtDefault(int currentValue) {
        return currentValue == getDefaultSeekBarValue();
    }

    /**
     * @return a new value for the SeekBar by applying a delta to the current value while respecting
     * the minimum and maximum value of the SeekBar.
     */
    private int getNewSeekBarValue(int delta) {
        int currentSeekBarValue = mSeekBar.getProgress();

        return Math.min(Math.max(currentSeekBarValue + delta, getSeekBarMinValue()), mSeekBar.getMax());
    }

    /**
     * @return the minimum allowed value for the SeekBar depending on the preference type.
     */
    private int getSeekBarMinValue() {
        return isScreensaverBrightnessPreference() ? MIN_BRIGHTNESS_VALUE
                : isShakeIntensityPreference() ? MIN_SHAKE_INTENSITY_VALUE
                : isTimerShakeIntensityPreference() ? MIN_TIMER_SHAKE_INTENSITY_VALUE
                : isAlarmShadowOffsetPreference() ? MIN_SHADOW_OFFSET_VALUE
                : isAlarmBlurIntensityPreference() ? MIN_BLUR_INTENSITY_VALUE
                : isBluetoothVolumePreference() ? MIN_BLUETOOTH_VOLUME
                : MIN_FONT_SIZE_VALUE;
    }

    /**
     * @return the default value for the SeekBar depending on the preference type.
     */
    private int getDefaultSeekBarValue() {
        if (isScreensaverBrightnessPreference()) {
            return DEFAULT_SCREENSAVER_BRIGHTNESS;
        } else if (isShakeIntensityPreference()) {
            return DEFAULT_SHAKE_INTENSITY;
        } else if (isTimerShakeIntensityPreference()) {
            return DEFAULT_TIMER_SHAKE_INTENSITY;
        } else if (isAlarmDigitalClockFontSizePreference()) {
            return DEFAULT_ALARM_DIGITAL_CLOCK_FONT_SIZE;
        } else if (isAlarmTitleFontSizePreference()) {
            return DEFAULT_ALARM_TITLE_FONT_SIZE_PREF;
        } else if (isAlarmShadowOffsetPreference()) {
            return DEFAULT_ALARM_SHADOW_OFFSET;
        } else if (isAlarmBlurIntensityPreference()) {
            return DEFAULT_BLUR_INTENSITY;
        } else if (isBluetoothVolumePreference()) {
            return DEFAULT_BLUETOOTH_VOLUME;
        } else {
            return DEFAULT_WIDGETS_FONT_SIZE;
        }
    }

    /**
     * Saves the current value of the SeekBar in SharedPreferences using the appropriate preference key.
     */
    private void saveSeekBarValue(int value) {
        mPrefs.edit().putInt(getKey(), value).apply();
    }

    /**
     * Resets the SeekBar value.
     */
    private void resetPreference() {
        mPrefs.edit().remove(getKey()).apply();
    }

    /**
     * Update digital widgets if the Preference is linked to the widgets one.
     */
    private void updateDigitalWidgets() {
        if (!isScreensaverBrightnessPreference()
                && !isShakeIntensityPreference()
                && !isTimerShakeIntensityPreference()
                && !isAlarmDigitalClockFontSizePreference()
                && !isAlarmTitleFontSizePreference()
                && !isAlarmShadowOffsetPreference()
                && !isAlarmBlurIntensityPreference()
                && !isBluetoothVolumePreference()) {

            WidgetUtils.updateAllDigitalWidgets(mContext);
        }
    }

    /**
     * Retrieves the current value of the SeekBar related to the widget preference (widget font size)
     * from the SharedPreferences.
     */
    private int getWidgetPreferenceValue() {
        return mPrefs.getInt(getKey(), DEFAULT_WIDGETS_FONT_SIZE);
    }

    /**
     * Retrieves the current value of the SeekBar related to the screensaver brightness from
     * the SharedPreferences.
     */
    private int getScreensaverBrightnessPreferenceValue() {
        return mPrefs.getInt(getKey(), DEFAULT_SCREENSAVER_BRIGHTNESS);
    }

    /**
     * Retrieves the current value of the SeekBar related to the shake intensity from
     * the SharedPreferences.
     */
    private int getShakeIntensityPreferenceValue() {
        return mPrefs.getInt(getKey(), DEFAULT_SHAKE_INTENSITY);
    }

    /**
     * Retrieves the current value of the SeekBar related to the timer shake intensity from
     * the SharedPreferences.
     */
    private int getTimerShakeIntensityPreferenceValue() {
        return mPrefs.getInt(getKey(), DEFAULT_TIMER_SHAKE_INTENSITY);
    }

    /**
     * Retrieves the current value of the SeekBar related to the font size of the alarm clock
     * from SharedPreferences.
     */
    private int getAlarmDigitalClockFontSizeValue() {
        return mPrefs.getInt(getKey(), DEFAULT_ALARM_DIGITAL_CLOCK_FONT_SIZE);
    }

    /**
     * Retrieves the current value of the SeekBar related to the alarm title font size
     * from SharedPreferences.
     */
    private int getAlarmTitleFontSizeValue() {
        return mPrefs.getInt(getKey(), DEFAULT_ALARM_TITLE_FONT_SIZE_PREF);
    }

    /**
     * @return the current value of the SeekBar related to alarm shadow offset from SharedPreferences.
     */
    private int getAlarmShadowOffsetValue() {
        return mPrefs.getInt(getKey(), DEFAULT_ALARM_SHADOW_OFFSET);
    }

    /**
     * @return the current value of the SeekBar related to alarm blur effect from SharedPreferences.
     */
    private int getAlarmBlurIntensityValue() {
        return mPrefs.getInt(getKey(), DEFAULT_BLUR_INTENSITY);
    }

    /**
     * @return the current value of the SeekBar related to volume when a Bluetooth device
     * is connected from SharedPreferences.
     */
    private int getBluetoothVolumeValue() {
        return mPrefs.getInt(getKey(), DEFAULT_BLUETOOTH_VOLUME);
    }

    /**
     * @return {@code true} if the current preference is related to screensaver brightness.
     * {@code false} otherwise.
     */
    private boolean isScreensaverBrightnessPreference() {
        return getKey().equals(KEY_SCREENSAVER_BRIGHTNESS);
    }

    /**
     * @return {@code true} if the current preference is related to shake intensity.
     * {@code false} otherwise.
     */
    private boolean isShakeIntensityPreference() {
        return getKey().equals(KEY_SHAKE_INTENSITY);
    }

    /**
     * @return {@code true} if the current preference is related to timer shake intensity.
     * {@code false} otherwise.
     */
    private boolean isTimerShakeIntensityPreference() {
        return getKey().equals(KEY_TIMER_SHAKE_INTENSITY);
    }

    /**
     * @return {@code true} if the current preference is related to the font size of the alarm clock.
     * {@code false} otherwise.
     */
    private boolean isAlarmDigitalClockFontSizePreference() {
        return getKey().equals(KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE);
    }

    /**
     * @return {@code true} if the current preference is related to the alarm title font size.
     * {@code false} otherwise.
     */
    private boolean isAlarmTitleFontSizePreference() {
        return getKey().equals(KEY_ALARM_TITLE_FONT_SIZE_PREF);
    }

    /**
     * @return {@code true} if the current preference is related to shadow offset for alarms.
     * {@code false} otherwise.
     */
    private boolean isAlarmShadowOffsetPreference() {
        return getKey().equals(KEY_ALARM_SHADOW_OFFSET);
    }

    /**
     * @return {@code true} if the current preference is related to blur intensity for alarms.
     * {@code false} otherwise.
     */
    private boolean isAlarmBlurIntensityPreference() {
        return getKey().equals(KEY_ALARM_BLUR_INTENSITY);
    }

    /**
     * @return {@code true} if the current preference is related to volume when a Bluetooth device
     * is connected. {@code false} otherwise.
     */
    private boolean isBluetoothVolumePreference() {
        return getKey().equals(KEY_BLUETOOTH_VOLUME);
    }

    /**
     * Plays ringtone preview if preference is Bluetooth volume or if there is a Bluetooth device connected.
     */
    private void startRingtonePreviewForBluetoothDevices() {
        if (!isBluetoothVolumePreference() || !RingtoneUtils.hasBluetoothDeviceConnected(mContext, mPrefs)) {
            return;
        }

        if (mRingtoneStopRunnable != null) {
            mRingtoneHandler.removeCallbacks(mRingtoneStopRunnable);
        }

        // If we are not currently playing, start.
        Uri ringtoneUri = DataModel.getDataModel().getAlarmRingtoneUriFromSettings();
        if (RingtoneUtils.isRandomRingtone(ringtoneUri)) {
            ringtoneUri = RingtoneUtils.getRandomRingtoneUri();
        } else if (RingtoneUtils.isRandomCustomRingtone(ringtoneUri)) {
            ringtoneUri = RingtoneUtils.getRandomCustomRingtoneUri();
        }

        RingtonePreviewKlaxon.start(mContext, mPrefs, ringtoneUri);
        mIsPreviewPlaying = true;

        mRingtoneStopRunnable = this::stopRingtonePreviewForBluetoothDevices;

        // Stop the preview after 5 seconds
        mRingtoneHandler.postDelayed(mRingtoneStopRunnable, ALARM_PREVIEW_DURATION_MS);
    }

    /**
     * Stops playing the ringtone preview if it is currently playing.
     */
    public void stopRingtonePreviewForBluetoothDevices() {
        if (!mIsPreviewPlaying) {
            return;
        }

        if (mRingtoneStopRunnable != null) {
            mRingtoneHandler.removeCallbacks(mRingtoneStopRunnable);
        }

        RingtonePreviewKlaxon.stop(mContext, mPrefs);
        RingtonePreviewKlaxon.stopListeningToPreferences();

        mIsPreviewPlaying = false;
    }

}
