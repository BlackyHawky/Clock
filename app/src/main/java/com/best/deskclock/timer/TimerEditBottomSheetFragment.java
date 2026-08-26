// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VOLUME_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_NEVER;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_ESCALATING;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_HEARTBEAT;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_SOFT;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_STRONG;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_TICK_TOCK;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.IntentCompat;
import androidx.core.os.BundleCompat;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.databinding.TimerEditBottomSheetBinding;
import com.best.deskclock.dialogfragment.AutoSilenceDurationDialogFragment;
import com.best.deskclock.dialogfragment.LabelDialogFragment;
import com.best.deskclock.dialogfragment.TimerAddTimeButtonDialogFragment;
import com.best.deskclock.dialogfragment.TimerSetNewDurationDialogFragment;
import com.best.deskclock.dialogfragment.VibrationPatternDialogFragment;
import com.best.deskclock.dialogfragment.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.events.Events;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Locale;

public class TimerEditBottomSheetFragment extends BottomSheetDialogFragment  {

    public static final String TAG = "timer_edit_bottom_sheet";
    private static final String ARG_TIMER_TAG = "arg_timer_tag";
    private static final String ARG_TIMER_ID = "arg_timer_id";
    private static final String STATE_TIMER_TIME_TEXT = "state_timer_time_text";
    private static final String STATE_TIMER_LABEL = "state_timer_label";
    private static final String STATE_ADD_TIME_BUTTON_VALUE = "state_add_time_button_value";
    private static final String STATE_TIMER_RINGTONE_URI = "state_timer_ringtone_uri";
    private static final String STATE_VIBRATE = "state_vibrate";
    private static final String STATE_VIBRATION_PATTERN = "state_vibration_pattern";
    private static final String STATE_FLASH_ON = "state_flash_on";
    private static final String STATE_TURN_OFF_MEDIA = "state_turn_off_media";
    private static final String STATE_DELETE_AFTER_USE = "state_delete_after_use";
    private static final String STATE_TIMER_AUTO_SILENCE = "state_timer_auto_silence";
    private static final String STATE_VOLUME_CRESCENDO_DURATION = "state_volume_crescendo_duration";

    private TimerEditBottomSheetBinding mBinding;

    private DataModel mDataModel;
    private SharedPreferences mPrefs;
    private Typeface mGeneralTypeface;
    private Typeface mTimerBoldTypeface;
    private DisplayMetrics mDisplayMetrics;

    private int mTimerId;
    private long mTimerTimeText;
    private String mTimerLabel;
    private int mAddTimeButtonValue;
    private Uri mTimerRingtoneUri;
    private boolean mVibrate;
    private String mVibrationPattern;
    private boolean mFlashOn;
    private boolean mTurnOffMedia;
    private boolean mDeleteAfterUse;
    private int mTimerAutoSilence;
    private int mVolumeCrescendoDuration;

    private boolean mIsDeleted;

    @NonNull
    public static TimerEditBottomSheetFragment newInstance(int timerId, @Nullable String tag) {

        final Bundle args = new Bundle();

        args.putInt(ARG_TIMER_ID, timerId);
        args.putString(ARG_TIMER_TAG, tag);

        final TimerEditBottomSheetFragment fragment = new TimerEditBottomSheetFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static void show(@NonNull FragmentManager manager, @NonNull TimerEditBottomSheetFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    private final ActivityResultLauncher<Intent> mRingtonePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = IntentCompat.getParcelableExtra(result.getData(), RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);

                mTimerRingtoneUri = (uri != null) ? uri : RingtoneUtils.RINGTONE_SILENT;

                bindRingtone();
            }
        }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDataModel = DataModel.getDataModel();
        mPrefs = getDefaultSharedPreferences(requireContext());
        mGeneralTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(mPrefs));
        mTimerBoldTypeface = ThemeUtils.boldTypeface(SettingsDAO.getTimerDurationFont(mPrefs));
        mDisplayMetrics = getResources().getDisplayMetrics();

        setupFragmentResultListeners();
    }

    @Override
    public void onDestroyView() {
        nullifyClickListeners(mBinding.timerTimeText, mBinding.timerLabel, mBinding.addTimeButtonLayout, mBinding.addTimeButton,
            mBinding.chooseRingtone, mBinding.vibrateOnOff, mBinding.vibrationPatternLayout, mBinding.flashOnOff, mBinding.turnOffMedia,
            mBinding.deleteTimerAfterUse, mBinding.autoSilenceDurationLayout, mBinding.crescendoDurationLayout, mBinding.deleteButton,
            mBinding.duplicateButton, mBinding.saveButton);

        mBinding = null;

        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as this dialog exists, save its state.
        if (mTimerId != -1) {
            outState.putInt(ARG_TIMER_ID, mTimerId);
            outState.putLong(STATE_TIMER_TIME_TEXT, mTimerTimeText);
            outState.putString(STATE_TIMER_LABEL, mTimerLabel);
            outState.putInt(STATE_ADD_TIME_BUTTON_VALUE, mAddTimeButtonValue);
            outState.putParcelable(STATE_TIMER_RINGTONE_URI, mTimerRingtoneUri);
            outState.putBoolean(STATE_VIBRATE, mVibrate);
            outState.putString(STATE_VIBRATION_PATTERN, mVibrationPattern);
            outState.putBoolean(STATE_FLASH_ON, mFlashOn);
            outState.putBoolean(STATE_DELETE_AFTER_USE, mDeleteAfterUse);
            outState.putBoolean(STATE_TURN_OFF_MEDIA, mTurnOffMedia);
            outState.putInt(STATE_TIMER_AUTO_SILENCE, mTimerAutoSilence);
            outState.putInt(STATE_VOLUME_CRESCENDO_DURATION, mVolumeCrescendoDuration);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        Window window = dialog.getWindow();
        if (window != null) {
            // Display within the cutout area
            ThemeUtils.allowDisplayCutout(window);

            // To prevent flickering when a 'MaterialAlertDialog' opens on top of this BottomSheet, remove the background dimming
            // caused by the BottomSheet. The 'MaterialAlertDialog' will handle this dimming.
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            // Prevent the BottomSheet from moving when the keyboard opens (for example, when editing the timer label).
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }

        final Bundle bundleToUse = (savedInstanceState != null) ? savedInstanceState : requireArguments();
        mTimerId = bundleToUse.getInt(ARG_TIMER_ID, -1);
        Timer timer = getTimer();

        if (mTimerId == -1 || timer == null) {
            dismiss();
            return super.onCreateDialog(savedInstanceState);
        }

        if (savedInstanceState != null) {
            mTimerTimeText = savedInstanceState.getLong(STATE_TIMER_TIME_TEXT);
            mTimerLabel = savedInstanceState.getString(STATE_TIMER_LABEL);
            mAddTimeButtonValue = savedInstanceState.getInt(STATE_ADD_TIME_BUTTON_VALUE);
            mTimerRingtoneUri = BundleCompat.getParcelable(savedInstanceState, STATE_TIMER_RINGTONE_URI, Uri.class);
            mVibrate = savedInstanceState.getBoolean(STATE_VIBRATE);
            mVibrationPattern = savedInstanceState.getString(STATE_VIBRATION_PATTERN);
            mFlashOn = savedInstanceState.getBoolean(STATE_FLASH_ON);
            mTurnOffMedia = savedInstanceState.getBoolean(STATE_TURN_OFF_MEDIA);
            mDeleteAfterUse = savedInstanceState.getBoolean(STATE_DELETE_AFTER_USE);
            mTimerAutoSilence = savedInstanceState.getInt(STATE_TIMER_AUTO_SILENCE);
            mVolumeCrescendoDuration = savedInstanceState.getInt(STATE_VOLUME_CRESCENDO_DURATION);
        } else {
            mTimerTimeText = timer.getLength();
            mTimerLabel = timer.getLabel();
            mAddTimeButtonValue = Integer.parseInt(timer.getButtonTime());
            mTimerRingtoneUri = timer.getRingtoneUri();
            mVibrate = timer.isVibrate();
            mVibrationPattern = timer.getVibrationPattern();
            mFlashOn = timer.isFlashOn();
            mTurnOffMedia = timer.getTurnOffMedia();
            mDeleteAfterUse = timer.getDeleteAfterUse();
            mTimerAutoSilence = timer.getAutoSilence();
            mVolumeCrescendoDuration = timer.getVolumeCrescendoDuration();
        }

        mBinding = TimerEditBottomSheetBinding.inflate(getLayoutInflater());

        dialog.setContentView(mBinding.getRoot());

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        bindTimerTimeText();
        bindLabel();
        bindAddTimeButtonValue();
        bindRingtone();
        bindVibrator();
        bindVibrationPattern();
        bindFlash();
        bindTurnOffMedia();
        bindDeleteTimerAfterUse();
        bindAutoSilenceValue();
        bindCrescendoDuration();
        bindSpace();
        bindDeleteButton();
        bindDuplicateButton();
        bindSaveButton();

        updateAllGroupBackgrounds();

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheetInternal != null) {
                bottomSheetInternal.setElevation(dpToPx(12, mDisplayMetrics));

                View parent = (View) bottomSheetInternal.getParent();

                if (parent != null) {
                    WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(parent);
                    int topInset = insets != null
                        ? insets.getInsets(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout()).top
                        : 0;
                    int availableHeight = parent.getHeight() - topInset;
                    BottomSheetBehavior.from(bottomSheetInternal).setMaxHeight(availableHeight);
                }
            }
        });

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (getActivity() != null && !getActivity().isChangingConfigurations()) {
            saveTimerSettings();
        }

        super.onDismiss(dialog);
    }

    private void bindTimerTimeText() {
        if (getTimer() == null) {
            return;
        }

        mBinding.timerTimeText.setBackground(ThemeUtils.pillRippleDrawable(requireContext(), Color.TRANSPARENT));

        String formattedTime = DateUtils.formatElapsedTime(mTimerTimeText / 1000);
        mBinding.timerTimeText.setText(formattedTime);
        mBinding.timerTimeText.setTypeface(mTimerBoldTypeface);

        mBinding.timerTimeText.setOnClickListener(v -> {
            Events.sendTimerEvent(R.string.action_set_new_timer_duration, R.string.label_deskclock);

            final TimerSetNewDurationDialogFragment fragment = TimerSetNewDurationDialogFragment.newInstance(mTimerId, mTimerTimeText);
            TimerSetNewDurationDialogFragment.show(getChildFragmentManager(), fragment);
        });
    }

    private void bindLabel() {
        if (getTimer() == null) {
            return;
        }

        final boolean timerLabelIsEmpty = TextUtils.isEmpty(mTimerLabel);

        mBinding.timerLabel.setText(timerLabelIsEmpty ? getString(R.string.add_label) : mTimerLabel);
        mBinding.timerLabel.setTypeface(mGeneralTypeface);
        mBinding.timerLabel.setContentDescription(timerLabelIsEmpty
            ? getString(R.string.no_label_specified)
            : getString(R.string.label_description) + " " + mTimerLabel);

        mBinding.timerLabel.setOnClickListener(v -> {
            Events.sendTimerEvent(R.string.action_set_label, R.string.label_deskclock);

            final LabelDialogFragment fragment = LabelDialogFragment.newInstance(mTimerId, mTimerLabel);
            LabelDialogFragment.show(getChildFragmentManager(), fragment);
        });
    }

    private void bindAddTimeButtonValue() {
        if (getTimer() == null) {
            return;
        }

        mBinding.addTimeButtonTitle.setTypeface(mGeneralTypeface);
        mBinding.addTimeButton.setTypeface(mGeneralTypeface);

        long totalSeconds = mAddTimeButtonValue;
        long buttonTimeMinutes = (totalSeconds) / 60;
        long buttonTimeSeconds = totalSeconds % 60;

        String buttonTimeFormatted = String.format(
            Locale.getDefault(),
            buttonTimeMinutes < 10 ? "%d:%02d" : "%02d:%02d",
            buttonTimeMinutes,
            buttonTimeSeconds);

        mBinding.addTimeButton.setText(getString(R.string.timer_add_custom_time, buttonTimeFormatted));

        View.OnClickListener addTimeButtonListener = v -> {
            Events.sendTimerEvent(R.string.action_set_add_time_button_value, R.string.label_deskclock);

            final TimerAddTimeButtonDialogFragment fragment = TimerAddTimeButtonDialogFragment.newInstance(mTimerId, mAddTimeButtonValue);
            TimerAddTimeButtonDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.addTimeButtonLayout.setOnClickListener(addTimeButtonListener);
        mBinding.addTimeButton.setOnClickListener(addTimeButtonListener);
    }

    private void bindRingtone() {
        if (getTimer() == null) {
            return;
        }

        final Uri defaultUri = mDataModel.getDefaultTimerRingtoneUri();
        final String title;

        if (defaultUri.equals(mTimerRingtoneUri)) {
            title = getString(R.string.default_timer_ringtone_title);
        } else {
            title = mDataModel.getRingtoneTitle(mTimerRingtoneUri);
        }

        mBinding.chooseRingtone.setText(title);
        mBinding.chooseRingtone.setTypeface(mGeneralTypeface);

        final String description = getString(R.string.ringtone_description);
        mBinding.chooseRingtone.setContentDescription(description + " " + title);

        final Drawable iconRingtone;
        Uri uri = mTimerRingtoneUri;

        if (RingtoneUtils.RINGTONE_SILENT.equals(uri)) {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_ringtone_silent);
        } else if (RingtoneUtils.isRandomRingtone(uri) || RingtoneUtils.isRandomCustomRingtone(uri)) {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_random);
        } else {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_ringtone);
        }

        mBinding.chooseRingtone.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRingtone, null, null, null);

        mBinding.chooseRingtone.setOnClickListener(v -> {
            Events.sendTimerEvent(R.string.action_set_ringtone, R.string.label_deskclock);
            final Intent intent = RingtonePickerActivity.createPerTimerRingtonePickerIntent(requireContext(), mTimerRingtoneUri);
            mRingtonePickerLauncher.launch(intent);
        });
    }

    private void bindVibrator() {
        if (getTimer() == null) {
            return;
        }

        if (!DeviceUtils.hasVibrator(requireContext())) {
            mBinding.vibrateOnOff.setVisibility(GONE);
            return;
        }

        mBinding.vibrateOnOff.setTypeface(mGeneralTypeface);
        mBinding.vibrateOnOff.setVisibility(VISIBLE);
        mBinding.vibrateOnOff.setOnCheckedChangeListener(null);
        mBinding.vibrateOnOff.setChecked(mVibrate);

        mBinding.vibrateOnOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Events.sendTimerEvent(R.string.action_toggle_vibrate, R.string.label_deskclock);
            mVibrate = isChecked;
            bindVibrationPattern();
            updateSecondGroup();
            if (isChecked) {
                Utils.setVibrationTime(requireContext(), 300);
            }
        });
    }

    private void bindVibrationPattern() {
        if (getTimer() == null) {
            return;
        }

        if (!mVibrate || SettingsDAO.isPerTimerVibrationPatternDisabled(mPrefs)) {
            mBinding.vibrationPatternLayout.setVisibility(GONE);
            return;
        }

        mBinding.vibrationPatternTitle.setTypeface(mGeneralTypeface);
        mBinding.vibrationPatternValue.setTypeface(mGeneralTypeface);
        mBinding.vibrationPatternLayout.setVisibility(VISIBLE);

        switch (mVibrationPattern) {
            case VIBRATION_PATTERN_SOFT -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_soft));
            case VIBRATION_PATTERN_STRONG -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_strong));
            case VIBRATION_PATTERN_HEARTBEAT -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_heartbeat));
            case VIBRATION_PATTERN_ESCALATING -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_escalating));
            case VIBRATION_PATTERN_TICK_TOCK -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_tick_tock));
            default -> mBinding.vibrationPatternValue.setText(getString(R.string.label_default));
        }

        View.OnClickListener openVibrationPatternFragment = v -> {
            Events.sendTimerEvent(R.string.action_set_vibration_pattern, R.string.label_deskclock);

            final VibrationPatternDialogFragment fragment = VibrationPatternDialogFragment.newInstance(mTimerId, mVibrationPattern);
            VibrationPatternDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.vibrationPatternLayout.setOnClickListener(openVibrationPatternFragment);
    }

    private void bindFlash() {
        if (getTimer() == null) {
            return;
        }

        if (!DeviceUtils.hasBackFlash(requireContext())) {
            mBinding.flashOnOff.setVisibility(GONE);
            return;
        }

        mBinding.flashOnOff.setTypeface(mGeneralTypeface);
        mBinding.flashOnOff.setOnCheckedChangeListener(null);
        mBinding.flashOnOff.setChecked(mFlashOn);
        mBinding.flashOnOff.setVisibility(VISIBLE);
        mBinding.flashOnOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Utils.performHapticFeedback(buttonView, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            Events.sendTimerEvent(R.string.action_toggle_flash, R.string.label_deskclock);
            mFlashOn = isChecked;
        });
    }

    private void bindTurnOffMedia() {
        if (getTimer() == null) {
            return;
        }

        mBinding.turnOffMedia.setTypeface(mGeneralTypeface);
        mBinding.turnOffMedia.setOnCheckedChangeListener(null);
        mBinding.turnOffMedia.setChecked(mTurnOffMedia);

        mBinding.turnOffMedia.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Utils.performHapticFeedback(buttonView, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            mTurnOffMedia = isChecked;
        });
    }

    private void bindDeleteTimerAfterUse() {
        if (getTimer() == null) {
            return;
        }

        if (SettingsDAO.isSingleTimerModeEnabled(mPrefs)) {
            mBinding.deleteTimerAfterUse.setVisibility(GONE);
            return;
        }

        mBinding.deleteTimerAfterUse.setTypeface(mGeneralTypeface);
        mBinding.deleteTimerAfterUse.setOnCheckedChangeListener(null);
        mBinding.deleteTimerAfterUse.setChecked(mDeleteAfterUse);
        mBinding.deleteTimerAfterUse.setVisibility(VISIBLE);

        mBinding.deleteTimerAfterUse.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Utils.performHapticFeedback(buttonView, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            mDeleteAfterUse = isChecked;
        });
    }

    private void bindAutoSilenceValue() {
        if (getTimer() == null) {
            return;
        }

        if (SettingsDAO.isPerTimerAutoSilenceDisabled(mPrefs)) {
            mBinding.autoSilenceDurationLayout.setVisibility(GONE);
            return;
        }

        mBinding.autoSilenceDurationTitle.setTypeface(mGeneralTypeface);
        mBinding.autoSilenceDurationValue.setTypeface(mGeneralTypeface);

        int autoSilenceDuration = mTimerAutoSilence;

        if (autoSilenceDuration == TIMEOUT_NEVER) {
            mBinding.autoSilenceDurationValue.setText(getString(R.string.label_never));
        } else if (autoSilenceDuration == TIMEOUT_END_OF_RINGTONE) {
            mBinding.autoSilenceDurationValue.setText(getString(R.string.auto_silence_end_of_ringtone));
        } else {
            int m = autoSilenceDuration / 60;
            int s = autoSilenceDuration % 60;

            if (m > 0 && s > 0) {
                String minutesString = getResources().getQuantityString(R.plurals.minutes_short, m, m);
                String secondsString = s + " " + getString(R.string.seconds_label);
                mBinding.autoSilenceDurationValue.setText(String.format("%s %s", minutesString, secondsString));
            } else if (m > 0) {
                mBinding.autoSilenceDurationValue.setText(getResources().getQuantityString(R.plurals.minutes_short, m, m));
            } else {
                String secondsString = s + " " + getString(R.string.seconds_label);
                mBinding.autoSilenceDurationValue.setText(secondsString);
            }
        }

        mBinding.autoSilenceDurationLayout.setVisibility(VISIBLE);

        View.OnClickListener openAutoSilenceDurationFragment = v -> {
            Events.sendTimerEvent(R.string.action_set_auto_silence_duration, R.string.label_deskclock);

            final AutoSilenceDurationDialogFragment fragment = AutoSilenceDurationDialogFragment.newInstance(mTimerId, mTimerAutoSilence);
            AutoSilenceDurationDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.autoSilenceDurationLayout.setOnClickListener(openAutoSilenceDurationFragment);
    }

    private void bindCrescendoDuration() {
        if (getTimer() == null) {
            return;
        }

        if (SettingsDAO.isPerTimerCrescendoDurationDisabled(mPrefs)) {
            mBinding.crescendoDurationLayout.setVisibility(GONE);
            return;
        }

        mBinding.crescendoDurationTitle.setTypeface(mGeneralTypeface);
        mBinding.crescendoDurationValue.setTypeface(mGeneralTypeface);

        int crescendoDuration = mVolumeCrescendoDuration;

        if (crescendoDuration == DEFAULT_VOLUME_CRESCENDO_DURATION) {
            mBinding.crescendoDurationValue.setText(getString(R.string.label_off));
        } else {
            int m = crescendoDuration / 60;
            int s = crescendoDuration % 60;

            if (m > 0 && s > 0) {
                String minutesString = getResources().getQuantityString(R.plurals.minutes_short, m, m);
                String secondsString = s + " " + getString(R.string.seconds_label);
                mBinding.crescendoDurationValue.setText(String.format("%s %s", minutesString, secondsString));
            } else if (m > 0) {
                mBinding.crescendoDurationValue.setText(getResources().getQuantityString(R.plurals.minutes_short, m, m));
            } else {
                String secondsString = s + " " + getString(R.string.seconds_label);
                mBinding.crescendoDurationValue.setText(secondsString);
            }
        }

        mBinding.crescendoDurationLayout.setVisibility(VISIBLE);

        View.OnClickListener openVolumeCrescendoFragment = v -> {
            Events.sendTimerEvent(R.string.action_set_crescendo_duration, R.string.label_deskclock);

            final VolumeCrescendoDurationDialogFragment fragment =
                VolumeCrescendoDurationDialogFragment.newInstance(mTimerId, mVolumeCrescendoDuration);

            VolumeCrescendoDurationDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.crescendoDurationLayout.setOnClickListener(openVolumeCrescendoFragment);
    }

    private void bindSpace() {
        if (mBinding.autoSilenceDurationLayout.getVisibility() == GONE && mBinding.crescendoDurationLayout.getVisibility() == GONE) {
            mBinding.space.setVisibility(GONE);
            return;
        }

        mBinding.space.setVisibility(VISIBLE);
    }

    private void bindDeleteButton() {
        if (getTimer() == null) {
            return;
        }

        mBinding.deleteButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            mIsDeleted = true;
            Events.sendTimerEvent(R.string.action_delete, R.string.label_deskclock);
            mDataModel.removeTimer(getTimer(), R.string.label_deskclock);
            dismiss();
        });
    }

    private void bindDuplicateButton() {
        if (getTimer() == null) {
            return;
        }

        if (SettingsDAO.isSingleTimerModeEnabled(mPrefs)) {
            mBinding.duplicateButton.setVisibility(INVISIBLE);
            return;
        }

        mBinding.duplicateButton.setVisibility(VISIBLE);

        mBinding.duplicateButton.setOnClickListener(v -> {
            Timer originalTimer = getTimer();

            if (originalTimer == null) {
                return;
            }

            Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            mDataModel.addTimer(
                mTimerTimeText,
                mTimerLabel,
                String.valueOf(mAddTimeButtonValue),
                mTimerRingtoneUri,
                mTimerAutoSilence,
                mVolumeCrescendoDuration,
                mVibrate,
                mVibrationPattern,
                mFlashOn,
                mTurnOffMedia,
                mDeleteAfterUse
            );

            dismiss();
        });
    }

    private void bindSaveButton() {
        mBinding.saveButton.setOnClickListener(v -> {
            Utils.performHapticFeedback(v, HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            Events.sendTimerEvent(R.string.action_save, R.string.label_deskclock);

            dismiss();
        });
    }

    // ********************
    // ** HELPER METHODS **
    // ********************

    /**
     * @return the timer currently being edited.
     */
    @Nullable
    private Timer getTimer() {
        if (mTimerId < 0) {
            return null;
        }

        return mDataModel.getTimer(mTimerId);
    }

    private void setupFragmentResultListeners() {
        FragmentManager childFragmentManager = getChildFragmentManager();

        childFragmentManager.setFragmentResultListener(TimerSetNewDurationDialogFragment.REQUEST_TIMER_DURATION, this,
            (requestKey, bundle) -> {
                long newDurationMillis = bundle.getLong(TimerSetNewDurationDialogFragment.RESULT_TIMER_DURATION);
                String oldDefaultLabel = Utils.buildDefaultTimerLabel(requireContext(), mTimerTimeText);

                if (mTimerLabel != null && mTimerLabel.equals(oldDefaultLabel)) {
                    mTimerLabel = Utils.buildDefaultTimerLabel(requireContext(), newDurationMillis);
                    bindLabel();
                }

                mTimerTimeText = newDurationMillis;
                bindTimerTimeText();
            });

        childFragmentManager.setFragmentResultListener(LabelDialogFragment.REQUEST_TIMER_LABEL, this,
            (requestKey, bundle) -> {
                mTimerLabel = bundle.getString(LabelDialogFragment.RESULT_TIMER_LABEL);
                bindLabel();
            });

        childFragmentManager.setFragmentResultListener(TimerAddTimeButtonDialogFragment.REQUEST_ADD_TIME_DURATION, this,
            (requestKey, bundle) -> {
                mAddTimeButtonValue = bundle.getInt(TimerAddTimeButtonDialogFragment.ADD_TIME_BUTTON_VALUE);
                bindAddTimeButtonValue();
            });

        childFragmentManager.setFragmentResultListener(VibrationPatternDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mVibrationPattern = bundle.getString(VibrationPatternDialogFragment.RESULT_PATTERN_KEY);
                bindVibrationPattern();
            });

        childFragmentManager.setFragmentResultListener(AutoSilenceDurationDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mTimerAutoSilence = bundle.getInt(AutoSilenceDurationDialogFragment.AUTO_SILENCE_DURATION_VALUE);
                bindAutoSilenceValue();
            });

        childFragmentManager.setFragmentResultListener(VolumeCrescendoDurationDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mVolumeCrescendoDuration = bundle.getInt(VolumeCrescendoDurationDialogFragment.VOLUME_CRESCENDO_DURATION_VALUE);
                bindCrescendoDuration();
            });
    }

    private void saveTimerSettings() {
        Timer timer = getTimer();

        if (mIsDeleted || timer == null) {
            return;
        }

        boolean durationChanged = timer.getLength() != mTimerTimeText;

        if (durationChanged) {
            mDataModel.setNewTimerDuration(timer, mTimerTimeText);

            timer = getTimer();
        }

        if (timer != null) {
            mDataModel.updateAllTimerSettings(
                timer,
                mTimerLabel,
                String.valueOf(mAddTimeButtonValue),
                mTimerRingtoneUri,
                mTimerAutoSilence,
                mVolumeCrescendoDuration,
                mVibrate,
                mVibrationPattern,
                mFlashOn,
                mTurnOffMedia,
                mDeleteAfterUse
            );
        }
    }

    private void updateAllGroupBackgrounds() {
        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            mPrefs,
            mBinding.timerLabel,
            mBinding.addTimeButtonLayout,
            mBinding.chooseRingtone
        );

        updateSecondGroup();

        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            mPrefs,
            mBinding.autoSilenceDurationLayout,
            mBinding.crescendoDurationLayout
        );
    }

    private void updateSecondGroup() {
        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            mPrefs,
            mBinding.vibrateOnOff,
            mBinding.vibrationPatternLayout,
            mBinding.flashOnOff,
            mBinding.turnOffMedia,
            mBinding.deleteTimerAfterUse
        );
    }

    private void nullifyClickListeners(@NonNull View... views) {
        for (View view : views) {
            if (view != null) {
                view.setOnClickListener(null);
            }
        }
    }

}
