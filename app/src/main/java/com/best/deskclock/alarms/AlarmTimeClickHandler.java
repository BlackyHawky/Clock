/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static android.content.Context.AUDIO_SERVICE;
import static android.media.AudioManager.STREAM_ALARM;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.ALARM_SNOOZE_DURATION_DISABLED;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_SNOOZE_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_AUTO_SILENCE_DURATION;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_DATE_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_MISSED_ALARM_REPEAT_LIMIT;
import static com.best.deskclock.settings.PreferencesDefaultValues.SPINNER_DATE_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesDefaultValues.SPINNER_TIME_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_END_OF_RINGTONE;
import static com.best.deskclock.settings.PreferencesDefaultValues.TIMEOUT_NEVER;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.best.deskclock.AlarmClockFragment;
import com.best.deskclock.AutoSilenceDurationDialogFragment;
import com.best.deskclock.AlarmSnoozeDurationDialogFragment;
import com.best.deskclock.LabelDialogFragment;
import com.best.deskclock.R;
import com.best.deskclock.VolumeCrescendoDurationDialogFragment;
import com.best.deskclock.alarms.dataadapter.AlarmItemHolder;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Click handler for an alarm time item.
 */
public final class AlarmTimeClickHandler implements OnTimeSetListener {

    private static final String TAG = "AlarmTimeClickHandler";
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger(TAG);
    private static final String KEY_PREVIOUS_DAY_MAP = "previousDayMap";
    private final Fragment mFragment;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final AlarmUpdateHandler mAlarmUpdateHandler;
    private Alarm mSelectedAlarm;
    private Bundle mPreviousDaysOfWeekMap;

    public AlarmTimeClickHandler(Fragment fragment, Bundle savedState, AlarmUpdateHandler alarmUpdateHandler) {

        mFragment = fragment;
        mContext = mFragment.requireContext();
        mPrefs = getDefaultSharedPreferences(mContext);
        mAlarmUpdateHandler = alarmUpdateHandler;

        if (savedState != null) {
            mPreviousDaysOfWeekMap = savedState.getBundle(KEY_PREVIOUS_DAY_MAP);
        }
        if (mPreviousDaysOfWeekMap == null) {
            mPreviousDaysOfWeekMap = new Bundle();
        }
    }

    public void setSelectedAlarm(Alarm selectedAlarm) {
        mSelectedAlarm = selectedAlarm;
    }

    public void setAlarmEnabled(Alarm alarm, boolean newState) {
        if (newState != alarm.enabled) {
            alarm.enabled = newState;
            // Necessary when an alarm is set on a specific date and it is not enabled:
            // if the date is in the past we replace that date with the current date:
            // indeed, an alarm cannot be triggered in the past.
            if (alarm.isDateInThePast()) {
                Calendar currentCalendar = Calendar.getInstance();
                alarm.year = currentCalendar.get(Calendar.YEAR);
                alarm.month = currentCalendar.get(Calendar.MONTH);
                alarm.day = currentCalendar.get(Calendar.DAY_OF_MONTH);
            }
            Events.sendAlarmEvent(newState ? R.string.action_enable : R.string.action_disable, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, alarm.enabled, false);
            Utils.setVibrationTime(mContext, 50);
            LOGGER.d("Updating alarm enabled state to " + newState);
        }
    }

    public void setAlarmVibrationEnabled(Alarm alarm, boolean newState) {
        if (newState != alarm.vibrate) {
            alarm.vibrate = newState;
            Events.sendAlarmEvent(R.string.action_toggle_vibrate, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
            LOGGER.d("Updating vibrate state to " + newState);

            if (newState) {
                // Buzz the vibrator to preview the alarm firing behavior.
                Utils.setVibrationTime(mContext, 300);
            }
        }
    }

    public void setAlarmFlashEnabled(Alarm alarm, boolean newState) {
        if (newState != alarm.flash) {
            alarm.flash = newState;
            Events.sendAlarmEvent(R.string.action_toggle_flash, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
            LOGGER.d("Updating flash state to " + newState);
            Utils.setVibrationTime(mContext, 50);
        }
    }

    public void deleteOccasionalAlarmAfterUse(Alarm alarm, boolean newState) {
        if (newState != alarm.deleteAfterUse) {
            alarm.deleteAfterUse = newState;
            Events.sendAlarmEvent(R.string.action_delete_alarm_after_use, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
            LOGGER.d("Delete alarm after use state to " + newState);
            Utils.setVibrationTime(mContext, 50);
        }
    }

    public void setAutoSilenceDuration(Alarm alarm) {
        Events.sendAlarmEvent(R.string.action_set_auto_silence_duration, R.string.label_deskclock);
        int autoSilenceDuration = alarm.autoSilenceDuration;
        final AutoSilenceDurationDialogFragment fragment =
                AutoSilenceDurationDialogFragment.newInstance(alarm, autoSilenceDuration,
                        autoSilenceDuration == TIMEOUT_END_OF_RINGTONE,
                        autoSilenceDuration == TIMEOUT_NEVER, mFragment.getTag());
        AutoSilenceDurationDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void setSnoozeDuration(Alarm alarm) {
        Events.sendAlarmEvent(R.string.action_set_snooze_duration, R.string.label_deskclock);
        int snoozeDuration = alarm.snoozeDuration;
        final AlarmSnoozeDurationDialogFragment fragment =
                AlarmSnoozeDurationDialogFragment.newInstance(alarm, snoozeDuration,
                        snoozeDuration == ALARM_SNOOZE_DURATION_DISABLED, mFragment.getTag());
        AlarmSnoozeDurationDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void setMissedAlarmRepeatLimit(Alarm alarm) {
        Events.sendAlarmEvent(R.string.action_set_missed_alarm_repeat_limit, R.string.label_deskclock);
        int missedAlarmRepeatLimit = alarm.missedAlarmRepeatLimit;
        final AlarmMissedRepeatLimitDialogFragment fragment =
                AlarmMissedRepeatLimitDialogFragment.newInstance(alarm, missedAlarmRepeatLimit, mFragment.getTag());
        AlarmMissedRepeatLimitDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void setCrescendoDuration(Alarm alarm) {
        Events.sendAlarmEvent(R.string.action_set_crescendo_duration, R.string.label_deskclock);
        int crescendoDuration = alarm.crescendoDuration;
        final VolumeCrescendoDurationDialogFragment fragment =
                VolumeCrescendoDurationDialogFragment.newInstance(alarm, crescendoDuration,
                        crescendoDuration == DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION,
                        mFragment.getTag());
        VolumeCrescendoDurationDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void setAlarmVolume(Alarm alarm) {
        Events.sendAlarmEvent(R.string.action_set_alarm_volume, R.string.label_deskclock);
        final AlarmVolumeDialogFragment fragment =
                AlarmVolumeDialogFragment.newInstance(alarm, alarm.alarmVolume, mFragment.getTag());
        AlarmVolumeDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void setDayOfWeekEnabled(Alarm alarm, boolean checked, int index) {
        final Calendar now = Calendar.getInstance();
        final Calendar oldNextAlarmTime = alarm.getNextAlarmTime(now);

        // Reset date if a date is specified
        if (alarm.isSpecifiedDate()) {
            alarm.year = now.get(Calendar.YEAR);
            alarm.month = now.get(Calendar.MONTH);
            alarm.day = now.get(Calendar.DAY_OF_MONTH);
        }

        final int weekday = SettingsDAO.getWeekdayOrder(mPrefs).getCalendarDays().get(index);
        alarm.daysOfWeek = alarm.daysOfWeek.setBit(weekday, checked);

        // If the change altered the next scheduled alarm time, tell the user
        final Calendar newNextAlarmTime = alarm.getNextAlarmTime(now);
        final boolean popupToast = !oldNextAlarmTime.equals(newNextAlarmTime);
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, popupToast, false);

        Utils.setVibrationTime(mContext, 10);
    }

    public void dismissAlarmInstance(Alarm alarm, AlarmInstance alarmInstance) {
        final Intent dismissIntent = AlarmStateManager.createStateChangeIntent(mContext,
                AlarmStateManager.ALARM_DISMISS_TAG, alarmInstance, AlarmInstance.PREDISMISSED_STATE);
        mContext.startService(dismissIntent);
        mAlarmUpdateHandler.showPredismissToast(alarm, alarmInstance);
        Utils.setVibrationTime(mContext, 50);
    }

    public void onDeleteClicked(AlarmItemHolder itemHolder) {
        if (mFragment instanceof AlarmClockFragment) {
            ((AlarmClockFragment) mFragment).removeItem(itemHolder);
        }
        final Alarm alarm = itemHolder.item;
        Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);
        mAlarmUpdateHandler.asyncDeleteAlarm(alarm);
        LOGGER.d("Deleting alarm.");
    }

    public void onDuplicateClicked(AlarmItemHolder itemHolder) {
        final Alarm alarm = itemHolder.item;
        mAlarmUpdateHandler.asyncAddAlarm(alarm);
        LOGGER.d("Adding alarm.");
    }

    public void onRingtoneClicked(Alarm alarm) {
        mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_ringtone, R.string.label_deskclock);

        final Intent intent = RingtonePickerActivity.createAlarmRingtonePickerIntent(mContext, alarm);
        mContext.startActivity(intent);
    }

    public void onEditLabelClicked(Alarm alarm) {
        Events.sendAlarmEvent(R.string.action_set_label, R.string.label_deskclock);
        final LabelDialogFragment fragment = LabelDialogFragment.newInstance(alarm, alarm.label, mFragment.getTag());
        LabelDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void onClockClicked(Alarm alarm) {
        mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);
        if (SettingsDAO.getMaterialTimePickerStyle(mPrefs).equals(SPINNER_TIME_PICKER_STYLE)) {
            showSpinnerTimePickerDialog(alarm.hour, alarm.minutes);
        } else {
            showMaterialTimePicker(alarm.hour, alarm.minutes);
        }
    }

    public void onClockLongClicked(Alarm alarm) {
        mSelectedAlarm = alarm;
        showAlarmDelayPickerDialog();
    }

    public void showAlarmDelayPickerDialog() {
        Events.sendAlarmEvent(R.string.action_set_delay, R.string.label_deskclock);

        final AlarmDelayPickerDialogFragment fragment =
                AlarmDelayPickerDialogFragment.newInstance(0, 0);
        AlarmDelayPickerDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void showSpinnerTimePickerDialog(int hours, int minutes) {
        Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);

        final SpinnerTimePickerDialogFragment fragment = SpinnerTimePickerDialogFragment.newInstance(hours, minutes);
        SpinnerTimePickerDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void showMaterialTimePicker(int hours, int minutes) {
        Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);

        MaterialTimePickerDialog.show(mContext, ((AppCompatActivity) mContext).getSupportFragmentManager(),
                TAG, hours, minutes, mPrefs, this);
    }

    public void onDateClicked(Alarm alarm) {
        mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_date, R.string.label_deskclock);
        if (SettingsDAO.getMaterialDatePickerStyle(mPrefs).equals(SPINNER_DATE_PICKER_STYLE)) {
            showSpinnerDatePicker(alarm);
        } else {
            showMaterialDatePicker(alarm);
        }
    }

    public void showSpinnerDatePicker(Alarm alarm) {
        LayoutInflater inflater = mFragment.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.spinner_date_picker, null);

        DatePicker datePicker = dialogView.findViewById(R.id.spinner_date_picker);
        Calendar now = Calendar.getInstance();
        Calendar selectionDate = (Calendar) now.clone();
        Calendar minDate = (Calendar) now.clone();

        // Date selection and minimum date to display
        boolean timePassed = alarm.isTimeBeforeOrEqual(now);
        boolean isTomorrow = Alarm.isTomorrow(alarm, now);

        // Date not specified
        if (!alarm.isSpecifiedDate()) {
            // Case 1: today or tomorrow depending on isTomorrow()
            if (isTomorrow) {
                selectionDate.add(Calendar.DAY_OF_MONTH, 1);
                minDate.add(Calendar.DAY_OF_MONTH, 1);
            }
            // else: keep today as selection and minDate
        } else {
            // Alarm has specified date
            if (alarm.isDateInThePast() || alarm.isScheduledForToday(now)) {
                // Case 2.1: date in the past or today
                if (timePassed) {
                    selectionDate.add(Calendar.DAY_OF_MONTH, 1);
                    minDate.add(Calendar.DAY_OF_MONTH, 1);
                }
                // else: today is valid
            } else {
                // Case 2.2: future date
                selectionDate.set(alarm.year, alarm.month, alarm.day);

                if (timePassed) {
                    minDate.add(Calendar.DAY_OF_MONTH, 1);
                }
            }
        }

        datePicker.setMinDate(minDate.getTimeInMillis());

        datePicker.init(selectionDate.get(Calendar.YEAR), selectionDate.get(Calendar.MONTH),
                selectionDate.get(Calendar.DAY_OF_MONTH), null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(mContext, R.style.SpinnerDialogTheme);
        builder
                .setTitle(mContext.getString(R.string.date_picker_dialog_title))
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    int newYear = datePicker.getYear();
                    int newMonth = datePicker.getMonth();
                    int newDay = datePicker.getDayOfMonth();

                    onDateSet(newYear, newMonth, newDay, alarm.hour, alarm.minutes);
                })
                .setNegativeButton(android.R.string.cancel, null);

        builder.create().show();
    }

    public void showMaterialDatePicker(Alarm alarm) {
        String materialDatePickerStyle = SettingsDAO.getMaterialDatePickerStyle(mPrefs);
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();

        // Set date picker style
        builder.setInputMode(materialDatePickerStyle.equals(DEFAULT_DATE_PICKER_STYLE)
                ? MaterialDatePicker.INPUT_MODE_CALENDAR
                : MaterialDatePicker.INPUT_MODE_TEXT);

        Calendar now = Calendar.getInstance();
        Calendar selectionDate = (Calendar) now.clone();
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();

        // Date selection
        boolean timePassed = alarm.isTimeBeforeOrEqual(now);

        // Date not specified
        if (!alarm.isSpecifiedDate()) {
            // Case 1: today or tomorrow depending on isTomorrow()
            if (Alarm.isTomorrow(alarm, now)) {
                selectionDate.add(Calendar.DAY_OF_MONTH, 1);
            }
        } else {
            // Alarm has specified date
            if (alarm.isDateInThePast() || alarm.isScheduledForToday(now)) {
                // Case 2.1: Date in the past or today's date
                if (timePassed) {
                    selectionDate.add(Calendar.DAY_OF_MONTH, 1);
                }
            } else {
                // Case 2.2: Date in the future
                selectionDate.set(alarm.year, alarm.month, alarm.day);
            }
        }

        // Set validator depending on whether the alarm time has passed or not
        if (timePassed) {
            constraintsBuilder.setValidator(DateValidatorPointForward.from(now.getTimeInMillis()));
        } else {
            constraintsBuilder.setValidator(DateValidatorPointForward.now());
        }

        builder.setSelection(selectionDate.getTimeInMillis());
        builder.setCalendarConstraints(constraintsBuilder.build());

        MaterialDatePicker<Long> materialDatePicker = builder.build();

        materialDatePicker.show(((AppCompatActivity) mContext).getSupportFragmentManager(), TAG);

        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            // Selection contains the selected date as a timestamp (long)
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            onDateSet(year, month, dayOfMonth, alarm.hour, alarm.minutes);
        });
    }

    public void onDateSet(int year, int month, int day, int hourOfDay, int minute) {
        if (mSelectedAlarm != null) {
            // Disable days of the week if one or more are selected
            if (mSelectedAlarm.daysOfWeek.isRepeating()) {
                mSelectedAlarm.daysOfWeek = Weekdays.NONE;
            }
            mSelectedAlarm.year = year;
            mSelectedAlarm.month = month;
            mSelectedAlarm.day = day;
            mSelectedAlarm.hour = hourOfDay;
            mSelectedAlarm.minutes = minute;
            mAlarmUpdateHandler.asyncUpdateAlarm(mSelectedAlarm, true, false);
            mSelectedAlarm = null;
        }
    }

    public void onRemoveDateClicked(Alarm alarm) {
        alarm.year = Calendar.getInstance().get(Calendar.YEAR);
        alarm.month = Calendar.getInstance().get(Calendar.MONTH);
        alarm.day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, true, false);
    }

    @Override
    public void onTimeSet(int hourOfDay, int minute) {
        setAlarm(hourOfDay, minute);
    }

    public void setAlarm(int hour, int minute) {
        if (mSelectedAlarm == null) {
            mAlarmUpdateHandler.asyncAddAlarm(buildNewAlarm(hour, minute));
        } else {
            updateExistingAlarm(hour, minute, false, false);
        }
    }

    public void setAlarmWithDelay(int hour, int minute) {
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.add(Calendar.HOUR_OF_DAY, hour);
        alarmTime.add(Calendar.MINUTE, minute);

        int h = alarmTime.get(Calendar.HOUR_OF_DAY);
        int m = alarmTime.get(Calendar.MINUTE);

        if (mSelectedAlarm == null) {
            mAlarmUpdateHandler.asyncAddAlarm(buildNewAlarm(h, m));
        } else {
            updateExistingAlarm(h, m, true, true);
        }
    }

    private Alarm buildNewAlarm(int hour, int minute) {
        final Alarm alarm = new Alarm();
        final AudioManager audioManager = (AudioManager) mContext.getSystemService(AUDIO_SERVICE);

        alarm.hour = hour;
        alarm.minutes = minute;
        alarm.enabled = true;
        alarm.vibrate = SettingsDAO.areAlarmVibrationsEnabledByDefault(mPrefs);
        alarm.flash = SettingsDAO.shouldTurnOnBackFlashForTriggeredAlarm(mPrefs);
        alarm.deleteAfterUse = SettingsDAO.isOccasionalAlarmDeletedByDefault(mPrefs);
        alarm.autoSilenceDuration = SettingsDAO.isPerAlarmAutoSilenceEnabled(mPrefs)
                ? DEFAULT_AUTO_SILENCE_DURATION
                : SettingsDAO.getAlarmTimeout(mPrefs);
        alarm.snoozeDuration = SettingsDAO.isPerAlarmAutoSilenceEnabled(mPrefs)
                ? DEFAULT_ALARM_SNOOZE_DURATION
                : SettingsDAO.getSnoozeLength(mPrefs);
        alarm.missedAlarmRepeatLimit = SettingsDAO.isPerAlarmMissedRepeatLimitEnabled(mPrefs)
                ? Integer.parseInt(DEFAULT_MISSED_ALARM_REPEAT_LIMIT)
                : SettingsDAO.getMissedAlarmRepeatLimit(mPrefs);
        alarm.crescendoDuration = SettingsDAO.isPerAlarmCrescendoDurationEnabled(mPrefs)
                ? DEFAULT_ALARM_VOLUME_CRESCENDO_DURATION
                : SettingsDAO.getAlarmVolumeCrescendoDuration(mPrefs);
        alarm.alarmVolume = audioManager.getStreamVolume(STREAM_ALARM);

        return alarm;
    }

    private void updateExistingAlarm(int hour, int minute, boolean resetDaysOfWeek, boolean checkSpecifiedDate) {
        mSelectedAlarm.hour = hour;
        mSelectedAlarm.minutes = minute;

        if (resetDaysOfWeek) {
            mSelectedAlarm.daysOfWeek = Weekdays.fromBits(0);
        }

        Calendar currentCalendar = Calendar.getInstance();

        // Necessary when an existing alarm has been created in the past and it is not enabled.
        // Even if the date is not specified, it is saved in AlarmInstance; we need to make
        // sure that the date is not in the past when changing time, in which case we reset
        // to the current date (an alarm cannot be triggered in the past).
        // This is due to the change in the code made with commit : 6ac23cf.
        // Fix https://github.com/BlackyHawky/Clock/issues/299
        boolean mustResetDate = mSelectedAlarm.isDateInThePast() ||
                (checkSpecifiedDate && mSelectedAlarm.isSpecifiedDate());

        if (mustResetDate) {
            mSelectedAlarm.year = currentCalendar.get(Calendar.YEAR);
            mSelectedAlarm.month = currentCalendar.get(Calendar.MONTH);
            mSelectedAlarm.day = currentCalendar.get(Calendar.DAY_OF_MONTH);
        }

        mSelectedAlarm.enabled = true;

        mAlarmUpdateHandler.asyncUpdateAlarm(mSelectedAlarm, true, false);
        mSelectedAlarm = null;
    }

}
