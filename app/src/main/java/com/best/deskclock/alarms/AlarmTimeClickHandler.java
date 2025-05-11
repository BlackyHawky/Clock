/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_DATE_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesDefaultValues.SPINNER_DATE_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesDefaultValues.SPINNER_TIME_PICKER_STYLE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.best.deskclock.AlarmClockFragment;
import com.best.deskclock.LabelDialogFragment;
import com.best.deskclock.R;
import com.best.deskclock.alarms.dataadapter.AlarmItemHolder;
import com.best.deskclock.data.SettingsDAO;
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

    public void setDayOfWeekEnabled(Alarm alarm, boolean checked, int index) {
        final Calendar now = Calendar.getInstance();
        final Calendar oldNextAlarmTime = alarm.getNextAlarmTime(now);

        final int weekday = SettingsDAO.getWeekdayOrder(getDefaultSharedPreferences(mContext)).getCalendarDays().get(index);
        alarm.daysOfWeek = alarm.daysOfWeek.setBit(weekday, checked);

        // if the change altered the next scheduled alarm time, tell the user
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

    public void onClockClicked(Alarm alarm) {
        mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);
        if (SettingsDAO.getMaterialTimePickerStyle(
                getDefaultSharedPreferences(mContext)).equals(SPINNER_TIME_PICKER_STYLE)) {
            showCustomSpinnerTimePicker(alarm.hour, alarm.minutes);
        } else {
            showMaterialTimePicker(alarm.hour, alarm.minutes);
        }
    }

    private void showCustomSpinnerTimePicker(int hour, int minutes) {
        CustomSpinnerTimePickerDialog.show(mFragment.requireContext(), mFragment, hour, minutes, this);
    }

    private void showMaterialTimePicker(int hour, int minutes) {
        Context context = mFragment.requireContext();
        MaterialTimePickerDialog.show(context, ((AppCompatActivity) context).getSupportFragmentManager(),
                TAG, hour, minutes, getDefaultSharedPreferences(mContext), this);
    }

    public void onDateClicked(Alarm alarm) {
        mSelectedAlarm = alarm;
        Events.sendAlarmEvent(R.string.action_set_date, R.string.label_deskclock);
        if (SettingsDAO.getMaterialDatePickerStyle(
                getDefaultSharedPreferences(mContext)).equals(SPINNER_DATE_PICKER_STYLE)) {
            showSpinnerDatePicker(alarm);
        } else {
            showMaterialDatePicker(alarm);
        }
    }

    public void showSpinnerDatePicker(Alarm alarm) {
        LayoutInflater inflater = mFragment.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.spinner_date_picker, null);
        final Context context = dialogView.getContext();

        DatePicker datePicker = dialogView.findViewById(R.id.spinner_date_picker);
        Calendar currentCalendar = Calendar.getInstance();
        long currentDateInMillis = currentCalendar.getTimeInMillis();

        // If the alarm is set for today or is still set in the past, and the time has already
        // passed, prevent today from being selected. Otherwise, allow the current day to be selected.
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        if (alarm.year == currentCalendar.get(Calendar.YEAR)
                && alarm.month == currentMonth
                && alarm.day == currentCalendar.get(Calendar.DAY_OF_MONTH)) {

            if (alarm.hour < currentCalendar.get(Calendar.HOUR_OF_DAY)
                    || (alarm.hour == currentCalendar.get(Calendar.HOUR_OF_DAY) && alarm.minutes < currentCalendar.get(Calendar.MINUTE))
                    || (alarm.hour == currentCalendar.get(Calendar.HOUR_OF_DAY) && alarm.minutes == currentCalendar.get(Calendar.MINUTE))) {
                currentCalendar.add(Calendar.DAY_OF_MONTH, 1);
                datePicker.setMinDate(currentCalendar.getTimeInMillis());
            } else {
                datePicker.setMinDate(currentDateInMillis);
            }
        } else {
            datePicker.setMinDate(currentDateInMillis);
        }

        datePicker.init(alarm.year, alarm.month, alarm.day, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.SpinnerDialogTheme);
        builder
                .setTitle(context.getString(R.string.date_picker_dialog_title))
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
        String materialDatePickerStyle = SettingsDAO.getMaterialDatePickerStyle(getDefaultSharedPreferences(mContext));
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();

        // Set date picker style
        builder.setInputMode(materialDatePickerStyle.equals(DEFAULT_DATE_PICKER_STYLE)
                ? MaterialDatePicker.INPUT_MODE_CALENDAR
                : MaterialDatePicker.INPUT_MODE_TEXT);

        // If a date has already been selected, select it when opening the MaterialDatePicker.
        if (alarm.isSpecifiedDate()) {
            Calendar currentCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            // If the date is in the past, select today's date.
            if (alarm.isDateInThePast()) {
                long currentDateInMillis = currentCalendar.getTimeInMillis();
                builder.setSelection(currentDateInMillis);
            } else {
                currentCalendar.set(alarm.year, alarm.month, alarm.day);
                long alarmDate = currentCalendar.getTimeInMillis();
                builder.setSelection(alarmDate);
            }
        }

        // If the alarm is set for today or is still set in the past, and the time has already
        // passed, prevent today from being selected. Otherwise, allow the current day to be selected.
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();

        Calendar currentTime = Calendar.getInstance();
        int currentMonth = currentTime.get(Calendar.MONTH);

        if (alarm.isDateInThePast()
                || (alarm.year == currentTime.get(Calendar.YEAR)
                && alarm.month == currentMonth
                && alarm.day == currentTime.get(Calendar.DAY_OF_MONTH))) {

            if (alarm.hour < currentTime.get(Calendar.HOUR_OF_DAY)
                    || (alarm.hour == currentTime.get(Calendar.HOUR_OF_DAY) && alarm.minutes < currentTime.get(Calendar.MINUTE))
                    || (alarm.hour == currentTime.get(Calendar.HOUR_OF_DAY) && alarm.minutes == currentTime.get(Calendar.MINUTE))) {
                constraintsBuilder.setValidator(DateValidatorPointForward.from(currentTime.getTimeInMillis()));
            } else {
                constraintsBuilder.setValidator(DateValidatorPointForward.now());
            }
        } else {
            constraintsBuilder.setValidator(DateValidatorPointForward.now());
        }

        // Don't display past months and years
        constraintsBuilder.setStart(currentTime.getTimeInMillis());

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

    public void onDateSet(int year, int month, int day, int hourOfDay, int minute) {
        if (mSelectedAlarm != null) {
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
        alarm.year = Calendar.getInstance().get(Calendar.YEAR);
        alarm.month = Calendar.getInstance().get(Calendar.MONTH);
        alarm.day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, true, false);
    }

    @Override
    public void onTimeSet(int hourOfDay, int minute) {
        if (mSelectedAlarm == null) {
            // If mSelectedAlarm is null then we're creating a new alarm.
            final Alarm alarm = new Alarm();
            final SharedPreferences prefs = getDefaultSharedPreferences(mContext);
            alarm.hour = hourOfDay;
            alarm.minutes = minute;
            alarm.enabled = true;
            alarm.dismissAlarmWhenRingtoneEnds = false;
            alarm.alarmSnoozeActions = true;
            alarm.vibrate = SettingsDAO.areAlarmVibrationsEnabledByDefault(prefs);
            alarm.flash = SettingsDAO.shouldTurnOnBackFlashForTriggeredAlarm(prefs);
            alarm.deleteAfterUse = SettingsDAO.isOccasionalAlarmDeletedByDefault(prefs);
            mAlarmUpdateHandler.asyncAddAlarm(alarm);
        } else {
            mSelectedAlarm.hour = hourOfDay;
            mSelectedAlarm.minutes = minute;
            mSelectedAlarm.enabled = true;
            mAlarmUpdateHandler.asyncUpdateAlarm(mSelectedAlarm, true, false);
            mSelectedAlarm = null;
        }
    }
}
