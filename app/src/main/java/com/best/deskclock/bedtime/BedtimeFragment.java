// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.bedtime;

import static android.content.Context.VIBRATOR_SERVICE;
import static com.best.deskclock.settings.AlarmSettingsActivity.MATERIAL_TIME_PICKER_ANALOG_STYLE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_AMOLED_DARK_MODE;
import static com.best.deskclock.uidata.UiDataModel.Tab.BEDTIME;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
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
import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.alarms.dataadapter.AlarmItemViewHolder;
import com.best.deskclock.bedtime.beddata.DataSaver;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.ringtone.RingtonePickerActivity;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.EmptyViewController;
import com.best.deskclock.widget.TextTime;
import com.best.deskclock.widget.toast.SnackbarManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.List;
import java.util.Objects;

/**
 * Fragment that shows the bedtime.
 */
public final class BedtimeFragment extends DeskClockFragment {

    private static final String TAG = "BedtimeFragment";
    public final static String BEDTIME_LABEL = "Wake-up alarm"; // We need a unique label to identify our wake alarm
    public final static int BEDTIME_ID = 0;

    Context mContext;
    DataSaver mSaver;
    View view;
    ViewGroup mBedtimeView;
    ViewGroup mMainLayout;
    EmptyViewController mEmptyViewController;
    TextView mEmptyView;
    TextView mHoursOfSleep;
    TextView mReminderNotificationTitle;
    TextView mNoBedtimeAlarmText;
    TextView mNoWakeupAlarmText;
    TextView mRingtone;
    TextTime mClock;
    TextTime mWakeupText;
    TextTime mBedtimeText;
    LinearLayout mRepeatDays;
    CheckBox mDismissBedtimeAlarmWhenRingtoneEnds;
    CheckBox mBedtimeAlarmSnoozeActions;
    CheckBox mVibrate;
    final CompoundButton[] mDayButtons = new CompoundButton[7];
    CompoundButton mOnOff;
    AlarmUpdateHandler mAlarmUpdateHandler;
    BottomSheetDialog mBottomSheetDialog;
    Spinner mNotificationList;
    Alarm mAlarm;

    /**
     * The public no-arg constructor required by all fragments.
     */
    public BedtimeFragment() {
        super(BEDTIME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.bedtime_fragment, container, false);

        mContext = requireContext();
        mBedtimeView = view.findViewById(R.id.bedtime_view);
        mMainLayout = view.findViewById(R.id.main);

        MaterialCardView bedtimeCardView = view.findViewById(R.id.bedtime_card_view);
        final boolean isCardBackgroundDisplayed = DataModel.getDataModel().isCardBackgroundDisplayed();
        if (isCardBackgroundDisplayed) {
            bedtimeCardView.setCardBackgroundColor(
                    MaterialColors.getColor(mContext, com.google.android.material.R.attr.colorSurface, Color.BLACK)
            );
        } else {
            bedtimeCardView.setCardBackgroundColor(Color.TRANSPARENT);
        }

        final boolean isCardBorderDisplayed = DataModel.getDataModel().isCardBorderDisplayed();
        if (isCardBorderDisplayed) {
            bedtimeCardView.setStrokeWidth(Utils.toPixel(2, mContext));
            bedtimeCardView.setStrokeColor(
                    MaterialColors.getColor(mContext, com.google.android.material.R.attr.colorPrimary, Color.BLACK)
            );
        }

        mEmptyView = view.findViewById(R.id.bedtime_empty_view);
        final Drawable noAlarmsIcon = Utils.toScaledBitmapDrawable(mContext, R.drawable.ic_alarm_off, 2.5f);
        if (noAlarmsIcon != null) {
            noAlarmsIcon.setTint(MaterialColors.getColor(
                    mContext, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK));
        }
        mEmptyView.setCompoundDrawablesWithIntrinsicBounds(null, noAlarmsIcon, null, null);
        mEmptyView.setCompoundDrawablePadding(Utils.toPixel(30, mContext));

        mEmptyViewController = new EmptyViewController(mBedtimeView, mMainLayout, mEmptyView);

        mBedtimeText = view.findViewById(R.id.bedtime_time);
        mWakeupText = view.findViewById(R.id.wakeup_time);
        TextView[] textViews = new TextView[]{mBedtimeText, mWakeupText};
        for (TextView time: textViews ) {
            time.setOnClickListener(v -> {
                if (mBedtimeText.equals(time)) {
                    mSaver.restore();
                    showBedtimeBottomSheetDialog();
                } else if (mWakeupText.equals(time)) {
                    mAlarm = getWakeupAlarm();
                    showWakeupBottomSheetDialog(mAlarm);
                }}
            );
        }

        mAlarmUpdateHandler = new AlarmUpdateHandler(mContext, null, mMainLayout);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAlarm = getWakeupAlarm();
        mSaver = DataSaver.getInstance(mContext);
        mSaver.restore();
        bindFragBedClock();

        if (mAlarm != null) {
            mEmptyViewController.setEmpty(false);
            hoursOfSleep(mAlarm);
            bindFragWakeClock(mAlarm);
            if (mRingtone != null) {
                bindRingtone(mContext, mAlarm);
            }
        } else {
            mEmptyViewController.setEmpty(true);
        }

        // Necessary if from the bedtime view we go to the settings, change one of these settings
        // and return to the bedtime view
        updateFab(FAB_AND_BUTTONS_IMMEDIATE);
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
        left.setVisibility(View.INVISIBLE);
        right.setVisibility(View.INVISIBLE);
    }

    // ****************************
    // *   Wake-up bottom sheet   *
    // ****************************

    private void showWakeupBottomSheetDialog(Alarm alarm) {
        mBottomSheetDialog = new BottomSheetDialog(mContext);
        mBottomSheetDialog.setContentView(R.layout.bedtime_wakeup_bottom_sheet);
        mBottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);

        final String getDarkMode = DataModel.getDataModel().getDarkMode();
        if (Utils.isNight(getResources()) && getDarkMode.equals(KEY_AMOLED_DARK_MODE)) {
            Objects.requireNonNull(mBottomSheetDialog.getWindow()).setNavigationBarColor(
                    MaterialColors.getColor(mContext, com.google.android.material.R.attr.colorSurface, Color.BLACK)
            );
        }

        mRingtone = mBottomSheetDialog.findViewById(R.id.choose_ringtone_bedtime);
        mClock = mBottomSheetDialog.findViewById(R.id.wakeup_time);
        mDismissBedtimeAlarmWhenRingtoneEnds = mBottomSheetDialog.findViewById(
                R.id.dismiss_bedtime_alarm_when_ringtone_ends_onoff);
        mBedtimeAlarmSnoozeActions = mBottomSheetDialog.findViewById(R.id.bedtime_alarm_snooze_actions_onoff);
        mVibrate = mBottomSheetDialog.findViewById(R.id.vibrate_onoff_wakeup);
        mOnOff = mBottomSheetDialog.findViewById(R.id.toggle_switch_wakeup);
        mNoWakeupAlarmText = mBottomSheetDialog.findViewById(R.id.no_wakeup_alarm_text);
        buildWakeButton(mBottomSheetDialog, alarm);
        bindWakeStuff(alarm);

        mRingtone.setOnClickListener(v -> {
            Events.sendBedtimeEvent(R.string.action_set_ringtone, R.string.label_deskclock);

            final Intent intent = RingtonePickerActivity.createAlarmRingtonePickerIntent(mContext, mAlarm);
            mContext.startActivity(intent);
            bindRingtone(mContext, mAlarm);
        });

        mClock.setOnClickListener(v -> {
            Events.sendBedtimeEvent(R.string.action_set_time, R.string.label_deskclock);
            ShowMaterialTimePicker(alarm.hour, alarm.minutes);
            bindClock(alarm);
        });

        mOnOff.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked != alarm.enabled) {
                alarm.enabled = checked;

                mClock.setTypeface(alarm.enabled ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                mClock.setAlpha(alarm.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);

                mWakeupText.setTypeface(alarm.enabled ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                mWakeupText.setAlpha(alarm.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);

                Events.sendBedtimeEvent(checked
                        ? R.string.action_enable
                        : R.string.action_disable, R.string.label_deskclock);
                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, alarm.enabled, false);

                Utils.setVibrationTime(mContext, 50);

                hoursOfSleep(alarm);

                bindNoWakeupAlarmText();
            }
        });

        mDismissBedtimeAlarmWhenRingtoneEnds.setOnClickListener(v -> {
            boolean newState = ((CheckBox) v).isChecked();
            if (newState != alarm.dismissAlarmWhenRingtoneEnds) {
                alarm.dismissAlarmWhenRingtoneEnds = newState;
                Events.sendBedtimeEvent(R.string.action_toggle_dismiss_alarm_when_ringtone_ends, R.string.label_deskclock);
                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                Utils.setVibrationTime(mContext, 50);
            }
        });

        mBedtimeAlarmSnoozeActions.setOnClickListener(v -> {
            boolean newState = ((CheckBox) v).isChecked();
            if (newState != alarm.alarmSnoozeActions) {
                alarm.alarmSnoozeActions = newState;
                Events.sendBedtimeEvent(R.string.action_toggle_alarm_snooze_actions, R.string.label_deskclock);
                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                Utils.setVibrationTime(mContext, 50);
            }
        });

        mVibrate.setOnClickListener(v -> {
            boolean newState = ((CheckBox) v).isChecked();
            if (newState != alarm.vibrate) {
                alarm.vibrate = newState;
                Events.sendBedtimeEvent(R.string.action_toggle_vibrate, R.string.label_deskclock);
                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
                if (newState) {
                    Utils.setVibrationTime(mContext, 300);
                }
            }
        });

        mBottomSheetDialog.show();
    }

    private void buildWakeButton(BottomSheetDialog bottomSheetDialog, Alarm alarm){
        mRepeatDays = bottomSheetDialog.findViewById(R.id.repeat_days_bedtime);
        final LayoutInflater inflaters = LayoutInflater.from(mContext);
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
                final int weekday = DataModel.getDataModel().getWeekdayOrder().getCalendarDays().get(index);
                alarm.daysOfWeek = alarm.daysOfWeek.setBit(weekday, checked);

                mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, false);

                Utils.setVibrationTime(mContext, 10);

                bindDaysOfWeekButtons(alarm, mContext);
            });
        }

    }

    private void bindWakeStuff(Alarm alarm) {
        bindDaysOfWeekButtons(alarm, mContext);
        bindDismissBedtimeAlarmWhenRingtoneEnds(alarm);
        bindBedtimeAlarmSnoozeActions(alarm);
        bindVibrator(alarm);
        bindRingtone(mContext, mAlarm);
        bindOnOffSwitch(alarm);
        bindClock(alarm);
        bindNoWakeupAlarmText();
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
                dayButton.setTextColor(MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorOnSurfaceInverse, Color.BLACK));
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorSurfaceInverse, Color.BLACK));
            }
        }
    }

    private void bindDismissBedtimeAlarmWhenRingtoneEnds(Alarm alarm) {
        final int timeoutMinutes = DataModel.getDataModel().getAlarmTimeout();
        if (timeoutMinutes == -2) {
            mDismissBedtimeAlarmWhenRingtoneEnds.setVisibility(View.GONE);
        } else {
            mDismissBedtimeAlarmWhenRingtoneEnds.setVisibility(View.VISIBLE);
            mDismissBedtimeAlarmWhenRingtoneEnds.setChecked(alarm.dismissAlarmWhenRingtoneEnds);
        }
    }

    private void bindBedtimeAlarmSnoozeActions(Alarm alarm) {
        final int snoozeMinutes = DataModel.getDataModel().getSnoozeLength();
        if (snoozeMinutes == -1) {
            mBedtimeAlarmSnoozeActions.setVisibility(View.GONE);
        } else {
            mBedtimeAlarmSnoozeActions.setVisibility(View.VISIBLE);
            mBedtimeAlarmSnoozeActions.setChecked(alarm.alarmSnoozeActions);
        }
    }

    private void bindVibrator(Alarm alarm) {
        if (hasVibrator()) {
            mVibrate.setVisibility(View.VISIBLE);
            mVibrate.setChecked(alarm.vibrate);
        } else {
            mVibrate.setVisibility(View.GONE);
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
        mClock.setTypeface(alarm.enabled ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        mClock.setAlpha(alarm.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
        bindFragWakeClock(alarm);
        hoursOfSleep(alarm);
    }

    private void bindFragWakeClock(Alarm alarm) {
        mWakeupText.setTime(alarm.hour, alarm.minutes);
        mWakeupText.setTypeface(alarm.enabled ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        mWakeupText.setAlpha(alarm.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
    }

    private void bindNoWakeupAlarmText() {
        final int timeoutMinutes = DataModel.getDataModel().getAlarmTimeout();
        final int snoozeMinutes = DataModel.getDataModel().getSnoozeLength();

        if (mOnOff.isChecked()) {
            mNoWakeupAlarmText.setVisibility(View.GONE);
            mDismissBedtimeAlarmWhenRingtoneEnds.setVisibility(timeoutMinutes == -2 ? View.GONE : View.VISIBLE);
            mBedtimeAlarmSnoozeActions.setVisibility(snoozeMinutes == -1 ? View.GONE : View.VISIBLE);
            mVibrate.setVisibility(hasVibrator() ? View.VISIBLE : View.GONE);
            mRingtone.setVisibility(View.VISIBLE);
        } else {
            mNoWakeupAlarmText.setVisibility(View.VISIBLE);
            mDismissBedtimeAlarmWhenRingtoneEnds.setVisibility(timeoutMinutes == -2 ? View.GONE : View.INVISIBLE);
            mBedtimeAlarmSnoozeActions.setVisibility(snoozeMinutes == -1 ? View.GONE : View.INVISIBLE);
            mVibrate.setVisibility(hasVibrator() ? View.INVISIBLE : View.GONE);
            mRingtone.setVisibility(View.INVISIBLE);
        }
    }

    // ****************************
    // *   Bedtime bottom sheet   *
    // ****************************

    public void showBedtimeBottomSheetDialog() {
        mBottomSheetDialog = new BottomSheetDialog(mContext);
        mBottomSheetDialog.setContentView(R.layout.bedtime_bottom_sheet);
        mBottomSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        mClock = mBottomSheetDialog.findViewById(R.id.bedtime_time);
        mOnOff = mBottomSheetDialog.findViewById(R.id.toggle_switch_bedtime);
        mReminderNotificationTitle = mBottomSheetDialog.findViewById(R.id.reminder_notification_title);
        mNotificationList = mBottomSheetDialog.findViewById(R.id.notif_spinner);
        mNoBedtimeAlarmText = mBottomSheetDialog.findViewById(R.id.no_bedtime_alarm_text);

        final String getDarkMode = DataModel.getDataModel().getDarkMode();
        if (Utils.isNight(mContext.getResources()) && getDarkMode.equals(KEY_AMOLED_DARK_MODE)) {
            mNotificationList.getPopupBackground().setColorFilter(
                    MaterialColors.getColor(mContext, com.google.android.material.R.attr.colorSurface, Color.BLACK),
                    PorterDuff.Mode.SRC_IN);
            Objects.requireNonNull(mBottomSheetDialog.getWindow()).setNavigationBarColor(
                    MaterialColors.getColor(mContext, com.google.android.material.R.attr.colorSurface, Color.BLACK)
            );
        }

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

                mClock.setTypeface(mSaver.enabled ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                mClock.setAlpha(mSaver.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);

                mBedtimeText.setTypeface(mSaver.enabled ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                mBedtimeText.setAlpha(mSaver.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);

                Events.sendBedtimeEvent(checked ? R.string.action_enable : R.string.action_disable, R.string.label_deskclock);

                Utils.setVibrationTime(mContext, 50);

                hoursOfSleep(mAlarm);
            }

            if (!checked) {
                BedtimeService.cancelBedtimeMode(mContext, BedtimeService.ACTION_LAUNCH_BEDTIME);
                BedtimeService.cancelBedtimeMode(mContext, BedtimeService.ACTION_BED_REMIND_NOTIF);
                BedtimeService.cancelNotification(mContext);
            } else {
                BedtimeService.scheduleBedtimeMode(mContext, mSaver, BedtimeService.ACTION_LAUNCH_BEDTIME);
                if (mSaver.notificationShowTime != -1) {
                    BedtimeService.scheduleBedtimeMode(mContext, mSaver, BedtimeService.ACTION_BED_REMIND_NOTIF);
                }
            }

            bindNoBedtimeAlarmText();
        });

        mNotificationList.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] values = mContext.getResources().getStringArray(R.array.array_reminder_notification_values);
                mSaver.notificationShowTime = Integer.parseInt(values[position]);
                mSaver.save();
                LogUtils.wtf("value saved for notif time:", mSaver.notificationShowTime);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mBottomSheetDialog.show();
    }

    private void buildButton(BottomSheetDialog bottomSheetDialog){
        mRepeatDays = bottomSheetDialog.findViewById(R.id.repeat_days_bedtime);
        final LayoutInflater inflaters = LayoutInflater.from(mContext);
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        // Build button for each day.
        for (int i = 0; i < 7; i++) {
            final View dayButtonFrame = inflaters.inflate(R.layout.day_button, mRepeatDays, false);
            final CompoundButton dayButton = dayButtonFrame.findViewById(R.id.day_button_box);
            final int weekday = weekdays.get(i);
            dayButton.setChecked(true);
            dayButton.setTextColor(MaterialColors.getColor(
                    mContext, com.google.android.material.R.attr.colorOnSurfaceInverse, Color.BLACK));
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
                mSaver.save();

                Utils.setVibrationTime(mContext, 10);

                bindDaysOfBedButtons(mContext);
            });
        }
    }

    private void bindBedStuff() {
        bindDaysOfBedButtons(mContext);
        bindBedSwitch();
        bindBedClock();
        bindSpinner();
        bindNoBedtimeAlarmText();
    }

    private void bindDaysOfBedButtons(Context context) {
        final List<Integer> weekdays = DataModel.getDataModel().getWeekdayOrder().getCalendarDays();
        for (int i = 0; i < weekdays.size(); i++) {
            final CompoundButton dayButton = mDayButtons[i];
            if (mSaver.daysOfWeek.isBitOn(weekdays.get(i))) {
                dayButton.setChecked(true);
                dayButton.setTextColor(MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorOnSurfaceInverse, Color.BLACK));
            } else {
                dayButton.setChecked(false);
                dayButton.setTextColor(MaterialColors.getColor(
                        context, com.google.android.material.R.attr.colorSurfaceInverse, Color.BLACK));
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
        mClock.setTypeface(mSaver.enabled ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        mClock.setAlpha(mSaver.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
        bindFragBedClock();
        hoursOfSleep(getWakeupAlarm());
    }

    private void bindFragBedClock() {
        mBedtimeText.setTime(mSaver.hour, mSaver.minutes);
        mBedtimeText.setTypeface(mSaver.enabled ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        mBedtimeText.setAlpha(mSaver.enabled ? AlarmItemViewHolder.CLOCK_ENABLED_ALPHA : AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
    }

    private void bindSpinner() {
        mNotificationList.setAdapter(ArrayAdapter.createFromResource(mContext, R.array.array_reminder_notification, R.layout.spinner_item));
        mNotificationList.setSelection(getSpinnerPosition(mSaver.notificationShowTime,
                mContext.getResources().getStringArray(R.array.array_reminder_notification_values)));
    }

    private void bindNoBedtimeAlarmText() {
        if (mOnOff.isChecked()) {
            mNoBedtimeAlarmText.setVisibility(View.GONE);
            mNotificationList.setVisibility(View.VISIBLE);
            mReminderNotificationTitle.setVisibility(View.VISIBLE);
        } else {
            mNoBedtimeAlarmText.setVisibility(View.VISIBLE);
            mNotificationList.setVisibility(View.INVISIBLE);
            mReminderNotificationTitle.setVisibility(View.INVISIBLE);
        }
    }

    // ******************************
    // *   Bedtime mode functions   *
    // ******************************

    private void ShowMaterialTimePicker(int hour, int minute) {

        @TimeFormat int clockFormat;
        boolean isSystem24Hour = DateFormat.is24HourFormat(mContext);
        clockFormat = isSystem24Hour ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H;
        String getMaterialTimePickerStyle = DataModel.getDataModel().getMaterialTimePickerStyle();

        MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(clockFormat)
                .setInputMode(getMaterialTimePickerStyle.equals(MATERIAL_TIME_PICKER_ANALOG_STYLE)
                        ? MaterialTimePicker.INPUT_MODE_CLOCK
                        : MaterialTimePicker.INPUT_MODE_KEYBOARD)
                .setHour(hour)
                .setMinute(minute)
                .build();

        materialTimePicker.show(((AppCompatActivity) mContext).getSupportFragmentManager(), TAG);

        materialTimePicker.addOnPositiveButtonClickListener(dialog -> {
            int newHour = materialTimePicker.getHour();
            int newMinute = materialTimePicker.getMinute();
            onTimeSet(newHour, newMinute);
        });
    }

    private void onTimeSet(int hourOfDay, int minute) {
        if (mClock == mBottomSheetDialog.findViewById(R.id.wakeup_time)) {
            Alarm selectedAlarm = getWakeupAlarm();

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
            BedtimeService.scheduleBedtimeMode(mContext, mSaver, BedtimeService.ACTION_LAUNCH_BEDTIME);
            if (mSaver.notificationShowTime != -1) {
                BedtimeService.scheduleBedtimeMode(mContext, mSaver, BedtimeService.ACTION_BED_REMIND_NOTIF);
            }
        }
    }

    /**
     * Calculate the difference between wake-up time and bedtime
     */
    private void hoursOfSleep(Alarm alarm) {
        mHoursOfSleep = view.findViewById(R.id.hours_of_sleep);

        if (alarm != null) {
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
            } else if (hDiff == 0) {
                diff = minDiff + "min";
            } else {
                diff = hDiff + "h " + minDiff + "min";
            }

            mHoursOfSleep.setText(alarm.enabled || mSaver.enabled
                    ? mContext.getString(R.string.sleeping_time, diff)
                    : mContext.getString(R.string.alarm_inactive)
            );
        }
    }

    public Alarm getWakeupAlarm() {
        ContentResolver cr = mContext.getContentResolver();
        List<Alarm> alarms = Alarm.getAlarms(cr, Alarm.LABEL + "=?", BEDTIME_LABEL);
        if (!alarms.isEmpty()) {
            return alarms.get(0);
        }

        return null;
    }

    private void createAlarm() {
        final Alarm alarm = new Alarm();
        final boolean areAlarmVibrationsEnabledByDefault = DataModel.getDataModel().areAlarmVibrationsEnabledByDefault();
        alarm.id = BEDTIME_ID;
        alarm.hour = 8;
        alarm.minutes = 30;
        alarm.enabled = false;
        alarm.daysOfWeek = Weekdays.fromBits(31);
        alarm.label = BEDTIME_LABEL;
        alarm.alert = DataModel.getDataModel().getAlarmRingtoneUriFromSettings();
        alarm.dismissAlarmWhenRingtoneEnds = false;
        alarm.alarmSnoozeActions = true;
        alarm.vibrate = areAlarmVibrationsEnabledByDefault;
        mWakeupText.setTime(8, 30);
        mWakeupText.setAlpha(AlarmItemViewHolder.CLOCK_DISABLED_ALPHA);
        hoursOfSleep(alarm);
        AlarmUpdateHandler mAlarmUpdateHandler = new AlarmUpdateHandler(mContext, null, null);
        mAlarmUpdateHandler.asyncAddAlarmForBedtime(alarm);
        SnackbarManager.show(Snackbar.make(mMainLayout, R.string.new_bedtime_alarm, Snackbar.LENGTH_LONG));
    }

    private int getSpinnerPosition(int savedValue, String[] valueArray) {
        for (int i = 0; i < valueArray.length; i++){
            String value = valueArray[i];
            if (Integer.parseInt(value) == savedValue) {
                return i;
            }
        }
        return 0;
    }

    private boolean hasVibrator() {
        return ((Vibrator) mContext.getSystemService(VIBRATOR_SERVICE)).hasVibrator();
    }
}