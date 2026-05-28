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
import com.best.deskclock.databinding.SettingsPreferenceSliderLayoutBinding;
import com.best.deskclock.ringtone.RingtonePreviewKlaxon;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.slider.Slider;

import java.util.Locale;

public class AlarmVolumePreference extends Preference {

    private SettingsPreferenceSliderLayoutBinding mBinding;

    private final SharedPreferences mPrefs;

    private final AudioManager mAudioManager;
    private final int mMinVolume;
    private final int mMaxVolume;
    private final Handler mRingtoneHandler = new Handler(Looper.getMainLooper());
    private Runnable mRingtoneStopRunnable;
    private boolean mIsPreviewPlaying = false;

    public AlarmVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPrefs = getDefaultSharedPreferences(context);
        mAudioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);

        // Minimum volume for alarm is not 0, calculate it.
        mMinVolume = RingtoneUtils.getAlarmMinVolume(mAudioManager);
        mMaxVolume = mAudioManager.getStreamMaxVolume(STREAM_ALARM) - mMinVolume;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        if (holder.itemView.isInEditMode()) {
            // Skip logic during Android Studio preview
            return;
        }

        super.onBindViewHolder(holder);

        mBinding = SettingsPreferenceSliderLayoutBinding.bind(holder.itemView);

        // Disable click feedback for this preference.
        holder.itemView.setClickable(false);

        mBinding.slider.setValueTo(mMaxVolume);
        mBinding.slider.setValueFrom(0f);
        mBinding.slider.setStepSize(1f);
        mBinding.slider.setValue((float) mAudioManager.getStreamVolume(STREAM_ALARM) - mMinVolume);

        updateSliderSummary(mBinding.summary);

        mBinding.sliderMinusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_volume_down));

        mBinding.sliderPlusIcon.setImageDrawable(AppCompatResources.getDrawable(getContext(), R.drawable.ic_volume_up));

        setupVolumeSliderButton(mBinding.sliderMinusIcon, -1);
        setupVolumeSliderButton(mBinding.sliderPlusIcon, +1);
        updateSliderButtonStates();

        mBinding.resetSliderValue.setVisibility(GONE);

        final ContentObserver volumeObserver = new ContentObserver(mBinding.slider.getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                // Volume was changed elsewhere, update our slider.
                float currentVolume = (float) (mAudioManager.getStreamVolume(STREAM_ALARM) - mMinVolume);
                mBinding.slider.setValue(currentVolume);
                updateSliderSummary(mBinding.summary);
            }
        };

        mBinding.slider.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                getContext().getContentResolver().unregisterContentObserver(volumeObserver);
            }
        });

        mBinding.slider.addOnChangeListener((slider, progress, fromUser) -> {
            if (fromUser) {
                int newVolume = (int) progress + mMinVolume;
                mAudioManager.setStreamVolume(STREAM_ALARM, newVolume, 0);
                updateSliderSummary(mBinding.summary);
                updateSliderButtonStates();
            }
        });

        mBinding.slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
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
            int current = (int) mBinding.slider.getValue();
            int max = (int) mBinding.slider.getValueTo();

            int newValue = Math.min(Math.max(current + delta, 0), max);
            if (newValue != current) {
                mBinding.slider.setValue(newValue);
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
        int progress = (int) mBinding.slider.getValue();
        int max = (int) mBinding.slider.getValueTo();

        ThemeUtils.updateSliderButtonEnabledState(getContext(), mBinding.sliderMinusIcon, isPrefEnabled
            && progress > 0
            && !RingtoneUtils.hasExternalAudioDeviceConnected(getContext(), mPrefs));
        ThemeUtils.updateSliderButtonEnabledState(getContext(), mBinding.sliderPlusIcon, isPrefEnabled
            && progress < max
            && !RingtoneUtils.hasExternalAudioDeviceConnected(getContext(), mPrefs));
    }

    private void updateVolume(AudioManager audioManager) {
        int newVolume = (int) mBinding.slider.getValue() + mMinVolume;
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

        RingtonePreviewKlaxon.start(ringtoneUri);
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

        RingtonePreviewKlaxon.stop();
        RingtonePreviewKlaxon.releaseResources();

        mIsPreviewPlaying = false;
    }

}
