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

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.deskclock.provider.Alarm;
import com.android.deskclock.widget.ActionableToastBar;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;

/**
 * AlarmClock application.
 */
public class AlarmClockFragment extends DeskClockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AlarmTimePickerDialogFragment.AlarmTimePickerDialogHandler,
        DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    private static final String KEY_EXPANDED_IDS = "expandedIds";
    private static final String KEY_REPEAT_CHECKED_IDS = "repeatCheckedIds";
    private static final String KEY_RINGTONE_TITLE_CACHE = "ringtoneTitleCache";
    private static final String KEY_SELECTED_ALARMS = "selectedAlarms";
    private static final String KEY_DELETED_ALARM = "deletedAlarm";
    private static final String KEY_UNDO_SHOWING = "undoShowing";
    private static final String KEY_PREVIOUS_DAY_MAP = "previousDayMap";
    private static final String KEY_SELECTED_ALARM = "selectedAlarm";
    private static final String KEY_DELETE_CONFIRMATION = "deleteConfirmation";

    private static final int REQUEST_CODE_RINGTONE = 1;

    private ListView mAlarmsList;
    private AlarmItemAdapter mAdapter;
    private View mEmptyView;
    private ImageView mAddAlarmButton;
    private Bundle mRingtoneTitleCache; // Key: ringtone uri, value: ringtone title
    private ActionableToastBar mUndoBar;

    private Alarm mSelectedAlarm;
    private int mScrollToAlarmId = -1;
    private boolean mInDeleteConfirmation = false;

    // This flag relies on the activity having a "standard" launchMode and a new instance of this
    // activity being created when launched.
    private boolean mFirstLoad = true;

    // Saved states for undo
    private Alarm mDeletedAlarm;
    private boolean mUndoShowing = false;

    public AlarmClockFragment() {
        // Basic provider required by Fragment.java
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getLoaderManager().initLoader(0, null, this);

        if (mInDeleteConfirmation) {
            showConfirmationDialog();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.alarm_clock, container, false);

        int[] expandedIds = null;
        int[] repeatCheckedIds = null;
        int[] selectedAlarms = null;
        Bundle previousDayMap = null;
        Log.v("oncreateview");
        if (savedState != null) {
            expandedIds = savedState.getIntArray(KEY_EXPANDED_IDS);
            Log.v("expanded: "+expandedIds);
            repeatCheckedIds = savedState.getIntArray(KEY_REPEAT_CHECKED_IDS);
            mRingtoneTitleCache = savedState.getBundle(KEY_RINGTONE_TITLE_CACHE);
            mDeletedAlarm = savedState.getParcelable(KEY_DELETED_ALARM);
            mUndoShowing = savedState.getBoolean(KEY_UNDO_SHOWING);
            selectedAlarms = savedState.getIntArray(KEY_SELECTED_ALARMS);
            previousDayMap = savedState.getBundle(KEY_PREVIOUS_DAY_MAP);
            mSelectedAlarm = savedState.getParcelable(KEY_SELECTED_ALARM);
            mInDeleteConfirmation = savedState.getBoolean(KEY_DELETE_CONFIRMATION, false);
        }

        mAddAlarmButton = (ImageButton) v.findViewById(R.id.alarm_add_alarm);
        mAddAlarmButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                asyncAddAlarm();
            }
        });
        // For landscape, put the add button on the right and the menu in the actionbar.
        View menuButton = v.findViewById(R.id.menu_button);
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) mAddAlarmButton.getLayoutParams();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutParams.gravity = Gravity.END;
            menuButton.setVisibility(View.GONE);
        } else {
            layoutParams.gravity = Gravity.CENTER;
            menuButton.setVisibility(View.VISIBLE);
        }
        mAddAlarmButton.setLayoutParams(layoutParams);


        mEmptyView = v.findViewById(R.id.alarms_empty_view);
        mAlarmsList = (ListView) v.findViewById(R.id.alarms_list);
        View footerView = inflater.inflate(R.layout.blank_footer_view, mAlarmsList, false);
        footerView.setBackgroundResource(R.color.blackish);
        mAlarmsList.addFooterView(footerView);
        mAdapter = new AlarmItemAdapter(getActivity(),
                expandedIds, repeatCheckedIds, selectedAlarms, previousDayMap, mAlarmsList);
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                // Hide/show the empty state.
                if (mAdapter.getCount() == 0) {
                    mAddAlarmButton.setBackgroundResource(R.drawable.main_button_red);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mAddAlarmButton.setBackgroundResource(R.drawable.main_button_normal);
                    mEmptyView.setVisibility(View.GONE);
                }
                super.onChanged();
            }
        });

        if (mRingtoneTitleCache == null) {
            mRingtoneTitleCache = new Bundle();
        }

        mAlarmsList.setAdapter(mAdapter);
        mAlarmsList.setVerticalScrollBarEnabled(true);
        mAlarmsList.setOnCreateContextMenuListener(this);
        mAlarmsList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                hideUndoBar(true, event);
                return false;
            }
        });

        mUndoBar = (ActionableToastBar) v.findViewById(R.id.undo_bar);

        if (mUndoShowing) {
            mUndoBar.show(new ActionableToastBar.ActionClickedListener() {
                @Override
                public void onActionClicked() {
                    asyncAddAlarm(mDeletedAlarm, false);
                    mDeletedAlarm = null;
                    mUndoShowing = false;
                }
            }, 0, getResources().getString(R.string.alarm_deleted), true, R.string.alarm_undo,
                    true);
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if another app asked us to create a blank new alarm.
        final Intent intent = getActivity().getIntent();
        boolean createNew = intent.getBooleanExtra(Alarms.ALARM_CREATE_NEW, false);
        if (createNew) {
            // An external app asked us to create a blank alarm.
            asyncAddAlarm();
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntArray(KEY_EXPANDED_IDS, mAdapter.getExpandedArray());
        outState.putIntArray(KEY_REPEAT_CHECKED_IDS, mAdapter.getRepeatArray());
        outState.putIntArray(KEY_SELECTED_ALARMS, mAdapter.getSelectedAlarmsArray());
        outState.putBundle(KEY_RINGTONE_TITLE_CACHE, mRingtoneTitleCache);
        outState.putParcelable(KEY_DELETED_ALARM, mDeletedAlarm);
        outState.putBoolean(KEY_UNDO_SHOWING, mUndoShowing);
        outState.putBundle(KEY_PREVIOUS_DAY_MAP, mAdapter.getPreviousDaysOfWeekMap());
        outState.putParcelable(KEY_SELECTED_ALARM, mSelectedAlarm);
        outState.putBoolean(KEY_DELETE_CONFIRMATION, mInDeleteConfirmation);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToastMaster.cancelToast();
    }

    @Override
    public void onPause() {
        super.onPause();
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
        final LabelDialogFragment newFragment =
                LabelDialogFragment.newInstance(alarm, alarm.label, getTag());
        newFragment.show(ft, "label_dialog");
    }

    public void setLabel(Alarm alarm, String label) {
        alarm.label = label;
        asyncUpdateAlarm(alarm, false);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return Alarms.getAlarmsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, final Cursor data) {
        mAdapter.swapCursor(data);
        gotoAlarmIfSpecified();
    }

    /** If an alarm was passed in via intent and goes to that particular alarm in the list. */
    private void gotoAlarmIfSpecified() {
        final Intent intent = getActivity().getIntent();
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
        // Save the last selected ringtone as the default for new alarms
        if (uri != null) {
            RingtoneManager.setActualDefaultRingtoneUri(
                    getActivity(), RingtoneManager.TYPE_ALARM, uri);
        }
        asyncUpdateAlarm(mSelectedAlarm, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
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
        private static final float EXPAND_DECELERATION = 1f;
        private static final float COLLAPSE_DECELERATION = 0.7f;
        private static final int EXPAND_DURATION = 300;
        private static final int COLLAPSE_DURATION = 250;

        private final Context mContext;
        private final LayoutInflater mFactory;
        private final String[] mShortWeekDayStrings;
        private final String[] mLongWeekDayStrings;
        private final int mColorLit;
        private final int mColorDim;
        private final int mBackgroundColorExpanded;
        private final int mBackgroundColor;
        private final Typeface mRobotoNormal;
        private final Typeface mRobotoBold;
        private final ListView mList;
        private final Interpolator mExpandInterpolator;
        private final Interpolator mCollapseInterpolator;

        private final HashSet<Integer> mExpanded = new HashSet<Integer>();
        private final HashSet<Integer> mRepeatChecked = new HashSet<Integer>();
        private final HashSet<Integer> mSelectedAlarms = new HashSet<Integer>();
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

        public class ItemHolder {

            // views for optimization
            LinearLayout alarmItem;
            DigitalClock clock;
            Switch onoff;
            TextView daysOfWeek;
            TextView label;
            ImageView delete;
            View expandArea;
            View summary;
            TextView clickableLabel;
            CheckBox repeat;
            LinearLayout repeatDays;
            ViewGroup[] dayButtonParents = new ViewGroup[7];
            ToggleButton[] dayButtons = new ToggleButton[7];
            CheckBox vibrate;
            TextView ringtone;
            View hairLine;
            View arrow;
            View collapseExpandArea;

            // Other states
            Alarm alarm;
        }

        // Used for scrolling an expanded item in the list to make sure it is fully visible.
        private int mScrollAlarmId = -1;
        private final Runnable mScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (mScrollAlarmId != -1) {
                    View v = getViewById(mScrollAlarmId);
                    if (v != null) {
                        Rect rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        mList.requestChildRectangleOnScreen(v, rect, false);
                    }
                    mScrollAlarmId = -1;
                }
            }
        };

        public AlarmItemAdapter(Context context, int[] expandedIds, int[] repeatCheckedIds,
                int[] selectedAlarms, Bundle previousDaysOfWeekMap, ListView list) {
            super(context, null, 0);
            mContext = context;
            mFactory = LayoutInflater.from(context);
            mList = list;

            DateFormatSymbols dfs = new DateFormatSymbols();
            mShortWeekDayStrings = dfs.getShortWeekdays();
            mLongWeekDayStrings = dfs.getWeekdays();

            Resources res = mContext.getResources();
            mColorLit = res.getColor(R.color.clock_white);
            mColorDim = res.getColor(R.color.clock_gray);
            mBackgroundColorExpanded = res.getColor(R.color.alarm_whiteish);
            mBackgroundColor = R.drawable.alarm_background_normal;

            mExpandInterpolator = new DecelerateInterpolator(EXPAND_DECELERATION);
            mCollapseInterpolator = new DecelerateInterpolator(COLLAPSE_DECELERATION);

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
            if (selectedAlarms != null) {
                buildHashSetFromArray(selectedAlarms, mSelectedAlarms);
            }

            mHasVibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
                    .hasVibrator();
        }

        public void removeSelectedId(int id) {
            mSelectedAlarms.remove(id);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!getCursor().moveToPosition(position)) {
                // May happen if the last alarm was deleted and the cursor refreshed while the
                // list is updated.
                Log.v("couldn't move cursor to position " + position);
                return null;
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
            holder.delete = (ImageView) view.findViewById(R.id.delete);
            holder.summary = view.findViewById(R.id.summary);
            holder.expandArea = view.findViewById(R.id.expand_area);
            holder.hairLine = view.findViewById(R.id.hairline);
            holder.arrow = view.findViewById(R.id.arrow);
            holder.repeat = (CheckBox) view.findViewById(R.id.repeat_onoff);
            holder.clickableLabel = (TextView) view.findViewById(R.id.edit_label);
            holder.repeatDays = (LinearLayout) view.findViewById(R.id.repeat_days);
            holder.collapseExpandArea = view.findViewById(R.id.collapse_expand);

            // Build button for each day.
            for (int i = 0; i < 7; i++) {
                final ViewGroup viewgroup = (ViewGroup) mFactory.inflate(R.layout.day_button,
                        holder.repeatDays, false);
                final ToggleButton button = (ToggleButton) viewgroup.getChildAt(0);
                final int dayToShowIndex = DAY_ORDER[i];
                button.setText(mShortWeekDayStrings[dayToShowIndex]);
                button.setTextOn(mShortWeekDayStrings[dayToShowIndex]);
                button.setTextOff(mShortWeekDayStrings[dayToShowIndex]);
                button.setContentDescription(mLongWeekDayStrings[dayToShowIndex]);
                holder.repeatDays.addView(viewgroup);
                holder.dayButtons[i] = button;
                holder.dayButtonParents[i] = viewgroup;
            }
            holder.vibrate = (CheckBox) view.findViewById(R.id.vibrate_onoff);
            holder.ringtone = (TextView) view.findViewById(R.id.choose_ringtone);

            view.setTag(holder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, final Cursor cursor) {
            final Alarm alarm = new Alarm(cursor);
            final ItemHolder itemHolder = (ItemHolder) view.getTag();
            if (itemHolder == null) {
                // TODO I was seeing NPE here a few times but unable to repro now, keep this check
                // in to hopefully see it again soon.
                Log.wtf("itemholder is null?? alarm:"+alarm.toString());
            }
            itemHolder.alarm = alarm;

            // We must unset the listener first because this maybe a recycled view so changing the
            // state would affect the wrong alarm.
            itemHolder.onoff.setOnCheckedChangeListener(null);
            itemHolder.onoff.setChecked(alarm.enabled);

            if (mSelectedAlarms.contains(itemHolder.alarm.id)) {
                itemHolder.alarmItem.setBackgroundColor(mBackgroundColorExpanded);
                setItemAlpha(itemHolder, true);
                itemHolder.onoff.setEnabled(false);
            } else {
                itemHolder.onoff.setEnabled(true);
                itemHolder.alarmItem.setBackgroundResource(mBackgroundColor);
                setItemAlpha(itemHolder, itemHolder.onoff.isChecked());
            }

            itemHolder.clock.updateTime(alarm.hour, alarm.minutes);
            itemHolder.clock.setClickable(true);
            itemHolder.clock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlarmUtils.showTimeEditDialog(AlarmClockFragment.this.getFragmentManager(),
                            alarm, AlarmClockFragment.this);
                    expandAlarm(itemHolder, true);
                    itemHolder.alarmItem.post(mScrollRunnable);
                }
            });

            final CompoundButton.OnCheckedChangeListener onOffListener =
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton,
                                boolean checked) {
                            if (checked != alarm.enabled) {
                                setItemAlpha(itemHolder, checked);
                                alarm.enabled = checked;
                                asyncUpdateAlarm(alarm, alarm.enabled);
                            }
                        }
                    };

            itemHolder.onoff.setOnCheckedChangeListener(onOffListener);

            boolean expanded = isAlarmExpanded(alarm);
            itemHolder.expandArea.setVisibility(expanded? View.VISIBLE : View.GONE);
            itemHolder.summary.setVisibility(expanded? View.GONE : View.VISIBLE);

            String colons = "";
            // Set the repeat text or leave it blank if it does not repeat.
            final String daysOfWeekStr =
                    alarm.daysOfWeek.toString(AlarmClockFragment.this.getActivity(), false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                itemHolder.daysOfWeek.setText(daysOfWeekStr);
                itemHolder.daysOfWeek.setContentDescription(alarm.daysOfWeek.toAccessibilityString(
                        AlarmClockFragment.this.getActivity()));
                itemHolder.daysOfWeek.setVisibility(View.VISIBLE);
                colons = ": ";
                itemHolder.daysOfWeek.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        expandAlarm(itemHolder, true);
                        itemHolder.alarmItem.post(mScrollRunnable);
                    }
                });

            } else {
                itemHolder.daysOfWeek.setVisibility(View.GONE);
            }

            if (alarm.label != null && alarm.label.length() != 0) {
                itemHolder.label.setText(alarm.label + colons);
                itemHolder.label.setVisibility(View.VISIBLE);
                itemHolder.label.setContentDescription(
                        mContext.getResources().getString(R.string.label_description) + " "
                        + alarm.label);
                itemHolder.label.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        expandAlarm(itemHolder, true);
                        itemHolder.alarmItem.post(mScrollRunnable);
                    }
                });
            } else {
                itemHolder.label.setVisibility(View.GONE);
            }

            itemHolder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    asyncDeleteAlarm(alarm);
                }
            });

            if (expanded) {
                expandAlarm(itemHolder, false);
            }

            itemHolder.alarmItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isAlarmExpanded(alarm)) {
                        collapseAlarm(itemHolder);
                    } else {
                        expandAlarm(itemHolder, true);
                    }
                }
            });
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

            if (mRepeatChecked.contains(alarm.id) || itemHolder.alarm.daysOfWeek.isRepeating()) {
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
                        final int bitSet = mPreviousDaysOfWeekMap.getInt("" + alarm.id);
                        alarm.daysOfWeek.setBitSet(bitSet);
                        if (!alarm.daysOfWeek.isRepeating()) {
                            alarm.daysOfWeek.setDaysOfWeek(true, DAY_ORDER);
                        }
                        updateDaysOfWeekButtons(itemHolder, alarm.daysOfWeek);
                    } else {
                        itemHolder.repeatDays.setVisibility(View.GONE);
                        mRepeatChecked.remove(alarm.id);

                        // Remember the set days in case the user wants it back.
                        final int bitSet = alarm.daysOfWeek.getBitSet();
                        mPreviousDaysOfWeekMap.putInt("" + alarm.id, bitSet);

                        // Remove all repeat days
                        alarm.daysOfWeek.clearAllDays();
                    }
                    asyncUpdateAlarm(alarm, false);
                }
            });

            updateDaysOfWeekButtons(itemHolder, alarm.daysOfWeek);
            for (int i = 0; i < 7; i++) {
                final int buttonIndex = i;

                itemHolder.dayButtonParents[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        itemHolder.dayButtons[buttonIndex].toggle();
                        final boolean checked = itemHolder.dayButtons[buttonIndex].isChecked();
                        int day = DAY_ORDER[buttonIndex];
                        alarm.daysOfWeek.setDaysOfWeek(checked, day);
                        if (checked) {
                            turnOnDayOfWeek(itemHolder, buttonIndex);
                        } else {
                            turnOffDayOfWeek(itemHolder, buttonIndex);

                            // See if this was the last day, if so, un-check the repeat box.
                            if (!alarm.daysOfWeek.isRepeating()) {
                                itemHolder.repeatDays.setVisibility(View.GONE);
                                itemHolder.repeat.setTextColor(mColorDim);
                                mRepeatChecked.remove(alarm.id);

                                // Set history to no days, so it will be everyday when repeat is
                                // turned back on
                                mPreviousDaysOfWeekMap.putInt("" + alarm.id,
                                        Alarm.DaysOfWeek.NO_DAYS_SET);
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

        // Sets the alpha of the item except the on/off switch. This gives a visual effect
        // for enabled/disabled alarm while leaving the on/off switch more visible
        private void setItemAlpha(ItemHolder holder, boolean enabled) {
            float alpha = enabled ? 1f : 0.5f;
            holder.clock.setAlpha(alpha);
            holder.summary.setAlpha(alpha);
            holder.expandArea.setAlpha(alpha);
            holder.delete.setAlpha(alpha);
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

        public void toggleSelectState(View v) {
            // long press could be on the parent view or one of its childs, so find the parent view
            v = getTopParent(v);
            if (v != null) {
                int id = ((ItemHolder)v.getTag()).alarm.id;
                if (mSelectedAlarms.contains(id)) {
                    mSelectedAlarms.remove(id);
                } else {
                    mSelectedAlarms.add(id);
                }
            }
        }

        private View getTopParent(View v) {
            while (v != null && v.getId() != R.id.alarm_item) {
                v = (View) v.getParent();
            }
            return v;
        }

        public int getSelectedItemsNum() {
            return mSelectedAlarms.size();
        }

        private void turnOffDayOfWeek(ItemHolder holder, int dayIndex) {
            holder.dayButtons[dayIndex].setChecked(false);
            holder.dayButtons[dayIndex].setTextColor(mColorDim);
            holder.dayButtons[dayIndex].setTypeface(mRobotoNormal);
        }

        private void turnOnDayOfWeek(ItemHolder holder, int dayIndex) {
            holder.dayButtons[dayIndex].setChecked(true);
            holder.dayButtons[dayIndex].setTextColor(mColorLit);
            holder.dayButtons[dayIndex].setTypeface(mRobotoBold);
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
        private void expandAlarm(final ItemHolder itemHolder, boolean animate) {
            mExpanded.add(itemHolder.alarm.id);
            bindExpandArea(itemHolder, itemHolder.alarm);
            // Scroll the view to make sure it is fully viewed
            mScrollAlarmId = itemHolder.alarm.id;

            // Save the starting height so we can animate from this value.
            final int startingHeight = itemHolder.alarmItem.getHeight();

            // Set the expand area to visible so we can measure the height to animate to.
            itemHolder.alarmItem.setBackgroundColor(mBackgroundColorExpanded);
            itemHolder.expandArea.setVisibility(View.VISIBLE);

            if (!animate) {
                // Set the "end" layout and don't do the animation.
                itemHolder.arrow.setRotation(180);
                int hairlineHeight = itemHolder.hairLine.getHeight();
                int collapseHeight = itemHolder.collapseExpandArea.getHeight() - hairlineHeight;
                itemHolder.hairLine.setTranslationY(-collapseHeight);
                return;
            }

            // Add an onPreDrawListener, which gets called after measurement but before the draw.
            // This way we can check the height we need to animate to before any drawing.
            // Note the series of events:
            //  * expandArea is set to VISIBLE, which causes a layout pass
            //  * the view is measured, and our onPreDrawListener is called
            //  * we set up the animation using the start and end values.
            //  * the height is set back to the starting point so it can be animated down.
            //  * request another layout pass.
            //  * return false so that onDraw() is not called for the single frame before
            //    the animations have started.
            final ViewTreeObserver observer = mAlarmsList.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    // We don't want to continue getting called for every listview drawing.
                    observer.removeOnPreDrawListener(this);

                    // Calculate some values to help with the animation.
                    final int endingHeight = itemHolder.alarmItem.getHeight();
                    final int distance = endingHeight - startingHeight;
                    final int collapseHeight = itemHolder.collapseExpandArea.getHeight();
                    int hairlineHeight = itemHolder.hairLine.getHeight();
                    final int hairlineDistance = collapseHeight - hairlineHeight;

                    // Set the height back to the start state of the animation.
                    itemHolder.alarmItem.getLayoutParams().height = startingHeight;
                    // To allow the expandArea to glide in with the expansion animation, set a
                    // negative top margin, which will animate down to a margin of 0 as the height
                    // is increased.
                    // Note that we need to maintain the bottom margin as a fixed value (instead of
                    // just using a listview, to allow for a flatter hierarchy) to fit the bottom
                    // bar underneath.
                    FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
                            itemHolder.expandArea.getLayoutParams();
                    expandParams.setMargins(0, -distance, 0, collapseHeight);
                    itemHolder.alarmItem.requestLayout();

                    // Set up the animator to animate the expansion.
                    ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f)
                            .setDuration(EXPAND_DURATION);
                    animator.setInterpolator(mExpandInterpolator);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            Float value = (Float) animator.getAnimatedValue();

                            // For each value from 0 to 1, animate the various parts of the layout.
                            itemHolder.alarmItem.getLayoutParams().height =
                                    (int) (value * distance + startingHeight);
                            FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
                                    itemHolder.expandArea.getLayoutParams();
                            expandParams.setMargins(
                                    0, (int) -((1 - value) * distance), 0, collapseHeight);
                            itemHolder.arrow.setRotation(180 * value);
                            itemHolder.hairLine.setTranslationY(-hairlineDistance * value);
                            itemHolder.summary.setAlpha(1 - value);

                            itemHolder.alarmItem.requestLayout();
                        }
                    });
                    // Set everything to their final values when the animation's done.
                    animator.addListener(new AnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            // Set it back to wrap content since we'd explicitly set the height.
                            itemHolder.alarmItem.getLayoutParams().height =
                                    LayoutParams.WRAP_CONTENT;
                            itemHolder.arrow.setRotation(180);
                            itemHolder.hairLine.setTranslationY(-hairlineDistance);
                            itemHolder.summary.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            // TODO we may have to deal with cancelations of the animation.
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) { }
                        @Override
                        public void onAnimationStart(Animator animation) { }
                    });
                    animator.start();

                    // Return false so this draw does not occur to prevent the final frame from
                    // being drawn for the single frame before the animations start.
                    return false;
                }
            });
        }

        private boolean isAlarmExpanded(Alarm alarm) {
            return mExpanded.contains(alarm.id);
        }

        private void collapseAlarm(final ItemHolder itemHolder) {
            mExpanded.remove(itemHolder.alarm.id);

            // Save the starting height so we can animate from this value.
            final int startingHeight = itemHolder.alarmItem.getHeight();

            // Set the expand area to gone so we can measure the height to animate to.
            itemHolder.alarmItem.setBackgroundResource(mBackgroundColor);
            itemHolder.expandArea.setVisibility(View.GONE);

            // Add an onPreDrawListener, which gets called after measurement but before the draw.
            // This way we can check the height we need to animate to before any drawing.
            // Note the series of events:
            //  * expandArea is set to GONE, which causes a layout pass
            //  * the view is measured, and our onPreDrawListener is called
            //  * we set up the animation using the start and end values.
            //  * expandArea is set to VISIBLE again so it can be shown animating.
            //  * request another layout pass.
            //  * return false so that onDraw() is not called for the single frame before
            //    the animations have started.
            final ViewTreeObserver observer = mAlarmsList.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    observer.removeOnPreDrawListener(this);

                    // Calculate some values to help with the animation.
                    final int endingHeight = itemHolder.alarmItem.getHeight();
                    final int distance = endingHeight - startingHeight;
                    final int collapseHeight = itemHolder.collapseExpandArea.getHeight();
                    int hairlineHeight = itemHolder.hairLine.getHeight();
                    final int hairlineDistance = collapseHeight - hairlineHeight;

                    // Re-set the visibilities for the start state of the animation.
                    itemHolder.expandArea.setVisibility(View.VISIBLE);
                    itemHolder.summary.setVisibility(View.VISIBLE);
                    itemHolder.summary.setAlpha(1);

                    // Set up the animator to animate the expansion.
                    ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f)
                            .setDuration(COLLAPSE_DURATION);
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            Float value = (Float) animator.getAnimatedValue();

                            // For each value from 0 to 1, animate the various parts of the layout.
                            itemHolder.alarmItem.getLayoutParams().height =
                                    (int) (value * distance + startingHeight);
                            FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
                                    itemHolder.expandArea.getLayoutParams();
                            expandParams.setMargins(0, (int) (value * distance), 0, collapseHeight);
                            itemHolder.arrow.setRotation(180 * (1 - value));
                            itemHolder.hairLine.setTranslationY(-hairlineDistance * (1 - value));
                            itemHolder.summary.setAlpha(value);

                            itemHolder.alarmItem.requestLayout();
                        }
                    });
                    animator.setInterpolator(mCollapseInterpolator);
                    // Set everything to their final values when the animation's done.
                    animator.addListener(new AnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            // Set it back to wrap content since we'd explicitly set the height.
                            itemHolder.alarmItem.getLayoutParams().height =
                                    LayoutParams.WRAP_CONTENT;

                            FrameLayout.LayoutParams expandParams = (FrameLayout.LayoutParams)
                                    itemHolder.expandArea.getLayoutParams();
                            expandParams.setMargins(0, 0, 0, 96);

                            itemHolder.expandArea.setVisibility(View.GONE);
                            itemHolder.arrow.setRotation(0);
                            itemHolder.hairLine.setTranslationY(0);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            // TODO we may have to deal with cancelations of the animation.
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) { }
                        @Override
                        public void onAnimationStart(Animator animation) { }
                    });
                    animator.start();

                    return false;
                }
            });
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        private View getViewById(int id) {
            for (int i = 0; i < mList.getCount(); i++) {
                View v = mList.getChildAt(i);
                if (v != null) {
                    ItemHolder h = (ItemHolder)(v.getTag());
                    if (h != null && h.alarm.id == id) {
                        return v;
                    }
                }
            }
            return null;
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

        public int[] getSelectedAlarmsArray() {
            final int[] ids = new int[mSelectedAlarms.size()];
            int index = 0;
            for (int id : mSelectedAlarms) {
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

        public void deleteSelectedAlarms() {
            Integer ids [] = new Integer[mSelectedAlarms.size()];
            int index = 0;
            for (int id : mSelectedAlarms) {
                ids[index] = id;
                index ++;
            }
            asyncDeleteAlarm(ids);
            clearSelectedAlarms();
        }

        public void clearSelectedAlarms() {
            mSelectedAlarms.clear();
            notifyDataSetChanged();
        }
    }

    private void asyncAddAlarm() {
        Alarm a = new Alarm();
        a.alert = RingtoneManager.getActualDefaultRingtoneUri(
                getActivity(), RingtoneManager.TYPE_ALARM);
        if (a.alert == null) {
            a.alert = Uri.parse("content://settings/system/alarm_alert");
        }
        asyncAddAlarm(a, true);
    }

    private void asyncDeleteAlarm(final Integer [] alarmIds) {
        final AsyncTask<Integer, Void, Void> deleteTask = new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... ids) {
                for (final int id : ids) {
                    Alarms.deleteAlarm(AlarmClockFragment.this.getActivity(), id);
                }
                return null;
            }
        };
        deleteTask.execute(alarmIds);
    }

    private void asyncDeleteAlarm(final Alarm alarm) {
        final AsyncTask<Alarm, Void, Void> deleteTask = new AsyncTask<Alarm, Void, Void>() {

            @Override
            protected Void doInBackground(Alarm... alarms) {
                for (final Alarm alarm : alarms) {
                    Alarms.deleteAlarm(AlarmClockFragment.this.getActivity(), alarm.id);
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
                asyncAddAlarm(alarm, false);
                mDeletedAlarm = null;
                mUndoShowing = false;
            }
        }, 0, getResources().getString(R.string.alarm_deleted), true, R.string.alarm_undo, true);
    }

    private void asyncAddAlarm(final Alarm alarm, final boolean showTimePicker) {
        final AsyncTask<Void, Void, Void> updateTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... aVoid) {
                Alarms.addAlarm(AlarmClockFragment.this.getActivity(), alarm);
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
                if (showTimePicker) {
                    AlarmUtils.showTimeEditDialog(AlarmClockFragment.this.getFragmentManager(),
                            alarm, AlarmClockFragment.this);
                }
            }
        };
        updateTask.execute();
    }

    private void asyncUpdateAlarm(final Alarm alarm, final boolean popToast) {
        final AsyncTask<Alarm, Void, Void> updateTask = new AsyncTask<Alarm, Void, Void>() {
            @Override
            protected Void doInBackground(Alarm... alarms) {
                for (final Alarm alarm : alarms) {
                    Alarms.setAlarm(AlarmClockFragment.this.getActivity(), alarm);
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
        AlarmUtils.popAlarmSetToast(getActivity().getApplicationContext(), alarm);
    }

    /***
     * Handle the delete alarms confirmation dialog
     */

    private void showConfirmationDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        Resources res = getResources();
        String msg = String.format(res.getQuantityText(R.plurals.alarm_delete_confirmation,
                mAdapter.getSelectedItemsNum()).toString());
        b.setCancelable(true).setMessage(msg)
                .setOnCancelListener(this)
                .setNegativeButton(res.getString(android.R.string.cancel), this)
                .setPositiveButton(res.getString(android.R.string.ok), this).show();
        mInDeleteConfirmation = true;
    }
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == -1) {
            if (mAdapter != null) {
                mAdapter.deleteSelectedAlarms();
            }
        }
        dialog.dismiss();
        mInDeleteConfirmation = false;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mInDeleteConfirmation = false;
    }
}
