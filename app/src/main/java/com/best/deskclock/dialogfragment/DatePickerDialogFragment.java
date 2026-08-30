// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.dialogfragment;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_DATE_PICKER_STYLE;
import static com.best.deskclock.settings.PreferencesDefaultValues.SPINNER_DATE_PICKER_STYLE;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.best.deskclock.R;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uicomponents.RepeatingDayDecorator;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.Calendar;
import java.util.TimeZone;

public class DatePickerDialogFragment {

    public static final String TAG_DATE_PICKER = "DatePickerDialog";
    public static final String TAG_DATE_RANGE_PICKER = "DateRangePickerDialog";

    public static void show(@NonNull FragmentManager fragmentManager, @NonNull Alarm alarm, @NonNull String datePickerStyle,
                            int firstDayOfWeek, @NonNull Typeface generalFont, @NonNull OnDateSelectedListener listener) {

        if (datePickerStyle.equals(SPINNER_DATE_PICKER_STYLE)) {
            showSpinnerDatePicker(fragmentManager, alarm);
        } else {
            showMaterialDatePicker(fragmentManager, alarm, datePickerStyle, firstDayOfWeek, generalFont, listener);
        }
    }

    private static void showSpinnerDatePicker(@NonNull FragmentManager fragmentManager, @NonNull Alarm alarm) {
        if (fragmentManager.findFragmentByTag(SpinnerDatePickerDialogFragment.TAG) != null) {
            return;
        }

        Events.sendAlarmEvent(R.string.action_set_date, R.string.label_deskclock);

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

        SpinnerDatePickerDialogFragment fragment = SpinnerDatePickerDialogFragment.newInstance(
            minDate.getTimeInMillis(),
            selectionDate.get(Calendar.YEAR),
            selectionDate.get(Calendar.MONTH),
            selectionDate.get(Calendar.DAY_OF_MONTH)
        );

        SpinnerDatePickerDialogFragment.show(fragmentManager, fragment);
    }

    private static void showMaterialDatePicker(@NonNull FragmentManager fragmentManager, @NonNull Alarm alarm,
                                               @NonNull String datePickerStyle, int firstDayOfWeek, @Nullable Typeface generalFont,
                                               @Nullable OnDateSelectedListener listener) {

        if (fragmentManager.findFragmentByTag(TAG_DATE_PICKER) != null) {
            return;
        }

        Events.sendAlarmEvent(R.string.action_set_date, R.string.label_deskclock);

        MaterialDatePicker.Builder<Long> builder = MaterialDatePicker.Builder.datePicker();

        // Set date picker style
        builder.setInputMode(datePickerStyle.equals(DEFAULT_DATE_PICKER_STYLE)
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

        Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcNow.clear();
        utcNow.set(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        );

        Calendar utcSelection = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcSelection.clear();
        utcSelection.set(
            selectionDate.get(Calendar.YEAR),
            selectionDate.get(Calendar.MONTH),
            selectionDate.get(Calendar.DAY_OF_MONTH)
        );

        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();

        // Respect the "Start week on" setting or use the device's regional settings if the setting has never been changed
        constraintsBuilder.setFirstDayOfWeek(firstDayOfWeek);

        // Prevents navigation to past months
        constraintsBuilder.setStart(utcNow.getTimeInMillis());

        long minAllowedTimestamp;
        if (timePassed) {
            // The time has passed : "Today" is invalid, is must be set to "Tomorrow"
            Calendar utcTomorrow = (Calendar) utcNow.clone();
            utcTomorrow.add(Calendar.DAY_OF_MONTH, 1);
            minAllowedTimestamp = utcTomorrow.getTimeInMillis();
        } else {
            // The time hasn't passed: "Today" is still valid
            minAllowedTimestamp = utcNow.getTimeInMillis();
        }

        // Set validator depending on whether the alarm time has passed or not
        constraintsBuilder.setValidator(DateValidatorPointForward.from(minAllowedTimestamp));

        builder.setSelection(utcSelection.getTimeInMillis());
        builder.setCalendarConstraints(constraintsBuilder.build());

        MaterialDatePicker<Long> materialDatePicker = builder.build();

        applyCustomFontToDatePicker(materialDatePicker, generalFont);

        materialDatePicker.show(fragmentManager, TAG_DATE_PICKER);

        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            // Selection contains the selected date as a timestamp (long)
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(selection);

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            if (listener != null) {
                listener.onDateSet(year, month, dayOfMonth);
            }
        });
    }

    public static void showMaterialDateRangePicker(@NonNull FragmentManager fragmentManager, @NonNull Alarm alarm, int firstDayOfWeek,
                                                   @Nullable Typeface generalFont, @NonNull OnDateRangeSelectedListener listener) {

        if (fragmentManager.findFragmentByTag(TAG_DATE_RANGE_PICKER) != null) {
            return;
        }

        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();

        builder.setTheme(R.style.AppMaterialCalendarTheme);

        Calendar now = Calendar.getInstance();
        boolean timePassed = alarm.isTimeBeforeOrEqual(now);

        Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcNow.clear();
        utcNow.set(
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        );

        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();

        // Respect the "Start week on" setting or use the device's regional settings if the setting has never been changed.
        constraintsBuilder.setFirstDayOfWeek(firstDayOfWeek);

        long minAllowedTimestamp;
        if (timePassed) {
            // The time has passed : "Today" is invalid, is must be set to "Tomorrow".
            Calendar utcTomorrow = (Calendar) utcNow.clone();
            utcTomorrow.add(Calendar.DAY_OF_MONTH, 1);
            minAllowedTimestamp = utcTomorrow.getTimeInMillis();
        } else {
            // The time hasn't passed: "Today" is still valid.
            minAllowedTimestamp = utcNow.getTimeInMillis();
        }

        // If a pause is already in progress (started yesterday or earlier), the validator must approve it to prevent
        // the selection screen from crashing.
        long validatorMin = minAllowedTimestamp;
        if (alarm.pauseStartDate > 0 && alarm.pauseStartDate < minAllowedTimestamp) {
            validatorMin = alarm.pauseStartDate;
        }

        constraintsBuilder.setStart(validatorMin);
        constraintsBuilder.setValidator(DateValidatorPointForward.from(validatorMin));

        // Apply the selection, if there is one.
        if (alarm.isPauseSet()) {
            builder.setSelection(new Pair<>(alarm.pauseStartDate, alarm.pauseEndDate));
        }

        builder.setCalendarConstraints(constraintsBuilder.build());

        builder.setDayViewDecorator(new RepeatingDayDecorator(alarm.daysOfWeek.getBits()));

        MaterialDatePicker<Pair<Long, Long>> materialDatePicker = builder.build();

        applyCustomFontToDatePicker(materialDatePicker, generalFont);

        materialDatePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection.first != null && selection.second != null) {
                listener.onDateRangeSet(selection.first, selection.second);
            }
        });

        materialDatePicker.show(fragmentManager, TAG_DATE_RANGE_PICKER);
    }

    private static void applyCustomFontToDatePicker(@NonNull MaterialDatePicker<?> picker, @Nullable Typeface generalFont) {
        picker.getViewLifecycleOwnerLiveData().observeForever(new Observer<>() {
            @Override
            public void onChanged(LifecycleOwner owner) {
                if (owner == null) {
                    return;
                }

                View root = picker.getView();
                if (root == null) {
                    return;
                }

                if (generalFont == null) {
                    picker.getViewLifecycleOwnerLiveData().removeObserver(this);
                    return;
                }

                int[] textViewIds = {
                    com.google.android.material.R.id.confirm_button,
                    com.google.android.material.R.id.cancel_button,
                    com.google.android.material.R.id.mtrl_picker_title_text,
                    com.google.android.material.R.id.mtrl_picker_header_selection_text
                };

                for (int id : textViewIds) {
                    TextView textView = root.findViewById(id);
                    if (textView != null) {
                        textView.setTypeface(generalFont);
                    }
                }

                // Unsubscribe
                picker.getViewLifecycleOwnerLiveData().removeObserver(this);
            }
        });
    }

    public interface OnDateSelectedListener {
        @SuppressWarnings("unused")
        void onDateSet(int year, int month, int day);
    }

    public interface OnDateRangeSelectedListener {
        void onDateRangeSet(long startDateUtc, long endDateUtc);
    }

}
