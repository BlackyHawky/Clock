/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.alarms;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.format.DateFormat;

import com.android.deskclock.LabelDialogFragment;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.RingtonePickerDialogFragment;
import com.android.deskclock.alarms.utils.DayOrderUtils;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;

import java.util.Calendar;

/**
 * Click handler for an alarm time item.
 */
public final class AlarmTimeClickHandler {

    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("AlarmTimeClickHandler");

    private static final String KEY_PREVIOUS_DAY_MAP = "previousDayMap";
    private static final String RINGTONE_PICKER_FRAG_TAG = "ringtone_picker_dialog";

    private final Fragment mFragment;
    private final AlarmUpdateHandler mAlarmUpdateHandler;
    private final ScrollHandler mScrollHandler;

    private Alarm mSelectedAlarm;
    private Bundle mPreviousDaysOfWeekMap;
    private int[] mDayOrder;

    public AlarmTimeClickHandler(Fragment fragment, Bundle savedState,
            AlarmUpdateHandler alarmUpdateHandler, ScrollHandler smoothScrollController) {
        mFragment = fragment;
        mAlarmUpdateHandler = alarmUpdateHandler;
        mScrollHandler = smoothScrollController;
        if (savedState != null) {
            mPreviousDaysOfWeekMap = savedState.getBundle(KEY_PREVIOUS_DAY_MAP);
        }
        if (mPreviousDaysOfWeekMap == null) {
            mPreviousDaysOfWeekMap = new Bundle();
        }
        mDayOrder = DayOrderUtils.getDayOrder(fragment.getActivity());
    }

    public Alarm getSelectedAlarm() {
        return mSelectedAlarm;
    }

    public void setSelectedAlarm(Alarm selectedAlarm) {
        mSelectedAlarm = selectedAlarm;
    }

    public void saveInstance(Bundle outState) {
        outState.putBundle(KEY_PREVIOUS_DAY_MAP, mPreviousDaysOfWeekMap);
    }

    public void setAlarmEnabled(Alarm alarm, boolean newState) {
        if (newState != alarm.enabled) {
            alarm.enabled = newState;
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, alarm.enabled, false);
            LOGGER.d("Updating alarm enabled state to " + newState);
        }
    }

    public void setAlarmVibrationEnabled(Alarm alarm, boolean newState) {
        if (newState != alarm.vibrate) {
            alarm.vibrate = newState;
            mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
            LOGGER.d("Updating vibrate state to " + newState);

            if (newState) {
                // Buzz the vibrator to preview the alarm firing behavior.
                final Context context = mFragment.getActivity();
                final Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (v.hasVibrator()) {
                    v.vibrate(300);
                }
            }
        }
    }

    public void setAlarmRepeatEnabled(Alarm alarm, boolean isEnabled) {
        final Calendar now = Calendar.getInstance();
        final Calendar oldNextAlarmTime = alarm.getNextAlarmTime(now);
        final String alarmId = String.valueOf(alarm.id);
        if (isEnabled) {
            // Set all previously set days
            // or
            // Set all days if no previous.
            final int bitSet = mPreviousDaysOfWeekMap.getInt(alarmId);
            alarm.daysOfWeek.setBitSet(bitSet);
            if (!alarm.daysOfWeek.isRepeating()) {
                alarm.daysOfWeek.setDaysOfWeek(true, mDayOrder);
            }
        } else {
            // Remember the set days in case the user wants it back.
            final int bitSet = alarm.daysOfWeek.getBitSet();
            mPreviousDaysOfWeekMap.putInt(alarmId, bitSet);

            // Remove all repeat days
            alarm.daysOfWeek.clearAllDays();
        }

        // if the change altered the next scheduled alarm time, tell the user
        final Calendar newNextAlarmTime = alarm.getNextAlarmTime(now);
        final boolean popupToast = !oldNextAlarmTime.equals(newNextAlarmTime);

        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, popupToast, false);
    }

    public void setDayOfWeekEnabled(Alarm alarm, boolean checked, int index) {
        final Calendar now = Calendar.getInstance();
        final Calendar oldNextAlarmTime = alarm.getNextAlarmTime(now);
        alarm.daysOfWeek.setDaysOfWeek(checked, mDayOrder[index]);
        // if the change altered the next scheduled alarm time, tell the user
        final Calendar newNextAlarmTime = alarm.getNextAlarmTime(now);
        final boolean popupToast = !oldNextAlarmTime.equals(newNextAlarmTime);
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, popupToast, false);
    }

    public void onDeleteClicked(Alarm alarm) {
        mAlarmUpdateHandler.asyncDeleteAlarm(alarm);
        LOGGER.d("Deleting alarm.");
    }

    public void onClockClicked(Alarm alarm) {
        mSelectedAlarm = alarm;
        TimePickerCompat.showTimeEditDialog(mFragment, alarm,
                DateFormat.is24HourFormat(mFragment.getActivity()));
    }

    public void dismissAlarmInstance(AlarmInstance alarmInstance) {
        final Context context = mFragment.getActivity().getApplicationContext();
        final Intent dismissIntent = AlarmStateManager.createStateChangeIntent(
                context, AlarmStateManager.ALARM_DISMISS_TAG, alarmInstance,
                AlarmInstance.PREDISMISSED_STATE);
        context.startService(dismissIntent);
        mAlarmUpdateHandler.showPredismissToast(alarmInstance);
    }

    public void onRingtoneClicked(Alarm alarm) {
        mSelectedAlarm = alarm;
        final FragmentManager fragmentManager = mFragment.getChildFragmentManager();
        fragmentManager.executePendingTransactions();
        final FragmentTransaction ft = fragmentManager.beginTransaction();
        final Fragment prev = fragmentManager.findFragmentByTag(RINGTONE_PICKER_FRAG_TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        new RingtonePickerDialogFragment.Builder()
                .setTitle(R.string.alert)
                .setDefaultRingtoneTitle(R.string.default_alarm_ringtone_title)
                .setDefaultRingtoneUri(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setExistingRingtoneUri(alarm.alert)
                .show(ft, RINGTONE_PICKER_FRAG_TAG);
    }

    public void onEditLabelClicked(Alarm alarm) {
        final FragmentManager fragmentManager = mFragment.getChildFragmentManager();
        fragmentManager.executePendingTransactions();
        final FragmentTransaction ft = fragmentManager.beginTransaction();
        final Fragment prev = fragmentManager.findFragmentByTag("label_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        final LabelDialogFragment newFragment =
                LabelDialogFragment.newInstance(alarm, alarm.label, mFragment.getTag());
        newFragment.show(ft, "label_dialog");
    }

    public void processTimeSet(int hourOfDay, int minute) {
        if (mSelectedAlarm == null) {
            // If mSelectedAlarm is null then we're creating a new alarm.
            final Alarm a = new Alarm();
            a.hour = hourOfDay;
            a.minutes = minute;
            a.enabled = true;
            mAlarmUpdateHandler.asyncAddAlarm(a);
        } else {
            mSelectedAlarm.hour = hourOfDay;
            mSelectedAlarm.minutes = minute;
            mSelectedAlarm.enabled = true;
            mScrollHandler.setSmoothScrollStableId(mSelectedAlarm.id);
            mAlarmUpdateHandler.asyncUpdateAlarm(mSelectedAlarm, true, false);
            mSelectedAlarm = null;
        }
    }
}
