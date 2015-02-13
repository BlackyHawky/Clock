package com.android.deskclock;

import android.text.format.DateFormat;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.android.deskclock.provider.Alarm;

/**
 * AlarmClockFragment for pre-L devices
 */
public class AlarmClockFragmentPreL extends AlarmClockFragment implements
        TimePickerDialog.OnTimeSetListener {

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
        processTimeSet(hourOfDay, minute);
    }

    @Override
    protected void setTimePickerListener() {
        // Make sure to use the child FragmentManager. We have to use that one for the
        // case where an intent comes in telling the activity to load the timepicker,
        // which means we have to use that one everywhere so that the fragment can get
        // correctly picked up here if it's open.
        final TimePickerDialog tpd = (TimePickerDialog) getChildFragmentManager()
                .findFragmentByTag(AlarmUtils.FRAG_TAG_TIME_PICKER);
        if (tpd != null) {
            // The dialog is already open so we need to set the listener again.
            tpd.setOnTimeSetListener(this);
        }
    }

    @Override
    protected void startCreatingAlarm() {
        // Set the "selected" alarm as null, and we'll create the new one when the timepicker
        // comes back.
        mSelectedAlarm = null;
        AlarmUtils.showTimeEditDialog(getChildFragmentManager(),
                null, AlarmClockFragmentPreL.this, DateFormat.is24HourFormat(getActivity()));
    }

    @Override
    public void showTimeEditDialog(Alarm alarm) {
        AlarmUtils.showTimeEditDialog(getChildFragmentManager(),
            alarm, AlarmClockFragmentPreL.this, DateFormat.is24HourFormat(getActivity()));
    }
}
