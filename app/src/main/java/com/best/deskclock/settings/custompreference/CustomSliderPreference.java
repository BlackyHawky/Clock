// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.*;
import static com.best.deskclock.settings.PreferencesKeys.*;
import static com.best.deskclock.utils.RingtoneUtils.ALARM_PREVIEW_DURATION_MS;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.SettingsPreferenceSliderLayoutBinding;
import com.best.deskclock.ringtone.RingtonePreviewKlaxon;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.WidgetUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.slider.Slider;

import java.util.Locale;

public class CustomSliderPreference extends Preference {

    private static final int MIN_FONT_SIZE_VALUE = 20;
    private static final int MIN_CORNER_RADIUS_VALUE = 0;
    private static final int MIN_SHAKE_INTENSITY_VALUE = DEFAULT_SHAKE_INTENSITY;
    private static final int MIN_TIMER_SHAKE_INTENSITY_VALUE = DEFAULT_TIMER_SHAKE_INTENSITY;
    private static final int MIN_BRIGHTNESS_VALUE = 0;
    private static final int MIN_EXTERNAL_AUDIO_DEVICE_VOLUME = 10;
    private static final int MIN_SHADOW_OFFSET_VALUE = 1;
    private static final int MIN_BLUR_INTENSITY_VALUE = 1;
    private static final int MIN_ANALOG_CLOCK_SIZE_VALUE = 1;

    // The max values below correspond to the max values defined in the preferences XML files.
    private static final int MAX_BRIGHTNESS_VALUE = 100;
    private static final int MAX_CORNER_RADIUS_VALUE = 100;
    private static final int MAX_SHAKE_INTENSITY_VALUE = 55;
    private static final int MAX_TIMER_SHAKE_INTENSITY_VALUE = 55;
    private static final int MAX_SHADOW_OFFSET_VALUE = 20;
    private static final int MAX_BLUR_INTENSITY_VALUE = 100;
    private static final int MAX_EXTERNAL_AUDIO_DEVICE_VOLUME = 100;
    private static final int MAX_ANALOG_CLOCK_SIZE_VALUE = 100;
    private static final int MAX_FONT_SIZE_VALUE = 200;

    private SettingsPreferenceSliderLayoutBinding mBinding;
    private final SharedPreferences mPrefs;
    private final Typeface mBoldTypeface;

    private final Handler mRingtoneHandler = new Handler(Looper.getMainLooper());
    private Runnable mRingtoneStopRunnable;
    private boolean mIsPreviewPlaying = false;

    public CustomSliderPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mPrefs = getDefaultSharedPreferences(context);
        mBoldTypeface = ThemeUtils.boldTypeface(SettingsDAO.getGeneralFont(mPrefs));
    }

    /**
     * Binds the preference view, initializes the slider and associated UI elements,
     * and sets up listeners for user interactions.
     */
    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        if (holder.itemView.isInEditMode()) {
            // Skip logic during Android Studio preview
            return;
        }

        super.onBindViewHolder(holder);

        mBinding = SettingsPreferenceSliderLayoutBinding.bind(holder.itemView);

        holder.itemView.setClickable(false);

        mBinding.slider.clearOnChangeListeners();
        mBinding.slider.clearOnSliderTouchListeners();

        mBinding.slider.setStepSize(1f);
        setSliderProgress(mBinding.summary);
        configureSliderButtonDrawables();
        setupSliderButton(mBinding.sliderMinusIcon, isExternalAudioDeviceVolumePreference() ? -10 : -5, mBinding.summary);
        setupSliderButton(mBinding.sliderPlusIcon, isExternalAudioDeviceVolumePreference() ? 10 : 5, mBinding.summary);
        updateSliderButtonStates();
        updateResetButtonStates();

        mBinding.resetSliderValue.setTypeface(mBoldTypeface);
        mBinding.resetSliderValue.setOnClickListener(v -> {
            resetPreference();
            setSliderProgress(mBinding.summary);
            startRingtonePreviewForExternalAudioDevices();
            updateDigitalWidgets();
            updateSliderButtonStates();
            updateResetButtonStates();
        });

        mBinding.slider.addOnChangeListener((slider, progress, fromUser) -> {
            if (fromUser) {
                updateSliderSummary(mBinding.summary, (int) progress);
                updateSliderButtonStates();
                updateResetButtonStates();
            }
        });

        mBinding.slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                startRingtonePreviewForExternalAudioDevices();
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                int finalProgress = (int) slider.getValue();
                saveSliderValue(finalProgress);
                updateDigitalWidgets();
            }
        });
    }

    /**
     * Configures the bounds (min/max) of the {@link Slider} based on the preference type and applies a value to it.
     *
     * <p>This method includes a safety mechanism for RecyclerViews: it temporarily widens the bounds of the Slider before applying
     * the new value. This prevents an IllegalStateException in case the ghost value of a recycled Slider falls outside
     * the new strict boundaries.</p>
     *
     * @param newValue The desired value for this Slider.
     * @return The "safe" value. If "newValue" exceeds the allowed bounds for this preference, it will be clamped to the legal
     * minimum or maximum.
     */
    private int getSafeSliderValue(float newValue) {
        float min, max;

        if (isScreensaverBrightnessPreference()) {
            min = MIN_BRIGHTNESS_VALUE;
            max = MAX_BRIGHTNESS_VALUE;
        } else if (isDigitalWidgetBackgroundCornerRadius()
            || isNextAlarmWidgetBackgroundCornerRadius()
            || isVerticalWidgetBackgroundCornerRadius()) {
            min = MIN_CORNER_RADIUS_VALUE;
            max = MAX_CORNER_RADIUS_VALUE;
        } else if (isShakeIntensityPreference()) {
            min = MIN_SHAKE_INTENSITY_VALUE;
            max = MAX_SHAKE_INTENSITY_VALUE;
        } else if (isTimerShakeIntensityPreference()) {
            min = MIN_TIMER_SHAKE_INTENSITY_VALUE;
            max = MAX_TIMER_SHAKE_INTENSITY_VALUE;
        } else if (isTimerShadowOffsetPreference() || isAlarmShadowOffsetPreference()) {
            min = MIN_SHADOW_OFFSET_VALUE;
            max = MAX_SHADOW_OFFSET_VALUE;
        } else if (isScreensaverBlurIntensityPreference()
            || isTimerBlurIntensityPreference()
            || isAlarmBlurIntensityPreference()) {
            min = MIN_BLUR_INTENSITY_VALUE;
            max = MAX_BLUR_INTENSITY_VALUE;
        } else if (isExternalAudioDeviceVolumePreference()) {
            min = MIN_EXTERNAL_AUDIO_DEVICE_VOLUME;
            max = MAX_EXTERNAL_AUDIO_DEVICE_VOLUME;
        } else if (isAnalogClockSizePreference()
            || isScreensaverAnalogClockSizePreference()
            || isAlarmAnalogClockSizePreference()) {
            min = MIN_ANALOG_CLOCK_SIZE_VALUE;
            max = MAX_ANALOG_CLOCK_SIZE_VALUE;
        } else {
            min = MIN_FONT_SIZE_VALUE;
            max = MAX_FONT_SIZE_VALUE;
        }

        int safeValue = (int) Math.max(min, Math.min(max, newValue));

        mBinding.slider.setValueFrom(Math.min(mBinding.slider.getValueFrom(), min));
        mBinding.slider.setValueTo(Math.max(mBinding.slider.getValueTo(), max));

        mBinding.slider.setValue(safeValue);

        mBinding.slider.setValueFrom(min);
        mBinding.slider.setValueTo(max);

        return safeValue;
    }

    /**
     * Sets the slider value base on the current preference (screensaver brightness, widget font size, etc.)
     * and updates the slider summary to reflect this value.
     */
    private void setSliderProgress(TextView sliderSummary) {
        int currentProgress = mPrefs.getInt(getKey(), getDefaultSliderValue());
        int safeProgress = getSafeSliderValue(currentProgress);

        updateSliderSummary(sliderSummary, safeProgress);

        if (safeProgress != currentProgress) {
            saveSliderValue(safeProgress);
        }
    }

    /**
     * Updates the slider summary.
     */
    private void updateSliderSummary(TextView sliderSummary, int progress) {
        if (progress == getDefaultSliderValue()) {
            sliderSummary.setText(R.string.label_default);
        } else if (isScreensaverBrightnessPreference()
            || isScreensaverAnalogClockSizePreference()
            || isExternalAudioDeviceVolumePreference()
            || isAnalogClockSizePreference()
            || isAlarmAnalogClockSizePreference()) {
            String formattedText = String.format(Locale.getDefault(), "%d%%", progress);
            sliderSummary.setText(formattedText);
        } else {
            sliderSummary.setText(String.valueOf(progress));
        }
    }

    /**
     * Sets the icons for the minus and plus buttons of the slider based on the current preference type.
     */
    private void configureSliderButtonDrawables() {
        if (isScreensaverBrightnessPreference()) {
            mBinding.sliderMinusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_brightness_decrease));
            mBinding.sliderPlusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_brightness_increase));
        } else if (isDigitalWidgetBackgroundCornerRadius()
            || isNextAlarmWidgetBackgroundCornerRadius()
            || isVerticalWidgetBackgroundCornerRadius()) {
            mBinding.sliderMinusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_rounded_corner_decrease));
            mBinding.sliderPlusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_rounded_corner_increase));
        } else if (isShakeIntensityPreference() || isTimerShakeIntensityPreference()) {
            mBinding.sliderMinusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_sensor_low));
            mBinding.sliderPlusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_sensor_high));
        } else if (isTimerShadowOffsetPreference() || isAlarmShadowOffsetPreference()) {
            mBinding.sliderMinusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_shadow_decrease));
            mBinding.sliderPlusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_shadow_increase));
        } else if (isScreensaverBlurIntensityPreference()
            || isTimerBlurIntensityPreference()
            || isAlarmBlurIntensityPreference()) {
            mBinding.sliderMinusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_blur_decrease));
            mBinding.sliderPlusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_blur_increase));
        } else if (isExternalAudioDeviceVolumePreference()) {
            mBinding.sliderMinusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_volume_down));
            mBinding.sliderPlusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_volume_up));
        } else if (isAnalogClockSizePreference()
            || isScreensaverAnalogClockSizePreference()
            || isAlarmAnalogClockSizePreference()) {
            mBinding.sliderMinusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_zoom_in));
            mBinding.sliderPlusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_zoom_out));
        } else {
            mBinding.sliderMinusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_text_decrease));
            mBinding.sliderPlusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_text_increase));
        }
    }

    /**
     * Configures the buttons to increase or decrease the value of the slider.
     * The interface is updated with the new value and saved in the preferences.
     * Sends a broadcast if it concerns buttons related to widget settings (widget font size).
     */
    private void setupSliderButton(ImageView button, final int delta, final TextView sliderSummary) {
        button.setOnClickListener(v -> {
            int newSliderValue = getNewSliderValue(delta);
            mBinding.slider.setValue(newSliderValue);
            updateSliderSummary(sliderSummary, newSliderValue);
            saveSliderValue(newSliderValue);
            startRingtonePreviewForExternalAudioDevices();
            updateDigitalWidgets();
            updateSliderButtonStates();
            updateResetButtonStates();
        });
    }

    /**
     * Updates the enabled state of the minus and plus buttons based on the current slider value
     * and the enabled state of the preference.
     *
     * <p>Disables the minus button if the current value is at the minimum allowed,
     * and disables the plus button if the value is at the maximum allowed.</p>
     */
    private void updateSliderButtonStates() {
        boolean isPrefEnabled = isEnabled();
        int current = (int) mBinding.slider.getValue();
        int min = (int) mBinding.slider.getValueFrom();
        int max = (int) mBinding.slider.getValueTo();

        ThemeUtils.updateSliderButtonEnabledState(getContext(), mBinding.sliderMinusIcon, isPrefEnabled && current > min);
        ThemeUtils.updateSliderButtonEnabledState(getContext(), mBinding.sliderPlusIcon, isPrefEnabled && current < max);
    }

    /**
     * Updates the enabled state of the reset button based on the current slider value
     * and the enabled state of the preference.
     *
     * <p>The reset button is enabled only when the slider value differs from the default
     * value for the current preference type.</p>
     */
    private void updateResetButtonStates() {
        boolean isEnabled = isEnabled() && !isSliderAtDefault((int) mBinding.slider.getValue());
        mBinding.resetSliderValue.setEnabled(isEnabled);

        if (isEnabled) {
            int enabledColor = MaterialColors.getColor(getContext(), androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

            mBinding.resetSliderValue.setTextColor(enabledColor);
            TextViewCompat.setCompoundDrawableTintList(mBinding.resetSliderValue, ColorStateList.valueOf(enabledColor));
        } else {
            int disabledColor = ContextCompat.getColor(getContext(), R.color.colorDisabled);
            mBinding.resetSliderValue.setTextColor(disabledColor);
            TextViewCompat.setCompoundDrawableTintList(mBinding.resetSliderValue, ColorStateList.valueOf(disabledColor));
        }
    }

    /**
     * @return true if the slider is currently set to its default value, depending on the preference type.
     */
    private boolean isSliderAtDefault(int currentValue) {
        return currentValue == getDefaultSliderValue();
    }

    /**
     * @return a new value for the slider by applying a delta to the current value while respecting
     * the minimum and maximum value of the slider.
     */
    private int getNewSliderValue(int delta) {
        int currentSliderValue = (int) mBinding.slider.getValue();

        return (int) Math.min(Math.max(currentSliderValue + delta, mBinding.slider.getValueFrom()), mBinding.slider.getValueTo());
    }

    /**
     * @return the default value for the slider depending on the preference type.
     */
    private int getDefaultSliderValue() {
        if (isScreensaverBrightnessPreference()) {
            return DEFAULT_SCREENSAVER_BRIGHTNESS;
        } else if (isDigitalWidgetBackgroundCornerRadius()
            || isNextAlarmWidgetBackgroundCornerRadius()
            || isVerticalWidgetBackgroundCornerRadius()) {
            return DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS;
        } else if (isShakeIntensityPreference()) {
            return DEFAULT_SHAKE_INTENSITY;
        } else if (isTimerShakeIntensityPreference()) {
            return DEFAULT_TIMER_SHAKE_INTENSITY;
        } else if (isScreensaverDigitalClockFontSizePreference()
            || isDigitalClockFontSizePreference()
            || isAlarmDigitalClockFontSizePreference()) {
            return DEFAULT_DIGITAL_CLOCK_FONT_SIZE;
        } else if (isAlarmTitleFontSizePreference()) {
            return DEFAULT_ALARM_TITLE_FONT_SIZE_PREF;
        } else if (isTimerShadowOffsetPreference() || isAlarmShadowOffsetPreference()) {
            return DEFAULT_SHADOW_OFFSET;
        } else if (isScreensaverBlurIntensityPreference()
            || isTimerBlurIntensityPreference()
            || isAlarmBlurIntensityPreference()) {
            return DEFAULT_BLUR_INTENSITY;
        } else if (isExternalAudioDeviceVolumePreference()) {
            return DEFAULT_EXTERNAL_AUDIO_DEVICE_VOLUME;
        } else if (isAnalogClockSizePreference()
            || isScreensaverAnalogClockSizePreference()
            || isAlarmAnalogClockSizePreference()) {
            return DEFAULT_ANALOG_CLOCK_SIZE;
        } else {
            return DEFAULT_WIDGETS_FONT_SIZE;
        }
    }

    /**
     * Saves the current value of the slider in SharedPreferences using the appropriate preference key.
     */
    private void saveSliderValue(int value) {
        mPrefs.edit().putInt(getKey(), value).apply();
    }

    /**
     * Resets the slider value.
     */
    private void resetPreference() {
        mPrefs.edit().remove(getKey()).apply();
    }

    /**
     * Update digital widgets if the Preference is linked to the widgets one.
     */
    private void updateDigitalWidgets() {
        if (!isScreensaverBrightnessPreference()
            && !isScreensaverDigitalClockFontSizePreference()
            && !isScreensaverAnalogClockSizePreference()
            && !isScreensaverBlurIntensityPreference()
            && !isShakeIntensityPreference()
            && !isTimerShakeIntensityPreference()
            && !isTimerShadowOffsetPreference()
            && !isTimerBlurIntensityPreference()
            && !isAlarmDigitalClockFontSizePreference()
            && !isAlarmTitleFontSizePreference()
            && !isAlarmShadowOffsetPreference()
            && !isAlarmBlurIntensityPreference()
            && !isExternalAudioDeviceVolumePreference()
            && !isAnalogClockSizePreference()
            && !isAlarmAnalogClockSizePreference()
            && !isDigitalClockFontSizePreference()) {

            WidgetUtils.updateAllDigitalWidgets(getContext());
        }
    }

    /**
     * @return {@code true} if the current preference is related to screensaver brightness.
     * {@code false} otherwise.
     */
    private boolean isScreensaverBrightnessPreference() {
        return getKey().equals(KEY_SCREENSAVER_BRIGHTNESS);
    }

    /**
     * @return {@code true} if the current preference is related to the screensaver analog clock size.
     * {@code false} otherwise.
     */
    private boolean isScreensaverAnalogClockSizePreference() {
        return getKey().equals(KEY_SCREENSAVER_ANALOG_CLOCK_SIZE);
    }

    /**
     * @return {@code true} if the current preference is related to the font size of the screensaver
     * clock. {@code false} otherwise.
     */
    private boolean isScreensaverDigitalClockFontSizePreference() {
        return getKey().equals(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT_SIZE);
    }

    /**
     * @return {@code true} if the current preference is related to blur intensity for screensaver.
     * {@code false} otherwise.
     */
    private boolean isScreensaverBlurIntensityPreference() {
        return getKey().equals(KEY_SCREENSAVER_BLUR_INTENSITY);
    }

    /**
     * @return {@code true} if the current preference is related to corner radius of the digital widget background.
     * {@code false} otherwise.
     */
    private boolean isDigitalWidgetBackgroundCornerRadius() {
        return getKey().equals(KEY_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if the current preference is related to corner radius of the Next alarm widget background.
     * {@code false} otherwise.
     */
    private boolean isNextAlarmWidgetBackgroundCornerRadius() {
        return getKey().equals(KEY_NEXT_ALARM_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if the current preference is related to corner radius of the vertical widget background.
     * {@code false} otherwise.
     */
    private boolean isVerticalWidgetBackgroundCornerRadius() {
        return getKey().equals(KEY_VERTICAL_WIDGET_BACKGROUND_CORNER_RADIUS);
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
     * @return {@code true} if the current preference is related to shadow offset for timers.
     * {@code false} otherwise.
     */
    private boolean isTimerShadowOffsetPreference() {
        return getKey().equals(KEY_TIMER_SHADOW_OFFSET);
    }

    /**
     * @return {@code true} if the current preference is related to blur intensity for timers.
     * {@code false} otherwise.
     */
    private boolean isTimerBlurIntensityPreference() {
        return getKey().equals(KEY_TIMER_BLUR_INTENSITY);
    }

    /**
     * @return {@code true} if the current preference is related to the analog clock size.
     * {@code false} otherwise.
     */
    private boolean isAnalogClockSizePreference() {
        return getKey().equals(KEY_ANALOG_CLOCK_SIZE);
    }

    /**
     * @return {@code true} if the current preference is related to the font size of the clock.
     * {@code false} otherwise.
     */
    private boolean isDigitalClockFontSizePreference() {
        return getKey().equals(KEY_DIGITAL_CLOCK_FONT_SIZE);
    }

    /**
     * @return {@code true} if the current preference is related to the alarm analog clock size.
     * {@code false} otherwise.
     */
    private boolean isAlarmAnalogClockSizePreference() {
        return getKey().equals(KEY_ALARM_ANALOG_CLOCK_SIZE);
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
     * @return {@code true} if the current preference is related to volume when
     * an external audio device is connected. {@code false} otherwise.
     */
    private boolean isExternalAudioDeviceVolumePreference() {
        return getKey().equals(KEY_EXTERNAL_AUDIO_DEVICE_VOLUME);
    }

    /**
     * Plays ringtone preview if preference is "External audio device volume" or if there is an
     * external audio device connected.
     */
    private void startRingtonePreviewForExternalAudioDevices() {
        if (!isExternalAudioDeviceVolumePreference() || !RingtoneUtils.hasExternalAudioDeviceConnected(getContext(), mPrefs)) {
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

        RingtonePreviewKlaxon.start(ringtoneUri);
        mIsPreviewPlaying = true;

        mRingtoneStopRunnable = this::stopRingtonePreviewForExternalAudioDevices;

        // Stop the preview after 5 seconds
        mRingtoneHandler.postDelayed(mRingtoneStopRunnable, ALARM_PREVIEW_DURATION_MS);
    }

    /**
     * Stops playing the ringtone preview if it is currently playing.
     */
    public void stopRingtonePreviewForExternalAudioDevices() {
        if (!mIsPreviewPlaying) {
            return;
        }

        if (mRingtoneStopRunnable != null) {
            mRingtoneHandler.removeCallbacks(mRingtoneStopRunnable);
        }

        RingtonePreviewKlaxon.stop();
        RingtonePreviewKlaxon.stopListeningToPreferences();

        mIsPreviewPlaying = false;
    }

}
