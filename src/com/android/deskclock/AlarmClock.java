/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.deskclock;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.deskclock.widget.ActionableToastBar;
import com.android.deskclock.widget.swipeablelistview.SwipeableListView;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;

/**
 * AlarmClock application.
 */
public class AlarmClock extends Activity implements LoaderManager.LoaderCallbacks<Cursor>,
        AlarmTimePickerDialogFragment.AlarmTimePickerDialogHandler,
        AlarmLabelDialogFragment.AlarmLabelDialogHandler {

    private static final String KEY_EXPANDED_IDS = "expandedIds";
    private static final String KEY_REPEAT_CHECKED_IDS = "repeatCheckedIds";
    private static final String KEY_RINGTONE_TITLE_CACHE = "ringtoneTitleCache";
    private static final String KEY_DELETED_ALARM = "deletedAlarm";
    private static final String KEY_UNDO_SHOWING = "undoShowing";
    private static final String KEY_PREVIOUS_DAY_MAP = "previousDayMap";

    private static final int REQUEST_CODE_RINGTONE = 1;

    private SwipeableListView mAlarmsList;
    private AlarmItemAdapter mAdapter;
    private Bundle mRingtoneTitleCache; // Key: ringtone uri, value: ringtone title
    private ActionableToastBar mUndoBar;

    private Alarm mSelectedAlarm;
    private int mScrollToAlarmId = -1;

    // This flag relies on the activity having a "standard" launchMode and a new instance of this
    // activity being created when launched.
    private boolean mFirstLoad = true;

    // Saved states for undo
    private Alarm mDeletedAlarm;
    private boolean mUndoShowing = false;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        initialize(savedState);
        updateLayout();
        getLoaderManager().initLoader(0, null, this);
    }

    private void initialize(Bundle savedState) {
        setContentView(R.layout.alarm_clock);
        int[] expandedIds = null;
        int[] repeatCheckedIds = null;
        Bundle previousDayMap = null;
        if (savedState != null) {
            expandedIds = savedState.getIntArray(KEY_EXPANDED_IDS);
            repeatCheckedIds = savedState.getIntArray(KEY_REPEAT_CHECKED_IDS);
            mRingtoneTitleCache = savedState.getBundle(KEY_RINGTONE_TITLE_CACHE);
            mDeletedAlarm = savedState.getParcelable(KEY_DELETED_ALARM);
            mUndoShowing = savedState.getBoolean(KEY_UNDO_SHOWING);
            previousDayMap = savedState.getBundle(KEY_PREVIOUS_DAY_MAP);
        }

        mAdapter = new AlarmItemAdapter(this, expandedIds, repeatCheckedIds, previousDayMap);

        if (mRingtoneTitleCache == null) {
            mRingtoneTitleCache = new Bundle();
        }

        mAlarmsList = (SwipeableListView) findViewById(R.id.alarms_list);
        mAlarmsList.setAdapter(mAdapter);
        mAlarmsList.setVerticalScrollBarEnabled(true);
        mAlarmsList.enableSwipe(true);
        mAlarmsList.setOnCreateContextMenuListener(this);
        mAlarmsList.setOnItemSwipeListener(new SwipeableListView.OnItemSwipeListener() {
            @Override
            public void onSwipe(View view) {
                final AlarmItemAdapter.ItemHolder itemHolder =
                        (AlarmItemAdapter.ItemHolder) view.getTag();
                asyncDeleteAlarm(itemHolder.alarm);
            }
        });
        mAlarmsList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                hideUndoBar(true, event);
                return false;
            }
        });

        mUndoBar = (ActionableToastBar) findViewById(R.id.undo_bar);

        if (mUndoShowing) {
            mUndoBar.show(new ActionableToastBar.ActionClickedListener() {
                @Override
                public void onActionClicked() {
                    asyncAddAlarm(mDeletedAlarm);
                    mDeletedAlarm = null;
                    mUndoShowing = false;
                }
            }, 0, getResources().getString(R.string.alarm_deleted), true, R.string.alarm_undo,
                    true);
        }
    }

    private void hideUndoBar(boolean animate, MotionEvent event) {
        if (mUndoBar != null) {
            if (event != null && mUndoBar.isEventInToastBar(event)) {
                // Avoid touches inside the undo bar.
                return;
            }
            mUndoBar.hide(animate);
        }
        mDeletedAlarm = null;
        mUndoShowing = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(KEY_EXPANDED_IDS, mAdapter.getExpandedArray());
        outState.putIntArray(KEY_REPEAT_CHECKED_IDS, mAdapter.getRepeatArray());
        outState.putBundle(KEY_RINGTONE_TITLE_CACHE, mRingtoneTitleCache);
        outState.putParcelable(KEY_DELETED_ALARM, mDeletedAlarm);
        outState.putBoolean(KEY_UNDO_SHOWING, mUndoShowing);
        outState.putBundle(KEY_PREVIOUS_DAY_MAP, mAdapter.getPreviousDaysOfWeekMap());
    }

    private void updateLayout() {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ToastMaster.cancelToast();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        hideUndoBar(true, null);
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_item_add_alarm:
                asyncAddAlarm();
                return true;
            case android.R.id.home:
                Intent intent = new Intent(this, DeskClock.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.alarm_list_menu, menu);
        MenuItem help = menu.findItem(R.id.menu_item_help);
        if (help != null) {
            Utils.prepareHelpMenuItem(this, help);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // When the user places the app in the background by pressing "home",
        // dismiss the toast bar. However, since there is no way to determine if
        // home was pressed, just dismiss any existing toast bar when restarting
        // the app.
        if (mUndoBar != null) {
            hideUndoBar(false, null);
        }
    }

    // Callback used by AlarmTimePickerDialogFragment
    @Override
    public void onDialogTimeSet(Alarm alarm, int hourOfDay, int minute) {
        alarm.hour = hourOfDay;
        alarm.minutes = minute;
        alarm.enabled = true;
        mScrollToAlarmId = alarm.id;
        asyncUpdateAlarm(alarm, true);
    }

    private void showLabelDialog(final Alarm alarm) {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        final Fragment prev = getFragmentManager().findFragmentByTag("label_dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        final AlarmLabelDialogFragment newFragment = AlarmLabelDialogFragment.newInstance(alarm,
                alarm.label);
        newFragment.show(ft, "label_dialog");
    }

    // Callback used by AlarmLabelDialogFragment.
    @Override
    public void onDialogLabelSet(Alarm alarm, String label) {
        alarm.label = label;
        asyncUpdateAlarm(alarm, false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return Alarms.getAlarmsCursorLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, final Cursor data) {
        mAdapter.swapCursor(data);
        gotoAlarmIfSpecified();
    }

    /** If an alarm was passed in via intent and goes to that particular alarm in the list. */
    private void gotoAlarmIfSpecified() {
        final Intent intent = getIntent();
        if (mFirstLoad && intent != null) {
            final Alarm alarm = (Alarm) intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
            if (alarm != null) {
                scrollToAlarm(alarm.id);
            }
        } else if (mScrollToAlarmId != -1) {
            scrollToAlarm(mScrollToAlarmId);
            mScrollToAlarmId = -1;
        }
        mFirstLoad = false;
    }

    /**
     * Scroll to alarm with given alarm id.
     *
     * @param alarmId The alarm id to scroll to.
     */
    private void scrollToAlarm(int alarmId) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            long id = mAdapter.getItemId(i);
            if (id == alarmId) {
                mAdapter.setNewAlarm(alarmId);
                mAlarmsList.smoothScrollToPositionFromTop(i, 0);

                final int firstPositionId = mAlarmsList.getFirstVisiblePosition();
                final int childId = i - firstPositionId;

                final View view = mAlarmsList.getChildAt(childId);
                mAdapter.getView(i, view, mAlarmsList);
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    private void launchRingTonePicker(Alarm alarm) {
        mSelectedAlarm = alarm;
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarm.alert);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        startActivityForResult(intent, REQUEST_CODE_RINGTONE);
    }

    private void saveRingtoneUri(Intent intent) {
        final Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        mSelectedAlarm.alert = uri;
        asyncUpdateAlarm(mSelectedAlarm, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_RINGTONE:
                    saveRingtoneUri(data);
                    break;
                default:
                    Log.w("Unhandled request code in onActivityResult: " + requestCode);
            }
        }
    }

    public class AlarmItemAdapter extends CursorAdapter {

        private final Context mContext;
        private final LayoutInflater mFactory;
        private final String[] mShortWeekDayStrings;
        private final String[] mLongWeekDayStrings;
        private final int mColorLit;
        private final int mColorDim;
        private final int mColorRed;
        private final Typeface mRobotoNormal;
        private final Typeface mRobotoBold;

        private final HashSet<Integer> mExpanded = new HashSet<Integer>();
        private final HashSet<Integer> mRepeatChecked = new HashSet<Integer>();
        private Bundle mPreviousDaysOfWeekMap = new Bundle();

        private final boolean mHasVibrator;

        // This determines the order in which it is shown and processed in the UI.
        private final int[] DAY_ORDER = new int[] {
                Calendar.SUNDAY,
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
        };

        private class ItemHolder {

            // views for optimization
            LinearLayout alarmItem;
            DigitalClock clock;
            Switch onoff;
            TextView daysOfWeek;
            TextView label;
            View expandArea;
            View infoArea;
            TextView clickableLabel;
            CheckBox repeat;
            LinearLayout repeatDays;
            ToggleButton[] daysButtons = new ToggleButton[7];
            CheckBox vibrate;
            ViewGroup collapse;
            TextView ringtone;

            // Other states
            Alarm alarm;
        }

        public AlarmItemAdapter(Context context, int[] expandedIds, int[] repeatCheckedIds,
                Bundle previousDaysOfWeekMap) {
            super(context, null, 0);
            mContext = context;
            mFactory = LayoutInflater.from(context);

            DateFormatSymbols dfs = new DateFormatSymbols();
            mShortWeekDayStrings = dfs.getShortWeekdays();
            mLongWeekDayStrings = dfs.getWeekdays();

            mColorLit = mContext.getResources().getColor(R.color.clock_white);
            mColorDim = mContext.getResources().getColor(R.color.clock_gray);
            mColorRed = mContext.getResources().getColor(R.color.clock_red);

            mRobotoBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            mRobotoNormal = Typeface.create("sans-serif-condensed", Typeface.NORMAL);

            if (expandedIds != null) {
                buildHashSetFromArray(expandedIds, mExpanded);
            }
            if (repeatCheckedIds != null) {
                buildHashSetFromArray(repeatCheckedIds, mRepeatChecked);
            }
            if (previousDaysOfWeekMap != null) {
                mPreviousDaysOfWeekMap = previousDaysOfWeekMap;
            }

            mHasVibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                    .hasVibrator();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!getCursor().moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
            View v;
            if (convertView == null) {
                v = newView(mContext, getCursor(), parent);
            } else {
                // Do a translation check to test for animation. Change this to something more
                // reliable and robust in the future.
                if (convertView.getTranslationX() != 0 || convertView.getTranslationY() != 0) {
                    // view was animated, reset
                    v = newView(mContext, getCursor(), parent);
                } else {
                    v = convertView;
                }
            }
            bindView(v, mContext, getCursor());
            return v;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mFactory.inflate(R.layout.alarm_time, parent, false);

            // standard view holder optimization
            final ItemHolder holder = new ItemHolder();
            holder.alarmItem = (LinearLayout) view.findViewById(R.id.alarm_item);
            holder.clock = (DigitalClock) view.findViewById(R.id.digital_clock);
            holder.clock.setLive(false);
            holder.onoff = (Switch) view.findViewById(R.id.onoff);
            holder.onoff.setTypeface(mRobotoNormal);
            holder.daysOfWeek = (TextView) view.findViewById(R.id.daysOfWeek);
            holder.label = (TextView) view.findViewById(R.id.label);
            holder.expandArea = view.findViewById(R.id.expand_area);
            holder.infoArea = view.findViewById(R.id.info_area);
            holder.repeat = (CheckBox) view.findViewById(R.id.repeat_onoff);
            holder.clickableLabel = (TextView) view.findViewById(R.id.edit_label);
            holder.repeatDays = (LinearLayout) view.findViewById(R.id.repeat_days);

            // Build button for each day.
            for (int i = 0; i < 7; i++) {
                final ToggleButton button = (ToggleButton) mFactory.inflate(R.layout.day_button,
                        holder.repeatDays, false);
                final int dayToShowIndex = DAY_ORDER[i];
                button.setText(mShortWeekDayStrings[dayToShowIndex]);
                button.setTextOn(mShortWeekDayStrings[dayToShowIndex]);
                button.setTextOff(mShortWeekDayStrings[dayToShowIndex]);
                button.setContentDescription(mLongWeekDayStrings[dayToShowIndex]);
                holder.repeatDays.addView(button);
                holder.daysButtons[i] = button;
            }
            holder.vibrate = (CheckBox) view.findViewById(R.id.vibrate_onoff);
            holder.collapse = (ViewGroup) view.findViewById(R.id.collapse);
            holder.ringtone = (TextView) view.findViewById(R.id.choose_ringtone);

            view.setTag(holder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, final Cursor cursor) {
            final Alarm alarm = new Alarm(cursor);
            final ItemHolder itemHolder = (ItemHolder) view.getTag();
            itemHolder.alarm = alarm;

            // We must unset the listener first because this maybe a recycled view so changing the
            // state would affect the wrong alarm.
            itemHolder.onoff.setOnCheckedChangeListener(null);
            itemHolder.onoff.setChecked(alarm.enabled);
            if (itemHolder.onoff.isChecked()) {
                itemHolder.alarmItem.setAlpha(1f);
            } else {
                itemHolder.alarmItem.setAlpha(0.5f);
            }
            final CompoundButton.OnCheckedChangeListener onOffListener =
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton,
                                boolean checked) {
                            if (checked != alarm.enabled) {
                                if (checked) {
                                    itemHolder.alarmItem.setAlpha(1f);
                                } else {
                                    itemHolder.alarmItem.setAlpha(0.5f);
                                }
                                alarm.enabled = checked;
                                asyncUpdateAlarm(alarm, alarm.enabled);
                            }
                        }
                    };

            itemHolder.onoff.setOnCheckedChangeListener(onOffListener);

            itemHolder.clock.updateTime(alarm.hour, alarm.minutes);
            itemHolder.clock.setClickable(true);
            itemHolder.clock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlarmUtils.showTimeEditDialog(AlarmClock.this.getFragmentManager(), alarm);
                    expandAlarm(itemHolder);
                }
            });

            itemHolder.expandArea.setVisibility(isAlarmExpanded(alarm) ? View.VISIBLE : View.GONE);
            itemHolder.infoArea.setVisibility(!isAlarmExpanded(alarm) ? View.VISIBLE : View.GONE);
            itemHolder.infoArea.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    expandAlarm(itemHolder);
                }
            });

            String colons = "";
            // Set the repeat text or leave it blank if it does not repeat.
            final String daysOfWeekStr = alarm.daysOfWeek.toString(AlarmClock.this, false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                itemHolder.daysOfWeek.setText(daysOfWeekStr);
                itemHolder.daysOfWeek.setContentDescription(
                        alarm.daysOfWeek.toAccessibilityString(AlarmClock.this));
                itemHolder.daysOfWeek.setVisibility(View.VISIBLE);
                colons = ": ";
            } else {
                itemHolder.daysOfWeek.setVisibility(View.GONE);
            }

            if (alarm.label != null && alarm.label.length() != 0) {
                itemHolder.label.setText(alarm.label + colons);
                itemHolder.label.setVisibility(View.VISIBLE);
                itemHolder.label.setContentDescription(
                        mContext.getResources().getString(R.string.label_description) + " "
                        + alarm.label);
            } else {
                itemHolder.label.setVisibility(View.GONE);
            }

            if (isAlarmExpanded(alarm)) {
                expandAlarm(itemHolder);
            }
        }

        private void bindExpandArea(final ItemHolder itemHolder, final Alarm alarm) {
            // Views in here are not bound until the item is expanded.

            if (alarm.label != null && alarm.label.length() > 0) {
                itemHolder.clickableLabel.setText(alarm.label);
                itemHolder.clickableLabel.setTextColor(mColorLit);
            } else {
                itemHolder.clickableLabel.setText(R.string.label);
                itemHolder.clickableLabel.setTextColor(mColorDim);
            }
            itemHolder.clickableLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showLabelDialog(alarm);
                }
            });

            if (mRepeatChecked.contains(alarm.id) || itemHolder.alarm.daysOfWeek.isRepeatSet()) {
                itemHolder.repeat.setChecked(true);
                itemHolder.repeatDays.setVisibility(View.VISIBLE);
            } else {
                itemHolder.repeat.setChecked(false);
                itemHolder.repeatDays.setVisibility(View.GONE);
            }
            itemHolder.repeat.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final boolean checked = ((CheckBox) view).isChecked();
                    if (checked) {
                        // Show days
                        itemHolder.repeatDays.setVisibility(View.VISIBLE);
                        mRepeatChecked.add(alarm.id);

                        // Set all previously set days
                        // or
                        // Set all days if no previous.
                        final int daysOfWeekCoded = mPreviousDaysOfWeekMap.getInt("" + alarm.id);
                        if (daysOfWeekCoded == 0) {
                            for (int day : DAY_ORDER) {
                                alarm.daysOfWeek.setDayOfWeek(day, true);
                            }
                        } else {
                            alarm.daysOfWeek.set(new Alarm.DaysOfWeek(daysOfWeekCoded));
                        }
                        updateDaysOfWeekButtons(itemHolder, alarm.daysOfWeek);
                    } else {
                        itemHolder.repeatDays.setVisibility(View.GONE);
                        mRepeatChecked.remove(alarm.id);

                        // Remember the set days in case the user wants it back.
                        final int daysOfWeekCoded = alarm.daysOfWeek.getCoded();
                        mPreviousDaysOfWeekMap.putInt("" + alarm.id, daysOfWeekCoded);

                        // Remove all repeat days
                        alarm.daysOfWeek.set(new Alarm.DaysOfWeek(0));
                    }
                    asyncUpdateAlarm(alarm, false);
                }
            });

            updateDaysOfWeekButtons(itemHolder, alarm.daysOfWeek);
            for (int i = 0; i < 7; i++) {
                final int buttonIndex = i;
                itemHolder.daysButtons[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final boolean checked = ((ToggleButton) view).isChecked();
                        int day = DAY_ORDER[buttonIndex];
                        alarm.daysOfWeek.setDayOfWeek(day, checked);
                        if (checked) {
                            turnOnDayOfWeek(itemHolder, buttonIndex);
                        } else {
                            turnOffDayOfWeek(itemHolder, buttonIndex);

                            // See if this was the last day, if so, un-check the repeat box.
                            if (alarm.daysOfWeek.getCoded() == 0) {
                                itemHolder.repeatDays.setVisibility(View.GONE);
                                itemHolder.repeat.setTextColor(mColorDim);
                                mRepeatChecked.remove(alarm.id);

                                // Remember the set days in case the user wants it back.
                                mPreviousDaysOfWeekMap.putInt("" + alarm.id, 0);
                            }
                        }
                        asyncUpdateAlarm(alarm, false);
                    }
                });
            }


            if (!mHasVibrator) {
                itemHolder.vibrate.setVisibility(View.INVISIBLE);
            } else {
                itemHolder.vibrate.setVisibility(View.VISIBLE);
                if (!alarm.vibrate) {
                    itemHolder.vibrate.setChecked(false);
                    itemHolder.vibrate.setTextColor(mColorDim);
                } else {
                    itemHolder.vibrate.setChecked(true);
                    itemHolder.vibrate.setTextColor(mColorLit);
                }
            }

            itemHolder.vibrate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final boolean checked = ((CheckBox) v).isChecked();
                    if (checked) {
                        itemHolder.vibrate.setTextColor(mColorLit);
                    } else {
                        itemHolder.vibrate.setTextColor(mColorDim);
                    }
                    alarm.vibrate = checked;
                    asyncUpdateAlarm(alarm, false);
                }
            });

            itemHolder.collapse.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemHolder.expandArea.setVisibility(LinearLayout.GONE);
                    itemHolder.infoArea.setVisibility(View.VISIBLE);
                    collapseAlarm(alarm);
                }
            });

            final String ringtone;
            if (alarm.alert == null) {
                ringtone = mContext.getResources().getString(R.string.silent_alarm_summary);
            } else {
                ringtone = getRingToneTitle(alarm.alert);
            }
            itemHolder.ringtone.setText(ringtone);
            itemHolder.ringtone.setContentDescription(
                    mContext.getResources().getString(R.string.ringtone_description) + " "
                    + ringtone);
            itemHolder.ringtone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launchRingTonePicker(alarm);
                }
            });
        }

        private void updateDaysOfWeekButtons(ItemHolder holder, Alarm.DaysOfWeek daysOfWeek) {
            HashSet<Integer> setDays = daysOfWeek.getSetDays();
            for (int i = 0; i < 7; i++) {
                if (setDays.contains(DAY_ORDER[i])) {
                    turnOnDayOfWeek(holder, i);
                } else {
                    turnOffDayOfWeek(holder, i);
                }
            }
        }

        private void turnOffDayOfWeek(ItemHolder holder, int dayIndex) {
            holder.daysButtons[dayIndex].setChecked(false);
            holder.daysButtons[dayIndex].setTextColor(mColorDim);
            holder.daysButtons[dayIndex].setTypeface(mRobotoNormal);
        }

        private void turnOnDayOfWeek(ItemHolder holder, int dayIndex) {
            holder.daysButtons[dayIndex].setChecked(true);
            holder.daysButtons[dayIndex].setTextColor(mColorLit);
            holder.daysButtons[dayIndex].setTypeface(mRobotoBold);
        }


        /**
         * Does a read-through cache for ringtone titles.
         *
         * @param uri The uri of the ringtone.
         * @return The ringtone title. {@literal null} if no matching ringtone found.
         */
        private String getRingToneTitle(Uri uri) {
            // Try the cache first
            String title = mRingtoneTitleCache.getString(uri.toString());
            if (title == null) {
                // This is slow because a media player is created during Ringtone object creation.
                Ringtone ringTone = RingtoneManager.getRingtone(mContext, uri);
                title = ringTone.getTitle(mContext);
                if (title != null) {
                    mRingtoneTitleCache.putString(uri.toString(), title);
                }
            }
            return title;
        }

        public void setNewAlarm(int alarmId) {
            mExpanded.add(alarmId);
        }

        /**
         * Expands the alarm for editing.
         *
         * @param itemHolder The item holder instance.
         */
        private void expandAlarm(ItemHolder itemHolder) {
            itemHolder.expandArea.setVisibility(View.VISIBLE);
            itemHolder.infoArea.setVisibility(View.GONE);

            mExpanded.add(itemHolder.alarm.id);
            bindExpandArea(itemHolder, itemHolder.alarm);
        }

        private boolean isAlarmExpanded(Alarm alarm) {
            return mExpanded.contains(alarm.id);
        }

        private void collapseAlarm(Alarm alarm) {
            mExpanded.remove(alarm.id);
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        public int[] getExpandedArray() {
            final int[] ids = new int[mExpanded.size()];
            int index = 0;
            for (int id : mExpanded) {
                ids[index] = id;
                index++;
            }
            return ids;
        }

        public int[] getRepeatArray() {
            final int[] ids = new int[mRepeatChecked.size()];
            int index = 0;
            for (int id : mRepeatChecked) {
                ids[index] = id;
                index++;
            }
            return ids;
        }

        public Bundle getPreviousDaysOfWeekMap() {
            return mPreviousDaysOfWeekMap;
        }

        private void buildHashSetFromArray(int[] ids, HashSet<Integer> set) {
            for (int id : ids) {
                set.add(id);
            }
        }
    }

    private void asyncAddAlarm() {
        asyncAddAlarm(new Alarm());
    }

    private void asyncDeleteAlarm(final Alarm alarm) {
        final AsyncTask<Alarm, Void, Void> deleteTask = new AsyncTask<Alarm, Void, Void>() {

            @Override
            protected Void doInBackground(Alarm... alarms) {
                for (final Alarm alarm : alarms) {
                    Alarms.deleteAlarm(AlarmClock.this, alarm.id);
                }
                return null;
            }
        };
        mDeletedAlarm = alarm;
        mUndoShowing = true;
        deleteTask.execute(alarm);
        mUndoBar.show(new ActionableToastBar.ActionClickedListener() {
            @Override
            public void onActionClicked() {
                asyncAddAlarm(alarm);
                mDeletedAlarm = null;
                mUndoShowing = false;
            }
        }, 0, getResources().getString(R.string.alarm_deleted), true, R.string.alarm_undo, true);
    }

    private void asyncAddAlarm(final Alarm alarm) {
        final AsyncTask<Void, Void, Void> updateTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... aVoid) {
                Alarms.addAlarm(AlarmClock.this, alarm);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (alarm.enabled) {
                    popToast(alarm);
                }
                mAdapter.setNewAlarm(alarm.id);
                scrollToAlarm(alarm.id);

                // We need to refresh the first view item because bindView may have been called
                // before setNewAlarm took effect. In that case, the newly created alarm will not be
                // expanded.
                View view = mAlarmsList.getChildAt(0);
                mAdapter.getView(0, view, mAlarmsList);
            }
        };
        updateTask.execute();
    }

    private void asyncUpdateAlarm(final Alarm alarm, final boolean popToast) {
        final AsyncTask<Alarm, Void, Void> updateTask = new AsyncTask<Alarm, Void, Void>() {
            @Override
            protected Void doInBackground(Alarm... alarms) {
                for (final Alarm alarm : alarms) {
                    Alarms.setAlarm(AlarmClock.this, alarm);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (popToast) {
                    popToast(alarm);
                }
            }
        };
        updateTask.execute(alarm);
    }

    private void popToast(Alarm alarm) {
        AlarmUtils.popAlarmSetToast(this, alarm.hour, alarm.minutes, alarm.daysOfWeek);
    }
}
