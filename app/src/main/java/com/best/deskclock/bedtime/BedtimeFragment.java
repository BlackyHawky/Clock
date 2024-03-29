package com.best.deskclock.bedtime;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.view.View.INVISIBLE;
import static com.best.deskclock.settings.SettingsActivity.KEY_AMOLED_DARK_MODE;
import static com.best.deskclock.uidata.UiDataModel.Tab.BEDTIME;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.best.deskclock.DeskClockFragment;
import com.best.deskclock.LogUtils;
import com.best.deskclock.R;
import com.best.deskclock.Utils;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.alarms.dataadapter.AlarmItemViewHolder;
import com.best.deskclock.bedtime.beddata.DataSaver;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.widget.EmptyViewController;
import com.best.deskclock.widget.TextTime;
import com.best.deskclock.widget.toast.SnackbarManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.List;


/**
 * Fragment that shows the bedtime.
 // */
public final class BedtimeFragment extends DeskClockFragment {

    private static final String TAG = "BedtimeFragment";

    //We need a unique label to identify our wake alarm
    public final static String BEDLABEL = "Wake-up alarm";
    ViewGroup mBedtimeView;
    private EmptyViewController mEmptyViewController;
    TextView mEmptyView;
    TextView mHoursOfSleep;
    Context mContext;
    DataSaver mSaver;
    View view;
    TextView mRingtone;
    TextTime mClock;
    TextTime mTxtWakeup;
    TextTime mTxtBedtime;
    LinearLayout mRepeatDays;
    CheckBox mVibrate;
    final CompoundButton[] mDayButtons = new CompoundButton[7];
    CompoundButton mOnOff;
    CompoundButton mWall;
    CompoundButton mDnd;
    AlarmUpdateHandler mAlarmUpdateHandler;
    ViewGroup mMainLayout;
    BottomSheetDialog mBottomSheetDialog;
    Spinner mNotifList;
    Alarm mAlarm;

    /** The public no-arg constructor required by all fragments. */
    public BedtimeFragment() {
        super(BEDTIME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.bedtime_fragment, container, false);

        mContext = getContext();
        mBedtimeView = view.findViewById(R.id.bedtime_view);
        mMainLayout = view.findViewById(R.id.main);

        mEmptyView = view.findViewById(R.id.bedtime_empty_view);
        final Drawable noAlarmsIcon = Utils.toScaledBitmapDrawable(getContext(), R.drawable.ic_alarm_off, 2.5f);
        if (noAlarmsIcon != null) {
            noAlarmsIcon.setTint(getContext().getColor(R.color.md_theme_onSurfaceVariant));
        }
        mEmptyView.setCompoundDrawablesWithIntrinsicBounds(null, noAlarmsIcon, null, null);
        mEmptyView.setCompoundDrawablePadding(Utils.toPixel(30, getContext()));

        mEmptyViewController = new EmptyViewController(mBedtimeView, mMainLayout, mEmptyView);

        mTxtBedtime = view.findViewById(R.id.bedtime_time);
        mTxtWakeup = view.findViewById(R.id.wakeup_time);
        TextView[] textViews = new TextView[]{mTxtBedtime, mTxtWakeup};
        // Sets the Click Listener for the bedtime and wakeup time
        for (TextView time: textViews ) {
            time.setOnClickListener(v -> {
                if (mTxtBedtime.equals(time)) {
                    mSaver.restore();
                    showBedtimeBottomSheetDialog();
                } else if (mTxtWakeup.equals(time)) {
                    mAlarm = getBedAlarm();
                    showWakeupBottomSheetDialog(mAlarm);
                }});}

        mAlarmUpdateHandler = new AlarmUpdateHandler(mContext, null, mMainLayout);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAlarm = getBedAlarm();

        mSaver = DataSaver.getInstance(mContext);
        mSaver.restore();
        bindFragBedClock();

        if (mAlarm != null) {
            mEmptyViewController.setEmpty(false);
            hoursOfSleep(mAlarm);
            bindFragWakeClock(mAlarm);
        } else {
            mEmptyViewController.setEmpty(true);
        }
    }

    // Calculates the different between the time times
    private void hoursOfSleep(Alarm alarm) {
        mHoursOfSleep = view.findViewById(R.id.hours_of_sleep);

        if (null != alarm) {
            int minDiff = alarm.minutes - mSaver.minutes;
            int hDiff;

            if (mSaver.hour > alarm.hour || mSaver.hour == alarm.hour && mSaver.minutes > alarm.minutes) {
                hDiff = alarm.hour + 24 - mSaver.hour;
            } else if (mSaver.hour == alarm.hour && mSaver.minutes == alarm.minutes) {
                hDiff = 24;
            } else {
                hDiff = alarm.hour - mSaver.hour;
            }

            if (minDiff < 0) {
                hDiff = hDiff - 1;
                minDiff = 60 + minDiff;
            }

            String diff;
            if (minDiff == 0) {
                diff = hDiff + "h";
            } else {
                diff = hDiff + "h " + minDiff + "min";
            }

            mHoursOfSleep.setText(diff);
            mHoursOfSleep.setAlpha(mSaver.enabled && alarm.enabled
                    ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA
                    : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA
            );
        } else {
            mHoursOfSleep.setText(R.string.wakeup_alarm_non_existent);
        }
    }

    @Override
    public void onFabClick(@NonNull ImageView fab) {
        createAlarm();
        mEmptyViewController.setEmpty(false);
        fab.setVisibility(View.GONE);
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        if (mAlarm != null) {
            fab.setVisibility(View.GONE);
        } else {
            fab.setVisibility(View.VISIBLE);
            fab.setImageResource(R.drawable.ic_add);
        }
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        left.setVisibility(INVISIBLE);
        right.setVisibility(INVISIBLE);
    }

    //Wake stuff is almost done, only ringtone picking makes problems makes problems
    //moved here for better structure
    private void showWakeupBottomSheetDialog(Alarm alarm) {
        mBottomSheetDialog = new BottomSheetDialog(getContext());
        mBottomSheetDialog.setContentView(R.layout.bedtime_wakeup_bottom_sheet);
        mBottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        mRingtone = mBottomSheetDialog.findViewById(R.id.choose_ringtone_bedtime);
        mClock = mBottomSheetDialog.findViewById(R.id.wake_time);
        mVibrate = mBottomSheetDialog.findViewById(R.id.vibrate_onoff_wake);
        mOnOff = mBottomSheetDialog.findViewById(R.id.toggle_switch_wakeup);
        buildWakeButton(mBottomSheetDialog, alarm);
        bindWakeStuff(alarm);

        // Ringtone editor handler
        mRingtone.setOnClickListener(v -> {
            Events.sendBedtimeEvent(R.string.action_set_ringtone, R.string.label_deskclock);

            final Intent intent = RingtonePickerActivity.createAlarmRingtonePickerIntent(mContext, alarm);
            mContext.startActivity(intent);
            bindRingtone(mContext, alarm);
        });

        mClock.setOnClickListener(v -> {
            Events.sendBedtimeEvent(R.string.action_set_time, R.string.label_deskclock);
            ShowMaterialTimePicker(alarm.hour, alarm.minutes);
            bindClock(alarm);
        });

        mOnOff.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked != alarm.enabled) {
                alarm.enabled = checked;
                mClock.setAlpha(alarm.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
                mTxtWakeup.setAlpha(alarm.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
                mHoursOfSleep.setAlpha(mSaver.enabled && alarm.enabled
                        ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA
                        : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA
                );
                Events.sendBedtimeEvent(checked
                        ? R.string.action_enable
                        : R.string.action_disable, R.string.label_deskclock);
                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, alarm.enabled, false);

                Utils.vibrationTime(mContext, 50);
            }
        });

        mVibrate.setOnClickListener(v -> {
            boolean newState = ((CheckBox) v).isChecked();
            if (newState != alarm.vibrate) {
                alarm.vibrate = newState;
                Events.sendBedtimeEvent(R.string.action_toggle_vibrate, R.string.label_deskclock);
                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);

                if (newState) {
                    // Buzz the vibrator to preview the alarm firing behavior.
                    Utils.vibrationTime(mContext, 300);
                }
            }
        });

        mBottomSheetDialog.show();
    }

    public Alarm getBedAlarm() {
        ContentResolver cr = mContext.getContentResolver();
        List<Alarm> alarms = Alarm.getAlarms(cr, Alarm.LABEL + "=?", BEDLABEL);
        if (!alarms.isEmpty()) {
            return alarms.get(0);
        }

        return null;
    }

    private void createAlarm() {
        final Alarm alarm = new Alarm();
        alarm.hour = 8;
        alarm.minutes = 30;
        alarm.enabled = false;
        alarm.daysOfWeek = Weekdays.fromBits(31);
        alarm.label = BEDLABEL;
        alarm.alert = DataModel.getDataModel().getDefaultAlarmRingtoneUri();
        alarm.vibrate = false;
        mTxtWakeup.setTime(8, 30);
        mTxtWakeup.setAlpha(AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
        AlarmUpdateHandler mAlarmUpdateHandler = new AlarmUpdateHandler(mContext, null, null);
        mAlarmUpdateHandler.asyncAddAlarmForBedtime(alarm);
        SnackbarManager.show(Snackbar.make(mMainLayout, R.string.new_bedtime_alarm, Snackbar.LENGTH_LONG));
    }

    // Build button for each day.
    private void buildWakeButton(BottomSheetDialog bottomSheetDialog, Alarm alarm){
        mRepeatDays = bottomSheetDialog.findViewById(R.id.repeat_days_bedtime);
        final LayoutInflater inflaters = LayoutInflater.from(getContext());
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        // Build button for each day.
        for (int i = 0; i < 7; i++) {
            final View dayButtonFrame = inflaters.inflate(R.layout.day_button, mRepeatDays, false);
            final CompoundButton dayButton = dayButtonFrame.findViewById(R.id.day_button_box);
            final int weekday = weekdays.get(i);
            dayButton.setChecked(true);
            dayButton.setText(UiDataModel.getUiDataModel().getShortWeekday(weekday));
            dayButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(weekday));
            mRepeatDays.addView(dayButtonFrame);
            mDayButtons[i] = dayButton;
        }
        // Day buttons handler
        for (int i = 0; i < mDayButtons.length; i++) {
            final int index = i;
            mDayButtons[i].setOnClickListener(view -> {
                final boolean checked = ((CompoundButton) view).isChecked();
                //final Calendar now = Calendar.getInstance();
                //final Calendar oldNextAlarmTime = alarm.getNextAlarmTime(now);

                final int weekday = DataModel.getDataModel().getWeekdayOrder().getCalendarDays().get(index);
                alarm.daysOfWeek = alarm.daysOfWeek.setBit(weekday, checked);

                // if the change altered the next scheduled alarm time, tell the user
                /*final Calendar newNextAlarmTime = alarm.getNextAlarmTime(now);
                final boolean popupToast = !oldNextAlarmTime.equals(newNextAlarmTime);*/
                final boolean popupToast = false; //TODO:normally we would tell the user but you can't see the toast behind the bottomSheet
                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, popupToast, false);

                Utils.vibrationTime(mContext, 10);
                //TODO:Is it really right to bind them again just to change the letter color
                bindDaysOfWeekButtons(alarm, mContext);
            });
        }

    }

    private void bindWakeStuff(Alarm alarm) {
        bindDaysOfWeekButtons(alarm, mContext);
        bindVibrator(alarm);
        bindRingtone(mContext, alarm);
        bindOnOffSwitch(alarm);
        bindClock(alarm);
    }

    private void bindRingtone(Context context, Alarm alarm) {
        final String title = DataModel.getDataModel().getRingtoneTitle(alarm.alert);
        mRingtone.setText(title);

        final String description = context.getString(R.string.ringtone_description);
        mRingtone.setContentDescription(description + " " + title);

        final boolean silent = Utils.RINGTONE_SILENT.equals(alarm.alert);
        final Drawable icon = silent
                ? AppCompatResources.getDrawable(context, R.drawable.ic_ringtone_silent)
                : AppCompatResources.getDrawable(context, R.drawable.ic_ringtone);
        mRingtone.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
    }

    private void bindDaysOfWeekButtons(Alarm alarm, Context context) {
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < weekdays.size(); i++) {
            final CompoundButton dayButton = mDayButtons[i];
            if (alarm.daysOfWeek.isBitOn(weekdays.get(i))) {
                dayButton.setChecked(true);
                dayButton.setTextColor(context.getColor(R.color.md_theme_inverseOnSurface));
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(context.getColor(R.color.md_theme_inverseSurface));
            }
        }
    }

    private void bindVibrator(Alarm alarm) {
        if (!((Vibrator) mContext.getSystemService(VIBRATOR_SERVICE)).hasVibrator()) {
            mVibrate.setVisibility(View.GONE);
        } else {
            mVibrate.setVisibility(View.VISIBLE);
            mVibrate.setChecked(alarm.vibrate);
        }
    }

    private void bindOnOffSwitch(Alarm alarm) {
        if (mOnOff.isChecked() != alarm.enabled) {
            mOnOff.setChecked(alarm.enabled);
            bindClock(alarm);
        }
    }

    private void bindClock(Alarm alarm) {
        mClock.setTime(alarm.hour, alarm.minutes);
        mClock.setAlpha(alarm.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
        bindFragWakeClock(alarm);
        hoursOfSleep(alarm);
    }

    private void bindFragWakeClock(Alarm alarm) {
        mTxtWakeup.setTime(alarm.hour, alarm.minutes);
        mTxtWakeup.setAlpha(alarm.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
    }

    private void onTimeSet(int hourOfDay, int minute) {
        if (mClock == mBottomSheetDialog.findViewById(R.id.wake_time)) {
            Alarm selectedAlarm = getBedAlarm();

            if (selectedAlarm == null) {
                return;
            }
            selectedAlarm.hour = hourOfDay;
            selectedAlarm.minutes = minute;
            selectedAlarm.enabled = true;
            mOnOff.setChecked(true);
            mAlarmUpdateHandler.asyncUpdateAlarm(selectedAlarm, true, false);
            bindClock(selectedAlarm);
        } else if (mClock == mBottomSheetDialog.findViewById(R.id.bedtime_time)) {
            mSaver.hour = hourOfDay;
            mSaver.minutes = minute;
            mSaver.enabled = true;
            mSaver.save();
            mOnOff.setChecked(true);
            bindBedClock();
            BedtimeService.scheduleBed(mContext, mSaver, BedtimeService.ACTION_LAUNCH_BEDTIME);
            if (mSaver.notifShowTime != -1) {
                BedtimeService.scheduleBed(mContext, mSaver, BedtimeService.ACTION_BED_REMIND_NOTIF);
            }
        }
    }

    //Bedtime bottom sheet
    public void showBedtimeBottomSheetDialog() {
        mBottomSheetDialog = new BottomSheetDialog(getContext());
        mBottomSheetDialog.setContentView(R.layout.bedtime_bottom_sheet);
        mBottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        mClock = mBottomSheetDialog.findViewById(R.id.bedtime_time);
        mOnOff = mBottomSheetDialog.findViewById(R.id.toggle_switch_bedtime);
        mNotifList = mBottomSheetDialog.findViewById(R.id.notif_spinner);
        final String getDarkMode = DataModel.getDataModel().getDarkMode();
        if (Utils.isNight(mContext.getResources()) && getDarkMode.equals(KEY_AMOLED_DARK_MODE)) {
            // Change color of the drop down arrow of Spinner
            mNotifList.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
        }
        mDnd = mBottomSheetDialog.findViewById(R.id.dnd_switch);
        mWall = mBottomSheetDialog.findViewById(R.id.wall_switch);
        buildButton(mBottomSheetDialog);
        bindBedStuff();

        mClock.setOnClickListener(v -> {
            Events.sendBedtimeEvent(R.string.action_set_time, R.string.label_deskclock);
            ShowMaterialTimePicker(mSaver.hour, mSaver.minutes);
            mSaver.save();
            bindBedClock();
        });

        mOnOff.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked != mSaver.enabled) {
                mSaver.enabled = checked;
                mSaver.save();
                mClock.setAlpha(mSaver.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
                mTxtBedtime.setAlpha(mSaver.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
                mHoursOfSleep.setAlpha(mSaver.enabled && mAlarm != null && mAlarm.enabled
                        ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA
                        : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA
                );
                Events.sendBedtimeEvent(checked ? R.string.action_enable : R.string.action_disable, R.string.label_deskclock);

                Utils.vibrationTime(mContext, 50);
            }

            if (!checked) {
                BedtimeService.cancelBed(mContext, BedtimeService.ACTION_LAUNCH_BEDTIME);
                BedtimeService.cancelBed(mContext, BedtimeService.ACTION_BED_REMIND_NOTIF);
                BedtimeService.cancelNotification(mContext);
            } else {
                BedtimeService.scheduleBed(mContext, mSaver, BedtimeService.ACTION_LAUNCH_BEDTIME);
                if (mSaver.notifShowTime != -1) {
                    BedtimeService.scheduleBed(mContext, mSaver, BedtimeService.ACTION_BED_REMIND_NOTIF);
                }
            }
        });

        mNotifList.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] values = mContext.getResources().getStringArray(R.array.array_reminder_notification_values);
                mSaver.notifShowTime = Integer.parseInt(values[position]);
                mSaver.save();
                LogUtils.wtf("value saved for notif time:", mSaver.notifShowTime);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mDnd.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked != mSaver.doNotDisturb) {
                mSaver.doNotDisturb = checked;
                mSaver.save();
                Events.sendBedtimeEvent(checked ? R.string.action_enable : R.string.action_disable, R.string.bedtime_dnd_title);
            }
        });

        mWall.setOnCheckedChangeListener((buttonView, checked) -> {
            if (checked != mSaver.dimWall) {
                mSaver.dimWall = checked;
                mSaver.save();
                Events.sendBedtimeEvent(checked ? R.string.action_enable : R.string.action_disable, R.string.bedtime_wallpaper_title);
            }
        });

        mBottomSheetDialog.show();
    }

    private void buildButton(BottomSheetDialog bottomSheetDialog){
        mRepeatDays = bottomSheetDialog.findViewById(R.id.repeat_days_bedtime);
        final LayoutInflater inflaters = LayoutInflater.from(getContext());
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        // Build button for each day.
        for (int i = 0; i < 7; i++) {
            final View dayButtonFrame = inflaters.inflate(R.layout.day_button, mRepeatDays, false);
            final CompoundButton dayButton = dayButtonFrame.findViewById(R.id.day_button_box);
            final int weekday = weekdays.get(i);
            dayButton.setChecked(true);
            dayButton.setTextColor(mContext.getColor(R.color.md_theme_inverseOnSurface));
            dayButton.setText(UiDataModel.getUiDataModel().getShortWeekday(weekday));
            dayButton.setContentDescription(UiDataModel.getUiDataModel().getLongWeekday(weekday));
            mRepeatDays.addView(dayButtonFrame);
            mDayButtons[i] = dayButton;
        }
        // Day buttons handler
        for (int i = 0; i < mDayButtons.length; i++) {
            final int index = i;
            mDayButtons[i].setOnClickListener(view -> {
                final boolean checked = ((CompoundButton) view).isChecked();

                final int weekday = DataModel.getDataModel().getWeekdayOrder().getCalendarDays().get(index);
                mSaver.daysOfWeek = mSaver.daysOfWeek.setBit(weekday, checked);

                //TODO: normally we would tell the user bedtime changed but the user can't see the toast behind the bottomSheet
                mSaver.save();

                Utils.vibrationTime(mContext, 10);

                //TODO: is it really right to bind all again just to change the letter color
                bindDaysOfBedButtons(mContext);
            });
        }
    }

    //private void showNotifPref()
    //binding
    private void bindBedStuff() {
        bindDaysOfBedButtons(mContext);
        bindBedSwitch();
        bindBedClock();
        bindSpinner();
        bindSwitches();
    }

    private void bindDaysOfBedButtons(Context context) {
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < weekdays.size(); i++) {
            final CompoundButton dayButton = mDayButtons[i];
            if (mSaver.daysOfWeek.isBitOn(weekdays.get(i))) {
                dayButton.setChecked(true);
                dayButton.setTextColor(context.getColor(R.color.md_theme_inverseOnSurface));
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(context.getColor(R.color.md_theme_inverseSurface));
            }
        }
    }

    private void bindBedSwitch() {
        if (mOnOff.isChecked() != mSaver.enabled) {
            mOnOff.setChecked(mSaver.enabled);
            bindBedClock();
        }
    }

    private void bindBedClock() {
        mClock.setTime(mSaver.hour, mSaver.minutes);
        mClock.setAlpha(mSaver.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
        bindFragBedClock();
        hoursOfSleep(getBedAlarm());
    }

    private void bindFragBedClock() {
        mTxtBedtime.setTime(mSaver.hour, mSaver.minutes);
        mTxtBedtime.setAlpha(mSaver.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
    }

    private void bindSpinner() {
        mNotifList.setAdapter(ArrayAdapter.createFromResource(mContext, R.array.array_reminder_notification, R.layout.spinner_item));
        mNotifList.setSelection(getSpinnerPos(mSaver.notifShowTime,
                mContext.getResources().getStringArray(R.array.array_reminder_notification_values)));
    }

    private void bindSwitches() {
        if (mDnd.isChecked() != mSaver.doNotDisturb) {
            mDnd.setChecked(mSaver.doNotDisturb);
        }
        if (mWall.isChecked() != mSaver.dimWall) {
            mWall.setChecked(mSaver.dimWall);
        }
    }

    private void ShowMaterialTimePicker(int hour, int minute) {

        @TimeFormat int clockFormat;
        boolean isSystem24Hour = DateFormat.is24HourFormat(getContext());
        clockFormat = isSystem24Hour ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;

        MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setHour(hour)
                .setMinute(minute)
                .build();
        Context context = getContext();
        materialTimePicker.show(((AppCompatActivity) context).getSupportFragmentManager(), TAG);

        materialTimePicker.addOnPositiveButtonClickListener(dialog -> {
            int newHour = materialTimePicker.getHour();
            int newMinute = materialTimePicker.getMinute();
            onTimeSet(newHour, newMinute);
        });
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