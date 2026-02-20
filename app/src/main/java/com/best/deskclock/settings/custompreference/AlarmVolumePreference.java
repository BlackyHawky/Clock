/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings.custompreference;

import static android.content.Context.AUDIO_SERVICE;
import static android.media.AudioManager.STREAM_ALARM;
import static android.view.View.GONE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.utils.RingtoneUtils.ALARM_PREVIEW_DURATION_MS;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.ringtone.RingtonePreviewKlaxon;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.slider.Slider;

import java.util.Locale;

public class AlarmVolumePreference extends Preference {

    private Context mContext;
    private SharedPreferences mPrefs;
    private Slider mSlider;
    private ImageView mSliderMinus;
    private ImageView mSliderPlus;
    private AudioManager mAudioManager;
    private int mMinVolume;
    private final Handler mRingtoneHandler = new Handler(Looper.getMainLooper());
    private Runnable mRingtoneStopRunnable;
    private boolean mIsPreviewPlaying = false;

    public AlarmVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        if (holder.itemView.isInEditMode()) {
            // Skip logic during Android Studio preview
            return;
        }

        mContext = getContext();
        mPrefs = getDefaultSharedPreferences(mContext);

        super.onBindViewHolder(holder);

        mAudioManager = (AudioManager) mContext.getSystemService(AUDIO_SERVICE);

        // Disable click feedback for this preference.
        holder.itemView.setClickable(false);

        // Minimum volume for alarm is not 0, calculate it.
        mMinVolume = RingtoneUtils.getAlarmMinVolume(mAudioManager);
        int maxVolume = mAudioManager.getStreamMaxVolume(STREAM_ALARM) - mMinVolume;
        mSlider = (Slider) holder.findViewById(R.id.slider);
        mSlider.setValueTo(maxVolume);
        mSlider.setValueFrom(0f);
        mSlider.setStepSize(1f);
        mSlider.setValue((float) mAudioManager.getStreamVolume(STREAM_ALARM) - mMinVolume);

        final TextView sliderSummary = (TextView) holder.findViewById(android.R.id.summary);
        updateSliderSummary(sliderSummary);

        mSliderMinus = (ImageView) holder.findViewById(R.id.slider_minus_icon);
        mSliderMinus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_volume_down));

        mSliderPlus = (ImageView) holder.findViewById(R.id.slider_plus_icon);
        mSliderPlus.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_volume_up));

        setupVolumeSliderButton(mSliderMinus, -1);
        setupVolumeSliderButton(mSliderPlus, +1);
        updateSliderButtonStates();

        final TextView resetSlider = (TextView) holder.findViewById(R.id.reset_slider_value);
        resetSlider.setVisibility(GONE);

        final ContentObserver volumeObserver = new ContentObserver(mSlider.getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                // Volume was changed elsewhere, update our slider.
                float currentVolume = (float) (mAudioManager.getStreamVolume(STREAM_ALARM) - mMinVolume);
                mSlider.setValue(currentVolume);
                updateSliderSummary(sliderSummary);
            }
        };

        mSlider.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                mContext.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI,
                        true, volumeObserver);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                mContext.getContentResolver().unregisterContentObserver(volumeObserver);
            }
        });

        mSlider.addOnChangeListener((slider, progress, fromUser) -> {
            if (fromUser) {
                int newVolume = (int) progress + mMinVolume;
                mAudioManager.setStreamVolume(STREAM_ALARM, newVolume, 0);
                updateSliderSummary(sliderSummary);
                updateSliderButtonStates();
            }
        });

        mSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                startRingtonePreview();
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
            }
        });
    }

    /**
     * Updates the summary text view to show the current alarm volume as a percentage.
     */
    private void updateSliderSummary(TextView sliderSummary) {
        int currentVolume = mAudioManager.getStreamVolume(STREAM_ALARM);
        int maxVolume = mAudioManager.getStreamMaxVolume(STREAM_ALARM);
        int volumePercentage = (int) (((float) currentVolume / maxVolume) * 100);

        String formattedText = String.format(Locale.getDefault(), "%d%%", volumePercentage);
        sliderSummary.post(() -> sliderSummary.setText(formattedText));
    }

    /**
     * Configures the minus or plus button to adjust the alarm volume when clicked.
     *
     * @param button the ImageView button (minus or plus)
     * @param delta  +1 to increase volume, -1 to decrease
     */
    private void setupVolumeSliderButton(@NonNull ImageView button, int delta) {
        button.setOnClickListener(v -> {
            int current = (int) mSlider.getValue();
            int max = (int) mSlider.getValueTo();

            int newValue = Math.min(Math.max(current + delta, 0), max);
            if (newValue != current) {
                mSlider.setValue(newValue);
                updateVolume(mAudioManager);
                startRingtonePreview();
                updateSliderButtonStates();
            }
        });
    }

    /**
     * Enables or disables the minus and plus buttons based on the current slider value
     * and the enabled state of the preference.
     */
    private void updateSliderButtonStates() {
        boolean isPrefEnabled = isEnabled();
        int progress = (int) mSlider.getValue();
        int max = (int) mSlider.getValueTo();

        ThemeUtils.updateSliderButtonEnabledState(mContext, mSliderMinus, isPrefEnabled
                && progress > 0
                && !RingtoneUtils.hasExternalAudioDeviceConnected(mContext, mPrefs));
        ThemeUtils.updateSliderButtonEnabledState(mContext, mSliderPlus, isPrefEnabled
                && progress < max
                && !RingtoneUtils.hasExternalAudioDeviceConnected(mContext, mPrefs));
    }

    private void updateVolume(AudioManager audioManager) {
        int newVolume = (int) mSlider.getValue() + mMinVolume;
        audioManager.setStreamVolume(STREAM_ALARM, newVolume, 0);
    }

    private void startRingtonePreview() {
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

        mRingtoneStopRunnable = this::stopRingtonePreview;
        // Stop the preview after 5 seconds
        mRingtoneHandler.postDelayed(mRingtoneStopRunnable, ALARM_PREVIEW_DURATION_MS);
    }

    public void stopRingtonePreview() {
        if (!mIsPreviewPlaying) {
            return;
        }

        if (mRingtoneStopRunnable != null) {
            mRingtoneHandler.removeCallbacks(mRingtoneStopRunnable);
        }

        RingtonePreviewKlaxon.stop(mContext, mPrefs);
        RingtonePreviewKlaxon.releaseResources();

        mIsPreviewPlaying = false;
    }

}
