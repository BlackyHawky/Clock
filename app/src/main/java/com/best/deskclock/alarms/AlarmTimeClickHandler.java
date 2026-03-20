/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static android.media.AudioManager.STREAM_ALARM;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.SPINNER_TIME_PICKER_STYLE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.dialogfragment.AlarmDelayPickerDialogFragment;
import com.best.deskclock.dialogfragment.MaterialTimePickerDialogFragment;
import com.best.deskclock.dialogfragment.SpinnerTimePickerDialogFragment;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;

import java.util.Calendar;
import java.util.List;

/**
 * Click handler for an alarm time item.
 */
public final class AlarmTimeClickHandler {

    private static final String TAG = "AlarmTimeClickHandler";
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger(TAG);

    private final Fragment mFragment;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final AlarmUpdateHandler mAlarmUpdateHandler;
    private Alarm mSelectedAlarm;

    public AlarmTimeClickHandler(Fragment fragment, AlarmUpdateHandler alarmUpdateHandler) {
        mFragment = fragment;
        mContext = mFragment.requireContext();
        mPrefs = getDefaultSharedPreferences(mContext);
        mAlarmUpdateHandler = alarmUpdateHandler;
    }

    public Alarm getSelectedAlarm() {
        return mSelectedAlarm;
    }

    public void setSelectedAlarm(Alarm selectedAlarm) {
        mSelectedAlarm = selectedAlarm;
    }

    public void displayBottomSheetDialog(Alarm alarm) {
        AlarmEditBottomSheetFragment fragment = AlarmEditBottomSheetFragment.newInstance(alarm, alarm.id, mFragment.getTag());

        AlarmEditBottomSheetFragment.show(mFragment.getParentFragmentManager(), fragment);
        LOGGER.v("Opening BottomSheet to edit alarm: " + alarm.id);
    }

    public void setAlarmEnabled(Alarm alarm, boolean newState) {
        if (newState != alarm.enabled) {
            alarm.enabled = newState;
            // If the alarm is set for a specific date and that date is already in the past,
            // update it to the current date. An alarm cannot be scheduled in the past.
            fixAlarmDateIfPast(alarm);

            Events.sendAlarmEvent(newState ? R.string.action_enable : R.string.action_disable, R.string.label_deskclock);

            // When enabling a synchronized alarm, enable all alarms sharing the same label.
            if (alarm.syncByLabel && newState) {
                syncAlarmsWithSameLabel(alarm, true);
                mAlarmUpdateHandler.useSyncToastForLabel(alarm.label);
            }

            if (newState && mFragment instanceof AlarmFragment) {
                ((AlarmFragment) mFragment).setSmoothScrollStableId(alarm.id);
            }

            // Update the current alarm instance.
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, alarm.enabled, false);

            // When disabling a synchronized alarm, disable the entire group only if this alarm
            // is not currently firing or snoozed.
            if (alarm.syncByLabel && !newState) {
                AlarmInstance activeInstance = AlarmInstance.getFiredOrSnoozedInstanceForAlarm(mContext.getContentResolver(), alarm.id);

                // If the alarm is not active (neither firing nor snoozed),
                // propagate the disabled state to the whole group.
                if (activeInstance == null) {
                    syncAlarmsWithSameLabel(alarm, false);
                }
            }

            Utils.setVibrationTime(mContext, 50);
            LOGGER.d("Updating alarm enabled state to " + newState);
        }
    }

    /**
     * Synchronizes the enabled state of all alarms sharing the same label and
     * synchronization setting as the given source alarm.
     *
     * @param sourceAlarm the alarm whose label and sync settings define the group
     * @param newState    the enabled state to apply to all matching alarms
     */
    private void syncAlarmsWithSameLabel(Alarm sourceAlarm, boolean newState) {
        if (sourceAlarm.label == null || sourceAlarm.label.trim().isEmpty()) {
            // No label: nothing to synchronize
            return;
        }

        List<Alarm> alarms = Alarm.getAlarms(mContext.getContentResolver(), null);

        for (Alarm alarm : alarms) {
            if (alarm.id != sourceAlarm.id
                && sourceAlarm.label.equals(alarm.label)
                && sourceAlarm.syncByLabel == alarm.syncByLabel) {

                if (alarm.enabled != newState) {
                    alarm.enabled = newState;

                    fixAlarmDateIfPast(alarm);

                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, false);
                    LOGGER.d("Sync alarm " + alarm.id + " with label " + alarm.label);
                }
            }
        }
    }

    /**
     * Ensures that the alarm's scheduled date is not in the past.
     *
     * <p>If the alarm is configured for a specific calendar date and that date has
     * already passed, this method updates the alarm's year, month, and day fields
     * to the current date. This prevents the creation of alarm instances that
     * would immediately be considered expired.</p>
     *
     * @param alarm the alarm whose date should be validated and corrected
     */
    private void fixAlarmDateIfPast(Alarm alarm) {
        if (alarm.isDateInThePast()) {
            Calendar currentCalendar = Calendar.getInstance();
            alarm.year = currentCalendar.get(Calendar.YEAR);
            alarm.month = currentCalendar.get(Calendar.MONTH);
            alarm.day = currentCalendar.get(Calendar.DAY_OF_MONTH);
        }
    }

    public void dismissAlarmInstance(AlarmItemHolder itemHolder, AlarmInstance alarmInstance) {
        final Alarm alarm = itemHolder.item;

        // For occasional alarms, handle in the same way as the Delete button.
        if (alarm.isDeleteAfterUse()) {
            if (mFragment instanceof AlarmFragment) {
                ((AlarmFragment) mFragment).removeItem(itemHolder);
            }

            Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncDeleteAlarm(alarm);
            LOGGER.d("Deleting alarm.");
            return;
        }

        // Otherwise, standard behavior: disable the alarm.
        final Intent dismissIntent = AlarmStateManager.createStateChangeIntent(
            mContext, AlarmStateManager.ALARM_DISMISS_TAG, alarmInstance, AlarmInstance.PREDISMISSED_STATE);
        mContext.startService(dismissIntent);
        Utils.setVibrationTime(mContext, 50);
    }

    public void onClockClicked(Alarm alarm) {
        mSelectedAlarm = alarm;

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

        final AlarmDelayPickerDialogFragment fragment = AlarmDelayPickerDialogFragment.newInstance(0, 0);
        AlarmDelayPickerDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void showSpinnerTimePickerDialog(int hours, int minutes) {
        Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);

        final SpinnerTimePickerDialogFragment fragment = SpinnerTimePickerDialogFragment.newInstance(hours, minutes);
        SpinnerTimePickerDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }

    public void showMaterialTimePicker(int hours, int minutes) {
        FragmentManager fragmentManager = ((AppCompatActivity) mContext).getSupportFragmentManager();

        // Prevents opening the same dialog twice
        if (fragmentManager.findFragmentByTag(TAG) != null) {
            return;
        }

        Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);

        MaterialTimePickerDialogFragment.show(mContext, fragmentManager, TAG, hours, minutes, mPrefs);
    }

    public void setAlarm(int hour, int minute) {
        if (mSelectedAlarm == null) {
            mAlarmUpdateHandler.asyncAddAlarm(buildNewAlarm(hour, minute));
        } else {
            updateExistingAlarm(hour, minute, false);
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
            updateExistingAlarm(h, m, true);
        }
    }

    private Alarm buildNewAlarm(int hour, int minute) {
        final Alarm alarm = new Alarm();
        final AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        alarm.hour = hour;
        alarm.minutes = minute;
        alarm.syncByLabel = false;
        alarm.enabled = true;
        alarm.vibrate = SettingsDAO.areAlarmVibrationsEnabledByDefault(mPrefs);
        alarm.vibrationPattern = SettingsDAO.getVibrationPattern(mPrefs);
        alarm.flash = SettingsDAO.shouldTurnOnBackFlashForTriggeredAlarm(mPrefs);
        alarm.deleteAfterUse = SettingsDAO.isOccasionalAlarmDeletedByDefault(mPrefs);
        alarm.autoSilenceDuration = SettingsDAO.getAlarmTimeout(mPrefs);
        alarm.snoozeDuration = SettingsDAO.getSnoozeLength(mPrefs);
        alarm.missedAlarmRepeatLimit = SettingsDAO.getMissedAlarmRepeatLimit(mPrefs);
        alarm.crescendoDuration = SettingsDAO.getAlarmVolumeCrescendoDuration(mPrefs);
        alarm.alarmVolume = audioManager.getStreamVolume(STREAM_ALARM);

        return alarm;
    }

    private void updateExistingAlarm(int hour, int minute, boolean isFromDelay) {
        mSelectedAlarm.hour = hour;
        mSelectedAlarm.minutes = minute;

        if (isFromDelay) {
            mSelectedAlarm.daysOfWeek = Weekdays.fromBits(0);
        }

        Calendar currentCalendar = Calendar.getInstance();

        // Necessary when an existing alarm has been created in the past and it is not enabled.
        // Even if the date is not specified, it is saved in AlarmInstance; we need to make
        // sure that the date is not in the past when changing time, in which case we reset
        // to the current date (an alarm cannot be scheduled in the past).
        // This is due to the change in the code made with commit : 6ac23cf.
        // Fix https://github.com/BlackyHawky/Clock/issues/299
        boolean mustResetDate = mSelectedAlarm.isDateInThePast() || (isFromDelay && mSelectedAlarm.isSpecifiedDate());

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
