// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.utils.RingtoneUtils.ALARM_PREVIEW_DURATION_MS;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.AlarmVolumeDialogBinding;
import com.best.deskclock.ringtone.RingtonePreviewKlaxon;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.slider.Slider;

import java.util.Locale;

/**
 * DialogFragment to set the volume for alarms.
 */
public class AlarmVolumeDialogFragment extends DialogFragment {

    /**
     * The tag that identifies instances of AlarmVolumeDialogFragment in the fragment manager.
     */
    public static final String TAG = "set_alarm_volume_dialog";

    public static final String REQUEST_KEY = "volume_request_key";
    public static final String RESULT_VOLUME_VALUE = "result_volume_value";
    private static final String ARG_ALARM_VOLUME_VALUE = "arg_alarm_volume_value";
    private static final String ARG_RINGTONE_URI = "arg_ringtone_uri";

    private AlarmVolumeDialogBinding mBinding;
    private AudioManager mAudioManager;
    private Uri mRingtoneUri;
    private TextView mDialogTitle;
    private final Handler mRingtoneHandler = new Handler(Looper.getMainLooper());
    private Runnable mRingtoneStopRunnable;
    private int mMinVolume;
    private int mPreviousVolume = -1;
    private boolean mIsPreviewPlaying = false;

    /**
     * Creates a new instance of {@link AlarmVolumeDialogFragment} for use
     * in the alarm editing panel, where the volume value is configured for a specific alarm.
     *
     * @param alarmVolumeValue The volume value in step.
     */
    public static AlarmVolumeDialogFragment newInstance(int alarmVolumeValue, Uri ringtoneUri) {
        final Bundle args = new Bundle();
        args.putInt(ARG_ALARM_VOLUME_VALUE, alarmVolumeValue);

        if (ringtoneUri != null) {
            args.putString(ARG_RINGTONE_URI, ringtoneUri.toString());
        }

        final AlarmVolumeDialogFragment fragment = new AlarmVolumeDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Displays {@link AlarmVolumeDialogFragment}.
     */
    public static void show(FragmentManager manager, AlarmVolumeDialogFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(ARG_ALARM_VOLUME_VALUE, (int) mBinding.alarmVolumeSlider.getValue() + mMinVolume);

        if (mRingtoneUri != null) {
            outState.putString(ARG_RINGTONE_URI, mRingtoneUri.toString());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SharedPreferences prefs = getDefaultSharedPreferences(requireContext());
        Typeface typeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        final Bundle args = requireArguments();
        String uriString = args.getString(ARG_RINGTONE_URI);
        int volumeValue = args.getInt(ARG_ALARM_VOLUME_VALUE, 0);

        if (savedInstanceState != null) {
            uriString = savedInstanceState.getString(ARG_RINGTONE_URI, uriString);
            volumeValue = savedInstanceState.getInt(ARG_ALARM_VOLUME_VALUE, volumeValue);
        }

        if (uriString != null) {
            mRingtoneUri = Uri.parse(uriString);
        } else {
            mRingtoneUri = RingtoneUtils.getFallbackRingtoneUri(requireContext());
        }

        mAudioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);

        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        mMinVolume = RingtoneUtils.getAlarmMinVolume(mAudioManager);
        int clampedVolume = Math.max(mMinVolume, Math.min(volumeValue, maxVolume));
        int currentVolume = clampedVolume - mMinVolume;

        mBinding = AlarmVolumeDialogBinding.inflate(getLayoutInflater());

        mBinding.alarmVolumeValue.setTypeface(typeface);

        mBinding.alarmVolumeWarning.setTypeface(typeface, Typeface.ITALIC);

        float maxRange = Math.max(1f, (float) (maxVolume - mMinVolume));
        mBinding.alarmVolumeSlider.setValueTo(maxRange);
        mBinding.alarmVolumeSlider.setValueFrom(0f);
        mBinding.alarmVolumeSlider.setStepSize(1f);
        mBinding.alarmVolumeSlider.setValue((float) currentVolume);

        updateVolumeText(clampedVolume, maxVolume);
        updateVolumeButtonStates(currentVolume, maxVolume - mMinVolume);
        updateWarningVisibility(mBinding.alarmVolumeWarning, currentVolume);

        mBinding.sliderMinusIcon.setOnClickListener(v -> {
            float newValue = mBinding.alarmVolumeSlider.getValue() - 1f;
            if (newValue >= mBinding.alarmVolumeSlider.getValueFrom()) {
                mBinding.alarmVolumeSlider.setValue(newValue);

                int newVolume = (int) newValue + mMinVolume;
                startRingtonePreview(newVolume);
            }
        });

        mBinding.sliderPlusIcon.setOnClickListener(v -> {
            float newValue = mBinding.alarmVolumeSlider.getValue() + 1f;
            if (newValue <= mBinding.alarmVolumeSlider.getValueTo()) {
                mBinding.alarmVolumeSlider.setValue(newValue);

                int newVolume = (int) newValue + mMinVolume;
                startRingtonePreview(newVolume);
            }
        });

        mBinding.alarmVolumeSlider.addOnChangeListener((slider, progress, fromUser) -> {
            int intProgress = (int) progress;
            int newVolume = intProgress + mMinVolume;

            updateDialogIcon(newVolume, maxVolume);
            updateVolumeText(newVolume, maxVolume);
            updateVolumeButtonStates(intProgress, (int) slider.getValueTo());
            updateWarningVisibility(mBinding.alarmVolumeWarning, intProgress);

            if (fromUser) {
                if (mIsPreviewPlaying) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0);
                }
            }
        });

        mBinding.alarmVolumeSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                int newVolume = (int) slider.getValue() + mMinVolume;
                startRingtonePreview(newVolume);
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
            }
        });

        return CustomDialog.create(
            requireContext(),
            null,
            null,
            getString(R.string.alarm_volume_title),
            null,
            mBinding.getRoot(),
            getString(android.R.string.ok),
            (d, w) -> {
                stopRingtonePreview();
                setVolumeValue();
            },
            getString(android.R.string.cancel),
            (d, w) -> stopRingtonePreview(),
            null,
            null,
            alertDialog -> {
                mDialogTitle = alertDialog.findViewById(R.id.dialog_title);
                int volume = (int) mBinding.alarmVolumeSlider.getValue() + mMinVolume;
                updateDialogIcon(volume, maxVolume);
            },
            CustomDialog.SoftInputMode.NONE
        );
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
        stopRingtonePreview();

        mRingtoneHandler.removeCallbacksAndMessages(null);

        mBinding.alarmVolumeSlider.clearOnChangeListeners();
        mBinding.alarmVolumeSlider.clearOnSliderTouchListeners();

        mBinding.sliderMinusIcon.setOnClickListener(null);
        mBinding.sliderPlusIcon.setOnClickListener(null);

        super.onDestroyView();

        mAudioManager = null;

        mBinding = null;

        mDialogTitle = null;
        mRingtoneStopRunnable = null;
        mRingtoneUri = null;
    }

    /**
     * Set the alarm volume.
     */
    private void setVolumeValue() {
        Bundle result = new Bundle();
        int volumeValue = (int) mBinding.alarmVolumeSlider.getValue() + mMinVolume;
        result.putInt(RESULT_VOLUME_VALUE, volumeValue);

        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    /**
     * Updates the dialog icon based on the set volume.
     *
     * @param currentVolume The current volume value (in steps).
     * @param maxVolume     The maximum possible volume (in steps).
     */
    private void updateDialogIcon(int currentVolume, int maxVolume) {
        int percent = (int) (((float) currentVolume / maxVolume) * 100);
        mDialogTitle.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(requireContext(), percent < 50
            ? R.drawable.ic_volume_down
            : R.drawable.ic_volume_up), null, null, null);
        mDialogTitle.setCompoundDrawablePadding((int) dpToPx(18, getResources().getDisplayMetrics()));
    }

    /**
     * Updates the text view displaying the current alarm volume as a percentage.
     *
     * @param currentVolume The current volume value (in steps).
     * @param maxVolume     The maximum possible volume (in steps).
     */
    private void updateVolumeText(int currentVolume, int maxVolume) {
        int percent = (int) (((float) currentVolume / maxVolume) * 100);
        mBinding.alarmVolumeValue.setText(String.format(Locale.getDefault(), "%d%%", percent));
    }

    /**
     * Enables or disables the volume plus/minus buttons based on the current slider progress.
     *
     * @param progress    The current progress of the slider (volume level in steps).
     * @param maxProgress The maximum progress of the slider.
     */
    private void updateVolumeButtonStates(int progress, int maxProgress) {
        ThemeUtils.updateSliderButtonEnabledState(requireContext(), mBinding.sliderMinusIcon, progress > 0);
        ThemeUtils.updateSliderButtonEnabledState(requireContext(), mBinding.sliderPlusIcon, progress < maxProgress);
    }

    /**
     * Shows or hides the low volume warning text based on the slider's progress.
     * The warning becomes visible when the volume reaches the minimum allowed level.
     *
     * @param warningText The TextView used for the warning message.
     * @param progress    The current slider value.
     */
    private void updateWarningVisibility(TextView warningText, int progress) {
        warningText.setVisibility(progress <= 0 ? VISIBLE : GONE);
    }

    /**
     * Starts a preview of the alarm ringtone at the given volume level.
     * Temporarily sets the alarm stream volume and restores it after a short delay.
     *
     * @param newVolume The volume level (in steps) to apply during the preview.
     */
    public void startRingtonePreview(int newVolume) {
        if (mRingtoneStopRunnable != null) {
            mRingtoneHandler.removeCallbacks(mRingtoneStopRunnable);
        }

        // Save the current system volume
        if (mPreviousVolume == -1) {
            mPreviousVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        }

        // Temporarily apply the new volume
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0);

        Uri ringtoneUri = mRingtoneUri;
        if (RingtoneUtils.isRandomRingtone(ringtoneUri)) {
            ringtoneUri = RingtoneUtils.getRandomRingtoneUri();
        } else if (RingtoneUtils.isRandomCustomRingtone(ringtoneUri)) {
            ringtoneUri = RingtoneUtils.getRandomCustomRingtoneUri();
        }

        // Start ringtone with volume applied
        RingtonePreviewKlaxon.startPreviewOnlyFromSpeakers(ringtoneUri);

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

        RingtonePreviewKlaxon.stopPreviewFromSpeakers();

        RingtonePreviewKlaxon.releaseResources();

        // Restore the system volume
        if (mPreviousVolume != -1) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mPreviousVolume, 0);
            mPreviousVolume = -1;
        }

        mIsPreviewPlaying = false;
    }

}
