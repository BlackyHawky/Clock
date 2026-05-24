// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.*;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.databinding.AlarmEditBottomSheetBinding;
import com.best.deskclock.databinding.DeskClockBinding;
import com.best.deskclock.dialogfragment.AlarmDelayPickerDialogFragment;
import com.best.deskclock.dialogfragment.AlarmMissedRepeatLimitDialogFragment;
import com.best.deskclock.dialogfragment.AlarmSnoozeDurationDialogFragment;
import com.best.deskclock.dialogfragment.AlarmVolumeDialogFragment;
import com.best.deskclock.dialogfragment.AutoSilenceDurationDialogFragment;
import com.best.deskclock.dialogfragment.DatePickerDialogFragment;
import com.best.deskclock.dialogfragment.LabelDialogFragment;
import com.best.deskclock.dialogfragment.MaterialTimePickerDialogFragment;
import com.best.deskclock.dialogfragment.SpinnerTimePickerDialogFragment;
import com.best.deskclock.dialogfragment.VibrationPatternDialogFragment;
import com.best.deskclock.dialogfragment.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.uicomponents.CustomTooltip;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.InsetsUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.DigitalAppWidgetProvider;
import com.best.deskclock.widgets.NextAlarmAppWidgetProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AlarmEditBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String TAG = "alarm_edit_bottom_sheet";
    private static final String ARG_ALARM = "arg_alarm";
    private static final String ARG_ALARM_ID = "arg_alarm_id";
    private static final String ARG_IS_NEW_ALARM = "arg_is_new_alarm";
    private static final String ARG_TAG = "arg_tag";
    public static final String SCROLL_TO_ALARM_ID = "scroll_to_alarm_id";
    public static final String REQUEST_KEY = "alarm_saved";

    private AlarmEditBottomSheetBinding mBinding;
    private SharedPreferences mPrefs;
    private Typeface mGeneralTypeface;
    private Typeface mAlarmBoldTypeface;
    private DisplayMetrics mDisplayMetrics;
    private Alarm mAlarm;
    private Alarm mOriginalAlarm;
    private AlarmUpdateHandler mAlarmUpdateHandler;
    private String mTag;
    private boolean mIsNewAlarm;
    private boolean mIsDeleted;
    private int mScreenHeight;
    private int mVisualPadding;

    public static AlarmEditBottomSheetFragment newInstance(Alarm alarm, long alarmId, String tag, boolean isNewAlarm) {

        final Bundle args = new Bundle();

        args.putParcelable(ARG_ALARM, alarm);
        args.putLong(ARG_ALARM_ID, alarmId);
        args.putString(ARG_TAG, tag);
        args.putBoolean(ARG_IS_NEW_ALARM, isNewAlarm);

        final AlarmEditBottomSheetFragment fragment = new AlarmEditBottomSheetFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static void show(FragmentManager manager, AlarmEditBottomSheetFragment fragment) {
        Utils.showDialogFragment(manager, fragment, TAG);
    }

    private final ActivityResultLauncher<Intent> mRingtonePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = SdkUtils.isAtLeastAndroid13()
                    ? result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class)
                    : result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

                mAlarm.alert = (uri != null) ? uri : RingtoneUtils.RINGTONE_SILENT;

                bindRingtone();
            }
        }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTag = requireArguments().getString(ARG_TAG);
        mIsNewAlarm = requireArguments().getBoolean(ARG_IS_NEW_ALARM, false);

        mPrefs = getDefaultSharedPreferences(requireContext());
        mGeneralTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(mPrefs));
        mAlarmBoldTypeface = ThemeUtils.boldTypeface(SettingsDAO.getAlarmFont(mPrefs));

        mDisplayMetrics = getResources().getDisplayMetrics();
        mScreenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        mVisualPadding = (int) dpToPx(8, mDisplayMetrics);

        setupFragmentResultListeners();
    }

    @Override
    public void onStart() {
        super.onStart();

        DeskClock activity = (DeskClock) requireActivity();
        DeskClockBinding activityBinding = activity.getDeskClockBinding();

        mAlarmUpdateHandler = new AlarmUpdateHandler(requireContext(), null, activityBinding.contentView);
    }

    @Override
    public void onDestroyView() {
        nullifyClickListeners(mBinding.digitalClock, mBinding.scheduleAlarmLayout, mBinding.pauseAlarmLayout, mBinding.editLabel,
            mBinding.chooseRingtone, mBinding.vibrationPatternLayout, mBinding.autoSilenceDurationLayout, mBinding.snoozeDurationLayout,
            mBinding.missedAlarmRepeatLimitLayout, mBinding.crescendoDurationLayout, mBinding.alarmVolumeLayout, mBinding.deleteButton,
            mBinding.duplicateButton);

        mAlarmUpdateHandler = null;

        mBinding = null;

        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // As long as this dialog exists, save its state.
        if (mAlarm != null) {
            outState.putParcelable(ARG_ALARM, mAlarm);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            // To prevent flickering when a 'MaterialAlertDialog' opens on top of this BottomSheet, remove the background dimming
            // caused by the BottomSheet. The 'MaterialAlertDialog' will handle this dimming.
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            // Prevent the BottomSheet from moving when the keyboard opens (for example, when editing the alarm label).
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        }

        final Bundle bundleToUse = (savedInstanceState != null) ? savedInstanceState : requireArguments();
        Alarm alarmFromArguments = SdkUtils.isAtLeastAndroid13()
            ? bundleToUse.getParcelable(ARG_ALARM, Alarm.class)
            : bundleToUse.getParcelable(ARG_ALARM);

        if (alarmFromArguments == null) {
            dismiss();
            return dialog;
        }

        mOriginalAlarm = new Alarm(alarmFromArguments);
        mAlarm = new Alarm(alarmFromArguments);

        mBinding = AlarmEditBottomSheetBinding.inflate(getLayoutInflater());

        dialog.setContentView(mBinding.getRoot());

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        InsetsUtils.doOnApplyWindowInsets(mBinding.getRoot(), (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            int statusBarHeight = statusBars.top;

            behavior.setMaxHeight(mScreenHeight - statusBarHeight - mVisualPadding);
        });

        bindCustomDragHandleTooltip();
        bindClock();
        bindDaysOfWeekButtons();
        bindSelectedDate();
        bindPauseAlarm();
        bindLabel();
        bindRingtone();
        bindVibrator();
        bindVibrationPattern();
        bindFlash();
        bindDeleteOccasionalAlarmAfterUse();
        bindAutoSilenceValue();
        bindSnoozeDurationValue();
        bindMissedAlarmRepeatLimit();
        bindCrescendoDuration();
        bindAlarmVolume();
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
            saveAlarmSettings();
        }

        // When the per-alarm volume feature is enabled, AlarmFragment temporarily "freezes"
        // its volume warning banner.
        // This prevents the banner from glitching or disappearing when the user tests
        // the alarm volume in the sub-dialog (AlarmVolumeDialogFragment).
        // Therefore, when this BottomSheet is fully dismissed, we must force the parent
        // AlarmFragment to re-evaluate the actual system volume.
        // This catches any system volume changes the user might have made
        // (e.g., using hardware buttons) while the UI was frozen, ensuring the banner state
        // remains perfectly synchronized.
        if (SettingsDAO.isPerAlarmVolumeEnabled(mPrefs)) {
            Fragment parentFragment = getParentFragmentManager().findFragmentByTag(mTag);
            if (parentFragment instanceof AlarmFragment alarmFragment) {
                alarmFragment.updateWarningBannerVisibility();
            }
        }

        super.onDismiss(dialog);
    }

    private void bindCustomDragHandleTooltip() {
        CharSequence nativeText = mBinding.dragHandle.getContentDescription();
        String tooltipText = nativeText != null ? nativeText.toString() : "";

        TooltipCompat.setTooltipText(mBinding.dragHandle, null);

        mBinding.dragHandle.setOnLongClickListener(v -> {
            if (!tooltipText.isEmpty()) {
                CustomTooltip.showBelow(v, tooltipText);
            }
            return true;
        });
    }

    private void bindClock() {
        mBinding.digitalClock.setBackground(ThemeUtils.pillRippleDrawable(requireContext(), Color.TRANSPARENT));
        mBinding.digitalClock.setTime(mAlarm.hour, mAlarm.minutes);
        mBinding.digitalClock.setTypeface(mAlarmBoldTypeface);

        mBinding.digitalClock.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);

            if (SettingsDAO.getMaterialTimePickerStyle(mPrefs).equals(SPINNER_TIME_PICKER_STYLE)) {
                final SpinnerTimePickerDialogFragment fragment = SpinnerTimePickerDialogFragment.newInstance(mAlarm.hour, mAlarm.minutes);
                SpinnerTimePickerDialogFragment.show(getChildFragmentManager(), fragment);
            } else {
                MaterialTimePickerDialogFragment.show(
                    requireContext(), getChildFragmentManager(), TAG, mAlarm.hour, mAlarm.minutes, mPrefs);
            }
        });

        mBinding.digitalClock.setOnLongClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_delay, R.string.label_deskclock);

            final AlarmDelayPickerDialogFragment fragment = AlarmDelayPickerDialogFragment.newInstance(0, 0);
            AlarmDelayPickerDialogFragment.show(getChildFragmentManager(), fragment);

            return true;
        });
    }

    private void bindDaysOfWeekButtons() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        List<Integer> weekdays = SettingsDAO.getWeekdayOrder(mPrefs).getCalendarDays();

        mBinding.repeatDaysGroup.removeAllViews();

        final MaterialButton[] dayButtons = new MaterialButton[7];

        for (int i = 0; i < 7; i++) {
            MaterialButton dayButton = (MaterialButton) inflater.inflate(R.layout.day_button, mBinding.repeatDaysGroup, false);
            int weekday = weekdays.get(i);

            dayButton.setId(View.generateViewId());
            dayButton.setTypeface(mGeneralTypeface);
            dayButton.setText(UiDataModel.getUiDataModel().getShortWeekday(weekday));
            dayButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(weekday));

            mBinding.repeatDaysGroup.addView(dayButton);
            dayButtons[i] = dayButton;

            boolean isChecked = mAlarm.daysOfWeek.isBitOn(weekday);

            if (isChecked) {
                mBinding.repeatDaysGroup.check(dayButton.getId());
            }

            updateDaysOfWeekButtonVisuals(dayButtons[i], isChecked);
        }

        mBinding.repeatDaysGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            for (int i = 0; i < dayButtons.length; i++) {
                if (dayButtons[i].getId() == checkedId) {
                    int weekday = weekdays.get(i);
                    mAlarm.daysOfWeek = mAlarm.daysOfWeek.setBit(weekday, isChecked);
                    updateDaysOfWeekButtonVisuals(dayButtons[i], isChecked);

                    if (!mAlarm.daysOfWeek.isRepeating()) {
                        mAlarm.pauseStartDate = 0;
                        mAlarm.pauseEndDate = 0;
                    }

                    if (mAlarm.daysOfWeek.getBits() == mOriginalAlarm.daysOfWeek.getBits()) {
                        // If the user has set the days exactly as they were originally, restore the original date to undo the change
                        // when saving the alarm.
                        mAlarm.year = mOriginalAlarm.year;
                        mAlarm.month = mOriginalAlarm.month;
                        mAlarm.day = mOriginalAlarm.day;
                    } else {
                        // Otherwise, set the date to today.
                        final Calendar now = Calendar.getInstance();
                        mAlarm.year = now.get(Calendar.YEAR);
                        mAlarm.month = now.get(Calendar.MONTH);
                        mAlarm.day = now.get(Calendar.DAY_OF_MONTH);
                    }

                    bindSelectedDate();
                    bindPauseAlarm();
                    bindDeleteOccasionalAlarmAfterUse();
                    Utils.setVibrationTime(requireContext(), 10);
                    break;
                }
            }
        });
    }

    private void bindSelectedDate() {
        int openCalendarText = R.string.schedule_alarm_title;

        mBinding.scheduleAlarm.setTypeface(mGeneralTypeface);

        mBinding.scheduleAlarmLayout.setOnClickListener(v -> DatePickerDialogFragment.show(
            requireContext(),
            getChildFragmentManager(),
            mPrefs,
            mAlarm,
            (year, month, day, hour, minute) -> {
                if (mAlarm.daysOfWeek.isRepeating()) {
                    mAlarm.daysOfWeek = Weekdays.NONE;
                }

                if (mAlarm.isPauseSet()) {
                    mAlarm.pauseStartDate = 0;
                    mAlarm.pauseEndDate = 0;
                }

                mAlarm.year = year;
                mAlarm.month = month;
                mAlarm.day = day;
                mAlarm.hour = hour;
                mAlarm.minutes = minute;

                bindSelectedDate();
                bindDaysOfWeekButtons();
                bindPauseAlarm();
                bindDeleteOccasionalAlarmAfterUse();
            })
        );

        if (mAlarm.daysOfWeek.isRepeating()) {
            clearSelectedDate(openCalendarText);
        } else if (mAlarm.isSpecifiedDate()) {
            if (mAlarm.isDateInThePast()) {
                clearSelectedDate(openCalendarText);
            } else {
                mBinding.scheduleAlarm.setText(AlarmUtils.formatAlarmDate(mAlarm));

                mBinding.cancelScheduledAlarm.setTypeface(mGeneralTypeface);
                mBinding.cancelScheduledAlarm.setOnClickListener(v -> {
                    Calendar now = Calendar.getInstance();
                    mAlarm.year = now.get(Calendar.YEAR);
                    mAlarm.month = now.get(Calendar.MONTH);
                    mAlarm.day = now.get(Calendar.DAY_OF_MONTH);

                    bindSelectedDate();
                });
                mBinding.cancelScheduledAlarm.setVisibility(VISIBLE);
            }
        } else {
            clearSelectedDate(openCalendarText);
        }
    }

    private void bindPauseAlarm() {
        boolean isRepeating = mAlarm.daysOfWeek.isRepeating();

        mBinding.pauseAlarm.setEnabled(isRepeating);
        mBinding.pauseAlarm.setTypeface(mGeneralTypeface);

        mAlarm.clearPauseIfExpired();

        if (isRepeating && mAlarm.isPauseSet()) {
            String dateRangeStr = AlarmUtils.formatPauseDateRange(requireContext(), mAlarm.pauseStartDate, mAlarm.pauseEndDate);

            mBinding.pauseAlarm.setText(getString(R.string.pause_alarm_range, dateRangeStr));

            mBinding.cancelPauseAlarm.setTypeface(mGeneralTypeface);
            mBinding.cancelPauseAlarm.setVisibility(View.VISIBLE);
        } else {
            mBinding.pauseAlarm.setText(R.string.pause_alarm_title);

            mBinding.cancelPauseAlarm.setVisibility(View.GONE);
        }

        if (isRepeating) {
            mBinding.pauseAlarmLayout.setOnClickListener(v -> DatePickerDialogFragment.showMaterialDateRangePicker(
                getChildFragmentManager(),
                mPrefs,
                mAlarm,
                (start, end) -> {
                    mAlarm.pauseStartDate = start;
                    mAlarm.pauseEndDate = end;
                    bindPauseAlarm();
                }
            ));

            mBinding.cancelPauseAlarm.setOnClickListener(v -> {
                mAlarm.pauseStartDate = 0;
                mAlarm.pauseEndDate = 0;
                bindPauseAlarm();
            });
        } else {
            mBinding.pauseAlarmLayout.setOnClickListener(null);
            mBinding.pauseAlarm.setOnClickListener(null);
        }
    }

    private void bindLabel() {
        final boolean alarmLabelIsEmpty = mAlarm.label == null || mAlarm.label.isEmpty();

        mBinding.editLabel.setText(alarmLabelIsEmpty ? getString(R.string.add_label) : mAlarm.label);
        mBinding.editLabel.setTypeface(mGeneralTypeface);
        mBinding.editLabel.setContentDescription(alarmLabelIsEmpty
            ? getString(R.string.no_label_specified)
            : getString(R.string.label_description) + " " + mAlarm.label);

        mBinding.editLabel.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_label, R.string.label_deskclock);

            final LabelDialogFragment fragment = LabelDialogFragment.newInstance(mAlarm.label, mAlarm.syncByLabel);
            LabelDialogFragment.show(getChildFragmentManager(), fragment);
        });
    }

    private void bindRingtone() {
        final String title = DataModel.getDataModel().getRingtoneTitle(mAlarm.alert);
        mBinding.chooseRingtone.setText(title);
        mBinding.chooseRingtone.setTypeface(mGeneralTypeface);

        final String description = getString(R.string.ringtone_description);
        mBinding.chooseRingtone.setContentDescription(description + " " + title);

        final Drawable iconRingtone;
        if (RingtoneUtils.RINGTONE_SILENT.equals(mAlarm.alert)) {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_ringtone_silent);
        } else if (RingtoneUtils.isRandomRingtone(mAlarm.alert) || RingtoneUtils.isRandomCustomRingtone(mAlarm.alert)) {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_random);
        } else {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_ringtone);
        }

        mBinding.chooseRingtone.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRingtone, null, null, null);

        mBinding.chooseRingtone.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_ringtone, R.string.label_deskclock);
            final Intent intent = RingtonePickerActivity.createAlarmRingtonePickerIntent(requireContext(), mAlarm);
            mRingtonePickerLauncher.launch(intent);
        });
    }

    private void bindVibrator() {
        if (!DeviceUtils.hasVibrator(requireContext())) {
            mBinding.vibrateOnOff.setVisibility(GONE);
            mBinding.vibrationPatternLayout.setVisibility(GONE);
            return;
        }

        mBinding.vibrateOnOff.setTypeface(mGeneralTypeface);
        mBinding.vibrateOnOff.setVisibility(VISIBLE);

        mBinding.vibrateOnOff.setOnCheckedChangeListener(null);
        mBinding.vibrateOnOff.setChecked(mAlarm.vibrate);

        mBinding.vibrateOnOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Events.sendAlarmEvent(R.string.action_toggle_vibrate, R.string.label_deskclock);
            mAlarm.vibrate = isChecked;
            bindVibrationPattern();
            updateSecondGroup();
            if (isChecked) {
                Utils.setVibrationTime(requireContext(), 300);
            }
        });
    }

    private void bindVibrationPattern() {
        if (!mAlarm.vibrate || !SettingsDAO.isPerAlarmVibrationPatternEnabled(mPrefs)) {
            mBinding.vibrationPatternLayout.setVisibility(GONE);
            return;
        }

        mBinding.vibrationPatternTitle.setTypeface(mGeneralTypeface);
        mBinding.vibrationPatternValue.setTypeface(mGeneralTypeface);
        mBinding.vibrationPatternLayout.setVisibility(VISIBLE);

        String vibrationPatternText = mAlarm.vibrationPattern;
        switch (vibrationPatternText) {
            case VIBRATION_PATTERN_SOFT -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_soft));
            case VIBRATION_PATTERN_STRONG -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_strong));
            case VIBRATION_PATTERN_HEARTBEAT -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_heartbeat));
            case VIBRATION_PATTERN_ESCALATING -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_escalating));
            case VIBRATION_PATTERN_TICK_TOCK -> mBinding.vibrationPatternValue.setText(getString(R.string.vibration_pattern_tick_tock));
            default -> mBinding.vibrationPatternValue.setText(getString(R.string.label_default));
        }

        View.OnClickListener openVibrationPatternFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_vibration_pattern, R.string.label_deskclock);

            final VibrationPatternDialogFragment fragment = VibrationPatternDialogFragment.newInstance(mAlarm.vibrationPattern);
            VibrationPatternDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.vibrationPatternLayout.setOnClickListener(openVibrationPatternFragment);
    }

    private void bindFlash() {
        if (!DeviceUtils.hasBackFlash(requireContext())) {
            mBinding.flashOnOff.setVisibility(GONE);
            return;
        }

        mBinding.flashOnOff.setTypeface(mGeneralTypeface);
        mBinding.flashOnOff.setVisibility(VISIBLE);
        mBinding.flashOnOff.setOnCheckedChangeListener(null);
        mBinding.flashOnOff.setChecked(mAlarm.flash);
        mBinding.flashOnOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Events.sendAlarmEvent(R.string.action_toggle_flash, R.string.label_deskclock);
            mAlarm.flash = isChecked;
            Utils.setVibrationTime(requireContext(), 50);
        });
    }

    private void bindDeleteOccasionalAlarmAfterUse() {
        final boolean isRepeating = mAlarm.daysOfWeek.isRepeating();

        mBinding.deleteOccasionalAlarmAfterUse.setTypeface(mGeneralTypeface);
        mBinding.deleteOccasionalAlarmAfterUse.setEnabled(!isRepeating);
        mBinding.deleteOccasionalAlarmAfterUse.setOnCheckedChangeListener(null);
        mBinding.deleteOccasionalAlarmAfterUse.setChecked(!isRepeating && mAlarm.deleteAfterUse);

        mBinding.deleteOccasionalAlarmAfterUse.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mAlarm.deleteAfterUse = isChecked;
            Utils.setVibrationTime(requireContext(), 50);
        });
    }

    private void bindAutoSilenceValue() {
        if (SettingsDAO.isPerAlarmAutoSilenceDisabled(mPrefs)) {
            mBinding.autoSilenceDurationLayout.setVisibility(GONE);
            return;
        }

        mBinding.autoSilenceDurationTitle.setTypeface(mGeneralTypeface);
        mBinding.autoSilenceDurationValue.setTypeface(mGeneralTypeface);

        int autoSilenceDuration = mAlarm.autoSilenceDuration;

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
            Events.sendAlarmEvent(R.string.action_set_auto_silence_duration, R.string.label_deskclock);

            final AutoSilenceDurationDialogFragment fragment = AutoSilenceDurationDialogFragment.newInstance(mAlarm.autoSilenceDuration);
            AutoSilenceDurationDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.autoSilenceDurationLayout.setOnClickListener(openAutoSilenceDurationFragment);
    }

    private void bindSnoozeDurationValue() {
        if (SettingsDAO.isPerAlarmSnoozeDurationDisabled(mPrefs)) {
            mBinding.snoozeDurationLayout.setVisibility(GONE);
            return;
        }

        mBinding.snoozeDurationTitle.setTypeface(mGeneralTypeface);
        mBinding.snoozeDurationValue.setTypeface(mGeneralTypeface);

        int snoozeDuration = mAlarm.snoozeDuration;

        if (snoozeDuration == ALARM_SNOOZE_DURATION_DISABLED) {
            mBinding.snoozeDurationValue.setText(getString(R.string.snooze_duration_none));
        } else {
            int h = snoozeDuration / 60;
            int m = snoozeDuration % 60;

            if (h > 0 && m > 0) {
                String hoursString = getResources().getQuantityString(R.plurals.hours_short, h, h);
                String minutesString = getResources().getQuantityString(R.plurals.minutes_short, m, m);
                mBinding.snoozeDurationValue.setText(String.format("%s %s", hoursString, minutesString));
            } else if (h > 0) {
                mBinding.snoozeDurationValue.setText(getResources().getQuantityString(R.plurals.hours_short, h, h));
            } else {
                mBinding.snoozeDurationValue.setText(getResources().getQuantityString(R.plurals.minutes_short, m, m));
            }
        }

        mBinding.snoozeDurationLayout.setVisibility(VISIBLE);

        View.OnClickListener openAlarmSnoozeDurationFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_snooze_duration, R.string.label_deskclock);

            final AlarmSnoozeDurationDialogFragment fragment = AlarmSnoozeDurationDialogFragment.newInstance(mAlarm.snoozeDuration);
            AlarmSnoozeDurationDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.snoozeDurationLayout.setOnClickListener(openAlarmSnoozeDurationFragment);
    }

    private void bindMissedAlarmRepeatLimit() {
        if (SettingsDAO.isPerAlarmMissedRepeatLimitDisabled(mPrefs) || mAlarm.autoSilenceDuration == TIMEOUT_NEVER) {
            mBinding.missedAlarmRepeatLimitLayout.setVisibility(GONE);
            return;
        }

        mBinding.missedAlarmRepeatLimitTitle.setTypeface(mGeneralTypeface);
        mBinding.missedAlarmRepeatLimitValue.setTypeface(mGeneralTypeface);

        int missedAlarmRepeatLimit = mAlarm.missedAlarmRepeatLimit;

        switch (missedAlarmRepeatLimit) {
            case 0 -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.label_never));
            case 1 -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_1_time));
            case 3 -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_3_times));
            case 5 -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_5_times));
            case 10 -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_10_times));
            default -> mBinding.missedAlarmRepeatLimitValue.setText(getString(R.string.label_indefinitely));
        }

        mBinding.missedAlarmRepeatLimitLayout.setVisibility(VISIBLE);

        View.OnClickListener openAlarmMissedRepeatLimitFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_missed_alarm_repeat_limit, R.string.label_deskclock);

            final AlarmMissedRepeatLimitDialogFragment fragment =
                AlarmMissedRepeatLimitDialogFragment.newInstance(mAlarm.missedAlarmRepeatLimit);

            AlarmMissedRepeatLimitDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.missedAlarmRepeatLimitLayout.setOnClickListener(openAlarmMissedRepeatLimitFragment);
    }

    private void bindCrescendoDuration() {
        if (SettingsDAO.isPerAlarmCrescendoDurationDisabled(mPrefs)) {
            mBinding.crescendoDurationLayout.setVisibility(GONE);
            return;
        }

        mBinding.crescendoDurationTitle.setTypeface(mGeneralTypeface);
        mBinding.crescendoDurationValue.setTypeface(mGeneralTypeface);

        int crescendoDuration = mAlarm.crescendoDuration;

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
            Events.sendAlarmEvent(R.string.action_set_crescendo_duration, R.string.label_deskclock);

            final VolumeCrescendoDurationDialogFragment fragment =
                VolumeCrescendoDurationDialogFragment.newInstance(mAlarm.crescendoDuration);

            VolumeCrescendoDurationDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.crescendoDurationLayout.setOnClickListener(openVolumeCrescendoFragment);
    }

    private void bindAlarmVolume() {
        if (!SettingsDAO.isPerAlarmVolumeEnabled(mPrefs)) {
            mBinding.alarmVolumeLayout.setVisibility(GONE);
            return;
        }

        mBinding.alarmVolumeTitle.setTypeface(mGeneralTypeface);
        mBinding.alarmVolumeValue.setTypeface(mGeneralTypeface);

        final AudioManager audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        final int currentVolume = Math.min(mAlarm.alarmVolume, maxVolume);

        int volumePercent = (int) (((float) currentVolume / maxVolume) * 100);
        String formatted = String.format(Locale.getDefault(), "%d%%", volumePercent);
        mBinding.alarmVolumeValue.setText(formatted);

        Drawable icon = AppCompatResources.getDrawable(requireContext(), volumePercent < 50
            ? R.drawable.ic_volume_down
            : R.drawable.ic_volume_up);

        if (icon != null) {
            mBinding.alarmVolumeTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }

        mBinding.alarmVolumeLayout.setVisibility(VISIBLE);

        View.OnClickListener openVolumeFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_alarm_volume, R.string.label_deskclock);

            final AlarmVolumeDialogFragment fragment = AlarmVolumeDialogFragment.newInstance(mAlarm.alarmVolume, mAlarm.alert);
            AlarmVolumeDialogFragment.show(getChildFragmentManager(), fragment);
        };

        mBinding.alarmVolumeLayout.setOnClickListener(openVolumeFragment);
    }

    private void bindDeleteButton() {
        mBinding.deleteButton.setTypeface(mGeneralTypeface);

        mBinding.deleteButton.setOnClickListener(v -> {
            mIsDeleted = true;
            Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncDeleteAlarm(mAlarm);
            dismiss();
        });
    }

    private void bindDuplicateButton() {
        mBinding.duplicateButton.setTypeface(mGeneralTypeface);

        mBinding.duplicateButton.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_duplicate, R.string.label_deskclock);

            Alarm duplicatedAlarm = new Alarm(mAlarm);
            duplicatedAlarm.id = Alarm.INVALID_ID;
            duplicatedAlarm.instanceState = AlarmInstance.SILENT_STATE;

            mAlarmUpdateHandler.asyncAddAlarm(duplicatedAlarm);

            dismiss();
        });
    }

    // ********************
    // ** HELPER METHODS **
    // ********************

    private void setupFragmentResultListeners() {
        FragmentManager childFragmentManager = getChildFragmentManager();

        childFragmentManager.setFragmentResultListener(MaterialTimePickerDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                int h = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                applyTime(h, m, false);
            });

        childFragmentManager.setFragmentResultListener(SpinnerTimePickerDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                int h = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                applyTime(h, m, false);
            });

        childFragmentManager.setFragmentResultListener(AlarmDelayPickerDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                int h = bundle.getInt(AlarmDelayPickerDialogFragment.BUNDLE_KEY_HOURS);
                int m = bundle.getInt(AlarmDelayPickerDialogFragment.BUNDLE_KEY_MINUTES);
                applyDelay(h, m);
            });

        childFragmentManager.setFragmentResultListener(LabelDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.label = bundle.getString(LabelDialogFragment.RESULT_LABEL);
                mAlarm.syncByLabel = bundle.getBoolean(LabelDialogFragment.RESULT_SYNC, false);
                bindLabel();
            });

        childFragmentManager.setFragmentResultListener(VibrationPatternDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                String selectedPattern = bundle.getString(VibrationPatternDialogFragment.RESULT_PATTERN_KEY);
                if (selectedPattern != null) {
                    mAlarm.vibrationPattern = selectedPattern;
                    bindVibrationPattern();
                }
            });

        childFragmentManager.setFragmentResultListener(AutoSilenceDurationDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.autoSilenceDuration = bundle.getInt(AutoSilenceDurationDialogFragment.AUTO_SILENCE_DURATION_VALUE);
                bindAutoSilenceValue();
                bindMissedAlarmRepeatLimit();
                updateThirdGroup();
            });

        childFragmentManager.setFragmentResultListener(AlarmSnoozeDurationDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.snoozeDuration = bundle.getInt(AlarmSnoozeDurationDialogFragment.ALARM_SNOOZE_DURATION_VALUE);
                bindSnoozeDurationValue();
            });

        childFragmentManager.setFragmentResultListener(AlarmMissedRepeatLimitDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.missedAlarmRepeatLimit = bundle.getInt(AlarmMissedRepeatLimitDialogFragment.RESULT_MISSED_REPEAT_LIMIT);
                bindMissedAlarmRepeatLimit();
            });

        childFragmentManager.setFragmentResultListener(VolumeCrescendoDurationDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.crescendoDuration = bundle.getInt(VolumeCrescendoDurationDialogFragment.VOLUME_CRESCENDO_DURATION_VALUE);
                bindCrescendoDuration();
            });

        childFragmentManager.setFragmentResultListener(AlarmVolumeDialogFragment.REQUEST_KEY, this,
            (requestKey, bundle) -> {
                mAlarm.alarmVolume = bundle.getInt(AlarmVolumeDialogFragment.RESULT_VOLUME_VALUE);
                bindAlarmVolume();
            });
    }

    private void applyDelay(int hoursToAdd, int minutesToAdd) {
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.add(Calendar.HOUR_OF_DAY, hoursToAdd);
        alarmTime.add(Calendar.MINUTE, minutesToAdd);

        applyTime(alarmTime.get(Calendar.HOUR_OF_DAY), alarmTime.get(Calendar.MINUTE), true);
    }

    private void applyTime(int hour, int minute, boolean isFromDelay) {
        mAlarm.hour = hour;
        mAlarm.minutes = minute;

        if (isFromDelay) {
            mAlarm.daysOfWeek = Weekdays.fromBits(0);
        }

        Calendar currentCalendar = Calendar.getInstance();

        // Necessary when an existing alarm has been created in the past, and it is not enabled.
        // Even if the date is not specified, it is saved in AlarmInstance; we need to make
        // sure that the date is not in the past when changing time, in which case we reset
        // to the current date (an alarm cannot be scheduled in the past).
        // This is due to the change in the code made with commit : 6ac23cf.
        // Fix https://github.com/BlackyHawky/Clock/issues/299
        boolean mustResetDate = mAlarm.isDateInThePast() || (isFromDelay && mAlarm.isSpecifiedDate());

        if (mustResetDate) {
            mAlarm.year = currentCalendar.get(Calendar.YEAR);
            mAlarm.month = currentCalendar.get(Calendar.MONTH);
            mAlarm.day = currentCalendar.get(Calendar.DAY_OF_MONTH);

            bindSelectedDate();
        }

        if (isFromDelay) {
            bindDaysOfWeekButtons();
            bindDeleteOccasionalAlarmAfterUse();
        }

        bindClock();
    }

    private void updateDaysOfWeekButtonVisuals(MaterialButton dayButton, boolean isSelected) {
        final int backgroundColor = isSelected
            ? MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorTertiary, Color.BLACK)
            : Color.TRANSPARENT;

        final ColorStateList strokeColor = ColorStateList.valueOf(
            MaterialColors.getColor(requireContext(), isSelected
                ? com.google.android.material.R.attr.colorTertiary
                : com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
        );

        final int textColor = MaterialColors.getColor(requireContext(), isSelected
            ? android.R.attr.colorBackground
            : android.R.attr.textColorPrimary, Color.BLACK);

        dayButton.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        dayButton.setStrokeColor(strokeColor);
        dayButton.setTextColor(textColor);
    }

    private void clearSelectedDate(@StringRes int text) {
        mBinding.cancelScheduledAlarm.setVisibility(GONE);
        mBinding.scheduleAlarm.setText(getString(text));
    }

    private void saveAlarmSettings() {
        if (mIsDeleted || mAlarm == null || mOriginalAlarm == null) {
            return;
        }

        boolean timeChanged = mAlarm.hasTimeChanged(mOriginalAlarm);
        boolean minorFieldsChanged = mAlarm.hasMinorFieldsChanged(mOriginalAlarm);
        boolean isNewAlarmCreated = mIsNewAlarm && mAlarm.enabled;

        if (!timeChanged && !minorFieldsChanged) {
            if (isNewAlarmCreated) {
                mAlarmUpdateHandler.asyncUpdateAlarm(mAlarm, true, false);
            }
            return;
        }

        boolean updateWidgets = !Objects.equals(mAlarm.label, mOriginalAlarm.label);
        boolean minorUpdate = !timeChanged;
        boolean popToast = timeChanged || isNewAlarmCreated;

        if (timeChanged) {
            mAlarm.enabled = true;
        }

        mAlarmUpdateHandler.asyncUpdateAlarm(mAlarm, popToast, minorUpdate);

        if (isAdded()) {
            Bundle result = new Bundle();
            result.putLong(SCROLL_TO_ALARM_ID, mAlarm.id);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        }

        if (updateWidgets) {
            Context appContext = requireContext().getApplicationContext();

            if (WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(mPrefs) && WidgetDAO.isNextAlarmTitleDisplayedOnDigitalWidget(mPrefs)) {
                WidgetUtils.updateWidget(appContext, DigitalAppWidgetProvider.class);
            }

            WidgetUtils.updateWidget(appContext, NextAlarmAppWidgetProvider.class);
        }
    }

    private void applyExpressiveBackgroundsToGroup(View... views) {
        List<View> visibleViews = new ArrayList<>();
        for (View view : views) {
            if (view.getVisibility() == View.VISIBLE) {
                visibleViews.add(view);
            }
        }

        int totalCount = visibleViews.size();
        if (totalCount == 0) {
            return;
        }

        Integer backgroundColor = null;
        if (!SettingsDAO.isCardBackgroundDisplayed(mPrefs)) {
            backgroundColor = MaterialColors.getColor(
                requireContext(), com.google.android.material.R.attr.colorSurfaceContainerLowest, Color.BLACK);
        }

        for (int i = 0; i < totalCount; i++) {
            View view = visibleViews.get(i);

            Drawable cardBackground = ThemeUtils.expressiveCardBackgroundWithColor(requireContext(), i, totalCount, backgroundColor);

            view.setBackground(ThemeUtils.rippleDrawable(requireContext(), cardBackground));
        }
    }

    private void updateSecondGroup() {
        applyExpressiveBackgroundsToGroup(
            mBinding.vibrateOnOff,
            mBinding.vibrationPatternLayout,
            mBinding.flashOnOff,
            mBinding.deleteOccasionalAlarmAfterUse
        );
    }

    private void updateThirdGroup() {
        applyExpressiveBackgroundsToGroup(
            mBinding.autoSilenceDurationLayout,
            mBinding.snoozeDurationLayout,
            mBinding.missedAlarmRepeatLimitLayout,
            mBinding.crescendoDurationLayout,
            mBinding.alarmVolumeLayout
        );
    }

    private void updateAllGroupBackgrounds() {
        applyExpressiveBackgroundsToGroup(mBinding.scheduleAlarmLayout, mBinding.pauseAlarmLayout);

        applyExpressiveBackgroundsToGroup(mBinding.editLabel, mBinding.chooseRingtone);

        applyExpressiveBackgroundsToGroup(
            mBinding.vibrateOnOff,
            mBinding.vibrationPatternLayout,
            mBinding.flashOnOff,
            mBinding.deleteOccasionalAlarmAfterUse
        );

        applyExpressiveBackgroundsToGroup(
            mBinding.autoSilenceDurationLayout,
            mBinding.snoozeDurationLayout,
            mBinding.missedAlarmRepeatLimitLayout,
            mBinding.crescendoDurationLayout,
            mBinding.alarmVolumeLayout
        );
    }

    private void nullifyClickListeners(View... views) {
        mBinding.digitalClock.setOnLongClickListener(null);

        for (View view : views) {
            if (view != null) {
                view.setOnClickListener(null);
            }
        }
    }

}
