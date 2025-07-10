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
import static com.best.deskclock.settings.PreferencesKeys.KEY_RINGTONE_PREVIEW_PLAYING;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
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

import java.util.Locale;

public class AlarmVolumePreference extends SeekBarPreference {

    private Context mContext;
    private SharedPreferences mPrefs;
    private SeekBar mSeekbar;
    private int mMinVolume;
    private boolean mPreviewPlaying = false;

    public AlarmVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mContext = getContext();
        mPrefs = getDefaultSharedPreferences(mContext);

        final AudioManager audioManager = (AudioManager) mContext.getSystemService(AUDIO_SERVICE);

        // Disable click feedback for this preference.
        holder.itemView.setClickable(false);

        // Minimum volume for alarm is not 0, calculate it.
        mMinVolume = RingtoneUtils.getAlarmMinVolume(audioManager);
        int maxVolume = audioManager.getStreamMaxVolume(STREAM_ALARM) - mMinVolume;
        mSeekbar = (SeekBar) holder.findViewById(R.id.seekbar);
        mSeekbar.setMax(maxVolume);
        mSeekbar.setProgress(audioManager.getStreamVolume(STREAM_ALARM) - mMinVolume);

        final TextView seekBarSummary = (TextView) holder.findViewById(android.R.id.summary);
        updateSeekBarSummary(audioManager, seekBarSummary);

        final ImageView seekBarMinus = (ImageView) holder.findViewById(R.id.seekbar_minus_icon);
        seekBarMinus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_volume_down));
        seekBarMinus.setOnClickListener(v -> {
            int currentProgress = mSeekbar.getProgress();
            if (currentProgress > 0) {
                mSeekbar.setProgress(currentProgress - 1);
                updateVolume(audioManager);
                startRingtonePreview();
            }
        });

        final ImageView seekBarPlus = (ImageView) holder.findViewById(R.id.seekbar_plus_icon);
        seekBarPlus.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_volume_up));
        seekBarPlus.setOnClickListener(v -> {
            int currentProgress = mSeekbar.getProgress();
            if (currentProgress < mSeekbar.getMax()) {
                mSeekbar.setProgress(currentProgress + 1);
                updateVolume(audioManager);
                startRingtonePreview();
            }
        });

        final TextView resetSeekBar = (TextView) holder.findViewById(R.id.reset_seekbar_value);
        resetSeekBar.setVisibility(GONE);

        final ContentObserver volumeObserver = new ContentObserver(mSeekbar.getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                // Volume was changed elsewhere, update our slider.
                mSeekbar.setProgress(audioManager.getStreamVolume(STREAM_ALARM) - mMinVolume);
                updateSeekBarSummary(audioManager, seekBarSummary);
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
                    audioManager.setStreamVolume(STREAM_ALARM, newVolume, 0);
                    updateSeekBarSummary(audioManager, seekBarSummary);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                startRingtonePreview();
            }
        });
    }

    private void updateSeekBarSummary(AudioManager audioManager, TextView seekBarSummary) {
        int currentVolume = audioManager.getStreamVolume(STREAM_ALARM);
        int maxVolume = audioManager.getStreamMaxVolume(STREAM_ALARM);
        int volumePercentage = (int) (((float) currentVolume / maxVolume) * 100);

        String formattedText = String.format(Locale.getDefault(), "%d%%", volumePercentage);
        seekBarSummary.post(() -> seekBarSummary.setText(formattedText));
    }

    private void updateVolume(AudioManager audioManager) {
        int newVolume = mSeekbar.getProgress() + mMinVolume;
        audioManager.setStreamVolume(STREAM_ALARM, newVolume, 0);
    }

    public void startRingtonePreview() {
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
            mSeekbar.postDelayed(() -> {
                stopRingtonePreview(mContext, mPrefs);
                mPreviewPlaying = false;
            }, 5000);
        }
    }

    public void stopRingtonePreview(Context context, SharedPreferences prefs) {
        if (mPreviewPlaying) {
            RingtonePreviewKlaxon.stop(context, prefs);
            mPrefs.edit().putBoolean(KEY_RINGTONE_PREVIEW_PLAYING, false).apply();
        }
    }

    public void releaseResources() {
        RingtonePreviewKlaxon.releaseResources();
    }
}
