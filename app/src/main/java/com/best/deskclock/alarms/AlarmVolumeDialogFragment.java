// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import static com.best.deskclock.utils.RingtoneUtils.ALARM_PREVIEW_DURATION_MS;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.best.deskclock.R;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.ringtone.RingtonePreviewKlaxon;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

/**
 * DialogFragment to set the volume for alarms.
 */
public class AlarmVolumeDialogFragment  extends DialogFragment {

    /**
     * The tag that identifies instances of AlarmVolumeDialogFragment in the fragment manager.
     */
    private static final String TAG = "set_alarm_volume_dialog";

    private static final String ARG_ALARM_VOLUME_VALUE = "arg_alarm_volume_value";
    private static final String ARG_ALARM = "arg_alarm";
    private static final String ARG_TAG = "arg_tag";

    private Context mContext;
    private AudioManager mAudioManager;
    private Alarm mAlarm;
    private String mTag;
    private SeekBar mSeekBar;
    private ImageView mVolumeMinus;
    private ImageView mVolumePlus;
    private TextView mVolumeValue;
    private final Handler mRingtoneHandler = new Handler(Looper.getMainLooper());
    private Runnable mRingtoneStopRunnable;
    private int mMinVolume;
    private int mPreviousVolume = -1;
    private boolean mIsPreviewPlaying = false;

    /**
     * Creates a new instance of {@link AlarmVolumeDialogFragment} for use
     * in the expanded alarm view, where the volume value is configured for a specific alarm.
     *
     * @param alarm             The alarm instance being edited.
     * @param alarmVolumeValue  The volume value in step.
     * @param tag               A tag identifying the fragment in the fragment manager.
     */
    public static AlarmVolumeDialogFragment newInstance(Alarm alarm, int alarmVolumeValue, String tag) {
        final Bundle args = new Bundle();
        args.putParcelable(ARG_ALARM, alarm);
        args.putString(ARG_TAG, tag);
        args.putInt(ARG_ALARM_VOLUME_VALUE, alarmVolumeValue);

        final AlarmVolumeDialogFragment fragment = new AlarmVolumeDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Replaces any existing AlarmVolumeDialogFragment with the given {@code fragment}.
     */
    public static void show(FragmentManager manager, AlarmVolumeDialogFragment fragment) {
        if (manager == null || manager.isDestroyed()) {
            return;
        }

        // Finish any outstanding fragment work.
        manager.executePendingTransactions();

        final FragmentTransaction tx = manager.beginTransaction();

        // Remove existing instance of this DialogFragment if necessary.
        final Fragment existing = manager.findFragmentByTag(TAG);
        if (existing != null) {
            tx.remove(existing);
        }
        tx.addToBackStack(null);

        fragment.show(tx, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mVolumeValue != null) {
            outState.putInt(ARG_ALARM_VOLUME_VALUE, mSeekBar.getProgress() + mMinVolume);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mContext = requireContext();

        final Bundle args = requireArguments();
        mAlarm = SdkUtils.isAtLeastAndroid13()
                ? args.getParcelable(ARG_ALARM, Alarm.class)
                : args.getParcelable(ARG_ALARM);
        mTag = args.getString(ARG_TAG);

        int volumeValue = args.getInt(ARG_ALARM_VOLUME_VALUE, 0);

        if (savedInstanceState != null) {
            volumeValue = savedInstanceState.getInt(ARG_ALARM_VOLUME_VALUE, volumeValue);
        }

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        mMinVolume = RingtoneUtils.getAlarmMinVolume(mAudioManager);
        int clampedVolume = Math.min(volumeValue, maxVolume);
        int currentVolume = clampedVolume - mMinVolume;

        View view = getLayoutInflater().inflate(R.layout.alarm_volume_dialog, null);

        mSeekBar = view.findViewById(R.id.alarm_volume_seekbar);
        mVolumeValue = view.findViewById(R.id.alarm_volume_value);
        mVolumeMinus = view.findViewById(R.id.volume_minus_icon);
        mVolumePlus = view.findViewById(R.id.volume_plus_icon);

        mSeekBar.setMax(maxVolume - mMinVolume);
        mSeekBar.setProgress(currentVolume);

        updateVolumeText(clampedVolume, maxVolume);
        updateVolumeButtonStates(currentVolume, maxVolume - mMinVolume);

        mVolumeMinus.setOnClickListener(v -> {
            int progress = mSeekBar.getProgress();
            if (progress > 0) {
                mSeekBar.setProgress(progress - 1);
                int newVolume = mSeekBar.getProgress() + mMinVolume;
                updateVolumeText(newVolume, maxVolume);
                updateVolumeButtonStates(mSeekBar.getProgress(), maxVolume - mMinVolume);
                startRingtonePreview(mAlarm, newVolume);
            }
        });

        mVolumePlus.setOnClickListener(v -> {
            int progress = mSeekBar.getProgress();
            if (progress < mSeekBar.getMax()) {
                mSeekBar.setProgress(progress + 1);
                int newVolume = mSeekBar.getProgress() + mMinVolume;
                updateVolumeText(newVolume, maxVolume);
                updateVolumeButtonStates(mSeekBar.getProgress(), maxVolume - mMinVolume);
                startRingtonePreview(mAlarm, newVolume);
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int newVolume = progress + mMinVolume;
                    updateVolumeText(newVolume, maxVolume);
                    updateVolumeButtonStates(progress, maxVolume - mMinVolume);
                    mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                int newVolume = seekBar.getProgress() + mMinVolume;
                startRingtonePreview(mAlarm, newVolume);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.alarm_volume_title)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    stopRingtonePreview();
                    setVolumeValue();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) ->
                        stopRingtonePreview()
                );

        return dialogBuilder.create();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        stopRingtonePreview();
    }

    @Override
    public void onStop() {
        super.onStop();

        stopRingtonePreview();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        stopRingtonePreview();
    }

    /**
     * Set the alarm volume.
     */
    private void setVolumeValue() {
        if (mAlarm != null) {
            int volumeValue = mSeekBar.getProgress() + mMinVolume;
            ((VolumeValueDialogHandler) requireActivity()).onVolumeValueSet(mAlarm, volumeValue, mTag);
        }
    }

    /**
     * Updates the text view displaying the current alarm volume as a percentage.
     *
     * @param currentVolume The current volume value (in steps).
     * @param maxVolume     The maximum possible volume (in steps).
     */
    private void updateVolumeText(int currentVolume, int maxVolume) {
        int percent = (int) (((float) currentVolume / maxVolume) * 100);
        mVolumeValue.setText(String.format(Locale.getDefault(), "%d%%", percent));
    }

    /**
     * Enables or disables the volume plus/minus buttons based on the current SeekBar progress.
     *
     * @param progress     The current progress of the SeekBar (volume level in steps).
     * @param maxProgress  The maximum progress of the SeekBar.
     */
    private void updateVolumeButtonStates(int progress, int maxProgress) {
        ThemeUtils.updateSeekBarButtonEnabledState(mContext, mVolumeMinus, progress > 0);
        ThemeUtils.updateSeekBarButtonEnabledState(mContext, mVolumePlus, progress < maxProgress);
    }

    /**
     * Starts a preview of the alarm ringtone at the given volume level.
     * Temporarily sets the alarm stream volume and restores it after a short delay.
     *
     * @param alarm      The alarm containing the ringtone to play.
     * @param newVolume  The volume level (in steps) to apply during the preview.
     */
    public void startRingtonePreview(Alarm alarm, int newVolume) {
        if (mRingtoneStopRunnable != null) {
            mRingtoneHandler.removeCallbacks(mRingtoneStopRunnable);
        }

        // Save the current system volume
        if (mPreviousVolume == -1) {
            mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        }

        // Temporarily apply the new volume
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0);

        Uri ringtoneUri = alarm.alert;
        if (RingtoneUtils.isRandomRingtone(ringtoneUri)) {
            ringtoneUri = RingtoneUtils.getRandomRingtoneUri();
        } else if (RingtoneUtils.isRandomCustomRingtone(ringtoneUri)) {
            ringtoneUri = RingtoneUtils.getRandomCustomRingtoneUri();
        }

        // Start ringtone with volume applied
        RingtonePreviewKlaxon.startPreviewOnlyFromSpeakers(mContext, ringtoneUri);

        mIsPreviewPlaying = true;

        mRingtoneStopRunnable = this::stopRingtonePreview;

        // Schedule volume shutdown and restore after 5 seconds
        mRingtoneHandler.postDelayed(mRingtoneStopRunnable, ALARM_PREVIEW_DURATION_MS);
    }

    /**
     * Stops the ringtone preview and restores the original alarm volume level.
     */
    public void stopRingtonePreview() {
        if (!mIsPreviewPlaying) {
            return;
        }

        if (mRingtoneStopRunnable != null) {
            mRingtoneHandler.removeCallbacks(mRingtoneStopRunnable);
        }

        RingtonePreviewKlaxon.stopPreviewFromSpeakers(mContext);

        RingtonePreviewKlaxon.releaseResources();

        // Restore the system volume
        if (mPreviousVolume != -1) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mPreviousVolume, 0);
            mPreviousVolume = -1;
        }

        mIsPreviewPlaying = false;
    }

    public interface VolumeValueDialogHandler {
        void onVolumeValueSet(Alarm alarm, int volumeValue, String tag);
    }
}
