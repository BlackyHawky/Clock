// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import static android.app.Activity.RESULT_OK;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VOLUME_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.SPINNER_TIME_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_NEVER;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_ESCALATING;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_HEARTBEAT;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_SOFT;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_STRONG;
import static com.best.deskclock.settings.PreferencesDefaultValues.VIBRATION_PATTERN_TICK_TOCK;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.AppExecutors;
import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
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
import com.best.deskclock.uicomponents.TextTime;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.AlarmUtils;
import com.best.deskclock.utils.DeviceUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.materialyouwidgets.MaterialYouNextAlarmAppWidgetProvider;
import com.best.deskclock.widgets.standardwidgets.NextAlarmAppWidgetProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
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
    private static final String ARG_TAG = "arg_tag";
    public static final String SCROLL_TO_ALARM_ID = "scroll_to_alarm_id";
    public static final String REQUEST_KEY = "alarm_saved";

    private SharedPreferences mPrefs;
    private Typeface mGeneralTypeface;
    private Typeface mAlarmBoldTypeface;
    private Alarm mAlarm;
    private Alarm mOriginalAlarm;
    private AlarmUpdateHandler mAlarmUpdateHandler;
    private FragmentManager.FragmentLifecycleCallbacks mLifecycleCallbacks;
    private String mTag;
    private boolean mIsActionPending = false;

    private TextTime mClock;
    private MaterialButton mDuplicateButton;
    private MaterialButtonToggleGroup mRepeatDaysGroup;
    private TextView mSelectedDate;
    private TextView mScheduleAlarm;
    private TextView mLabel;
    private TextView mRingtone;
    private CheckBox mVibrate;
    private LinearLayout mVibrationPatternLayout;
    private TextView mVibrationPatternTitle;
    private TextView mVibrationPatternValue;
    private CheckBox mFlash;
    private CheckBox mDeleteOccasionalAlarmAfterUse;
    private LinearLayout mAutoSilenceDurationLayout;
    private TextView mAutoSilenceDurationTitle;
    private TextView mAutoSilenceDurationValue;
    private LinearLayout mSnoozeDurationLayout;
    private TextView mSnoozeDurationTitle;
    private TextView mSnoozeDurationValue;
    private LinearLayout mMissedAlarmRepeatLimitLayout;
    private TextView mMissedAlarmRepeatLimitTitle;
    private TextView mMissedAlarmRepeatLimitValue;
    private LinearLayout mCrescendoDurationLayout;
    private TextView mCrescendoDurationTitle;
    private TextView mCrescendoDurationValue;
    private LinearLayout mAlarmVolumeLayout;
    private TextView mAlarmVolumeTitle;
    private TextView mAlarmVolumeValue;
    private MaterialButton mDeleteButton;

    public static AlarmEditBottomSheetFragment newInstance(Alarm alarm, long alarmId, String tag) {

        final Bundle args = new Bundle();

        args.putParcelable(ARG_ALARM, alarm);
        args.putLong(ARG_ALARM_ID, alarmId);
        args.putString(ARG_TAG, tag);

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
        mPrefs = getDefaultSharedPreferences(requireContext());
        mGeneralTypeface = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(mPrefs));
        mAlarmBoldTypeface = ThemeUtils.boldTypeface(SettingsDAO.getAlarmFont(mPrefs));

        View rootView = requireActivity().findViewById(R.id.content);
        mAlarmUpdateHandler = new AlarmUpdateHandler(requireContext(), null, (ViewGroup) rootView);
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

        mLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentViewDestroyed(fm, f);

                if (f instanceof DialogFragment && f != AlarmEditBottomSheetFragment.this) {
                    AppExecutors.getMainThread().postDelayed(() -> setBottomSheetExpanded(), 350);
                }
            }
        };
        getChildFragmentManager().registerFragmentLifecycleCallbacks(mLifecycleCallbacks, false);

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

        @SuppressLint("InflateParams")
        View dialogView = getLayoutInflater().inflate(R.layout.alarm_edit_bottom_sheet, null);

        dialog.setContentView(dialogView);

        setupFragmentResultListeners();

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        mClock = dialogView.findViewById(R.id.digital_clock);
        mRepeatDaysGroup = dialogView.findViewById(R.id.repeat_days_group);
        mSelectedDate = dialogView.findViewById(R.id.selected_date);
        mScheduleAlarm = dialogView.findViewById(R.id.schedule_alarm);
        mLabel = dialogView.findViewById(R.id.edit_label);
        mRingtone = dialogView.findViewById(R.id.choose_ringtone);
        mVibrate = dialogView.findViewById(R.id.vibrate_onoff);
        mVibrationPatternLayout = dialogView.findViewById(R.id.vibration_pattern_layout);
        mVibrationPatternTitle = dialogView.findViewById(R.id.vibration_pattern_title);
        mVibrationPatternValue = dialogView.findViewById(R.id.vibration_pattern_value);
        mFlash = dialogView.findViewById(R.id.flash_onoff);
        mDeleteOccasionalAlarmAfterUse = dialogView.findViewById(R.id.delete_occasional_alarm_after_use);
        mAutoSilenceDurationLayout = dialogView.findViewById(R.id.auto_silence_duration_layout);
        mAutoSilenceDurationTitle = dialogView.findViewById(R.id.auto_silence_duration_title);
        mAutoSilenceDurationValue = dialogView.findViewById(R.id.auto_silence_duration_value);
        mSnoozeDurationLayout = dialogView.findViewById(R.id.snooze_duration_layout);
        mSnoozeDurationTitle = dialogView.findViewById(R.id.snooze_duration_title);
        mSnoozeDurationValue = dialogView.findViewById(R.id.snooze_duration_value);
        mMissedAlarmRepeatLimitLayout = dialogView.findViewById(R.id.missed_alarm_repeat_limit_layout);
        mMissedAlarmRepeatLimitTitle = dialogView.findViewById(R.id.missed_alarm_repeat_limit_title);
        mMissedAlarmRepeatLimitValue = dialogView.findViewById(R.id.missed_alarm_repeat_limit_value);
        mCrescendoDurationLayout = dialogView.findViewById(R.id.crescendo_duration_layout);
        mCrescendoDurationTitle = dialogView.findViewById(R.id.crescendo_duration_title);
        mCrescendoDurationValue = dialogView.findViewById(R.id.crescendo_duration_value);
        mAlarmVolumeLayout = dialogView.findViewById(R.id.alarm_volume_layout);
        mAlarmVolumeTitle = dialogView.findViewById(R.id.alarm_volume_title);
        mAlarmVolumeValue = dialogView.findViewById(R.id.alarm_volume_value);
        mDeleteButton = dialogView.findViewById(R.id.delete);
        mDuplicateButton = dialogView.findViewById(R.id.duplicate);

        bindClock();
        bindDaysOfWeekButtons();
        bindSelectedDate();
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

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        saveAlarmSettings();


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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mLifecycleCallbacks != null) {
            getChildFragmentManager().unregisterFragmentLifecycleCallbacks(mLifecycleCallbacks);
        }
    }

    private void bindClock() {
        applyRipplePillBackground(mClock);
        mClock.setTime(mAlarm.hour, mAlarm.minutes);
        mClock.setTypeface(mAlarmBoldTypeface);

        mClock.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);

            if (SettingsDAO.getMaterialTimePickerStyle(mPrefs).equals(SPINNER_TIME_PICKER_STYLE)) {
                final SpinnerTimePickerDialogFragment fragment =
                        SpinnerTimePickerDialogFragment.newInstance(mAlarm.hour, mAlarm.minutes);

                hideBottomSheetAndRun(() ->
                        SpinnerTimePickerDialogFragment.show(getChildFragmentManager(), fragment));
            } else {
                hideBottomSheetAndRun(() ->
                        MaterialTimePickerDialogFragment.show(requireContext(), getChildFragmentManager(), TAG,
                                mAlarm.hour, mAlarm.minutes, mPrefs));
            }
        });

        mClock.setOnLongClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_delay, R.string.label_deskclock);

            final AlarmDelayPickerDialogFragment fragment =
                    AlarmDelayPickerDialogFragment.newInstance(0, 0);

            hideBottomSheetAndRun(() ->
                    AlarmDelayPickerDialogFragment.show(getChildFragmentManager(), fragment));

            return true;
        });
    }

    private void bindDaysOfWeekButtons() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        List<Integer> weekdays = SettingsDAO.getWeekdayOrder(mPrefs).getCalendarDays();

        mRepeatDaysGroup.removeAllViews();

        final MaterialButton[] dayButtons = new MaterialButton[7];

        for (int i = 0; i < 7; i++) {
            MaterialButton dayButton = (MaterialButton) inflater.inflate(R.layout.day_button, mRepeatDaysGroup, false);
            int weekday = weekdays.get(i);

            dayButton.setId(View.generateViewId());
            dayButton.setTypeface(mGeneralTypeface);
            dayButton.setText(UiDataModel.getUiDataModel().getShortWeekday(weekday));
            dayButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(weekday));

            mRepeatDaysGroup.addView(dayButton);
            dayButtons[i] = dayButton;

            boolean isChecked = mAlarm.daysOfWeek.isBitOn(weekday);

            if (isChecked) {
                mRepeatDaysGroup.check(dayButton.getId());
            }

            updateDaysOfWeekButtonVisuals(dayButtons[i], isChecked);
        }

        mRepeatDaysGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            for (int i = 0; i < dayButtons.length; i++) {
                if (dayButtons[i].getId() == checkedId) {
                    int weekday = weekdays.get(i);
                    mAlarm.daysOfWeek = mAlarm.daysOfWeek.setBit(weekday, isChecked);
                    updateDaysOfWeekButtonVisuals(dayButtons[i], isChecked);

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
                    bindDeleteOccasionalAlarmAfterUse();
                    updateSecondGroup();
                    Utils.setVibrationTime(requireContext(), 10);
                    break;
                }
            }
        });
    }

    private void bindSelectedDate() {
        applyRipplePillBackground(mScheduleAlarm);
        mScheduleAlarm.setTypeface(mGeneralTypeface);

        int openCalendarText = R.string.schedule_alarm_title;

        View.OnClickListener openCalendarListener = v -> hideBottomSheetAndRun(() ->
                DatePickerDialogFragment.show(
                        requireContext(),
                        getChildFragmentManager(),
                        mPrefs,
                        mAlarm,
                        (year, month, day, hour, minute) -> {
                            if (mAlarm.daysOfWeek.isRepeating()) {
                                mAlarm.daysOfWeek = Weekdays.NONE;
                            }
                            mAlarm.year = year;
                            mAlarm.month = month;
                            mAlarm.day = day;
                            mAlarm.hour = hour;
                            mAlarm.minutes = minute;

                            bindSelectedDate();
                            bindDaysOfWeekButtons();
                            bindDeleteOccasionalAlarmAfterUse();
                        }
                ));

        View.OnClickListener removeDateListener = v -> {
            Calendar now = Calendar.getInstance();
            mAlarm.year = now.get(Calendar.YEAR);
            mAlarm.month = now.get(Calendar.MONTH);
            mAlarm.day = now.get(Calendar.DAY_OF_MONTH);

            bindSelectedDate();
        };

        if (mAlarm.daysOfWeek.isRepeating()) {
            clearSelectedDate(openCalendarListener, openCalendarText);
        } else if (mAlarm.isSpecifiedDate()) {
            if (mAlarm.isDateInThePast()) {
                clearSelectedDate(openCalendarListener, openCalendarText);
            } else {
                applyRipplePillBackground(mSelectedDate);
                mSelectedDate.setTypeface(mGeneralTypeface);
                mSelectedDate.setText(AlarmUtils.formatAlarmDate(mAlarm));
                mSelectedDate.setOnClickListener(openCalendarListener);
                mSelectedDate.setVisibility(VISIBLE);
                mScheduleAlarm.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(
                        requireContext(), R.drawable.ic_calendar_cancel), null, null, null);
                mScheduleAlarm.setText(getString(android.R.string.cancel));
                mScheduleAlarm.setOnClickListener(removeDateListener);
            }
        } else {
            clearSelectedDate(openCalendarListener, openCalendarText);
        }
    }

    private void bindLabel() {
        final boolean alarmLabelIsEmpty = mAlarm.label == null || mAlarm.label.isEmpty();

        mLabel.setText(alarmLabelIsEmpty ? getString(R.string.add_label) : mAlarm.label);

        mLabel.setTypeface(mGeneralTypeface);

        mLabel.setContentDescription(alarmLabelIsEmpty
                ? getString(R.string.no_label_specified)
                : getString(R.string.label_description) + " " + mAlarm.label);

        mLabel.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_label, R.string.label_deskclock);

            final LabelDialogFragment fragment =
                    LabelDialogFragment.newInstance(mAlarm.label, mAlarm.syncByLabel);

            hideBottomSheetAndRun(() ->
                    LabelDialogFragment.show(getChildFragmentManager(), fragment));
        });
    }

    private void bindRingtone() {
        final String title = DataModel.getDataModel().getRingtoneTitle(mAlarm.alert);
        mRingtone.setText(title);
        mRingtone.setTypeface(mGeneralTypeface);

        final String description = getString(R.string.ringtone_description);
        mRingtone.setContentDescription(description + " " + title);

        final Drawable iconRingtone;
        if (RingtoneUtils.RINGTONE_SILENT.equals(mAlarm.alert)) {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_ringtone_silent);
        } else if (RingtoneUtils.isRandomRingtone(mAlarm.alert)
                || RingtoneUtils.isRandomCustomRingtone(mAlarm.alert)) {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_random);
        } else {
            iconRingtone = AppCompatResources.getDrawable(requireContext(), R.drawable.ic_ringtone);
        }

        mRingtone.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRingtone, null, null, null);

        mRingtone.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_set_ringtone, R.string.label_deskclock);
            final Intent intent = RingtonePickerActivity.createAlarmRingtonePickerIntent(requireContext(), mAlarm);
            mRingtonePickerLauncher.launch(intent);
        });
    }

    private void bindVibrator() {
        if (!DeviceUtils.hasVibrator(requireContext())) {
            mVibrate.setVisibility(GONE);
            mVibrationPatternLayout.setVisibility(GONE);
            return;
        }

        mVibrate.setTypeface(mGeneralTypeface);
        mVibrate.setVisibility(VISIBLE);

        mVibrate.setOnCheckedChangeListener(null);
        mVibrate.setChecked(mAlarm.vibrate);

        mVibrate.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
            mVibrationPatternLayout.setVisibility(GONE);
            return;
        }

        mVibrationPatternTitle.setTypeface(mGeneralTypeface);
        mVibrationPatternValue.setTypeface(mGeneralTypeface);
        mVibrationPatternLayout.setVisibility(VISIBLE);

        String vibrationPatternText = mAlarm.vibrationPattern;
        switch (vibrationPatternText) {
            case VIBRATION_PATTERN_SOFT ->
                    mVibrationPatternValue.setText(getString(R.string.vibration_pattern_soft));
            case VIBRATION_PATTERN_STRONG ->
                    mVibrationPatternValue.setText(getString(R.string.vibration_pattern_strong));
            case VIBRATION_PATTERN_HEARTBEAT ->
                    mVibrationPatternValue.setText(getString(R.string.vibration_pattern_heartbeat));
            case VIBRATION_PATTERN_ESCALATING ->
                    mVibrationPatternValue.setText(getString(R.string.vibration_pattern_escalating));
            case VIBRATION_PATTERN_TICK_TOCK ->
                    mVibrationPatternValue.setText(getString(R.string.vibration_pattern_tick_tock));
            default -> mVibrationPatternValue.setText(getString(R.string.label_default));
        }

        View.OnClickListener openVibrationPatternFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_vibration_pattern, R.string.label_deskclock);

            final VibrationPatternDialogFragment fragment =
                    VibrationPatternDialogFragment.newInstance(mAlarm.vibrationPattern);

            hideBottomSheetAndRun(() ->
                    VibrationPatternDialogFragment.show(getChildFragmentManager(), fragment));
        };

        mVibrationPatternLayout.setOnClickListener(openVibrationPatternFragment);
    }

    private void bindFlash() {
        if (!DeviceUtils.hasBackFlash(requireContext())) {
            mFlash.setVisibility(GONE);
            return;
        }

        mFlash.setTypeface(mGeneralTypeface);
        mFlash.setVisibility(VISIBLE);
        mFlash.setOnCheckedChangeListener(null);
        mFlash.setChecked(mAlarm.flash);
        mFlash.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Events.sendAlarmEvent(R.string.action_toggle_flash, R.string.label_deskclock);
            mAlarm.flash = isChecked;
            Utils.setVibrationTime(requireContext(), 50);
        });
    }

    private void bindDeleteOccasionalAlarmAfterUse() {
        if (mAlarm.daysOfWeek.isRepeating()) {
            mDeleteOccasionalAlarmAfterUse.setVisibility(GONE);
            return;
        }

        mDeleteOccasionalAlarmAfterUse.setTypeface(mGeneralTypeface);
        mDeleteOccasionalAlarmAfterUse.setVisibility(VISIBLE);
        mDeleteOccasionalAlarmAfterUse.setOnCheckedChangeListener(null);
        mDeleteOccasionalAlarmAfterUse.setChecked(mAlarm.deleteAfterUse);

        mDeleteOccasionalAlarmAfterUse.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mAlarm.deleteAfterUse = isChecked;
            Utils.setVibrationTime(requireContext(), 50);
        });
    }

    private void bindAutoSilenceValue() {
        if (SettingsDAO.isPerAlarmAutoSilenceDisabled(mPrefs)) {
            mAutoSilenceDurationLayout.setVisibility(GONE);
            return;
        }

        mAutoSilenceDurationTitle.setTypeface(mGeneralTypeface);
        mAutoSilenceDurationValue.setTypeface(mGeneralTypeface);

        int autoSilenceDuration = mAlarm.autoSilenceDuration;

        if (autoSilenceDuration == TIMEOUT_NEVER) {
            mAutoSilenceDurationValue.setText(getString(R.string.label_never));
        } else if (autoSilenceDuration == TIMEOUT_END_OF_RINGTONE) {
            mAutoSilenceDurationValue.setText(getString(R.string.auto_silence_end_of_ringtone));
        } else {
            int m = autoSilenceDuration / 60;
            int s = autoSilenceDuration % 60;

            if (m > 0 && s > 0) {
                String minutesString = getResources().getQuantityString(R.plurals.minutes_short, m, m);
                String secondsString = s + " " + getString(R.string.seconds_label);
                mAutoSilenceDurationValue.setText(String.format("%s %s", minutesString, secondsString));
            } else if (m > 0) {
                mAutoSilenceDurationValue.setText(getResources().getQuantityString(R.plurals.minutes_short, m, m));
            }else {
                String secondsString = s + " " + getString(R.string.seconds_label);
                mAutoSilenceDurationValue.setText(secondsString);
            }
        }

        mAutoSilenceDurationLayout.setVisibility(VISIBLE);

        View.OnClickListener openAutoSilenceDurationFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_auto_silence_duration, R.string.label_deskclock);

            final AutoSilenceDurationDialogFragment fragment =
                    AutoSilenceDurationDialogFragment.newInstance(mAlarm.autoSilenceDuration);

            hideBottomSheetAndRun(() ->
                    AutoSilenceDurationDialogFragment.show(getChildFragmentManager(), fragment));
        };

        mAutoSilenceDurationLayout.setOnClickListener(openAutoSilenceDurationFragment);
    }

    private void bindSnoozeDurationValue() {
        if (SettingsDAO.isPerAlarmSnoozeDurationDisabled(mPrefs)) {
            mSnoozeDurationLayout.setVisibility(GONE);
            return;
        }

        mSnoozeDurationTitle.setTypeface(mGeneralTypeface);
        mSnoozeDurationValue.setTypeface(mGeneralTypeface);

        int snoozeDuration = mAlarm.snoozeDuration;

        if (snoozeDuration == ALARM_SNOOZE_DURATION_DISABLED) {
            mSnoozeDurationValue.setText(getString(R.string.snooze_duration_none));
        } else {
            int h = snoozeDuration / 60;
            int m = snoozeDuration % 60;

            if (h > 0 && m > 0) {
                String hoursString = getResources().getQuantityString(R.plurals.hours_short, h, h);
                String minutesString = getResources().getQuantityString(R.plurals.minutes_short, m, m);
                mSnoozeDurationValue.setText(String.format("%s %s", hoursString, minutesString));
            } else if (h > 0) {
                mSnoozeDurationValue.setText(getResources().getQuantityString(R.plurals.hours_short, h, h));
            } else {
                mSnoozeDurationValue.setText(getResources().getQuantityString(R.plurals.minutes_short, m, m));
            }
        }

        mSnoozeDurationLayout.setVisibility(VISIBLE);

        View.OnClickListener openAlarmSnoozeDurationFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_snooze_duration, R.string.label_deskclock);

            final AlarmSnoozeDurationDialogFragment fragment =
                    AlarmSnoozeDurationDialogFragment.newInstance(mAlarm.snoozeDuration);

            hideBottomSheetAndRun(() ->
                    AlarmSnoozeDurationDialogFragment.show(getChildFragmentManager(), fragment));
        };

        mSnoozeDurationLayout.setOnClickListener(openAlarmSnoozeDurationFragment);
    }

    private void bindMissedAlarmRepeatLimit() {
        if (SettingsDAO.isPerAlarmMissedRepeatLimitDisabled(mPrefs)
                || mAlarm.autoSilenceDuration == TIMEOUT_NEVER) {
            mMissedAlarmRepeatLimitLayout.setVisibility(GONE);
            return;
        }

        mMissedAlarmRepeatLimitTitle.setTypeface(mGeneralTypeface);
        mMissedAlarmRepeatLimitValue.setTypeface(mGeneralTypeface);

        int missedAlarmRepeatLimit = mAlarm.missedAlarmRepeatLimit;

        switch (missedAlarmRepeatLimit) {
            case 0 ->
                    mMissedAlarmRepeatLimitValue.setText(getString(R.string.label_never));
            case 1 ->
                    mMissedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_1_time));
            case 3 ->
                    mMissedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_3_times));
            case 5 ->
                    mMissedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_5_times));
            case 10 ->
                    mMissedAlarmRepeatLimitValue.setText(getString(R.string.missed_alarm_repeat_limit_10_times));
            default -> mMissedAlarmRepeatLimitValue.setText(getString(R.string.label_indefinitely));
        }

        mMissedAlarmRepeatLimitLayout.setVisibility(VISIBLE);

        View.OnClickListener openAlarmMissedRepeatLimitFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_missed_alarm_repeat_limit, R.string.label_deskclock);

            final AlarmMissedRepeatLimitDialogFragment fragment =
                    AlarmMissedRepeatLimitDialogFragment.newInstance(mAlarm.missedAlarmRepeatLimit);

            hideBottomSheetAndRun(() ->
                    AlarmMissedRepeatLimitDialogFragment.show(getChildFragmentManager(), fragment));
        };

        mMissedAlarmRepeatLimitLayout.setOnClickListener(openAlarmMissedRepeatLimitFragment);
    }

    private void bindCrescendoDuration() {
        if (SettingsDAO.isPerAlarmCrescendoDurationDisabled(mPrefs)) {
            mCrescendoDurationLayout.setVisibility(GONE);
            return;
        }

        mCrescendoDurationTitle.setTypeface(mGeneralTypeface);
        mCrescendoDurationValue.setTypeface(mGeneralTypeface);

        int crescendoDuration = mAlarm.crescendoDuration;

        if (crescendoDuration == DEFAULT_VOLUME_CRESCENDO_DURATION) {
            mCrescendoDurationValue.setText(getString(R.string.label_off));
        } else {
            int m = crescendoDuration / 60;
            int s = crescendoDuration % 60;

            if (m > 0 && s > 0) {
                String minutesString = getResources().getQuantityString(R.plurals.minutes_short, m, m);
                String secondsString = s + " " + getString(R.string.seconds_label);
                mCrescendoDurationValue.setText(String.format("%s %s", minutesString, secondsString));
            } else if (m > 0) {
                mCrescendoDurationValue.setText(getResources().getQuantityString(R.plurals.minutes_short, m, m));
            } else {
                String secondsString = s + " " + getString(R.string.seconds_label);
                mCrescendoDurationValue.setText(secondsString);
            }
        }

        mCrescendoDurationLayout.setVisibility(VISIBLE);

        View.OnClickListener openVolumeCrescendoFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_crescendo_duration, R.string.label_deskclock);

            final VolumeCrescendoDurationDialogFragment fragment =
                    VolumeCrescendoDurationDialogFragment.newInstance(mAlarm.crescendoDuration);

            hideBottomSheetAndRun(() ->
                    VolumeCrescendoDurationDialogFragment.show(getChildFragmentManager(), fragment));
        };

        mCrescendoDurationLayout.setOnClickListener(openVolumeCrescendoFragment);
    }

    private void bindAlarmVolume() {
        if (!SettingsDAO.isPerAlarmVolumeEnabled(mPrefs)) {
            mAlarmVolumeLayout.setVisibility(GONE);
            return;
        }

        mAlarmVolumeTitle.setTypeface(mGeneralTypeface);
        mAlarmVolumeValue.setTypeface(mGeneralTypeface);

        final AudioManager audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);
        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        final int currentVolume = Math.min(mAlarm.alarmVolume, maxVolume);

        int volumePercent = (int) (((float) currentVolume / maxVolume) * 100);
        String formatted = String.format(Locale.getDefault(), "%d%%", volumePercent);
        mAlarmVolumeValue.setText(formatted);

        Drawable icon = AppCompatResources.getDrawable(requireContext(), volumePercent < 50
                ? R.drawable.ic_volume_down
                : R.drawable.ic_volume_up);

        if (icon != null) {
            mAlarmVolumeTitle.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }

        mAlarmVolumeLayout.setVisibility(VISIBLE);

        View.OnClickListener openVolumeFragment = v -> {
            Events.sendAlarmEvent(R.string.action_set_alarm_volume, R.string.label_deskclock);

            final AlarmVolumeDialogFragment fragment =
                    AlarmVolumeDialogFragment.newInstance(mAlarm.alarmVolume, mAlarm.alert);

            hideBottomSheetAndRun(() ->
                    AlarmVolumeDialogFragment.show(getChildFragmentManager(), fragment));
        };

        mAlarmVolumeLayout.setOnClickListener(openVolumeFragment);
    }

    private void bindDeleteButton() {
        mDeleteButton.setTypeface(mGeneralTypeface);

        mDeleteButton.setOnClickListener(v -> {
            Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncDeleteAlarm(mAlarm);
            dismiss();
        });
    }

    private void bindDuplicateButton() {
        mDuplicateButton.setTypeface(mGeneralTypeface);

        mDuplicateButton.setOnClickListener(v -> {
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

        childFragmentManager.setFragmentResultListener(
                MaterialTimePickerDialogFragment.REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    int h = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                    int m = bundle.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                    applyTime(h, m, false);
                }
        );

        childFragmentManager.setFragmentResultListener(
                SpinnerTimePickerDialogFragment.REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    int h = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                    int m = bundle.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                    applyTime(h, m, false);
                }
        );

        childFragmentManager.setFragmentResultListener(
                AlarmDelayPickerDialogFragment.REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    int h = bundle.getInt(AlarmDelayPickerDialogFragment.BUNDLE_KEY_HOURS);
                    int m = bundle.getInt(AlarmDelayPickerDialogFragment.BUNDLE_KEY_MINUTES);
                    applyDelay(h, m);
                }
        );

        childFragmentManager.setFragmentResultListener(
                LabelDialogFragment.REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    mAlarm.label = bundle.getString(LabelDialogFragment.RESULT_LABEL);
                    mAlarm.syncByLabel = bundle.getBoolean(LabelDialogFragment.RESULT_SYNC, false);
                    bindLabel();
                }
        );

        childFragmentManager.setFragmentResultListener(
                VibrationPatternDialogFragment.REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    String selectedPattern = bundle.getString(VibrationPatternDialogFragment.RESULT_PATTERN_KEY);
                    if (selectedPattern != null) {
                        mAlarm.vibrationPattern = selectedPattern;
                        bindVibrationPattern();
                    }
                }
        );

        childFragmentManager.setFragmentResultListener(
                AutoSilenceDurationDialogFragment.REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    mAlarm.autoSilenceDuration = bundle.getInt(AutoSilenceDurationDialogFragment.AUTO_SILENCE_DURATION_VALUE);
                    bindAutoSilenceValue();
                    bindMissedAlarmRepeatLimit();
                    updateThirdGroup();
                }
        );

        childFragmentManager.setFragmentResultListener(
                AlarmSnoozeDurationDialogFragment.REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    mAlarm.snoozeDuration = bundle.getInt(AlarmSnoozeDurationDialogFragment.ALARM_SNOOZE_DURATION_VALUE);
                    bindSnoozeDurationValue();
                }
        );

        childFragmentManager.setFragmentResultListener(
                AlarmMissedRepeatLimitDialogFragment.REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    mAlarm.missedAlarmRepeatLimit = bundle.getInt(AlarmMissedRepeatLimitDialogFragment.RESULT_MISSED_REPEAT_LIMIT);
                    bindMissedAlarmRepeatLimit();
                }
        );

        childFragmentManager.setFragmentResultListener(
                VolumeCrescendoDurationDialogFragment.REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    mAlarm.crescendoDuration = bundle.getInt(VolumeCrescendoDurationDialogFragment.VOLUME_CRESCENDO_DURATION_VALUE);
                    bindCrescendoDuration();
                }
        );

        childFragmentManager.setFragmentResultListener(
                AlarmVolumeDialogFragment.REQUEST_KEY, this,
                (requestKey, bundle) -> {
                    mAlarm.alarmVolume = bundle.getInt(AlarmVolumeDialogFragment.RESULT_VOLUME_VALUE);
                    bindAlarmVolume();
                }
        );
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

        // Necessary when an existing alarm has been created in the past and it is not enabled.
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
            updateSecondGroup();
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

    private void clearSelectedDate(View.OnClickListener listener, @StringRes int text) {
        mSelectedDate.setVisibility(GONE);
        mScheduleAlarm.setCompoundDrawablesWithIntrinsicBounds(AppCompatResources.getDrawable(
                requireContext(), R.drawable.ic_calendar_clock), null, null, null);
        mScheduleAlarm.setText(getString(text));
        mScheduleAlarm.setOnClickListener(listener);
    }

    private void saveAlarmSettings() {
        if (mAlarm == null || mOriginalAlarm == null) {
            return;
        }

        boolean timeChanged = mAlarm.hasTimeChanged(mOriginalAlarm);
        boolean minorFieldsChanged = mAlarm.hasMinorFieldsChanged(mOriginalAlarm);

        if (!timeChanged && !minorFieldsChanged) {
            return;
        }

        boolean updateWidgets = !Objects.equals(mAlarm.label, mOriginalAlarm.label);
        boolean minorUpdate = !timeChanged;

        if (timeChanged) {
            mAlarm.enabled = true;
        }

        mAlarmUpdateHandler.asyncUpdateAlarm(mAlarm, timeChanged, minorUpdate);

        if (isAdded()) {
            Bundle result = new Bundle();
            result.putLong(SCROLL_TO_ALARM_ID, mAlarm.id);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        }

        if (updateWidgets) {
            WidgetUtils.updateWidget(requireContext(), NextAlarmAppWidgetProvider.class);
            WidgetUtils.updateWidget(requireContext(), MaterialYouNextAlarmAppWidgetProvider.class);
        }
    }

    private void hideBottomSheetAndRun(Runnable actionToRun) {
        if (mIsActionPending) {
            return;
        }
        mIsActionPending = true;

        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog bottomSheetDialog) {
            bottomSheetDialog.setCancelable(false);
            bottomSheetDialog.setCanceledOnTouchOutside(false);

            BottomSheetBehavior<?> behavior = bottomSheetDialog.getBehavior();
            behavior.setDraggable(false);
            behavior.setPeekHeight(0);
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            AppExecutors.getMainThread().postDelayed(() -> {
                mIsActionPending = false;

                if (!isAdded() || getChildFragmentManager().isDestroyed()) {
                    return;
                }

                actionToRun.run();
            }, 350);
        } else {
            mIsActionPending = false;

            if (!isAdded() || getChildFragmentManager().isDestroyed()) {
                return;
            }
            actionToRun.run();
        }
    }

    private void setBottomSheetExpanded() {
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog bottomSheetDialog) {
            bottomSheetDialog.setCancelable(true);
            bottomSheetDialog.setCanceledOnTouchOutside(true);

            BottomSheetBehavior<?> behavior = bottomSheetDialog.getBehavior();
            behavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setDraggable(true);
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
            backgroundColor = MaterialColors.getColor(requireContext(),
                    com.google.android.material.R.attr.colorSurfaceContainerLowest, Color.BLACK);
        }

        for (int i = 0; i < totalCount; i++) {
            View view = visibleViews.get(i);

            Drawable cardBackground =
                    ThemeUtils.expressiveCardBackgroundWithColor(requireContext(), i, totalCount, backgroundColor);

            view.setBackground(ThemeUtils.rippleDrawable(requireContext(), cardBackground));
        }
    }

    private void updateSecondGroup() {
        applyExpressiveBackgroundsToGroup(
                mVibrate,
                mVibrationPatternLayout,
                mFlash,
                mDeleteOccasionalAlarmAfterUse
        );
    }

    private void updateThirdGroup() {
        applyExpressiveBackgroundsToGroup(
                mAutoSilenceDurationLayout,
                mSnoozeDurationLayout,
                mMissedAlarmRepeatLimitLayout,
                mCrescendoDurationLayout,
                mAlarmVolumeLayout
        );
    }

    private void updateAllGroupBackgrounds() {
        applyExpressiveBackgroundsToGroup(mLabel, mRingtone);

        applyExpressiveBackgroundsToGroup(
                mVibrate,
                mVibrationPatternLayout,
                mFlash,
                mDeleteOccasionalAlarmAfterUse
        );

        applyExpressiveBackgroundsToGroup(
                mAutoSilenceDurationLayout,
                mSnoozeDurationLayout,
                mMissedAlarmRepeatLimitLayout,
                mCrescendoDurationLayout,
                mAlarmVolumeLayout
        );
    }

    private void applyRipplePillBackground(View view) {
        view.setBackground(ThemeUtils.pillRippleDrawable(requireContext(), Color.TRANSPARENT));
    }

}
