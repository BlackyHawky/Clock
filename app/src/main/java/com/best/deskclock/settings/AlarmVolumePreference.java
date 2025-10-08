/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

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
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.ringtone.RingtonePreviewKlaxon;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;

import java.util.Locale;

public class AlarmVolumePreference extends SeekBarPreference {

    private Context mContext;
    private SharedPreferences mPrefs;
    private SeekBar mSeekbar;
    private ImageView mSeekBarMinus;
    private ImageView mSeekBarPlus;
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
        super.onBindViewHolder(holder);

        mContext = getContext();
        mPrefs = getDefaultSharedPreferences(mContext);

        mAudioManager = (AudioManager) mContext.getSystemService(AUDIO_SERVICE);

        // Disable click feedback for this preference.
        holder.itemView.setClickable(false);

        // Minimum volume for alarm is not 0, calculate it.
        mMinVolume = RingtoneUtils.getAlarmMinVolume(mAudioManager);
        int maxVolume = mAudioManager.getStreamMaxVolume(STREAM_ALARM) - mMinVolume;
        mSeekbar = (SeekBar) holder.findViewById(R.id.seekbar);
        mSeekbar.setMax(maxVolume);
        mSeekbar.setProgress(mAudioManager.getStreamVolume(STREAM_ALARM) - mMinVolume);

        final TextView seekBarSummary = (TextView) holder.findViewById(android.R.id.summary);
        updateSeekBarSummary(seekBarSummary);

        mSeekBarMinus = (ImageView) holder.findViewById(R.id.seekbar_minus_icon);
        mSeekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_volume_down));

        mSeekBarPlus = (ImageView) holder.findViewById(R.id.seekbar_plus_icon);
        mSeekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_volume_up));

        setupVolumeSeekBarButton(mSeekBarMinus, -1);
        setupVolumeSeekBarButton(mSeekBarPlus, +1);
        updateSeekBarButtonStates();

        final TextView resetSeekBar = (TextView) holder.findViewById(R.id.reset_seekbar_value);
        resetSeekBar.setVisibility(GONE);

        final ContentObserver volumeObserver = new ContentObserver(mSeekbar.getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                // Volume was changed elsewhere, update our slider.
                mSeekbar.setProgress(mAudioManager.getStreamVolume(STREAM_ALARM) - mMinVolume);
                updateSeekBarSummary(seekBarSummary);
            }
        };

        mSeekbar.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
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

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int newVolume = progress + mMinVolume;
                    mAudioManager.setStreamVolume(STREAM_ALARM, newVolume, 0);
                    updateSeekBarSummary(seekBarSummary);
                    updateSeekBarButtonStates();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                startRingtonePreview();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    /**
     * Updates the summary text view to show the current alarm volume as a percentage.
     */
    private void updateSeekBarSummary(TextView seekBarSummary) {
        int currentVolume = mAudioManager.getStreamVolume(STREAM_ALARM);
        int maxVolume = mAudioManager.getStreamMaxVolume(STREAM_ALARM);
        int volumePercentage = (int) (((float) currentVolume / maxVolume) * 100);

        String formattedText = String.format(Locale.getDefault(), "%d%%", volumePercentage);
        seekBarSummary.post(() -> seekBarSummary.setText(formattedText));
    }

    /**
     * Configures the minus or plus button to adjust the alarm volume when clicked.
     *
     * @param button the ImageView button (minus or plus)
     * @param delta  +1 to increase volume, -1 to decrease
     */
    private void setupVolumeSeekBarButton(@NonNull ImageView button, int delta) {
        button.setOnClickListener(v -> {
            int current = mSeekbar.getProgress();
            int max = mSeekbar.getMax();

            int newValue = Math.min(Math.max(current + delta, 0), max);
            if (newValue != current) {
                mSeekbar.setProgress(newValue);
                updateVolume(mAudioManager);
                startRingtonePreview();
                updateSeekBarButtonStates();
            }
        });
    }

    /**
     * Enables or disables the minus and plus buttons based on the current SeekBar value
     * and the enabled state of the preference.
     */
    private void updateSeekBarButtonStates() {
        boolean isPrefEnabled = isEnabled();
        int progress = mSeekbar.getProgress();
        int max = mSeekbar.getMax();

        ThemeUtils.updateSeekBarButtonEnabledState(mContext, mSeekBarMinus, isPrefEnabled
                && progress > 0
                && !RingtoneUtils.hasBluetoothDeviceConnected(mContext, mPrefs));
        ThemeUtils.updateSeekBarButtonEnabledState(mContext, mSeekBarPlus, isPrefEnabled
                && progress < max
                && !RingtoneUtils.hasBluetoothDeviceConnected(mContext, mPrefs));
    }

    private void updateVolume(AudioManager audioManager) {
        int newVolume = mSeekbar.getProgress() + mMinVolume;
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
