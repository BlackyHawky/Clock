// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings.custompreference;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ANALOG_CLOCK_SIZE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SHADOW_OFFSET;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_TITLE_FONT_SIZE_PREF;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_EXTERNAL_AUDIO_DEVICE_VOLUME;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_MATERIAL_YOU_WIDGET_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SCREENSAVER_BRIGHTNESS;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_TIMER_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_WIDGETS_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_WIDGET_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_ANALOG_CLOCK_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SHADOW_OFFSET;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_TITLE_FONT_SIZE_PREF;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_CLOCK_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_EXTERNAL_AUDIO_DEVICE_VOLUME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_ANALOG_CLOCK_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_BRIGHTNESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_BLUR_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHADOW_OFFSET;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SHAKE_INTENSITY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_WIDGET_BACKGROUND_CORNER_RADIUS;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.TextViewCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
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

    private Context mContext;
    private SharedPreferences mPrefs;
    private Slider mSlider;
    private ImageView mSliderMinus;
    private ImageView mSliderPlus;
    private TextView mResetSlider;
    private final Handler mRingtoneHandler = new Handler(Looper.getMainLooper());
    private Runnable mRingtoneStopRunnable;
    private boolean mIsPreviewPlaying = false;

    public CustomSliderPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
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

        mContext = getContext();
        mPrefs = getDefaultSharedPreferences(mContext);
        String fontPath = SettingsDAO.getGeneralFont(mPrefs);

        holder.itemView.setClickable(false);

        mSlider = (Slider) holder.findViewById(R.id.slider);
        configureSliderBounds();
        mSlider.setStepSize(1f);

        final TextView sliderSummary = (TextView) holder.findViewById(android.R.id.summary);
        setSliderProgress(sliderSummary);

        mSliderMinus = (ImageView) holder.findViewById(R.id.slider_minus_icon);
        mSliderPlus = (ImageView) holder.findViewById(R.id.slider_plus_icon);
        mResetSlider = (TextView) holder.findViewById(R.id.reset_slider_value);

        configureSliderButtonDrawables();
        setupSliderButton(mSliderMinus, isExternalAudioDeviceVolumePreference() ? -10 : -5, sliderSummary);
        setupSliderButton(mSliderPlus, isExternalAudioDeviceVolumePreference() ? 10 : 5, sliderSummary);
        updateSliderButtonStates();
        updateResetButtonStates();

        mResetSlider.setTypeface(ThemeUtils.boldTypeface(fontPath));
        mResetSlider.setOnClickListener(v -> {
            resetPreference();
            setSliderProgress(sliderSummary);
            startRingtonePreviewForExternalAudioDevices();
            updateDigitalWidgets();
            updateSliderButtonStates();
            updateResetButtonStates();
        });

        mSlider.addOnChangeListener((slider, progress, fromUser) -> {
            if (fromUser) {
                updateSliderSummary(sliderSummary, (int) progress);
                updateSliderButtonStates();
                updateResetButtonStates();
            }
        });

        mSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
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
     * Sets the minimum and maximum value of the slider based on the preference type.
     */
    private void configureSliderBounds() {
        if (isScreensaverBrightnessPreference()) {
            mSlider.setValueTo(MAX_BRIGHTNESS_VALUE);
            mSlider.setValueFrom(MIN_BRIGHTNESS_VALUE);
        } else if (isDigitalWidgetBackgroundCornerRadius()
                || isNextAlarmWidgetBackgroundCornerRadius()
                || isVerticalWidgetBackgroundCornerRadius()
                || isMaterialYouDigitalWidgetBackgroundCornerRadius()
                || isMaterialYouNextAlarmWidgetBackgroundCornerRadius()
                || isMaterialYouVerticalWidgetBackgroundCornerRadius()) {
            mSlider.setValueTo(MAX_CORNER_RADIUS_VALUE);
            mSlider.setValueFrom(MIN_CORNER_RADIUS_VALUE);
        } else if (isShakeIntensityPreference()) {
            mSlider.setValueTo(MAX_SHAKE_INTENSITY_VALUE);
            mSlider.setValueFrom(MIN_SHAKE_INTENSITY_VALUE);
        } else if (isTimerShakeIntensityPreference()) {
            mSlider.setValueTo(MAX_TIMER_SHAKE_INTENSITY_VALUE);
            mSlider.setValueFrom(MIN_TIMER_SHAKE_INTENSITY_VALUE);
        } else if (isTimerShadowOffsetPreference() || isAlarmShadowOffsetPreference()) {
            mSlider.setValueTo(MAX_SHADOW_OFFSET_VALUE);
            mSlider.setValueFrom(MIN_SHADOW_OFFSET_VALUE);
        } else if (isScreensaverBlurIntensityPreference()
                || isTimerBlurIntensityPreference()
                || isAlarmBlurIntensityPreference()) {
            mSlider.setValueTo(MAX_BLUR_INTENSITY_VALUE);
            mSlider.setValueFrom(MIN_BLUR_INTENSITY_VALUE);
        } else if (isExternalAudioDeviceVolumePreference()) {
            mSlider.setValueTo(MAX_EXTERNAL_AUDIO_DEVICE_VOLUME);
            mSlider.setValueFrom(MIN_EXTERNAL_AUDIO_DEVICE_VOLUME);
        } else if (isAnalogClockSizePreference()
                || isScreensaverAnalogClockSizePreference()
                || isAlarmAnalogClockSizePreference()) {
            mSlider.setValueTo(MAX_ANALOG_CLOCK_SIZE_VALUE);
            mSlider.setValueFrom(MIN_ANALOG_CLOCK_SIZE_VALUE);
        } else {
            mSlider.setValueTo(MAX_FONT_SIZE_VALUE);
            mSlider.setValueFrom(MIN_FONT_SIZE_VALUE);
        }
    }

    /**
     * Sets the slider value base on the current preference (screensaver brightness, widget font size, etc.)
     * and updates the slider summary to reflect this value.
     */
    private void setSliderProgress(TextView sliderSummary) {
        int currentProgress = mPrefs.getInt(getKey(), getDefaultSliderValue());

        updateSliderSummary(sliderSummary, currentProgress);
        mSlider.setValue(currentProgress);
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
            mSliderMinus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_brightness_decrease));
            mSliderPlus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_brightness_increase));
        } else if (isDigitalWidgetBackgroundCornerRadius()
                || isNextAlarmWidgetBackgroundCornerRadius()
                || isVerticalWidgetBackgroundCornerRadius()
                || isMaterialYouDigitalWidgetBackgroundCornerRadius()
                || isMaterialYouNextAlarmWidgetBackgroundCornerRadius()
                || isMaterialYouVerticalWidgetBackgroundCornerRadius()) {
            mSliderMinus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_rounded_corner_decrease));
            mSliderPlus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_rounded_corner_increase));
        } else if (isShakeIntensityPreference() || isTimerShakeIntensityPreference()) {
            mSliderMinus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_sensor_low));
            mSliderPlus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_sensor_high));
        } else if (isTimerShadowOffsetPreference() || isAlarmShadowOffsetPreference()) {
            mSliderMinus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_shadow_decrease));
            mSliderPlus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_shadow_increase));
        } else if (isScreensaverBlurIntensityPreference()
                || isTimerBlurIntensityPreference()
                || isAlarmBlurIntensityPreference()) {
            mSliderMinus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_blur_decrease));
            mSliderPlus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_blur_increase));
        } else if (isExternalAudioDeviceVolumePreference()) {
            mSliderMinus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_volume_down));
            mSliderPlus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_volume_up));
        } else if (isAnalogClockSizePreference()
                || isScreensaverAnalogClockSizePreference()
                || isAlarmAnalogClockSizePreference()) {
            mSliderMinus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_zoom_in));
            mSliderPlus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_zoom_out));
        } else {
            mSliderMinus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_text_decrease));
            mSliderPlus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_text_increase));
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
            mSlider.setValue(newSliderValue);
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
        int current = (int) mSlider.getValue();
        int min = (int) mSlider.getValueFrom();
        int max = (int) mSlider.getValueTo();

        ThemeUtils.updateSliderButtonEnabledState(mContext, mSliderMinus, isPrefEnabled && current > min);
        ThemeUtils.updateSliderButtonEnabledState(mContext, mSliderPlus, isPrefEnabled && current < max);
    }

    /**
     * Updates the enabled state of the reset button based on the current slider value
     * and the enabled state of the preference.
     *
     * <p>The reset button is enabled only when the slider value differs from the default
     * value for the current preference type.</p>
     */
    private void updateResetButtonStates() {
        boolean isEnabled = isEnabled() && !isSliderAtDefault((int) mSlider.getValue());
        mResetSlider.setEnabled(isEnabled);

        if (isEnabled) {
            int enabledColor = MaterialColors.getColor(mContext, androidx.appcompat.R.attr.colorPrimary, Color.BLACK);

            mResetSlider.setTextColor(enabledColor);
            TextViewCompat.setCompoundDrawableTintList(mResetSlider, ColorStateList.valueOf(enabledColor));
        } else {
            int disabledColor = mContext.getColor(R.color.colorDisabled);
            mResetSlider.setTextColor(disabledColor);
            TextViewCompat.setCompoundDrawableTintList(mResetSlider, ColorStateList.valueOf(disabledColor));
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
        int currentSliderValue = (int) mSlider.getValue();

        return (int) Math.min(Math.max(currentSliderValue + delta, mSlider.getValueFrom()), mSlider.getValueTo());
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
        } else if (isMaterialYouDigitalWidgetBackgroundCornerRadius()
                || isMaterialYouNextAlarmWidgetBackgroundCornerRadius()
                || isMaterialYouVerticalWidgetBackgroundCornerRadius()) {
            return DEFAULT_MATERIAL_YOU_WIDGET_BACKGROUND_CORNER_RADIUS;
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

            WidgetUtils.updateAllDigitalWidgets(mContext);
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
     * @return {@code true} if the current preference is related to corner radius of
     * the digital widget background. {@code false} otherwise.
     */
    private boolean isDigitalWidgetBackgroundCornerRadius() {
        return getKey().equals(KEY_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if the current preference is related to corner radius of
     * the Next alarm widget background. {@code false} otherwise.
     */
    private boolean isNextAlarmWidgetBackgroundCornerRadius() {
        return getKey().equals(KEY_NEXT_ALARM_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if the current preference is related to corner radius of
     * the vertical widget background. {@code false} otherwise.
     */
    private boolean isVerticalWidgetBackgroundCornerRadius() {
        return getKey().equals(KEY_VERTICAL_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if the current preference is related to corner radius of
     * the Material You digital widget background. {@code false} otherwise.
     */
    private boolean isMaterialYouDigitalWidgetBackgroundCornerRadius() {
        return getKey().equals(KEY_MATERIAL_YOU_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if the current preference is related to corner radius of
     * the Material You Next alarm widget background. {@code false} otherwise.
     */
    private boolean isMaterialYouNextAlarmWidgetBackgroundCornerRadius() {
        return getKey().equals(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_BACKGROUND_CORNER_RADIUS);
    }

    /**
     * @return {@code true} if the current preference is related to corner radius of
     * the Material You vertical digital widget background. {@code false} otherwise.
     */
    private boolean isMaterialYouVerticalWidgetBackgroundCornerRadius() {
        return getKey().equals(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS);
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
        if (!isExternalAudioDeviceVolumePreference()
                || !RingtoneUtils.hasExternalAudioDeviceConnected(mContext, mPrefs)) {
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
