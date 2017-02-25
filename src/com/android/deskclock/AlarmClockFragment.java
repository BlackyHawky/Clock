/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.deskclock.alarms.AlarmTimeClickHandler;
import com.android.deskclock.alarms.AlarmUpdateHandler;
import com.android.deskclock.alarms.ScrollHandler;
import com.android.deskclock.alarms.TimePickerDialogFragment;
import com.android.deskclock.alarms.dataadapter.AlarmItemHolder;
import com.android.deskclock.alarms.dataadapter.CollapsedAlarmViewHolder;
import com.android.deskclock.alarms.dataadapter.ExpandedAlarmViewHolder;
import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.widget.EmptyViewController;
import com.android.deskclock.widget.toast.SnackbarManager;
import com.android.deskclock.widget.toast.ToastManager;

import java.util.ArrayList;
import java.util.List;

import static com.android.deskclock.uidata.UiDataModel.Tab.ALARMS;

/**
 * A fragment that displays a list of alarm time and allows interaction with them.
 */
public final class AlarmClockFragment extends DeskClockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ScrollHandler,
        TimePickerDialogFragment.OnTimeSetListener {

    // This extra is used when receiving an intent to create an alarm, but no alarm details
    // have been passed in, so the alarm page should start the process of creating a new alarm.
    public static final String ALARM_CREATE_NEW_INTENT_EXTRA = "deskclock.create.new";

    // This extra is used when receiving an intent to scroll to specific alarm. If alarm
    // can not be found, and toast message will pop up that the alarm has be deleted.
    public static final String SCROLL_TO_ALARM_INTENT_EXTRA = "deskclock.scroll.to.alarm";

    private static final String KEY_EXPANDED_ID = "expandedId";

    // Updates "Today/Tomorrow" in the UI when midnight passes.
    private final Runnable mMidnightUpdater = new MidnightRunnable();

    // Views
    private ViewGroup mMainLayout;
    private RecyclerView mRecyclerView;

    // Data
    private Loader mCursorLoader;
    private long mScrollToAlarmId = Alarm.INVALID_ID;
    private long mExpandedAlarmId = Alarm.INVALID_ID;
    private long mCurrentUpdateToken;

    // Controllers
    private ItemAdapter<AlarmItemHolder> mItemAdapter;
    private AlarmUpdateHandler mAlarmUpdateHandler;
    private EmptyViewController mEmptyViewController;
    private AlarmTimeClickHandler mAlarmTimeClickHandler;
    private LinearLayoutManager mLayoutManager;

    /**
     * The public no-arg constructor required by all fragments.
     */
    public AlarmClockFragment() {
        super(ALARMS);
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mCursorLoader = getLoaderManager().initLoader(0, null, this);
        if (savedState != null) {
            mExpandedAlarmId = savedState.getLong(KEY_EXPANDED_ID, Alarm.INVALID_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.alarm_clock, container, false);
        final Context context = getActivity();

        mRecyclerView = (RecyclerView) v.findViewById(R.id.alarms_recycler_view);
        mLayoutManager = new LinearLayoutManager(context) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                final int extraSpace = super.getExtraLayoutSpace(state);
                if (state.willRunPredictiveAnimations()) {
                    return Math.max(getHeight(), extraSpace);
                }
                return extraSpace;
            }
        };
        mRecyclerView.setLayoutManager(mLayoutManager);
        mMainLayout = (ViewGroup) v.findViewById(R.id.main);
        mAlarmUpdateHandler = new AlarmUpdateHandler(context, this, mMainLayout);
        final TextView emptyView = (TextView) v.findViewById(R.id.alarms_empty_view);
        final Drawable noAlarms = Utils.getVectorDrawable(context, R.drawable.ic_noalarms);
        emptyView.setCompoundDrawablesWithIntrinsicBounds(null, noAlarms, null, null);
        mEmptyViewController = new EmptyViewController(mMainLayout, mRecyclerView, emptyView);
        mAlarmTimeClickHandler = new AlarmTimeClickHandler(this, savedState, mAlarmUpdateHandler,
                this);

        mItemAdapter = new ItemAdapter<>();
        mItemAdapter.setHasStableIds();
        mItemAdapter.withViewTypes(new CollapsedAlarmViewHolder.Factory(inflater),
                null, CollapsedAlarmViewHolder.VIEW_TYPE);
        mItemAdapter.withViewTypes(new ExpandedAlarmViewHolder.Factory(context),
                null, ExpandedAlarmViewHolder.VIEW_TYPE);
        mItemAdapter.setOnItemChangedListener(new ItemAdapter.OnItemChangedListener() {
            @Override
            public void onItemChanged(ItemAdapter.ItemHolder<?> holder) {
                if (((AlarmItemHolder) holder).isExpanded()) {
                    if (mExpandedAlarmId != holder.itemId) {
                        // Collapse the prior expanded alarm.
                        final AlarmItemHolder aih = mItemAdapter.findItemById(mExpandedAlarmId);
                        if (aih != null) {
                            aih.collapse();
                        }
                        // Record the freshly expanded alarm.
                        mExpandedAlarmId = holder.itemId;
                        final RecyclerView.ViewHolder viewHolder =
                                mRecyclerView.findViewHolderForItemId(mExpandedAlarmId);
                        if (viewHolder != null) {
                            smoothScrollTo(viewHolder.getAdapterPosition());
                        }
                    }
                } else if (mExpandedAlarmId == holder.itemId) {
                    // The expanded alarm is now collapsed so update the tracking id.
                    mExpandedAlarmId = Alarm.INVALID_ID;
                }
            }

            @Override
            public void onItemChanged(ItemAdapter.ItemHolder<?> holder, Object payload) {
                /* No additional work to do */
            }
        });
        final ScrollPositionWatcher scrollPositionWatcher = new ScrollPositionWatcher();
        mRecyclerView.addOnLayoutChangeListener(scrollPositionWatcher);
        mRecyclerView.addOnScrollListener(scrollPositionWatcher);
        mRecyclerView.setAdapter(mItemAdapter);
        final ItemAnimator itemAnimator = new ItemAnimator();
        itemAnimator.setChangeDuration(300L);
        itemAnimator.setMoveDuration(300L);
        mRecyclerView.setItemAnimator(itemAnimator);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!isTabSelected()) {
            TimePickerDialogFragment.removeTimeEditDialog(getFragmentManager());
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Schedule a runnable to update the "Today/Tomorrow" values displayed for non-repeating
        // alarms when midnight passes.
        UiDataModel.getUiDataModel().addMidnightCallback(mMidnightUpdater, 100);

        // Check if another app asked us to create a blank new alarm.
        final Intent intent = getActivity().getIntent();
        if (intent == null) {
            return;
        }

        if (intent.hasExtra(ALARM_CREATE_NEW_INTENT_EXTRA)) {
            UiDataModel.getUiDataModel().setSelectedTab(ALARMS);
            if (intent.getBooleanExtra(ALARM_CREATE_NEW_INTENT_EXTRA, false)) {
                // An external app asked us to create a blank alarm.
                startCreatingAlarm();
            }

            // Remove the CREATE_NEW extra now that we've processed it.
            intent.removeExtra(ALARM_CREATE_NEW_INTENT_EXTRA);
        } else if (intent.hasExtra(SCROLL_TO_ALARM_INTENT_EXTRA)) {
            UiDataModel.getUiDataModel().setSelectedTab(ALARMS);

            long alarmId = intent.getLongExtra(SCROLL_TO_ALARM_INTENT_EXTRA, Alarm.INVALID_ID);
            if (alarmId != Alarm.INVALID_ID) {
                setSmoothScrollStableId(alarmId);
                if (mCursorLoader != null && mCursorLoader.isStarted()) {
                    // We need to force a reload here to make sure we have the latest view
                    // of the data to scroll to.
                    mCursorLoader.forceLoad();
                }
            }

            // Remove the SCROLL_TO_ALARM extra now that we've processed it.
            intent.removeExtra(SCROLL_TO_ALARM_INTENT_EXTRA);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        UiDataModel.getUiDataModel().removePeriodicCallback(mMidnightUpdater);

        // When the user places the app in the background by pressing "home",
        // dismiss the toast bar. However, since there is no way to determine if
        // home was pressed, just dismiss any existing toast bar when restarting
        // the app.
        mAlarmUpdateHandler.hideUndoBar();
    }

    @Override
    public void smoothScrollTo(int position) {
        mLayoutManager.scrollToPositionWithOffset(position, 0);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAlarmTimeClickHandler.saveInstance(outState);
        outState.putLong(KEY_EXPANDED_ID, mExpandedAlarmId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToastManager.cancelToast();
    }

    public void setLabel(Alarm alarm, String label) {
        alarm.label = label;
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return Alarm.getAlarmsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
        final List<AlarmItemHolder> itemHolders = new ArrayList<>(data.getCount());
        for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
            final Alarm alarm = new Alarm(data);
            final AlarmInstance alarmInstance = alarm.canPreemptivelyDismiss()
                    ? new AlarmInstance(data, true /* joinedTable */) : null;
            final AlarmItemHolder itemHolder =
                    new AlarmItemHolder(alarm, alarmInstance, mAlarmTimeClickHandler);
            itemHolders.add(itemHolder);
        }
        setAdapterItems(itemHolders, SystemClock.elapsedRealtime());
    }

    /**
     * Updates the adapters items, deferring the update until the current animation is finished or
     * if no animation is running then the listener will be automatically be invoked immediately.
     *
     * @param items       the new list of {@link AlarmItemHolder} to use
     * @param updateToken a monotonically increasing value used to preserve ordering of deferred
     *                    updates
     */
    private void setAdapterItems(final List<AlarmItemHolder> items, final long updateToken) {
        if (updateToken < mCurrentUpdateToken) {
            LogUtils.v("Ignoring adapter update: %d < %d", updateToken, mCurrentUpdateToken);
            return;
        }

        if (mRecyclerView.getItemAnimator().isRunning()) {
            // RecyclerView is currently animating -> defer update.
            mRecyclerView.getItemAnimator().isRunning(
                    new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                @Override
                public void onAnimationsFinished() {
                    setAdapterItems(items, updateToken);
                }
            });
        } else if (mRecyclerView.isComputingLayout()) {
            // RecyclerView is currently computing a layout -> defer update.
            mRecyclerView.post(new Runnable() {
                @Override
                public void run() {
                    setAdapterItems(items, updateToken);
                }
            });
        } else {
            mCurrentUpdateToken = updateToken;
            mItemAdapter.setItems(items);

            // Show or hide the empty view as appropriate.
            final boolean noAlarms = items.isEmpty();
            mEmptyViewController.setEmpty(noAlarms);
            if (noAlarms) {
                // Ensure the drop shadow is hidden when no alarms exist.
                setTabScrolledToTop(true);
            }

            // Expand the correct alarm.
            if (mExpandedAlarmId != Alarm.INVALID_ID) {
                final AlarmItemHolder aih = mItemAdapter.findItemById(mExpandedAlarmId);
                if (aih != null) {
                    mAlarmTimeClickHandler.setSelectedAlarm(aih.item);
                    aih.expand();
                } else {
                    mAlarmTimeClickHandler.setSelectedAlarm(null);
                    mExpandedAlarmId = Alarm.INVALID_ID;
                }
            }

            // Scroll to the selected alarm.
            if (mScrollToAlarmId != Alarm.INVALID_ID) {
                scrollToAlarm(mScrollToAlarmId);
                setSmoothScrollStableId(Alarm.INVALID_ID);
            }
        }
    }

    /**
     * @param alarmId identifies the alarm to be displayed
     */
    private void scrollToAlarm(long alarmId) {
        final int alarmCount = mItemAdapter.getItemCount();
        int alarmPosition = -1;
        for (int i = 0; i < alarmCount; i++) {
            long id = mItemAdapter.getItemId(i);
            if (id == alarmId) {
                alarmPosition = i;
                break;
            }
        }

        if (alarmPosition >= 0) {
            mItemAdapter.findItemById(alarmId).expand();
            smoothScrollTo(alarmPosition);
        } else {
            // Trying to display a deleted alarm should only happen from a missed notification for
            // an alarm that has been marked deleted after use.
            SnackbarManager.show(Snackbar.make(mMainLayout, R.string
                    .missed_alarm_has_been_deleted, Snackbar.LENGTH_LONG));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }

    @Override
    public void setSmoothScrollStableId(long stableId) {
        mScrollToAlarmId = stableId;
    }

    @Override
    public void onFabClick(@NonNull ImageView fab) {
        mAlarmUpdateHandler.hideUndoBar();
        startCreatingAlarm();
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        fab.setVisibility(View.VISIBLE);
        fab.setImageResource(R.drawable.ic_add_white_24dp);
        fab.setContentDescription(fab.getResources().getString(R.string.button_alarms));
    }

    @Override
    public void onUpdateFabButtons(@NonNull Button left, @NonNull Button right) {
        left.setVisibility(View.INVISIBLE);
        right.setVisibility(View.INVISIBLE);
    }

    private void startCreatingAlarm() {
        // Clear the currently selected alarm.
        mAlarmTimeClickHandler.setSelectedAlarm(null);
        TimePickerDialogFragment.show(this);
    }

    @Override
    public void onTimeSet(TimePickerDialogFragment fragment, int hourOfDay, int minute) {
        mAlarmTimeClickHandler.onTimeSet(hourOfDay, minute);
    }

    public void removeItem(AlarmItemHolder itemHolder) {
        mItemAdapter.removeItem(itemHolder);
    }

    /**
     * Updates the vertical scroll state of this tab in the {@link UiDataModel} as the user scrolls
     * the recyclerview or when the size/position of elements within the recyclerview changes.
     */
    private final class ScrollPositionWatcher extends RecyclerView.OnScrollListener
            implements View.OnLayoutChangeListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            setTabScrolledToTop(Utils.isScrolledToTop(mRecyclerView));
        }

        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                int oldLeft, int oldTop, int oldRight, int oldBottom) {
            setTabScrolledToTop(Utils.isScrolledToTop(mRecyclerView));
        }
    }

    /**
     * This runnable executes at midnight and refreshes the display of all alarms. Collapsed alarms
     * that do no repeat will have their "Tomorrow" strings updated to say "Today".
     */
    private final class MidnightRunnable implements Runnable {
        @Override
        public void run() {
            mItemAdapter.notifyDataSetChanged();
        }
    }
}
