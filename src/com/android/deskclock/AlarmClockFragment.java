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
import android.animation.AnimatorInflater;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
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
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
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
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.datetimepicker.time.RadialPickerLayout;
import com.android.datetimepicker.time.TimePickerDialog;
import com.android.deskclock.alarms.AlarmStateManager;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.widget.ActionableToastBar;
import com.android.deskclock.widget.TextTime;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AlarmClock application.
 */
public class AlarmClockFragment extends DeskClockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        TimePickerDialog.OnTimeSetListener,
        View.OnTouchListener
        {
    private static final float EXPAND_DECELERATION = 1f;
    private static final float COLLAPSE_DECELERATION = 0.7f;
    private static final int ANIMATION_DURATION = 300;
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

    // This extra is used when receiving an intent to create an alarm, but no alarm details
    // have been passed in, so the alarm page should start the process of creating a new alarm.
    public static final String ALARM_CREATE_NEW_INTENT_EXTRA = "deskclock.create.new";

    // This extra is used when receiving an intent to scroll to specific alarm. If alarm
    // can not be found, and toast message will pop up that the alarm has be deleted.
    public static final String SCROLL_TO_ALARM_INTENT_EXTRA = "deskclock.scroll.to.alarm";

    private ListView mAlarmsList;
    private AlarmItemAdapter mAdapter;
    private View mEmptyView;
    private ImageView mAddAlarmButton;
    private View mAlarmsView;
    private View mTimelineLayout;
    private AlarmTimelineView mTimelineView;
    private View mFooterView;

    private Bundle mRingtoneTitleCache; // Key: ringtone uri, value: ringtone title
    private ActionableToastBar mUndoBar;
    private View mUndoFrame;

    private Alarm mSelectedAlarm;
    private long mScrollToAlarmId = -1;

    private Loader mCursorLoader = null;

    // Saved states for undo
    private Alarm mDeletedAlarm;
    private Alarm mAddedAlarm;
    private boolean mUndoShowing = false;

    private Animator mFadeIn;
    private Animator mFadeOut;

    private Interpolator mExpandInterpolator;
    private Interpolator mCollapseInterpolator;

    private int mTimelineViewWidth;
    private int mUndoBarInitialMargin;

    // Cached layout positions of items in listview prior to add/removal of alarm item
    private ConcurrentHashMap<Long, Integer> mItemIdTopMap = new ConcurrentHashMap<Long, Integer>();

    public AlarmClockFragment() {
        // Basic provider required by Fragment.java
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mCursorLoader = getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.alarm_clock, container, false);

        long[] expandedIds = null;
        long[] repeatCheckedIds = null;
        long[] selectedAlarms = null;
        Bundle previousDayMap = null;
        if (savedState != null) {
            expandedIds = savedState.getLongArray(KEY_EXPANDED_IDS);
            repeatCheckedIds = savedState.getLongArray(KEY_REPEAT_CHECKED_IDS);
            mRingtoneTitleCache = savedState.getBundle(KEY_RINGTONE_TITLE_CACHE);
            mDeletedAlarm = savedState.getParcelable(KEY_DELETED_ALARM);
            mUndoShowing = savedState.getBoolean(KEY_UNDO_SHOWING);
            selectedAlarms = savedState.getLongArray(KEY_SELECTED_ALARMS);
            previousDayMap = savedState.getBundle(KEY_PREVIOUS_DAY_MAP);
            mSelectedAlarm = savedState.getParcelable(KEY_SELECTED_ALARM);
        }

        mExpandInterpolator = new DecelerateInterpolator(EXPAND_DECELERATION);
        mCollapseInterpolator = new DecelerateInterpolator(COLLAPSE_DECELERATION);

        mAddAlarmButton = (ImageButton) v.findViewById(R.id.alarm_add_alarm);
        mAddAlarmButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideUndoBar(true, null);
                startCreatingAlarm();
            }
        });
        // For landscape, put the add button on the right and the menu in the actionbar.
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) mAddAlarmButton.getLayoutParams();
        boolean isLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        if (isLandscape) {
            layoutParams.gravity = Gravity.END;
        } else {
            layoutParams.gravity = Gravity.CENTER;
        }
        mAddAlarmButton.setLayoutParams(layoutParams);

        View menuButton = v.findViewById(R.id.menu_button);
        if (menuButton != null) {
            if (isLandscape) {
                menuButton.setVisibility(View.GONE);
            } else {
                menuButton.setVisibility(View.VISIBLE);
                setupFakeOverflowMenuButton(menuButton);
            }
        }

        mEmptyView = v.findViewById(R.id.alarms_empty_view);
        mEmptyView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startCreatingAlarm();
            }
        });
        mAlarmsList = (ListView) v.findViewById(R.id.alarms_list);

        mFadeIn = AnimatorInflater.loadAnimator(getActivity(), R.anim.fade_in);
        mFadeIn.setDuration(ANIMATION_DURATION);
        mFadeIn.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                mEmptyView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // Do nothing.
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Do nothing.
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // Do nothing.
            }
        });
        mFadeIn.setTarget(mEmptyView);
        mFadeOut = AnimatorInflater.loadAnimator(getActivity(), R.anim.fade_out);
        mFadeOut.setDuration(ANIMATION_DURATION);
        mFadeOut.addListener(new AnimatorListener() {

            @Override
            public void onAnimationStart(Animator arg0) {
                mEmptyView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator arg0) {
                // Do nothing.
            }

            @Override
            public void onAnimationEnd(Animator arg0) {
                mEmptyView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animator arg0) {
                // Do nothing.
            }
        });
        mFadeOut.setTarget(mEmptyView);
        mAlarmsView = v.findViewById(R.id.alarm_layout);
        mTimelineLayout = v.findViewById(R.id.timeline_layout);

        mUndoBar = (ActionableToastBar) v.findViewById(R.id.undo_bar);
        mUndoBarInitialMargin = getActivity().getResources()
                .getDimensionPixelOffset(R.dimen.alarm_undo_bar_horizontal_margin);
        mUndoFrame = v.findViewById(R.id.undo_frame);
        mUndoFrame.setOnTouchListener(this);

        mFooterView = v.findViewById(R.id.alarms_footer_view);
        mFooterView.setOnTouchListener(this);

        // Timeline layout only exists in tablet landscape mode for now.
        if (mTimelineLayout != null) {
            mTimelineView = (AlarmTimelineView) v.findViewById(R.id.alarm_timeline_view);
            mTimelineViewWidth = getActivity().getResources()
                    .getDimensionPixelOffset(R.dimen.alarm_timeline_layout_width);
        }

        mAdapter = new AlarmItemAdapter(getActivity(),
                expandedIds, repeatCheckedIds, selectedAlarms, previousDayMap, mAlarmsList);
        mAdapter.registerDataSetObserver(new DataSetObserver() {

            private int prevAdapterCount = -1;

            @Override
            public void onChanged() {

                final int count = mAdapter.getCount();
                if (mDeletedAlarm != null && prevAdapterCount > count) {
                    showUndoBar();
                }

                // If there are no alarms in the adapter...
                if (count == 0) {
                    mAddAlarmButton.setBackgroundResource(R.drawable.main_button_red);

                    // ...and if there exists a timeline view (currently only in tablet landscape)
                    if (mTimelineLayout != null && mAlarmsView != null) {

                        // ...and if the previous adapter had alarms (indicating a removal)...
                        if (prevAdapterCount > 0) {

                            // Then animate in the "no alarms" icon...
                            mFadeIn.start();

                            // and animate out the alarm timeline view, expanding the width of the
                            // alarms list / undo bar.
                            mTimelineLayout.setVisibility(View.VISIBLE);
                            ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f)
                                    .setDuration(ANIMATION_DURATION);
                            animator.setInterpolator(mCollapseInterpolator);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animator) {
                                    Float value = (Float) animator.getAnimatedValue();
                                    int currentTimelineWidth = (int) (value * mTimelineViewWidth);
                                    float rightOffset = mTimelineViewWidth * (1 - value);
                                    mTimelineLayout.setTranslationX(rightOffset);
                                    mTimelineLayout.setAlpha(value);
                                    mTimelineLayout.requestLayout();
                                    setUndoBarRightMargin(currentTimelineWidth
                                            + mUndoBarInitialMargin);
                                }
                            });
                            animator.addListener(new AnimatorListener() {

                                @Override
                                public void onAnimationCancel(Animator animation) {
                                    // Do nothing.
                                }

                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    mTimelineView.setIsAnimatingOut(false);
                                }

                                @Override
                                public void onAnimationRepeat(Animator animation) {
                                    // Do nothing.
                                }

                                @Override
                                public void onAnimationStart(Animator animation) {
                                    mTimelineView.setIsAnimatingOut(true);
                                }

                            });
                            animator.start();
                        } else {
                            // If the previous adapter did not have alarms, no animation needed,
                            // just hide the timeline view and show the "no alarms" icon.
                            mTimelineLayout.setVisibility(View.GONE);
                            mEmptyView.setVisibility(View.VISIBLE);
                            setUndoBarRightMargin(mUndoBarInitialMargin);
                        }
                    } else {

                        // If there is no timeline view, just show the "no alarms" icon.
                        mEmptyView.setVisibility(View.VISIBLE);
                    }
                } else {

                    // Otherwise, if the adapter DOES contain alarms...
                    mAddAlarmButton.setBackgroundResource(R.drawable.main_button_normal);

                    // ...and if there exists a timeline view (currently in tablet landscape mode)
                    if (mTimelineLayout != null && mAlarmsView != null) {

                        mTimelineLayout.setVisibility(View.VISIBLE);
                        // ...and if the previous adapter did not have alarms (indicating an add)
                        if (prevAdapterCount == 0) {

                            // Then, animate to hide the "no alarms" icon...
                            mFadeOut.start();

                            // and animate to show the timeline view, reducing the width of the
                            // alarms list / undo bar.
                            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f)
                                    .setDuration(ANIMATION_DURATION);
                            animator.setInterpolator(mExpandInterpolator);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animator) {
                                    Float value = (Float) animator.getAnimatedValue();
                                    int currentTimelineWidth = (int) (value * mTimelineViewWidth);
                                    float rightOffset = mTimelineViewWidth * (1 - value);
                                    mTimelineLayout.setTranslationX(rightOffset);
                                    mTimelineLayout.setAlpha(value);
                                    mTimelineLayout.requestLayout();
                                    ((FrameLayout.LayoutParams) mAlarmsView.getLayoutParams())
                                        .setMargins(0, 0, (int) -rightOffset, 0);
                                    mAlarmsView.requestLayout();
                                    setUndoBarRightMargin(currentTimelineWidth
                                            + mUndoBarInitialMargin);
                                }
                            });
                            animator.start();
                        } else {
                            mTimelineLayout.setVisibility(View.VISIBLE);
                            mEmptyView.setVisibility(View.GONE);
                            setUndoBarRightMargin(mUndoBarInitialMargin + mTimelineViewWidth);
                        }
                    } else {

                        // If there is no timeline view, just hide the "no alarms" icon.
                        mEmptyView.setVisibility(View.GONE);
                    }
                }

                // Cache this adapter's count for when the adapter changes.
                prevAdapterCount = count;
                super.onChanged();
            }
        });

        if (mRingtoneTitleCache == null) {
            mRingtoneTitleCache = new Bundle();
        }

        mAlarmsList.setAdapter(mAdapter);
        mAlarmsList.setVerticalScrollBarEnabled(true);
        mAlarmsList.setOnCreateContextMenuListener(this);

        if (mUndoShowing) {
            showUndoBar();
        }
        return v;
    }

    private void setUndoBarRightMargin(int margin) {
        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) mUndoBar.getLayoutParams();
        ((FrameLayout.LayoutParams) mUndoBar.getLayoutParams())
            .setMargins(params.leftMargin, params.topMargin, margin, params.bottomMargin);
        mUndoBar.requestLayout();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if another app asked us to create a blank new alarm.
        final Intent intent = getActivity().getIntent();
        if (intent.hasExtra(ALARM_CREATE_NEW_INTENT_EXTRA)) {
            if (intent.getBooleanExtra(ALARM_CREATE_NEW_INTENT_EXTRA, false)) {
                // An external app asked us to create a blank alarm.
                startCreatingAlarm();
            }

            // Remove the CREATE_NEW extra now that we've processed it.
            intent.removeExtra(ALARM_CREATE_NEW_INTENT_EXTRA);
        } else if (intent.hasExtra(SCROLL_TO_ALARM_INTENT_EXTRA)) {
            long alarmId = intent.getLongExtra(SCROLL_TO_ALARM_INTENT_EXTRA, Alarm.INVALID_ID);
            if (alarmId != Alarm.INVALID_ID) {
                mScrollToAlarmId = alarmId;
                if (mCursorLoader != null && mCursorLoader.isStarted()) {
                    // We need to force a reload here to make sure we have the latest view
                    // of the data to scroll to.
                    mCursorLoader.forceLoad();
                }
            }

            // Remove the SCROLL_TO_ALARM extra now that we've processed it.
            intent.removeExtra(SCROLL_TO_ALARM_INTENT_EXTRA);
        }

        // Make sure to use the child FragmentManager. We have to use that one for the
        // case where an intent comes in telling the activity to load the timepicker,
        // which means we have to use that one everywhere so that the fragment can get
        // correctly picked up here if it's open.
        TimePickerDialog tpd = (TimePickerDialog) getChildFragmentManager().
                findFragmentByTag(AlarmUtils.FRAG_TAG_TIME_PICKER);
        if (tpd != null) {
            // The dialog is already open so we need to set the listener again.
            tpd.setOnTimeSetListener(this);
        }
    }

    private void hideUndoBar(boolean animate, MotionEvent event) {
        if (mUndoBar != null) {
            mUndoFrame.setVisibility(View.GONE);
            if (event != null && mUndoBar.isEventInToastBar(event)) {
                // Avoid touches inside the undo bar.
                return;
            }
            mUndoBar.hide(animate);
        }
        mDeletedAlarm = null;
        mUndoShowing = false;
    }

    private void showUndoBar() {
        mUndoFrame.setVisibility(View.VISIBLE);
        mUndoBar.show(new ActionableToastBar.ActionClickedListener() {
            @Override
            public void onActionClicked() {
                asyncAddAlarm(mDeletedAlarm);
                mDeletedAlarm = null;
                mUndoShowing = false;
            }
        }, 0, getResources().getString(R.string.alarm_deleted), true, R.string.alarm_undo, true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLongArray(KEY_EXPANDED_IDS, mAdapter.getExpandedArray());
        outState.putLongArray(KEY_REPEAT_CHECKED_IDS, mAdapter.getRepeatArray());
        outState.putLongArray(KEY_SELECTED_ALARMS, mAdapter.getSelectedAlarmsArray());
        outState.putBundle(KEY_RINGTONE_TITLE_CACHE, mRingtoneTitleCache);
        outState.putParcelable(KEY_DELETED_ALARM, mDeletedAlarm);
        outState.putBoolean(KEY_UNDO_SHOWING, mUndoShowing);
        outState.putBundle(KEY_PREVIOUS_DAY_MAP, mAdapter.getPreviousDaysOfWeekMap());
        outState.putParcelable(KEY_SELECTED_ALARM, mSelectedAlarm);
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
        hideUndoBar(false, null);
    }

    // Callback used by TimePickerDialog
    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute) {
        if (mSelectedAlarm == null) {
            // If mSelectedAlarm is null then we're creating a new alarm.
            Alarm a = new Alarm();
            a.alert = RingtoneManager.getActualDefaultRingtoneUri(getActivity(),
                    RingtoneManager.TYPE_ALARM);
            if (a.alert == null) {
                a.alert = Uri.parse("content://settings/system/alarm_alert");
            }
            a.hour = hourOfDay;
            a.minutes = minute;
            a.enabled = true;
            asyncAddAlarm(a);
        } else {
            mSelectedAlarm.hour = hourOfDay;
            mSelectedAlarm.minutes = minute;
            mSelectedAlarm.enabled = true;
            mScrollToAlarmId = mSelectedAlarm.id;
            asyncUpdateAlarm(mSelectedAlarm, true);
            mSelectedAlarm = null;
        }
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
        return Alarm.getAlarmsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, final Cursor data) {
        mAdapter.swapCursor(data);
        if (mScrollToAlarmId != -1) {
            scrollToAlarm(mScrollToAlarmId);
            mScrollToAlarmId = -1;
        }
    }

    /**
     * Scroll to alarm with given alarm id.
     *
     * @param alarmId The alarm id to scroll to.
     */
    private void scrollToAlarm(long alarmId) {
        int alarmPosition = -1;
        for (int i = 0; i < mAdapter.getCount(); i++) {
            long id = mAdapter.getItemId(i);
            if (id == alarmId) {
                alarmPosition = i;
                break;
            }
        }

        if (alarmPosition >= 0) {
            mAdapter.setNewAlarm(alarmId);
            mAlarmsList.smoothScrollToPositionFromTop(alarmPosition, 0);
        } else {
            // Trying to display a deleted alarm should only happen from a missed notification for
            // an alarm that has been marked deleted after use.
            Context context = getActivity().getApplicationContext();
            Toast toast = Toast.makeText(context, R.string.missed_alarm_has_been_deleted,
                    Toast.LENGTH_LONG);
            ToastMaster.setToast(toast);
            toast.show();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.swapCursor(null);
    }

    private void launchRingTonePicker(Alarm alarm) {
        mSelectedAlarm = alarm;
        Uri oldRingtone = Alarm.NO_RINGTONE_URI.equals(alarm.alert) ? null : alarm.alert;
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, oldRingtone);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        startActivityForResult(intent, REQUEST_CODE_RINGTONE);
    }

    private void saveRingtoneUri(Intent intent) {
        Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        if (uri == null) {
            uri = Alarm.NO_RINGTONE_URI;
        }
        mSelectedAlarm.alert = uri;

        // Save the last selected ringtone as the default for new alarms
        if (!Alarm.NO_RINGTONE_URI.equals(uri)) {
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

        private final HashSet<Long> mExpanded = new HashSet<Long>();
        private final HashSet<Long> mRepeatChecked = new HashSet<Long>();
        private final HashSet<Long> mSelectedAlarms = new HashSet<Long>();
        private Bundle mPreviousDaysOfWeekMap = new Bundle();

        private final boolean mHasVibrator;
        private final int mCollapseExpandHeight;

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
            TextTime clock;
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
            View footerFiller;

            // Other states
            Alarm alarm;
        }

        // Used for scrolling an expanded item in the list to make sure it is fully visible.
        private long mScrollAlarmId = -1;
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

        public AlarmItemAdapter(Context context, long[] expandedIds, long[] repeatCheckedIds,
                long[] selectedAlarms, Bundle previousDaysOfWeekMap, ListView list) {
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

            mCollapseExpandHeight = (int) res.getDimension(R.dimen.collapse_expand_height);
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
                // TODO temporary hack to prevent the convertView from not having stuff we need.
                boolean badConvertView = convertView.findViewById(R.id.digital_clock) == null;
                // Do a translation check to test for animation. Change this to something more
                // reliable and robust in the future.
                if (convertView.getTranslationX() != 0 || convertView.getTranslationY() != 0 ||
                        badConvertView) {
                    // view was animated, reset
                    v = newView(mContext, getCursor(), parent);
                } else {
                    v = convertView;
                }
            }
            bindView(v, mContext, getCursor());
            ItemHolder holder = (ItemHolder) v.getTag();

            // We need the footer for the last element of the array to allow the user to scroll
            // the item beyond the bottom button bar, which obscures the view.
            holder.footerFiller.setVisibility(position < getCount() - 1 ? View.GONE : View.VISIBLE);
            return v;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mFactory.inflate(R.layout.alarm_time, parent, false);
            setNewHolder(view);
            return view;
        }

        /**
         * In addition to changing the data set for the alarm list, swapCursor is now also
         * responsible for preparing the list view's pre-draw operation for any animations that
         * need to occur if an alarm was removed or added.
         */
        @Override
        public synchronized Cursor swapCursor(Cursor cursor) {
            Cursor c = super.swapCursor(cursor);

            if (mItemIdTopMap.isEmpty() && mAddedAlarm == null) {
                return c;
            }

            final ListView list = mAlarmsList;
            final ViewTreeObserver observer = list.getViewTreeObserver();

            /*
             * Add a pre-draw listener to the observer to prepare for any possible animations to
             * the alarms within the list view.  The animations will occur if an alarm has been
             * removed or added.
             *
             * For alarm removal, the remaining children should all retain their initial starting
             * positions, and transition to their new positions.
             *
             * For alarm addition, the other children should all retain their initial starting
             * positions, transition to their new positions, and at the end of that transition, the
             * newly added alarm should appear in the designated space.
             */
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                private View mAddedView;

                @Override
                public boolean onPreDraw() {
                    // Remove the pre-draw listener, as this only needs to occur once.
                    if (observer.isAlive()) {
                        observer.removeOnPreDrawListener(this);
                    }
                    boolean firstAnimation = true;
                    int firstVisiblePosition = list.getFirstVisiblePosition();

                    // Iterate through the children to prepare the add/remove animation.
                    for (int i = 0; i< list.getChildCount(); i++) {
                        final View child = list.getChildAt(i);

                        int position = firstVisiblePosition + i;
                        long itemId = mAdapter.getItemId(position);

                        // If this is the added alarm, set it invisible for now, and animate later.
                        if (mAddedAlarm != null && itemId == mAddedAlarm.id) {
                            mAddedView = child;
                            mAddedView.setAlpha(0.0f);
                            continue;
                        }

                        // The cached starting position of the child view.
                        Integer startTop = mItemIdTopMap.get(itemId);
                        // The new starting position of the child view.
                        int top = child.getTop();

                        // If there is no cached starting position, determine whether the item has
                        // come from the top of bottom of the list view.
                        if (startTop == null) {
                            int childHeight = child.getHeight() + list.getDividerHeight();
                            startTop = top + (i > 0 ? childHeight : -childHeight);
                        }

                        Log.d("Start Top: " + startTop + ", Top: " + top);
                        // If the starting position of the child view is different from the
                        // current position, animate the child.
                        if (startTop != top) {
                            int delta = startTop - top;
                            child.setTranslationY(delta);
                            child.animate().setDuration(ANIMATION_DURATION).translationY(0);
                            final View addedView = mAddedView;
                            if (firstAnimation) {

                                // If this is the first child being animated, then after the
                                // animation is complete, and animate in the added alarm (if one
                                // exists).
                                child.animate().withEndAction(new Runnable() {

                                    @Override
                                    public void run() {


                                        // If there was an added view, animate it in after
                                        // the other views have animated.
                                        if (addedView != null) {
                                            addedView.animate().alpha(1.0f)
                                                .setDuration(ANIMATION_DURATION)
                                                .withEndAction(new Runnable() {

                                                    @Override
                                                    public void run() {
                                                        // Re-enable the list after the add
                                                        // animation is complete.
                                                        list.setEnabled(true);
                                                    }

                                                });
                                        } else {
                                            // Re-enable the list after animations are complete.
                                            list.setEnabled(true);
                                        }
                                    }

                                });
                                firstAnimation = false;
                            }
                        }
                    }

                    // If there were no child views (outside of a possible added view)
                    // that require animation...
                    if (firstAnimation) {
                        if (mAddedView != null) {
                            // If there is an added view, prepare animation for the added view.
                            Log.d("Animating added view...");
                            mAddedView.animate().alpha(1.0f)
                                .setDuration(ANIMATION_DURATION)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Re-enable the list after animations are complete.
                                        list.setEnabled(true);
                                    }
                                });
                        } else {
                            // Re-enable the list after animations are complete.
                            list.setEnabled(true);
                        }
                    }

                    mAddedAlarm = null;
                    mItemIdTopMap.clear();
                    return true;
                }
            });
            return c;
        }

        private void setNewHolder(View view) {
            // standard view holder optimization
            final ItemHolder holder = new ItemHolder();
            holder.alarmItem = (LinearLayout) view.findViewById(R.id.alarm_item);
            holder.clock = (TextTime) view.findViewById(R.id.digital_clock);
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
            holder.footerFiller = view.findViewById(R.id.alarm_footer_filler);
            holder.footerFiller.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    // Do nothing.
                }
            });

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
        }

        @Override
        public void bindView(final View view, Context context, final Cursor cursor) {
            final Alarm alarm = new Alarm(cursor);
            Object tag = view.getTag();
            if (tag == null) {
                // The view was converted but somehow lost its tag.
                setNewHolder(view);
            }
            final ItemHolder itemHolder = (ItemHolder) tag;
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
            itemHolder.clock.setFormat(
                    (int)mContext.getResources().getDimension(R.dimen.alarm_label_size));
            itemHolder.clock.setTime(alarm.hour, alarm.minutes);
            itemHolder.clock.setClickable(true);
            itemHolder.clock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mSelectedAlarm = itemHolder.alarm;
                    AlarmUtils.showTimeEditDialog(getChildFragmentManager(),
                            alarm, AlarmClockFragment.this
                            , DateFormat.is24HourFormat(getActivity()));
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

            String labelSpace = "";
            // Set the repeat text or leave it blank if it does not repeat.
            final String daysOfWeekStr =
                    alarm.daysOfWeek.toString(AlarmClockFragment.this.getActivity(), false);
            if (daysOfWeekStr != null && daysOfWeekStr.length() != 0) {
                itemHolder.daysOfWeek.setText(daysOfWeekStr);
                itemHolder.daysOfWeek.setContentDescription(alarm.daysOfWeek.toAccessibilityString(
                        AlarmClockFragment.this.getActivity()));
                itemHolder.daysOfWeek.setVisibility(View.VISIBLE);
                labelSpace = "  ";
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
                itemHolder.label.setText(alarm.label + labelSpace);
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
                    mDeletedAlarm = alarm;

                    view.animate().setDuration(ANIMATION_DURATION).alpha(0).translationY(-1)
                    .withEndAction(new Runnable() {

                        @Override
                        public void run() {
                            asyncDeleteAlarm(mDeletedAlarm, view);
                        }
                    });
                }
            });

            if (expanded) {
                expandAlarm(itemHolder, false);
            } else {
                collapseAlarm(itemHolder, false);
            }

            itemHolder.alarmItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (isAlarmExpanded(alarm)) {
                        collapseAlarm(itemHolder, true);
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
                                        DaysOfWeek.NO_DAYS_SET);
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
            if (Alarm.NO_RINGTONE_URI.equals(alarm.alert)) {
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
            holder.daysOfWeek.setAlpha(alpha);
        }

        private void updateDaysOfWeekButtons(ItemHolder holder, DaysOfWeek daysOfWeek) {
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
                long id = ((ItemHolder)v.getTag()).alarm.id;
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

        public void setNewAlarm(long alarmId) {
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
                // We need to translate the hairline up, so the height of the collapseArea
                // needs to be measured to know how high to translate it.
                final ViewTreeObserver observer = mAlarmsList.getViewTreeObserver();
                observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        // We don't want to continue getting called for every listview drawing.
                        if (observer.isAlive()) {
                            observer.removeOnPreDrawListener(this);
                        }
                        int hairlineHeight = itemHolder.hairLine.getHeight();
                        int collapseHeight =
                                itemHolder.collapseExpandArea.getHeight() - hairlineHeight;
                        itemHolder.hairLine.setTranslationY(-collapseHeight);
                        return true;
                    }
                });
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
                    if (observer.isAlive()) {
                        observer.removeOnPreDrawListener(this);
                    }
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

        private void collapseAlarm(final ItemHolder itemHolder, boolean animate) {
            mExpanded.remove(itemHolder.alarm.id);

            // Save the starting height so we can animate from this value.
            final int startingHeight = itemHolder.alarmItem.getHeight();

            // Set the expand area to gone so we can measure the height to animate to.
            itemHolder.alarmItem.setBackgroundResource(mBackgroundColor);
            itemHolder.expandArea.setVisibility(View.GONE);

            if (!animate) {
                // Set the "end" layout and don't do the animation.
                itemHolder.arrow.setRotation(0);
                itemHolder.hairLine.setTranslationY(0);
                return;
            }

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
                    if (observer.isAlive()) {
                        observer.removeOnPreDrawListener(this);
                    }

                    // Calculate some values to help with the animation.
                    final int endingHeight = itemHolder.alarmItem.getHeight();
                    final int distance = endingHeight - startingHeight;
                    int hairlineHeight = itemHolder.hairLine.getHeight();
                    final int hairlineDistance = mCollapseExpandHeight - hairlineHeight;

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
                            expandParams.setMargins(
                                    0, (int) (value * distance), 0, mCollapseExpandHeight);
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
                            expandParams.setMargins(0, 0, 0, mCollapseExpandHeight);

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

        private View getViewById(long id) {
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

        public long[] getExpandedArray() {
            int index = 0;
            long[] ids = new long[mExpanded.size()];
            for (long id : mExpanded) {
                ids[index] = id;
                index++;
            }
            return ids;
        }

        public long[] getSelectedAlarmsArray() {
            int index = 0;
            long[] ids = new long[mSelectedAlarms.size()];
            for (long id : mSelectedAlarms) {
                ids[index] = id;
                index++;
            }
            return ids;
        }

        public long[] getRepeatArray() {
            int index = 0;
            long[] ids = new long[mRepeatChecked.size()];
            for (long id : mRepeatChecked) {
                ids[index] = id;
                index++;
            }
            return ids;
        }

        public Bundle getPreviousDaysOfWeekMap() {
            return mPreviousDaysOfWeekMap;
        }

        private void buildHashSetFromArray(long[] ids, HashSet<Long> set) {
            for (long id : ids) {
                set.add(id);
            }
        }
    }

    private void startCreatingAlarm() {
        // Set the "selected" alarm as null, and we'll create the new one when the timepicker
        // comes back.
        mSelectedAlarm = null;
        AlarmUtils.showTimeEditDialog(getChildFragmentManager(),
                null, AlarmClockFragment.this, DateFormat.is24HourFormat(getActivity()));
    }

    private static AlarmInstance setupAlarmInstance(Context context, Alarm alarm) {
        ContentResolver cr = context.getContentResolver();
        AlarmInstance newInstance = alarm.createInstanceAfter(Calendar.getInstance());
        newInstance = AlarmInstance.addInstance(cr, newInstance);
        // Register instance to state manager
        AlarmStateManager.registerInstance(context, newInstance, true);
        return newInstance;
    }

    private void asyncDeleteAlarm(final Alarm alarm, final View viewToRemove) {
        final Context context = AlarmClockFragment.this.getActivity().getApplicationContext();
        final AsyncTask<Void, Void, Void> deleteTask = new AsyncTask<Void, Void, Void>() {
            @Override
            public synchronized void onPreExecute() {
                if (viewToRemove == null) {
                    return;
                }
                // The alarm list needs to be disabled until the animation finishes to prevent
                // possible concurrency issues.  It becomes re-enabled after the animations have
                // completed.
                mAlarmsList.setEnabled(false);

                // Store all of the current list view item positions in memory for animation.
                final ListView list = mAlarmsList;
                int firstVisiblePosition = list.getFirstVisiblePosition();
                for (int i=0; i<list.getChildCount(); i++) {
                    View child = list.getChildAt(i);
                    if (child != viewToRemove) {
                        int position = firstVisiblePosition + i;
                        long itemId = mAdapter.getItemId(position);
                        mItemIdTopMap.put(itemId, child.getTop());
                    }
                }
            }

            @Override
            protected Void doInBackground(Void... parameters) {
                // Activity may be closed at this point , make sure data is still valid
                if (context != null && alarm != null) {
                    ContentResolver cr = context.getContentResolver();
                    AlarmStateManager.deleteAllInstances(context, alarm.id);
                    Alarm.deleteAlarm(cr, alarm.id);
                }
                return null;
            }
        };
        mUndoShowing = true;
        deleteTask.execute();
    }

    private void asyncAddAlarm(final Alarm alarm) {
        final Context context = AlarmClockFragment.this.getActivity().getApplicationContext();
        final AsyncTask<Void, Void, AlarmInstance> updateTask =
                new AsyncTask<Void, Void, AlarmInstance>() {
            @Override
            public synchronized void onPreExecute() {
                final ListView list = mAlarmsList;
                // The alarm list needs to be disabled until the animation finishes to prevent
                // possible concurrency issues.  It becomes re-enabled after the animations have
                // completed.
                mAlarmsList.setEnabled(false);

                // Store all of the current list view item positions in memory for animation.
                int firstVisiblePosition = list.getFirstVisiblePosition();
                for (int i=0; i<list.getChildCount(); i++) {
                    View child = list.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    long itemId = mAdapter.getItemId(position);
                    mItemIdTopMap.put(itemId, child.getTop());
                }
            }

            @Override
            protected AlarmInstance doInBackground(Void... parameters) {
                if (context != null && alarm != null) {
                    ContentResolver cr = context.getContentResolver();

                    // Add alarm to db
                    Alarm newAlarm = Alarm.addAlarm(cr, alarm);
                    mScrollToAlarmId = newAlarm.id;

                    // Create and add instance to db
                    if (newAlarm.enabled) {
                        return setupAlarmInstance(context, newAlarm);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(AlarmInstance instance) {
                if (instance != null) {
                    AlarmUtils.popAlarmSetToast(context, instance.getAlarmTime().getTimeInMillis());
                }
            }
        };
        updateTask.execute();
    }

    private void asyncUpdateAlarm(final Alarm alarm, final boolean popToast) {
        final Context context = AlarmClockFragment.this.getActivity().getApplicationContext();
        final AsyncTask<Void, Void, AlarmInstance> updateTask =
                new AsyncTask<Void, Void, AlarmInstance>() {
            @Override
            protected AlarmInstance doInBackground(Void ... parameters) {
                ContentResolver cr = context.getContentResolver();

                // Dismiss all old instances
                AlarmStateManager.deleteAllInstances(context, alarm.id);

                // Update alarm
                Alarm.updateAlarm(cr, alarm);
                if (alarm.enabled) {
                    return setupAlarmInstance(context, alarm);
                }

                return null;
            }

            @Override
            protected void onPostExecute(AlarmInstance instance) {
                if (popToast && instance != null) {
                    AlarmUtils.popAlarmSetToast(context, instance.getAlarmTime().getTimeInMillis());
                }
            }
        };
        updateTask.execute();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        hideUndoBar(true, event);
        return false;
    }
}
