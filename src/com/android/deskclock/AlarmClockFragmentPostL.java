package com.android.deskclock;

import android.app.TimePickerDialog;
import android.widget.TimePicker;

import com.android.deskclock.provider.Alarm;

/**
 * AlarmClockFragment for L+ devices
 */
public class AlarmClockFragmentPostL extends AlarmClockFragment implements
        TimePickerDialog.OnTimeSetListener {

    // Callback used by TimePickerDialog
    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
        processTimeSet(hourOfDay, minute);
    }

    @Override
    protected void setTimePickerListener() {
        // Do nothing
    }

    @Override
    protected void startCreatingAlarm() {
        // Set the "selected" alarm as null, and we'll create the new one when the timepicker
        // comes back.
        mSelectedAlarm = null;
        AlarmUtils.showTimeEditDialog(this, null);
    }

    @Override
    protected void showTimeEditDialog(Alarm alarm) {
        AlarmUtils.showTimeEditDialog(AlarmClockFragmentPostL.this, alarm);
    }
}
