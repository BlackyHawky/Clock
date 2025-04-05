// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_TITLE_FONT_SIZE_PREF;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SCREENSAVER_BRIGHTNESS;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_WIDGETS_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_TITLE_FONT_SIZE_PREF;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BRIGHTNESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_INTENSITY;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
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

import java.util.Locale;

public class CustomSeekbarPreference extends SeekBarPreference {

    private static final int MIN_FONT_SIZE_VALUE = 20;
    private static final int MIN_SHAKE_INTENSITY_VALUE = DEFAULT_SHAKE_INTENSITY;
    private static final int MIN_BRIGHTNESS_VALUE = 0;

    private Context mContext;
    private SharedPreferences mPrefs;
    private SeekBar mSeekBar;

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
            sendBroadcastUpdateIfNeeded();
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        if (isShakeIntensityPreference() && progress < MIN_SHAKE_INTENSITY_VALUE) {
                            seekBar.setProgress(MIN_SHAKE_INTENSITY_VALUE);
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
                sendBroadcastUpdateIfNeeded();
            }
        });
    }

    /**
     * For Android Oreo and above, sets the minimum value of the SeekBar based on the preference type.
     */
    private void configureSeekBarMinValue() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isScreensaverBrightnessPreference()) {
                mSeekBar.setMin(MIN_BRIGHTNESS_VALUE);
            } else if (isShakeIntensityPreference()) {
                mSeekBar.setMin(MIN_SHAKE_INTENSITY_VALUE);
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
        } else if (isAlarmDigitalClockFontSizePreference()) {
            currentProgress = getAlarmDigitalClockFontSizeValue();
        } else if (isAlarmTitleFontSizePreference()) {
            currentProgress = getAlarmTitleFontSizeValue();
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && progress < MIN_SHAKE_INTENSITY_VALUE) {
                return;
            }

            if (progress == DEFAULT_SHAKE_INTENSITY) {
                seekBarSummary.setText(R.string.label_default);
            } else {
                seekBarSummary.setText(String.valueOf(progress));
            }
        } else if (isAlarmDigitalClockFontSizePreference()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && progress < MIN_FONT_SIZE_VALUE) {
                return;
            }

            if (progress == DEFAULT_ALARM_DIGITAL_CLOCK_FONT_SIZE) {
                seekBarSummary.setText(R.string.label_default);
            } else {
                seekBarSummary.setText(String.valueOf(progress));
            }
        } else if (isAlarmTitleFontSizePreference()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && progress < MIN_FONT_SIZE_VALUE) {
                return;
            }

            if (progress == DEFAULT_ALARM_TITLE_FONT_SIZE_PREF) {
                seekBarSummary.setText(R.string.label_default);
            } else {
                seekBarSummary.setText(String.valueOf(progress));
            }
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && progress < MIN_FONT_SIZE_VALUE) {
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
        } else if (isShakeIntensityPreference()) {
            seekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_sensor_low));
            seekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_sensor_high));
        } else {
            seekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_text_decrease));
            seekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_text_increase));
        }

        configureSeekBarButton(seekBarMinus, -5, seekBarSummary);
        configureSeekBarButton(seekBarPlus, 5, seekBarSummary);
    }

    /**
     * Configures the buttons to increase or decrease the value of the SeekBar.
     * The interface is updated with the new value and saved in the preferences.
     * Sends a broadcast if it concerns buttons related to widget settings (widget font size).
     */
    private void configureSeekBarButton(ImageView button, final int delta, final TextView seekBarSummary) {
        button.setOnClickListener(v -> {
            int currentSeekBarValue = mSeekBar.getProgress();
            int newSeekBarValue = Math.min(Math.max(currentSeekBarValue + delta,
                    isScreensaverBrightnessPreference() ? MIN_BRIGHTNESS_VALUE
                    : isShakeIntensityPreference() ? MIN_SHAKE_INTENSITY_VALUE
                    : MIN_FONT_SIZE_VALUE), mSeekBar.getMax());
            mSeekBar.setProgress(newSeekBarValue);
            updateSeekBarSummary(seekBarSummary, newSeekBarValue);
            saveSeekBarValue(newSeekBarValue);
            sendBroadcastUpdateIfNeeded();
        });
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
                && !isAlarmDigitalClockFontSizePreference()
                && !isAlarmTitleFontSizePreference()) {
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

}
