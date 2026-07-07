// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_NEVER;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.HapticFeedbackConstantsCompat;
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
import com.best.deskclock.events.Events;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.InsetsUtils;
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
    private static final String STATE_VIBRATE = "state_vibrate";
    private static final String STATE_DELETE_AFTER_USE = "state_delete_after_use";
    private static final String STATE_TIMER_AUTO_SILENCE = "state_timer_auto_silence";

    private TimerEditBottomSheetBinding mBinding;
    private SharedPreferences mPrefs;
    private Typeface mGeneralTypeface;
    private Typeface mTimerBoldTypeface;
    private DisplayMetrics mDisplayMetrics;

    private int mTimerId;
    private long mTimerTimeText;
    private String mTimerLabel;
    private int mAddTimeButtonValue;
    private boolean mVibrate;
    private boolean mDeleteAfterUse;
    private int mTimerAutoSilence;

    private boolean mIsDeleted;
    private int mScreenHeight;
    private int mVisualPadding;

    public static TimerEditBottomSheetFragment newInstance(int timerId, String tag) {

        final Bundle args = new Bundle();

        args.putInt(ARG_TIMER_ID, timerId);
        args.putString(ARG_TIMER_TAG, tag);

        final TimerEditBottomSheetFragment fragment = new TimerEditBottomSheetFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static void show(FragmentManager manager, TimerEditBottomSheetFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPrefs = getDefaultSharedPreferences(requireContext());
        mGeneralTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(mPrefs));
        mTimerBoldTypeface = ThemeUtils.boldTypeface(SettingsDAO.getTimerDurationFont(mPrefs));
        mDisplayMetrics = getResources().getDisplayMetrics();
        mScreenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        mVisualPadding = (int) dpToPx(8, mDisplayMetrics);

        setupFragmentResultListeners();
    }

    @Override
    public void onDestroyView() {
        nullifyClickListeners(mBinding.timerTimeText, mBinding.timerLabel, mBinding.addTimeButtonLayout, mBinding.addTimeButton,
            mBinding.vibrateOnOff, mBinding.deleteTimerAfterUse, mBinding.autoSilenceDurationLayout, mBinding.deleteButton,
            mBinding.duplicateButton);

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
            outState.putBoolean(STATE_VIBRATE, mVibrate);
            outState.putBoolean(STATE_DELETE_AFTER_USE, mDeleteAfterUse);
            outState.putInt(STATE_TIMER_AUTO_SILENCE, mTimerAutoSilence);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        Window window = dialog.getWindow();
        if (window != null) {
            // Display within the cutout area
            ThemeUtils.allowDisplayCutout(window);

            // To prevent flickering when a 'MaterialAlertDialog' opens on top of this BottomSheet, remove the background dimming
            // caused by the BottomSheet. The 'MaterialAlertDialog' will handle this dimming.
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            // Prevent the BottomSheet from moving when the keyboard opens (for example, when editing the alarm label).
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
            mVibrate = savedInstanceState.getBoolean(STATE_VIBRATE);
            mDeleteAfterUse = savedInstanceState.getBoolean(STATE_DELETE_AFTER_USE);
            mTimerAutoSilence = savedInstanceState.getInt(STATE_TIMER_AUTO_SILENCE);
        } else {
            mTimerTimeText = timer.getLength();
            mTimerLabel = timer.getLabel();
            mAddTimeButtonValue = Integer.parseInt(timer.getButtonTime());
            mVibrate = timer.isVibrate();
            mDeleteAfterUse = timer.getDeleteAfterUse();
            mTimerAutoSilence = timer.getAutoSilence();
        }

        mBinding = TimerEditBottomSheetBinding.inflate(getLayoutInflater());

        dialog.setContentView(mBinding.getRoot());

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        InsetsUtils.doOnApplyWindowInsets(mBinding.getRoot(), (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            int statusBarHeight = statusBars.top;

            behavior.setMaxHeight(mScreenHeight - statusBarHeight - mVisualPadding);
        });

        bindTimerTimeText();
        bindLabel();
        bindAddTimeButtonValue();
        bindVibrator();
        bindDeleteTimerAfterUse();
        bindAutoSilenceValue();
        bindDeleteButton();
        bindDuplicateButton();

        updateAllGroupBackgrounds();

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog d = (BottomSheetDialog) dialogInterface;
            View bottomSheetInternal = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheetInternal != null) {
                bottomSheetInternal.setElevation(dpToPx(12, mDisplayMetrics));
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

            if (isChecked) {
                Utils.setVibrationTime(requireContext(), 300);
            }
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
            mDeleteAfterUse = isChecked;
            Utils.performHapticFeedback(mBinding.deleteTimerAfterUse, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
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

    private void bindDeleteButton() {
        if (getTimer() == null) {
            return;
        }

        mBinding.deleteButton.setTypeface(mGeneralTypeface);

        mBinding.deleteButton.setOnClickListener(v -> {
            mIsDeleted = true;
            Events.sendTimerEvent(R.string.action_delete, R.string.label_deskclock);
            DataModel.getDataModel().removeTimer(getTimer(), R.string.label_deskclock);
            Utils.performHapticFeedback(mBinding.deleteButton, HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            dismiss();
        });
    }

    private void bindDuplicateButton() {
        if (getTimer() == null) {
            return;
        }

        if (SettingsDAO.isSingleTimerModeEnabled(mPrefs)) {
            mBinding.duplicateButton.setVisibility(GONE);
            return;
        }

        mBinding.duplicateButton.setTypeface(mGeneralTypeface);

        mBinding.duplicateButton.setVisibility(VISIBLE);

        mBinding.duplicateButton.setOnClickListener(v -> {
            Timer originalTimer = getTimer();

            if (originalTimer == null) {
                return;
            }

            DataModel.getDataModel().addTimer(mTimerTimeText, mTimerLabel, String.valueOf(mAddTimeButtonValue), mTimerAutoSilence,
                mVibrate, mDeleteAfterUse);

            Utils.performHapticFeedback(mBinding.duplicateButton, HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            dismiss();
        });
    }

    // ********************
    // ** HELPER METHODS **
    // ********************

    /**
     * @return the timer currently being edited.
     */
    private Timer getTimer() {
        if (mTimerId < 0) {
            return null;
        }

        return DataModel.getDataModel().getTimer(mTimerId);
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

        childFragmentManager.setFragmentResultListener(AutoSilenceDurationDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mTimerAutoSilence = bundle.getInt(AutoSilenceDurationDialogFragment.AUTO_SILENCE_DURATION_VALUE);
                bindAutoSilenceValue();
            });
    }

    private void saveTimerSettings() {
        Timer timer = getTimer();

        if (mIsDeleted || timer == null) {
            return;
        }

        boolean durationChanged = timer.getLength() != mTimerTimeText;

        if (durationChanged) {
            DataModel.getDataModel().setNewTimerDuration(timer, mTimerTimeText);

            timer = getTimer();
        }

        if (timer != null) {
            DataModel.getDataModel().updateAllTimerSettings(
                timer,
                mTimerLabel,
                String.valueOf(mAddTimeButtonValue),
                mTimerAutoSilence,
                mVibrate,
                mDeleteAfterUse
            );
        }
    }

    private void updateAllGroupBackgrounds() {
        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            mPrefs,
            mBinding.timerLabel,
            mBinding.addTimeButtonLayout
        );

        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            mPrefs,
            mBinding.vibrateOnOff,
            mBinding.deleteTimerAfterUse
        );

        ThemeUtils.applyExpressiveBackgroundsToGroup(
            requireContext(),
            mPrefs,
            mBinding.autoSilenceDurationLayout
        );
    }

    private void nullifyClickListeners(View... views) {
        for (View view : views) {
            if (view != null) {
                view.setOnClickListener(null);
            }
        }
    }

}
