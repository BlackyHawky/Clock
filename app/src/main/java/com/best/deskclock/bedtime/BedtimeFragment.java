package com.best.deskclock.bedtime;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.view.View.INVISIBLE;
import static com.best.deskclock.uidata.UiDataModel.Tab.BEDTIME;

import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.best.deskclock.DeskClockFragment;
import com.best.deskclock.LogUtils;
import com.best.deskclock.R;
import com.best.deskclock.ThemeUtils;
import com.best.deskclock.Utils;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.alarms.TimePickerDialogFragment;
import com.best.deskclock.alarms.dataadapter.AlarmItemViewHolder;
import com.best.deskclock.bedtime.beddata.DataSaver;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.widget.TextTime;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;


/**
 * Fragment that shows the bedtime.
 // */
public final class BedtimeFragment extends DeskClockFragment implements
        TimePickerDialogFragment.OnTimeSetListener {

    //We need a unique label to identify our wake alarm
    public static String BEDLABEL = "irmvwtiucrhdsgcjidsjfmrvdokksvjiuhmfdrijlanscifmreiucmehjniafcnfmoraimciufmhjiafuomiu";
    Context context;
    Vibrator vibrator;

    DataSaver saver;

    View view;

    TextView ringtone;
    TextTime clock;
    TextTime txtWakeup;
    TextTime txtBedtime;
    LinearLayout repeatDays;
    CheckBox vibrate;
    CompoundButton[] dayButtons = new CompoundButton[7];
    CompoundButton onOff;
    CompoundButton wall;
    CompoundButton dnd;

    AlarmUpdateHandler mAlarmUpdateHandler;
    ViewGroup mMainLayout;

    BottomSheetDialog bottomSheetDialog;

    Spinner notifList;

    Alarm alarm;

    /** The public no-arg constructor required by all fragments. */
    public BedtimeFragment() {
        super(BEDTIME);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.bedtime_fragment, container, false);

        context = getActivity();
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        txtBedtime = view.findViewById(R.id.bedtime_time);
        txtWakeup = view.findViewById(R.id.wakeup_time);
        TextView[] textViews = new TextView[]{ txtBedtime, txtWakeup };

        mMainLayout = view.findViewById(R.id.main);
        mAlarmUpdateHandler = new AlarmUpdateHandler(context, null, mMainLayout);

        // Sets the Click Listener for the bedtime and wakeup time
        for (TextView time: textViews ) {
            time.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (txtBedtime.equals(time)) {
                        saver.restore();
                        showBedtimeBottomSheetDialog();
                    } else if (txtWakeup.equals(time)) {
                        alarm = getBedAlarm(true);
                        showWakeupBottomSheetDialog(alarm);
                    }}});}

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        alarm = getBedAlarm(false);

        saver = DataSaver.getInstance(context);
        saver.restore();
        bindFragBedClock();

        if (null != alarm) {
            hoursOfSleep(alarm);
            bindFragWakeClock(alarm);
        }
    }

    // Calculates the different between the time times
    private void hoursOfSleep(Alarm alarm) {

        //TODO: what if someone goes to bed after 12 am
        int minDiff = alarm.minutes - saver.minutes;
        int hDiff = alarm.hour + 24 - saver.hour;
        if (minDiff < 0){
            hDiff = hDiff - 1;
            minDiff = 60 + minDiff;
        }
        String diff;
        if (minDiff == 0) {
            diff = hDiff + "h";
        } else {
            diff = hDiff + "h " + minDiff + "min";
        }

        TextView hours_of_sleep_text = (TextView) view.findViewById(R.id.hours_of_sleep);
        hours_of_sleep_text.setText(diff);
        hours_of_sleep_text.setAlpha(saver.enabled && alarm.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
    }


    @Override
    public void onUpdateFab(@NonNull ImageView fab) { fab.setVisibility(INVISIBLE); }

    @Override
    public void onUpdateFabButtons(@NonNull Button left, @NonNull Button right) {
        left.setVisibility(INVISIBLE);
        left.setClickable(false);

        right.setVisibility(INVISIBLE);
        right.setClickable(false);
    }

    @Override
    public void onFabClick(@NonNull ImageView fab) {}

    //Wake stuff is almost done, only ringtone picking makes problems makes problems
    //moved here for better structure
    private void showWakeupBottomSheetDialog(Alarm alarm) {
        bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.wakeup_bottom_sheet);
        Fragment mFragment = this;

        ringtone = bottomSheetDialog.findViewById(R.id.choose_ringtone_bedtime);
        clock = bottomSheetDialog.findViewById(R.id.wake_time);
        vibrate = bottomSheetDialog.findViewById(R.id.vibrate_onoff_wake);
        onOff = bottomSheetDialog.findViewById(R.id.toggle_switch_wakeup);
        buildWakeButton(bottomSheetDialog, alarm);
        bindWakeStuff(alarm);

        // Ringtone editor handler
        ringtone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "ERROR: The ringtone change will only show in alarm tab", Toast.LENGTH_LONG).show();
                Events.sendBedtimeEvent(R.string.action_set_ringtone, R.string.label_deskclock);

                final Intent intent =
                        RingtonePickerActivity.createAlarmRingtonePickerIntent(context, alarm);
                context.startActivity(intent);
                bindRingtone(context, alarm);
            }
        });

        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Events.sendBedtimeEvent(R.string.action_set_time, R.string.label_deskclock);
                TimePickerDialogFragment.show(mFragment, alarm.hour, alarm.minutes);
                bindClock(alarm);
            }
        });

        onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked != alarm.enabled) {
                    alarm.enabled = checked;
                    Events.sendBedtimeEvent(checked ? R.string.action_enable : R.string.action_disable,
                            R.string.label_deskclock);
                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, alarm.enabled, false);
                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(10);
                    }
                    //LOGGER.d("Updating alarm enabled state to " + checked);
                }
            }
        });

        vibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newState = ((CheckBox) v).isChecked();
                if (newState != alarm.vibrate) {
                    alarm.vibrate = newState;
                    Events.sendBedtimeEvent(R.string.action_toggle_vibrate, R.string.label_deskclock);
                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                    //LOGGER.d("Updating vibrate state to " + newState);

                    if (newState) {
                        // Buzz the vibrator to preview the alarm firing behavior.
                        if (vibrator.hasVibrator()) {
                            vibrator.vibrate(300);
                        }
                    }
                }
            }
        });

        bottomSheetDialog.show();
    }

    public Alarm getBedAlarm(boolean create) {
        ContentResolver cr = context.getApplicationContext().getContentResolver();
        List<Alarm> alarms = Alarm.getAlarms(cr, Alarm.LABEL + "=?", BEDLABEL);
        if (!alarms.isEmpty()) {
            return alarms.get(0);
        } else {
            if (create) {
                final Alarm alarm = new Alarm();
                alarm.hour = 8;
                alarm.minutes = 30;
                alarm.enabled = false;
                alarm.daysOfWeek = Weekdays.fromBits(31);
                alarm.label = BEDLABEL;
                AlarmUpdateHandler mAlarmUpdateHandler = new AlarmUpdateHandler(context, null, null);
                mAlarmUpdateHandler.asyncAddAlarm(alarm);
                Toast.makeText(context, context.getString(R.string.new_bed_alarm), Toast.LENGTH_SHORT).show();
            }
            // Alarm with the given label not found
            return null;
        }
    }

    // Build button for each day.
    private void buildWakeButton(BottomSheetDialog bottomSheetDialog, Alarm alarm){
        repeatDays = bottomSheetDialog.findViewById(R.id.repeat_days_bedtime);
        final LayoutInflater inflaters = LayoutInflater.from(getContext());
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        // Build button for each day.
        for (int i = 0; i < 7; i++) {
            final View dayButtonFrame = inflaters.inflate(R.layout.day_button, repeatDays,
                    false /* attachToRoot */);
            final CompoundButton dayButton = dayButtonFrame.findViewById(R.id.day_button_box);
            final int weekday = weekdays.get(i);
            dayButton.setChecked(true);
            dayButton.setTextColor(ThemeUtils.resolveColor(getContext(),
                    android.R.attr.textColorPrimaryInverse));
            dayButton.setText(UiDataModel.getUiDataModel().getShortWeekday(weekday));
            dayButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(weekday));
            repeatDays.addView(dayButtonFrame);
            dayButtons[i] = dayButton;
        }
        // Day buttons handler
        for (int i = 0; i < dayButtons.length; i++) {
            final int index = i;
            dayButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean checked = ((CompoundButton) view).isChecked();
                    //final Calendar now = Calendar.getInstance();
                    //final Calendar oldNextAlarmTime = alarm.getNextAlarmTime(now);

                    final int weekday = DataModel.getDataModel().getWeekdayOrder().getCalendarDays().get(index);
                    alarm.daysOfWeek = alarm.daysOfWeek.setBit(weekday, checked);

                    // if the change altered the next scheduled alarm time, tell the user
                    /*final Calendar newNextAlarmTime = alarm.getNextAlarmTime(now);
                    final boolean popupToast = !oldNextAlarmTime.equals(newNextAlarmTime);*/
                    final boolean popupToast = false;//TODO:normally we would tell the user but you can't see the toast behind the bottomSheet
                    mAlarmUpdateHandler.asyncUpdateAlarm(alarm, popupToast, false);

                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(10);
                    }
                    //TODO:Is it really right to bind them again just to change the letter color
                    bindDaysOfWeekButtons(alarm, context);
                }
            });
        }

    }

    private void bindWakeStuff(Alarm alarm) {
        bindDaysOfWeekButtons(alarm, context);
        bindVibrator(alarm);
        bindRingtone(context, alarm);
        bindOnOffSwitch(alarm);
        bindClock(alarm);
    }

    private void bindRingtone(Context context, Alarm alarm) {
        final String title = DataModel.getDataModel().getRingtoneTitle(alarm.alert);
        ringtone.setText(title);

        final String description = context.getString(R.string.ringtone_description);
        ringtone.setContentDescription(description + " " + title);

        final boolean silent = Utils.RINGTONE_SILENT.equals(alarm.alert);
        final Drawable icon = Utils.getVectorDrawable(context,
                silent ? R.drawable.ic_ringtone_silent : R.drawable.ic_ringtone);
        ringtone.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
    }

    private void bindDaysOfWeekButtons(Alarm alarm, Context context) {
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < weekdays.size(); i++) {
            final CompoundButton dayButton = dayButtons[i];
            if (alarm.daysOfWeek.isBitOn(weekdays.get(i))) {
                dayButton.setChecked(true);
                dayButton.setTextColor(ThemeUtils.resolveColor(context,
                        android.R.attr.textColorPrimaryInverse));
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(ThemeUtils.resolveColor(context,
                        android.R.attr.textColorPrimary));
            }
        }
    }

    private void bindVibrator(Alarm alarm) {
        if (!((Vibrator) context.getSystemService(VIBRATOR_SERVICE)).hasVibrator()) {
            vibrate.setVisibility(View.GONE);
        } else {
            vibrate.setVisibility(View.VISIBLE);
            vibrate.setChecked(alarm.vibrate);
        }
    }

    private void bindOnOffSwitch(Alarm alarm) {
        if (onOff.isChecked() != alarm.enabled) {
            onOff.setChecked(alarm.enabled);
            bindClock(alarm);
        }
    }

    private void bindClock(Alarm alarm) {
        clock.setTime(alarm.hour, alarm.minutes);
        clock.setAlpha(alarm.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
        bindFragWakeClock(alarm);
        hoursOfSleep(alarm);
    }

    private void bindFragWakeClock(Alarm alarm) {
        txtWakeup.setTime(alarm.hour, alarm.minutes);
        txtWakeup.setAlpha(alarm.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
    }

    @Override
    public void onTimeSet(TimePickerDialogFragment fragment, int hourOfDay, int minute) {
        if (clock == bottomSheetDialog.findViewById(R.id.wake_time)) {
            Alarm mSelectedAlarm = getBedAlarm(false);

            mSelectedAlarm.hour = hourOfDay;
            mSelectedAlarm.minutes = minute;
            mSelectedAlarm.enabled = true;
            mAlarmUpdateHandler.asyncUpdateAlarm(mSelectedAlarm, true, false);
            bindClock(mSelectedAlarm);
        } else if (clock == bottomSheetDialog.findViewById(R.id.bedtime_time)) {
            saver.hour = hourOfDay;
            saver.minutes = minute;
            saver.enabled = true;
            saver.save();
            bindBedClock();
            BedtimeService.scheduleBed(context, saver, BedtimeService.ACTION_LAUNCH_BEDTIME);
            if (saver.notifShowTime != -1) {
                BedtimeService.scheduleBed(context, saver, BedtimeService.ACTION_BED_REMIND_NOTIF);
            }
        }
    }



    //Bedtime bottom sheet
    //moved here for better structure
    public void showBedtimeBottomSheetDialog() {
        bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.bedtime_bottom_sheet);
        Fragment mFragment = this;
        clock = bottomSheetDialog.findViewById(R.id.bedtime_time);
        onOff = bottomSheetDialog.findViewById(R.id.toggle_switch_bedtime);
        notifList = (Spinner) bottomSheetDialog.findViewById(R.id.notif_spinner);
        dnd = bottomSheetDialog.findViewById(R.id.dnd_switch);
        wall = bottomSheetDialog.findViewById(R.id.wall_switch);
        buildButton(bottomSheetDialog);
        bindBedStuff();

        clock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Events.sendBedtimeEvent(R.string.action_set_time, R.string.label_deskclock);
                TimePickerDialogFragment.show(mFragment, saver.hour, saver.minutes);
                saver.save();
                bindBedClock();
            }
        });

        onOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                if (checked != saver.enabled) {
                    saver.enabled = checked;
                    saver.save();
                    Events.sendBedtimeEvent(checked ? R.string.action_enable : R.string.action_disable,
                            R.string.label_deskclock);
                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(10);
                    }
                }
                if (!checked) {
                    BedtimeService.cancelBed(context, BedtimeService.ACTION_LAUNCH_BEDTIME);
                    BedtimeService.cancelBed(context, BedtimeService.ACTION_BED_REMIND_NOTIF);
                    BedtimeService.cancelNotification(context);
                } else {
                    BedtimeService.scheduleBed(context, saver, BedtimeService.ACTION_LAUNCH_BEDTIME);
                    if (saver.notifShowTime != -1) {
                        BedtimeService.scheduleBed(context, saver, BedtimeService.ACTION_BED_REMIND_NOTIF);
                    }
                }
            }
        });

        notifList.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] values = context.getResources().getStringArray(R.array.array_reminder_notification_values);
                saver.notifShowTime = Integer.parseInt(values[position]);
                saver.save();
                LogUtils.wtf("value saved for notif time:", saver.notifShowTime);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        dnd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                if (checked != saver.doNotDisturb) {
                    saver.doNotDisturb = checked;
                    saver.save();
                    Events.sendBedtimeEvent(checked ? R.string.action_enable : R.string.action_disable,
                            R.string.bed_dnd_title);
                }
            }
        });

        wall.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                if (checked != saver.dimWall) {
                    saver.dimWall = checked;
                    saver.save();
                    Events.sendBedtimeEvent(checked ? R.string.action_enable : R.string.action_disable,
                            R.string.bed_wall_title);
                }
            }
        });

        bottomSheetDialog.show();
    }

    private void buildButton(BottomSheetDialog bottomSheetDialog){
        repeatDays = bottomSheetDialog.findViewById(R.id.repeat_days_bedtime);
        final LayoutInflater inflaters = LayoutInflater.from(getContext());
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        // Build button for each day.
        for (int i = 0; i < 7; i++) {
            final View dayButtonFrame = inflaters.inflate(R.layout.day_button, repeatDays,
                    false /* attachToRoot */);
            final CompoundButton dayButton = dayButtonFrame.findViewById(R.id.day_button_box);
            final int weekday = weekdays.get(i);
            dayButton.setChecked(true);
            dayButton.setTextColor(ThemeUtils.resolveColor(getContext(),
                    android.R.attr.textColorPrimaryInverse));
            dayButton.setText(UiDataModel.getUiDataModel().getShortWeekday(weekday));
            dayButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(weekday));
            repeatDays.addView(dayButtonFrame);
            dayButtons[i] = dayButton;
        }
        // Day buttons handler
        for (int i = 0; i < dayButtons.length; i++) {
            final int index = i;
            dayButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean checked = ((CompoundButton) view).isChecked();

                    final int weekday = DataModel.getDataModel().getWeekdayOrder().getCalendarDays().get(index);
                    saver.daysOfWeek = saver.daysOfWeek.setBit(weekday, checked);

                    //TODO: normally we would tell the user bedtime changed but the user can't see the toast behind the bottomSheet
                    saver.save();

                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(10);
                    }
                    //TODO: is it really right to bind all again just to change the letter color
                    bindDaysOfBedButtons(context);
                }
            });
        }

    }

    //private void showNotifPref()

    //binding
    private void bindBedStuff() {
        bindDaysOfBedButtons(context);
        bindBedSwitch();
        bindBedClock();
        bindSpinner();
        bindSwitches();
    }

    private void bindDaysOfBedButtons(Context context) {
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < weekdays.size(); i++) {
            final CompoundButton dayButton = dayButtons[i];
            if (saver.daysOfWeek.isBitOn(weekdays.get(i))) {
                dayButton.setChecked(true);
                dayButton.setTextColor(ThemeUtils.resolveColor(context,
                        android.R.attr.textColorPrimaryInverse));
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(ThemeUtils.resolveColor(context,
                        android.R.attr.textColorPrimary));
            }
        }
    }

    private void bindBedSwitch() {
        if (onOff.isChecked() != saver.enabled) {
            onOff.setChecked(saver.enabled);
            bindBedClock();
        }
    }

    private void bindBedClock() {
        clock.setTime(saver.hour, saver.minutes);
        clock.setAlpha(saver.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
        bindFragBedClock();
        hoursOfSleep(getBedAlarm(false));
    }

    private void bindFragBedClock() {
        txtBedtime.setTime(saver.hour, saver.minutes);
        txtBedtime.setAlpha(saver.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
    }

    private void bindSpinner() {
        notifList.setAdapter(ArrayAdapter.createFromResource(context, R.array.array_reminder_notification, R.layout.spinner_item));
        notifList.setSelection(getSpinnerPos(saver.notifShowTime, context.getResources().getStringArray(R.array.array_reminder_notification_values)));
    }

    private void bindSwitches() {
        if (dnd.isChecked() != saver.doNotDisturb) {
            dnd.setChecked(saver.doNotDisturb);
        }
        if (wall.isChecked() != saver.dimWall) {
            wall.setChecked(saver.dimWall);
        }
    }

    //TODO: implement sleep-timers with common media support(songs, albums, artists and playlists) in here



    //general stuff

    private int getSpinnerPos(int savedValue, String[] valueArray) {
        for (int i = 0; i < valueArray.length; i++){
            String value = valueArray[i];
            if (Integer.parseInt(value) == savedValue) {
                return i;
            }
        }
        return 0;
    }
}