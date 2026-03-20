// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_DATE_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesDefaultValues.SPINNER_DATE_PICKER_STYLE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
import java.util.TimeZone;

public class DatePickerDialogFragment {

    private static final String TAG = "DatePickerDialog";
    private static AlertDialog mCurrentSpinnerDatePickerDialog = null;

    public interface OnDateSelectedListener {
        void onDateSet(int year, int month, int day, int hour, int minute);
    }

    public static void show(Context context, FragmentManager fragmentManager, SharedPreferences prefs, Alarm alarm,
                            OnDateSelectedListener listener) {

        if (SettingsDAO.getMaterialDatePickerStyle(prefs).equals(SPINNER_DATE_PICKER_STYLE)) {
            showSpinnerDatePicker(context, alarm, listener);
        } else {
            showMaterialDatePicker(fragmentManager, prefs, alarm, listener);
        }
    }

    private static void showSpinnerDatePicker(Context context, Alarm alarm, OnDateSelectedListener listener) {

        if (mCurrentSpinnerDatePickerDialog != null && mCurrentSpinnerDatePickerDialog.isShowing()) {
            return;
        }

        Events.sendAlarmEvent(R.string.action_set_date, R.string.label_deskclock);

        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(R.layout.spinner_date_picker, null);

        DatePicker datePicker = dialogView.findViewById(R.id.spinner_date_picker);
        Calendar now = Calendar.getInstance();
        Calendar selectionDate = (Calendar) now.clone();
        Calendar minDate = (Calendar) now.clone();

        // Date selection and minimum date to display
        boolean timePassed = alarm.isTimeBeforeOrEqual(now);
        boolean isTomorrow = alarm.isTomorrow(now);

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

        datePicker.init(selectionDate.get(Calendar.YEAR), selectionDate.get(Calendar.MONTH), selectionDate.get(Calendar.DAY_OF_MONTH), null);

        mCurrentSpinnerDatePickerDialog = CustomDialog.create(
            context,
            R.style.SpinnerDialogTheme,
            null,
            context.getString(R.string.date_picker_dialog_title),
            null,
            dialogView,
            context.getString(android.R.string.ok),
            (d, w) -> {
                int newYear = datePicker.getYear();
                int newMonth = datePicker.getMonth();
                int newDay = datePicker.getDayOfMonth();

                if (listener != null) {
                    listener.onDateSet(newYear, newMonth, newDay, alarm.hour, alarm.minutes);
                }
            },
            context.getString(android.R.string.cancel),
            null,
            null,
            null,
            null,
            CustomDialog.SoftInputMode.SHOW_KEYBOARD
        );

        mCurrentSpinnerDatePickerDialog.setOnDismissListener(dialog -> mCurrentSpinnerDatePickerDialog = null);

        mCurrentSpinnerDatePickerDialog.show();
    }

    private static void showMaterialDatePicker(FragmentManager fragmentManager, SharedPreferences prefs, Alarm alarm,
                                               OnDateSelectedListener listener) {

        if (fragmentManager.findFragmentByTag(TAG) != null) {
            return;
        }

        Events.sendAlarmEvent(R.string.action_set_date, R.string.label_deskclock);

        String materialDatePickerStyle = SettingsDAO.getMaterialDatePickerStyle(prefs);
        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();

        // Set date picker style
        builder.setInputMode(materialDatePickerStyle.equals(DEFAULT_DATE_PICKER_STYLE)
            ? MaterialDatePicker.INPUT_MODE_CALENDAR
            : MaterialDatePicker.INPUT_MODE_TEXT);

        Calendar now = Calendar.getInstance();
        Calendar selectionDate = (Calendar) now.clone();

        // Date selection
        boolean timePassed = alarm.isTimeBeforeOrEqual(now);

        // Date not specified
        if (!alarm.isSpecifiedDate()) {
            // Case 1: today or tomorrow depending on isTomorrow()
            if (alarm.isTomorrow(now)) {
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

        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();

        // Prevents navigation to past months
        constraintsBuilder.setStart(now.getTimeInMillis());

        // Set validator depending on whether the alarm time has passed or not
        if (timePassed) {
            constraintsBuilder.setValidator(DateValidatorPointForward.from(now.getTimeInMillis()));
        } else {
            constraintsBuilder.setValidator(DateValidatorPointForward.now());
        }

        builder.setSelection(selectionDate.getTimeInMillis());
        builder.setCalendarConstraints(constraintsBuilder.build());

        MaterialDatePicker<Long> materialDatePicker = builder.build();

        materialDatePicker.getViewLifecycleOwnerLiveData().observeForever(new Observer<>() {
            @Override
            public void onChanged(LifecycleOwner owner) {
                if (owner == null) {
                    return;
                }

                View root = materialDatePicker.getView();
                if (root == null) {
                    return;
                }

                Typeface generalFont = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));
                if (generalFont == null) {
                    materialDatePicker.getViewLifecycleOwnerLiveData().removeObserver(this);
                    return;
                }

                // OK button
                TextView ok = root.findViewById(com.google.android.material.R.id.confirm_button);
                if (ok != null) {
                    ok.setTypeface(generalFont);
                }

                // Cancel button
                TextView cancel = root.findViewById(com.google.android.material.R.id.cancel_button);
                if (cancel != null) {
                    cancel.setTypeface(generalFont);
                }

                // "Select date" title
                TextView title = root.findViewById(com.google.android.material.R.id.mtrl_picker_title_text);
                if (title != null) {
                    title.setTypeface(generalFont);
                }

                // Selection text (date in big letters)
                TextView headerSelection = root.findViewById(com.google.android.material.R.id.mtrl_picker_header_selection_text);
                if (headerSelection != null) {
                    headerSelection.setTypeface(generalFont);
                }

                // Unsuscribe
                materialDatePicker.getViewLifecycleOwnerLiveData().removeObserver(this);
            }
        });

        materialDatePicker.show(fragmentManager, TAG);

        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            // Selection contains the selected date as a timestamp (long)
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            if (listener != null) {
                listener.onDateSet(year, month, dayOfMonth, alarm.hour, alarm.minutes);
            }
        });
    }
}
