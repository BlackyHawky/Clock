/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static com.best.deskclock.settings.AlarmSettingsActivity.MATERIAL_DATE_PICKER_CALENDAR_STYLE;
import static com.best.deskclock.settings.AlarmSettingsActivity.MATERIAL_TIME_PICKER_ANALOG_STYLE;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.best.deskclock.AlarmClockFragment;
import com.best.deskclock.LabelDialogFragment;
import com.best.deskclock.LogUtils;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.alarms.dataadapter.AlarmItemHolder;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Click handler for an alarm time item.
 */
public final class AlarmTimeClickHandler {

    private static final String TAG = "AlarmTimeClickHandler";
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger(TAG);
    private static final String KEY_PREVIOUS_DAY_MAP = "previousDayMap";
    private final Fragment mFragment;
    private final Context mContext;
    private final AlarmUpdateHandler mAlarmUpdateHandler;
    private Alarm mSelectedAlarm;
    private Bundle mPreviousDaysOfWeekMap;

    public AlarmTimeClickHandler(Fragment fragment, Bundle savedState, AlarmUpdateHandler alarmUpdateHandler) {

        mFragment = fragment;
        mContext = mFragment.requireActivity().getApplicationContext();
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
            // Necessary when an alarm is set on a specific date and it is not activated:
            // If the date is in the past we replace that date with the current date:
            // indeed, an alarm cannot be triggered in the past.
            if (alarm.isDateSpecifiedInThePast()) {
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

    public void setDismissAlarmWhenRingtoneEndsEnabled(Alarm alarm, boolean newState) {
        if (newState != alarm.dismissAlarmWhenRingtoneEnds) {
            alarm.dismissAlarmWhenRingtoneEnds = newState;
            Events.sendAlarmEvent(R.string.action_toggle_dismiss_alarm_when_ringtone_ends, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
            LOGGER.d("Updating dismiss alarm state to " + newState);
            Utils.setVibrationTime(mContext, 50);
        }
    }

    public void setAlarmSnoozeActionsEnabled(Alarm alarm, boolean newState) {
        if (newState != alarm.alarmSnoozeActions) {
            alarm.alarmSnoozeActions = newState;
            Events.sendAlarmEvent(R.string.action_toggle_alarm_snooze_actions, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
            LOGGER.d("Updating snooze alarm state to " + newState);
            Utils.setVibrationTime(mContext, 50);
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

    public void deleteOccasionalAlarmAfterUse(Alarm alarm, boolean newState) {
        if (newState != alarm.deleteAfterUse) {
            alarm.deleteAfterUse = newState;
            Events.sendAlarmEvent(R.string.action_delete_alarm_after_use, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, false);
            LOGGER.d("Delete alarm after use state to " + newState);
            Utils.setVibrationTime(mContext, 50);
        }
    }

    public void setDayOfWeekEnabled(Alarm alarm, boolean checked, int index) {
        final Calendar now = Calendar.getInstance();
        final Calendar oldNextAlarmTime = alarm.getNextAlarmTime(now);

        final int weekday = DataModel.getDataModel().getWeekdayOrder().getCalendarDays().get(index);
        alarm.daysOfWeek = alarm.daysOfWeek.setBit(weekday, checked);

        // if the change altered the next scheduled alarm time, tell the user
        final Calendar newNextAlarmTime = alarm.getNextAlarmTime(now);
        final boolean popupToast = !oldNextAlarmTime.equals(newNextAlarmTime);
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, popupToast, false);

        Utils.setVibrationTime(mContext, 10);
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

    public void onClockClicked(Alarm alarm) {
        mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);
        ShowMaterialTimePicker(alarm.hour, alarm.minutes);
    }

    public void onDateClicked(Alarm alarm) {
        mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_date, R.string.label_deskclock);
        ShowMaterialDatePicker(alarm);
    }

    private void ShowMaterialTimePicker(int hour, int minute) {
        @TimeFormat int clockFormat;
        boolean isSystem24Hour = DateFormat.is24HourFormat(mFragment.getContext());
        clockFormat = isSystem24Hour ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;
        String materialTimePickerStyle = DataModel.getDataModel().getMaterialTimePickerStyle();

        MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setInputMode(materialTimePickerStyle.equals(MATERIAL_TIME_PICKER_ANALOG_STYLE)
                        ? MaterialTimePicker.INPUT_MODE_CLOCK
                        : MaterialTimePicker.INPUT_MODE_KEYBOARD)
                .setHour(hour)
                .setMinute(minute)
                .build();
        Context context = mFragment.requireContext();
        materialTimePicker.show(((AppCompatActivity) context).getSupportFragmentManager(), TAG);

        materialTimePicker.addOnPositiveButtonClickListener(dialog -> {
            int newHour = materialTimePicker.getHour();
            int newMinute = materialTimePicker.getMinute();
            onTimeSet(newHour, newMinute);
        });
    }

    public void ShowMaterialDatePicker(Alarm alarm) {
        String materialDatePickerStyle = DataModel.getDataModel().getMaterialDatePickerStyle();
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();

        // Set date picker style
        builder.setInputMode(materialDatePickerStyle.equals(MATERIAL_DATE_PICKER_CALENDAR_STYLE)
                ? MaterialDatePicker.INPUT_MODE_CALENDAR
                : MaterialDatePicker.INPUT_MODE_TEXT);

        // If a date has already been selected, select it when opening the MaterialDatePicker.
        if (alarm.isDateSpecified()) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.set(alarm.year, alarm.month, alarm.day);
            long date = calendar.getTimeInMillis();
            builder.setSelection(date);
        }

        // Do not allow selection of past dates.
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
        constraintsBuilder.setValidator(DateValidatorPointForward.now());
        builder.setCalendarConstraints(constraintsBuilder.build());
        MaterialDatePicker<Long> materialDatePicker = builder.build();

        Context context = mFragment.requireContext();
        materialDatePicker.show(((AppCompatActivity) context).getSupportFragmentManager(), TAG);
        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            // Selection contains the selected date as a timestamp (long)
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selection);

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            onDateSet(year, month, dayOfMonth, alarm.hour, alarm.minutes);
        });
    }

    public void onRingtoneClicked(Context context, Alarm alarm) {
        mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_ringtone, R.string.label_deskclock);

        final Intent intent = RingtonePickerActivity.createAlarmRingtonePickerIntent(context, alarm);
        context.startActivity(intent);
    }

    public void onEditLabelClicked(Alarm alarm) {
        Events.sendAlarmEvent(R.string.action_set_label, R.string.label_deskclock);
        final LabelDialogFragment fragment = LabelDialogFragment.newInstance(alarm, alarm.label, mFragment.getTag());
        LabelDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void onTimeSet(int hourOfDay, int minute) {
        if (mSelectedAlarm == null) {
            // If mSelectedAlarm is null then we're creating a new alarm.
            final Alarm alarm = new Alarm();
            final boolean areAlarmVibrationsEnabledByDefault = DataModel.getDataModel().areAlarmVibrationsEnabledByDefault();
            final boolean isOccasionalAlarmDeletedByDefault = DataModel.getDataModel().isOccasionalAlarmDeletedByDefault();
            alarm.hour = hourOfDay;
            alarm.minutes = minute;
            alarm.enabled = true;
            alarm.dismissAlarmWhenRingtoneEnds = false;
            alarm.alarmSnoozeActions = true;
            alarm.vibrate = areAlarmVibrationsEnabledByDefault;
            alarm.deleteAfterUse = isOccasionalAlarmDeletedByDefault;
            mAlarmUpdateHandler.asyncAddAlarm(alarm);
        } else {
            mSelectedAlarm.hour = hourOfDay;
            mSelectedAlarm.minutes = minute;
            mSelectedAlarm.enabled = true;
            mAlarmUpdateHandler.asyncUpdateAlarm(mSelectedAlarm, true, false);
            mSelectedAlarm = null;
        }
    }

    public void onDateSet(int year, int month, int day, int hourOfDay, int minute) {
        if (mSelectedAlarm == null) {
            // If mSelectedAlarm is null then we're creating a new alarm.
            final Alarm alarm = new Alarm();
            final boolean areAlarmVibrationsEnabledByDefault = DataModel.getDataModel().areAlarmVibrationsEnabledByDefault();
            final boolean isOccasionalAlarmDeletedByDefault = DataModel.getDataModel().isOccasionalAlarmDeletedByDefault();
            alarm.year = year;
            alarm.month = month;
            alarm.day = day;
            alarm.hour = hourOfDay;
            alarm.minutes = minute;
            alarm.enabled = true;
            alarm.dismissAlarmWhenRingtoneEnds = false;
            alarm.alarmSnoozeActions = true;
            alarm.vibrate = areAlarmVibrationsEnabledByDefault;
            alarm.deleteAfterUse = isOccasionalAlarmDeletedByDefault;
            mAlarmUpdateHandler.asyncAddAlarm(alarm);
        } else {
            mSelectedAlarm.year = year;
            mSelectedAlarm.month = month;
            mSelectedAlarm.day = day;
            mSelectedAlarm.hour = hourOfDay;
            mSelectedAlarm.minutes = minute;
            mAlarmUpdateHandler.asyncUpdateAlarm(mSelectedAlarm, true, false);
            mSelectedAlarm = null;
        }
    }

    public void removeDate(Alarm alarm) {
        if (mSelectedAlarm == null) {
            alarm.year = Calendar.getInstance().get(Calendar.YEAR);
            alarm.month = Calendar.getInstance().get(Calendar.MONTH);
            alarm.day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, true, false);
        } else {
            mSelectedAlarm.year = Calendar.getInstance().get(Calendar.YEAR);
            mSelectedAlarm.month = Calendar.getInstance().get(Calendar.MONTH);
            mSelectedAlarm.day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
            mAlarmUpdateHandler.asyncUpdateAlarm(mSelectedAlarm, true, false);
            mSelectedAlarm = null;
        }
    }
}
