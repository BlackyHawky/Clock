// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_TITLE_FONT_SIZE_PREF;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_BLUETOOTH_VOLUME;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SCREENSAVER_BRIGHTNESS;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_TIMER_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_WIDGETS_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_TITLE_FONT_SIZE_PREF;
import static com.best.deskclock.settings.PreferencesKeys.KEY_BLUETOOTH_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_RINGTONE_PREVIEW_PLAYING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BRIGHTNESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHAKE_INTENSITY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.ringtone.RingtonePreviewKlaxon;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;

import java.util.Locale;

public class CustomSeekbarPreference extends SeekBarPreference {

    private static final int MIN_FONT_SIZE_VALUE = 20;
    private static final int MIN_SHAKE_INTENSITY_VALUE = DEFAULT_SHAKE_INTENSITY;
    private static final int MIN_TIMER_SHAKE_INTENSITY_VALUE = DEFAULT_TIMER_SHAKE_INTENSITY;
    private static final int MIN_BRIGHTNESS_VALUE = 0;
    private static final int MIN_BLUETOOTH_VOLUME = 10;

    private Context mContext;
    private SharedPreferences mPrefs;
    private SeekBar mSeekBar;
    private boolean mPreviewPlaying = false;

    public CustomSeekbarPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

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

        configureSeekBarButtons(holder, seekBarSummary);

        final TextView resetSeekBar = (TextView) holder.findViewById(R.id.reset_seekbar_value);
        resetSeekBar.setOnClickListener(v -> {
            resetPreference();
            setSeekBarProgress(seekBarSummary);
            startRingtonePreviewForBluetoothDevices();
            sendBroadcastUpdateIfNeeded();
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
                        } else if (isBluetoothVolumePreference() && progress < MIN_BLUETOOTH_VOLUME) {
                            seekBar.setProgress(MIN_BLUETOOTH_VOLUME);
                        } else if (!isScreensaverBrightnessPreference() && progress < MIN_FONT_SIZE_VALUE) {
                            seekBar.setProgress(MIN_FONT_SIZE_VALUE);
                        }
                    }

                    updateSeekBarSummary(seekBarSummary, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int finalProgress = seekBar.getProgress();
                saveSeekBarValue(finalProgress);
                updateSeekBarSummary(seekBarSummary, finalProgress);
                startRingtonePreviewForBluetoothDevices();
                sendBroadcastUpdateIfNeeded();
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
        if (isScreensaverBrightnessPreference()) {
            if (progress == DEFAULT_SCREENSAVER_BRIGHTNESS) {
                seekBarSummary.setText(R.string.label_default);
            } else {
                String formattedText = String.format(Locale.getDefault(), "%d%%", progress);
                seekBarSummary.setText(formattedText);
            }
        } else if (isShakeIntensityPreference()) {
            if (SdkUtils.isBeforeAndroid8() && progress < MIN_SHAKE_INTENSITY_VALUE) {
                return;
            }

            if (progress == DEFAULT_SHAKE_INTENSITY) {
                seekBarSummary.setText(R.string.label_default);
            } else {
                seekBarSummary.setText(String.valueOf(progress));
            }
        } else if (isTimerShakeIntensityPreference()) {
            if (SdkUtils.isBeforeAndroid8() && progress < MIN_TIMER_SHAKE_INTENSITY_VALUE) {
                return;
            }

            if (progress == DEFAULT_TIMER_SHAKE_INTENSITY) {
                seekBarSummary.setText(R.string.label_default);
            } else {
                seekBarSummary.setText(String.valueOf(progress));
            }
        } else if (isAlarmDigitalClockFontSizePreference()) {
            if (SdkUtils.isBeforeAndroid8() && progress < MIN_FONT_SIZE_VALUE) {
                return;
            }

            if (progress == DEFAULT_ALARM_DIGITAL_CLOCK_FONT_SIZE) {
                seekBarSummary.setText(R.string.label_default);
            } else {
                seekBarSummary.setText(String.valueOf(progress));
            }
        } else if (isAlarmTitleFontSizePreference()) {
            if (SdkUtils.isBeforeAndroid8() && progress < MIN_FONT_SIZE_VALUE) {
                return;
            }

            if (progress == DEFAULT_ALARM_TITLE_FONT_SIZE_PREF) {
                seekBarSummary.setText(R.string.label_default);
            } else {
                seekBarSummary.setText(String.valueOf(progress));
            }
        } else if (isBluetoothVolumePreference()) {
            if (SdkUtils.isBeforeAndroid8() && progress < MIN_BLUETOOTH_VOLUME) {
                return;
            }

            if (progress == DEFAULT_BLUETOOTH_VOLUME) {
                seekBarSummary.setText(R.string.label_default);
            } else {
                String formattedText = String.format(Locale.getDefault(), "%d%%", progress);
                seekBarSummary.setText(formattedText);
            }
        } else {
            if (SdkUtils.isBeforeAndroid8() && progress < MIN_FONT_SIZE_VALUE) {
                return;
            }

            if (progress == DEFAULT_WIDGETS_FONT_SIZE) {
                seekBarSummary.setText(R.string.label_default);
            } else {
                seekBarSummary.setText(String.valueOf(progress));
            }
        }
    }

    /**
     * Configures the buttons that increase or decrease the value of the SeekBar.
     */
    private void configureSeekBarButtons(@NonNull PreferenceViewHolder holder, final TextView seekBarSummary) {
        final ImageView seekBarMinus = (ImageView) holder.findViewById(R.id.seekbar_minus_icon);
        final ImageView seekBarPlus = (ImageView) holder.findViewById(R.id.seekbar_plus_icon);

        if (isScreensaverBrightnessPreference()) {
            seekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_brightness_decrease));
            seekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_brightness_increase));
        } else if (isShakeIntensityPreference() || isTimerShakeIntensityPreference()) {
            seekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_sensor_low));
            seekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_sensor_high));
        } else if (isBluetoothVolumePreference()) {
            seekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_volume_down));
            seekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_volume_up));
        } else {
            seekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_text_decrease));
            seekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_text_increase));
        }

        configureSeekBarButton(seekBarMinus, isBluetoothVolumePreference() ? -10 : -5, seekBarSummary);
        configureSeekBarButton(seekBarPlus, isBluetoothVolumePreference() ? 10 : 5, seekBarSummary);
    }

    /**
     * Configures the buttons to increase or decrease the value of the SeekBar.
     * The interface is updated with the new value and saved in the preferences.
     * Sends a broadcast if it concerns buttons related to widget settings (widget font size).
     */
    private void configureSeekBarButton(ImageView button, final int delta, final TextView seekBarSummary) {
        button.setOnClickListener(v -> {
            int newSeekBarValue = getNewSeekBarValue(delta);
            mSeekBar.setProgress(newSeekBarValue);
            updateSeekBarSummary(seekBarSummary, newSeekBarValue);
            saveSeekBarValue(newSeekBarValue);
            startRingtonePreviewForBluetoothDevices();
            sendBroadcastUpdateIfNeeded();
        });
    }

    /**
     * @return a new value for the SeekBar by applying a delta to the current value while respecting
     * the minimum and maximum value of the SeekBar.
     */
    private int getNewSeekBarValue(int delta) {
        int currentSeekBarValue = mSeekBar.getProgress();

        return Math.min(Math.max(currentSeekBarValue + delta,
                isScreensaverBrightnessPreference() ? MIN_BRIGHTNESS_VALUE
                : isShakeIntensityPreference() ? MIN_SHAKE_INTENSITY_VALUE
                : isTimerShakeIntensityPreference() ? MIN_TIMER_SHAKE_INTENSITY_VALUE
                : isBluetoothVolumePreference() ? MIN_BLUETOOTH_VOLUME
                : MIN_FONT_SIZE_VALUE), mSeekBar.getMax());
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
     * Sends a broadcast to update widgets.
     */
    private void sendBroadcastUpdateIfNeeded() {
        if (!isScreensaverBrightnessPreference()
                && !isShakeIntensityPreference()
                && !isTimerShakeIntensityPreference()
                && !isAlarmDigitalClockFontSizePreference()
                && !isAlarmTitleFontSizePreference()
                && !isBluetoothVolumePreference()) {
            mContext.sendBroadcast(new Intent(ACTION_APPWIDGET_UPDATE));
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
     * @return {@code true} if the current preference is related to volume when a Bluetooth device
     * is connected. {@code false} otherwise.
     */
    private boolean isBluetoothVolumePreference() {
        return getKey().equals(KEY_BLUETOOTH_VOLUME);
    }

    /**
     * Plays ringtone preview if preference is Bluetooth volume or if there is a Bluetooth device connected.
     */
    public void startRingtonePreviewForBluetoothDevices() {
        if (!isBluetoothVolumePreference() || !RingtoneUtils.hasBluetoothDeviceConnected(mContext, mPrefs)) {
            return;
        }

        if (!mPreviewPlaying) {
            // If we are not currently playing, start.
            Uri ringtoneUri = DataModel.getDataModel().getAlarmRingtoneUriFromSettings();
            if (RingtoneUtils.isRandomRingtone(ringtoneUri)) {
                ringtoneUri = RingtoneUtils.getRandomRingtoneUri();
            } else if (RingtoneUtils.isRandomCustomRingtone(ringtoneUri)) {
                ringtoneUri = RingtoneUtils.getRandomCustomRingtoneUri();
            }

            RingtonePreviewKlaxon.start(mContext, mPrefs, ringtoneUri);
            mPrefs.edit().putBoolean(KEY_RINGTONE_PREVIEW_PLAYING, true).apply();
            mPreviewPlaying = true;

            // Stop the preview after 5 seconds
            mSeekBar.postDelayed(() -> {
                stopRingtonePreviewForBluetoothDevices(mContext, mPrefs);
                mPreviewPlaying = false;
            }, 5000);
        }
    }

    /**
     * Stops playing the ringtone preview if it is currently playing.
     */
    public void stopRingtonePreviewForBluetoothDevices(Context context, SharedPreferences prefs) {
        if (mPreviewPlaying) {
            RingtonePreviewKlaxon.stop(context, prefs);
            mPrefs.edit().putBoolean(KEY_RINGTONE_PREVIEW_PLAYING, false).apply();
        }
    }

    /**
     * Stops listening for changes to ringtone preferences.
     */
    public void stopListeningToPreferences() {
        RingtonePreviewKlaxon.stopListeningToPreferences();
    }

}
