/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static android.content.Context.AUDIO_SERVICE;
import static android.media.AudioManager.STREAM_ALARM;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;

import com.best.deskclock.R;

public class AlarmVolumePreference extends SeekBarPreference {

    private AlarmSettingsActivity mAlarmSettingsActivity;
    private SeekBar mSeekbar;

    public AlarmVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (context instanceof AlarmSettingsActivity) {
            mAlarmSettingsActivity = (AlarmSettingsActivity) context;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final AudioManager audioManager = (AudioManager) holder.itemView.getContext().getSystemService(AUDIO_SERVICE);

        // Disable click feedback for this preference.
        holder.itemView.setClickable(false);

        // Hide the value of the seekbar located to the right of it to have a seekbar that fills the screen
        final TextView seekBarValue = (TextView) holder.findViewById(R.id.seekbar_value);
        seekBarValue.setVisibility(View.GONE);

        // Minimum volume for alarm is not 0, calculate it.
        int maxVolume = audioManager.getStreamMaxVolume(STREAM_ALARM) - getMinVolume(audioManager);
        mSeekbar = (SeekBar) holder.findViewById(R.id.seekbar);
        mSeekbar.setMax(maxVolume);
        mSeekbar.setProgress(audioManager.getStreamVolume(STREAM_ALARM) - getMinVolume(audioManager));

        final ContentObserver volumeObserver = new ContentObserver(mSeekbar.getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                // Volume was changed elsewhere, update our slider.
                mSeekbar.setProgress(audioManager.getStreamVolume(STREAM_ALARM) - getMinVolume(audioManager));
            }
        };

        mSeekbar.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                v.getContext().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI,
                        true, volumeObserver);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                v.getContext().getContentResolver().unregisterContentObserver(volumeObserver);
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int newVolume = progress + getMinVolume(audioManager);
                    audioManager.setStreamVolume(STREAM_ALARM, newVolume, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mAlarmSettingsActivity != null) {
                    // Start preview if not already running
                    mAlarmSettingsActivity.startRingtonePreview();
                }
            }
        });
    }

    private int getMinVolume(AudioManager audioManager) {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ? audioManager.getStreamMinVolume(STREAM_ALARM) : 0;
    }
}
