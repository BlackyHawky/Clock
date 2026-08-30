/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static com.best.deskclock.settings.PreferencesDefaultValues.SPINNER_TIME_PICKER_STYLE;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.dialogfragment.AlarmDelayPickerDialogFragment;
import com.best.deskclock.dialogfragment.MaterialTimePickerDialogFragment;
import com.best.deskclock.dialogfragment.SpinnerTimePickerDialogFragment;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.uidata.UiConfig;
import com.best.deskclock.utils.LogUtils;

import java.util.Calendar;

/**
 * Click handler for an alarm time item.
 */
public final class AlarmTimeClickHandler {

    public static final String TAG = "AlarmTimeClickHandler";
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger(TAG);

    public record Config(@NonNull String timePickerStyle, @NonNull UiConfig.Fonts fonts, int globalIntentId) {}

    private final AlarmFragment mAlarmFragment;
    private final Context mContext;
    private final AlarmUpdateHandler mAlarmUpdateHandler;
    private final Config mConfig;
    private final AlarmFactory mAlarmFactory;
    private Alarm mSelectedAlarm;

    public AlarmTimeClickHandler(@NonNull AlarmFragment alarmFragment, @NonNull AlarmUpdateHandler alarmUpdateHandler,
                                 @NonNull Config config, @NonNull AlarmFactory alarmFactory) {

        mAlarmFragment = alarmFragment;
        mContext = mAlarmFragment.requireContext();
        mAlarmUpdateHandler = alarmUpdateHandler;
        mConfig = config;
        mAlarmFactory = alarmFactory;
    }

    public Alarm getSelectedAlarm() {
        return mSelectedAlarm;
    }

    public void setSelectedAlarm(@Nullable Alarm selectedAlarm) {
        mSelectedAlarm = selectedAlarm;
    }

    public void displayBottomSheetDialog(@NonNull Alarm alarm, boolean isNewAlarm) {
        AlarmEditBottomSheetFragment fragment =
            AlarmEditBottomSheetFragment.newInstance(alarm, alarm.id, mAlarmFragment.getTag(), isNewAlarm);

        AlarmEditBottomSheetFragment.show(mAlarmFragment.getParentFragmentManager(), fragment);
        LOGGER.v("Opening BottomSheet to edit alarm: " + alarm.id);
    }

    public void setAlarmEnabled(@NonNull Alarm alarm, boolean newState) {
        if (newState != alarm.enabled) {
            alarm.enabled = newState;

            if (newState) {
                AlarmVisualCache.invalidate(alarm.id);
            }

            // If the alarm is set for a specific date and that date is already in the past,
            // update it to the current date. An alarm cannot be scheduled in the past.
            alarm.fixDateIfPast();

            Events.sendAlarmEvent(newState ? R.string.action_enable : R.string.action_disable, R.string.label_deskclock);

            // When enabling a synchronized alarm, enable all alarms sharing the same label.
            if (alarm.syncByLabel && newState) {
                mAlarmUpdateHandler.asyncSyncAlarmsWithSameLabel(alarm, true);
                mAlarmUpdateHandler.useSyncToastForLabel(alarm.label);
            }

            if (newState) {
                mAlarmFragment.setSmoothScrollStableId(alarm.id);
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
                    mAlarmUpdateHandler.asyncSyncAlarmsWithSameLabel(alarm, false);
                }
            }

            LOGGER.d("Updating alarm enabled state to " + newState);
        }
    }

    public void dismissAlarmInstance(@NonNull AlarmItemHolder itemHolder, @NonNull AlarmInstance alarmInstance) {
        final Alarm alarm = itemHolder.item;

        // For occasional alarms, handle in the same way as the Delete button.
        if (alarm.isDeleteAfterUse()) {
            mAlarmFragment.removeItem(itemHolder);

            Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);
            mAlarmUpdateHandler.asyncDeleteAlarm(alarm);
            LOGGER.d("Deleting alarm.");
            return;
        }

        // Otherwise, standard behavior: disable the alarm.
        final Intent dismissIntent = AlarmStateManager.createStateChangeIntent(
            mContext, alarmInstance,
            AlarmStateManager.ALARM_DISMISS_TAG,
            AlarmInstance.PREDISMISSED_STATE,
            mConfig.globalIntentId()
        );

        mContext.startService(dismissIntent);
    }

    public void onClockClicked(@NonNull Alarm alarm) {
        mSelectedAlarm = alarm;

        if (mConfig.timePickerStyle().equals(SPINNER_TIME_PICKER_STYLE)) {
            showSpinnerTimePickerDialog(alarm.hour, alarm.minutes);
        } else {
            showMaterialTimePicker(alarm.hour, alarm.minutes);
        }
    }

    public void onClockLongClicked(@NonNull Alarm alarm) {
        mSelectedAlarm = alarm;
        showAlarmDelayPickerDialog();
    }

    public void showAlarmDelayPickerDialog() {
        Events.sendAlarmEvent(R.string.action_set_delay, R.string.label_deskclock);

        final AlarmDelayPickerDialogFragment fragment = AlarmDelayPickerDialogFragment.newInstance(0, 0);
        AlarmDelayPickerDialogFragment.show(mAlarmFragment.getParentFragmentManager(), fragment);
    }

    public void showSpinnerTimePickerDialog(int hours, int minutes) {
        Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);

        final SpinnerTimePickerDialogFragment fragment = SpinnerTimePickerDialogFragment.newInstance(hours, minutes);
        SpinnerTimePickerDialogFragment.show(mAlarmFragment.getParentFragmentManager(), fragment);
    }

    public void showMaterialTimePicker(int hours, int minutes) {
        FragmentManager fragmentManager = ((AppCompatActivity) mContext).getSupportFragmentManager();

        // Prevents opening the same dialog twice
        if (fragmentManager.findFragmentByTag(TAG) != null) {
            return;
        }

        Events.sendAlarmEvent(R.string.action_set_time, R.string.label_deskclock);

        MaterialTimePickerDialogFragment.show(
            mContext,
            fragmentManager,
            TAG,
            hours,
            minutes,
            mConfig.timePickerStyle(),
            mConfig.fonts().alarmClockFont(),
            mConfig.fonts().general()
        );
    }

    public void setAlarm(int hour, int minute) {
        if (mSelectedAlarm == null) {
            Alarm newAlarm = mAlarmFactory.createDefaultAlarm(hour, minute);

            AlarmVisualCache.invalidate(newAlarm.id);

            mAlarmUpdateHandler.asyncAddAlarm(newAlarm, false, addedAlarm ->
                AppExecutors.getMainThread().post(() -> {
                    if (mAlarmFragment.isAdded()) {
                        mAlarmFragment.setPendingAlarmToEdit(addedAlarm);
                    }
                })
            );
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
            Alarm newAlarm = mAlarmFactory.createDefaultAlarm(h, m);

            AlarmVisualCache.invalidate(newAlarm.id);

            mAlarmUpdateHandler.asyncAddAlarm(newAlarm, false, addedAlarm ->
                AppExecutors.getMainThread().post(() -> {
                    if (mAlarmFragment.isAdded()) {
                        mAlarmFragment.setPendingAlarmToEdit(addedAlarm);
                    }
                })
            );
        } else {
            updateExistingAlarm(h, m, true);
        }
    }

    private void updateExistingAlarm(int hour, int minute, boolean isFromDelay) {
        mSelectedAlarm.hour = hour;
        mSelectedAlarm.minutes = minute;

        if (isFromDelay) {
            mSelectedAlarm.daysOfWeek = Weekdays.fromBits(0);
        }

        Calendar currentCalendar = Calendar.getInstance();

        // Necessary when an existing alarm has been created in the past, and it is not enabled.
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

        AlarmVisualCache.invalidate(mSelectedAlarm.id);

        mAlarmUpdateHandler.asyncUpdateAlarm(mSelectedAlarm, true, false);
        mSelectedAlarm = null;
    }

    @SuppressWarnings("unused")
    public interface AlarmFactory {
        Alarm createDefaultAlarm(int hour, int minute);
    }

}
