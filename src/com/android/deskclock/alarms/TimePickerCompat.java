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

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.provider.Alarm;

import java.util.Calendar;

/**
 * Displays and handles callback for time picker UI.
 */
public final class TimePickerCompat {

    /**
     * Callback when a valid time is selected from UI.
     */
    public interface OnTimeSetListener {
        void processTimeSet(int hourOfDay, int minute);
    }

    // Tag for timer picker fragment in FragmentManager.
    private static final String FRAG_TAG_TIME_PICKER = "time_dialog";

    // Do not instantiate.
    private TimePickerCompat() {}

    /**
     * Pre-L implementation of timer picker UI.
     */
    public static class TimerPickerPreL extends TimePickerDialog {

        public static TimerPickerPreL newInstance(final Fragment targetFragment, final int hour,
                final int minutes, final boolean use24hourFormat) {
            final TimerPickerPreL dialog = new TimerPickerPreL();
            dialog.initialize(new OnTimeSetListener() {
                @Override
                public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
                    ((TimePickerCompat.OnTimeSetListener) targetFragment)
                            .processTimeSet(hourOfDay, minute);
                }
            }, hour, minutes, use24hourFormat);
            dialog.setTargetFragment(targetFragment, 0);
            dialog.setThemeDark(true);
            return dialog;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            if (getTargetFragment() instanceof TimePickerCompat.OnTimeSetListener) {
                setOnTimeSetListener(new OnTimeSetListener() {
                    @Override
                    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
                        ((TimePickerCompat.OnTimeSetListener) getTargetFragment())
                                .processTimeSet(hourOfDay, minute);
                    }
                });
            }
        }
    }

    /**
     * Post-L implmenetation of timer picker UI.
     */
    public static class TimePickerPostL extends DialogFragment {

        private Alarm mAlarm;
        private android.app.TimePickerDialog.OnTimeSetListener mListener;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int hour, minute;
            if (mAlarm == null) {
                final Calendar c = Calendar.getInstance();
                hour = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);
            } else {
                hour = mAlarm.hour;
                minute = mAlarm.minutes;
            }

            return new android.app.TimePickerDialog(getActivity(), R.style.TimePickerTheme,
                    mListener, hour, minute, DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            if (getTargetFragment() instanceof OnTimeSetListener) {
                setOnTimeSetListener((OnTimeSetListener) getTargetFragment());
            }
        }

        public void setOnTimeSetListener(final OnTimeSetListener listener) {
            mListener = new android.app.TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    listener.processTimeSet(hourOfDay, minute);
                }
            };
        }

        public void setAlarm(Alarm alarm) {
            mAlarm = alarm;
        }
    }

    /**
     * Show the time picker dialog for post-L devices.
     * This is called from AlarmClockFragment to set alarm.
     *
     * @param targetFragment  The calling fragment (which is also a {@link OnTimeSetListener}),
     *                        we use it as the target fragment of the time picker fragment, so later
     *                        the latter can retrieve it and set it as its onTimeSetListener when
     *                        the fragment is recreated.
     * @param alarm           The clicked alarm, it can be null if user was clicking the fab.
     * @param use24hourFormat Whether or not the time picker should use 24-hour format if supported.
     */
    public static void showTimeEditDialog(Fragment targetFragment, Alarm alarm,
            boolean use24hourFormat) {
        // Make sure the dialog isn't already added.
        final FragmentManager manager = targetFragment.getFragmentManager();
        final FragmentTransaction ft = manager.beginTransaction();
        final Fragment prev = manager.findFragmentByTag(FRAG_TAG_TIME_PICKER);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.commit();

        if (Utils.isLOrLater()) {
            final TimePickerPostL picker = new TimePickerPostL();
            picker.setTargetFragment(targetFragment, 0);
            picker.setOnTimeSetListener((OnTimeSetListener) targetFragment);
            picker.setAlarm(alarm);
            picker.show(manager, FRAG_TAG_TIME_PICKER);
        } else {
            final int hour, minutes;
            if (alarm == null) {
                hour = 0;
                minutes = 0;
            } else {
                hour = alarm.hour;
                minutes = alarm.minutes;
            }
            TimerPickerPreL picker = TimerPickerPreL.newInstance(targetFragment, hour,
                    minutes, use24hourFormat);
            if (!picker.isAdded()) {
                picker.show(manager, FRAG_TAG_TIME_PICKER);
            }
        }
    }
}
