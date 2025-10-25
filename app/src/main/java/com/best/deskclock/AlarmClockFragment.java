/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_ALARM_BY_NAME;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_ALARM_BY_NEXT_ALARM_TIME;
import static com.best.deskclock.settings.PreferencesDefaultValues.SPINNER_TIME_PICKER_STYLE;
import static com.best.deskclock.uidata.UiDataModel.Tab.ALARMS;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.alarms.AlarmDelayPickerDialogFragment;
import com.best.deskclock.alarms.AlarmTimeClickHandler;
import com.best.deskclock.alarms.AlarmUpdateCallback;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.alarms.ScrollHandler;
import com.best.deskclock.alarms.SpinnerTimePickerDialogFragment;
import com.best.deskclock.alarms.dataadapter.AlarmItemHolder;
import com.best.deskclock.alarms.dataadapter.AlarmItemViewHolder;
import com.best.deskclock.alarms.dataadapter.CollapsedAlarmViewHolder;
import com.best.deskclock.alarms.dataadapter.ExpandedAlarmViewHolder;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.uicomponents.EmptyViewController;
import com.best.deskclock.uicomponents.toast.SnackbarManager;
import com.best.deskclock.uicomponents.toast.ToastManager;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A fragment that displays a list of alarm time and allows interaction with them.
 */
public final class AlarmClockFragment extends DeskClockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, ScrollHandler, AlarmUpdateCallback {

    // This extra is used when receiving an intent to create an alarm, but no alarm details
    // have been passed in, so the alarm page should start the process of creating a new alarm.
    public static final String ALARM_CREATE_NEW_INTENT_EXTRA = "deskclock.create.new";

    // This extra is used when receiving an intent to scroll to specific alarm. If alarm
    // can not be found, and toast message will pop up that the alarm has be deleted.
    public static final String SCROLL_TO_ALARM_INTENT_EXTRA = "deskclock.scroll.to.alarm";

    private static final String KEY_EXPANDED_ID = "expandedId";

    private static final String KEY_SIDE_BUTTONS_VISIBLE = "side_buttons_visible";

    private Context mContext;
    private SharedPreferences mPrefs;

    // Updates "Today/Tomorrow" in the UI when midnight passes.
    private final Runnable mMidnightUpdater = new MidnightRunnable();

    // Views
    private ViewGroup mMainLayout;
    private AlarmRecyclerView mRecyclerView;
    private boolean mIsTablet;
    private boolean mIsPhoneInLandscape;

    // Data
    private Loader<?> mCursorLoader;
    private long mScrollToAlarmId = Alarm.INVALID_ID;
    private long mExpandedAlarmId = Alarm.INVALID_ID;
    private long mCurrentUpdateToken;
    private String mLastSortOrder = null;
    private boolean mBlockSortingUntilCollapse = false;

    // Controllers
    private ItemAdapter<AlarmItemHolder> mItemAdapter;
    private AlarmUpdateHandler mAlarmUpdateHandler;
    private EmptyViewController mEmptyViewController;
    private AlarmTimeClickHandler mAlarmTimeClickHandler;
    private LinearLayoutManager mLayoutManager;

    private boolean sideButtonsVisible = false;

    /**
     * The public no-arg constructor required by all fragments.
     */
    public AlarmClockFragment() {
        super(ALARMS);
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mContext = requireContext();
        mPrefs = getDefaultSharedPreferences(mContext);
        mCursorLoader = LoaderManager.getInstance(this).initLoader(0, null, this);
        mItemAdapter = new ItemAdapter<>();
        mIsTablet = ThemeUtils.isTablet();
        mIsPhoneInLandscape = !mIsTablet && ThemeUtils.isLandscape();

        if (savedState != null) {
            mExpandedAlarmId = savedState.getLong(KEY_EXPANDED_ID, Alarm.INVALID_ID);
            sideButtonsVisible = savedState.getBoolean(KEY_SIDE_BUTTONS_VISIBLE, false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.alarm_clock, container, false);

        mMainLayout = v.findViewById(R.id.main);
        mRecyclerView = v.findViewById(R.id.alarms_recycler_view);
        ConstraintLayout emptyAlarmView = v.findViewById(R.id.alarms_empty_view);

        // Set a bottom padding for phones in portrait mode and tablets to center correctly
        // the alarms empty view between the FAB and the top of the screen
        if (!mIsPhoneInLandscape) {
            emptyAlarmView.setPadding(0, 0, 0, ThemeUtils.convertDpToPixels(80, mContext));
        }

        mEmptyViewController = new EmptyViewController(mMainLayout, mRecyclerView, emptyAlarmView);
        mAlarmUpdateHandler = new AlarmUpdateHandler(mContext, this, mMainLayout);
        mAlarmUpdateHandler.setAlarmUpdateCallback(this);
        mAlarmTimeClickHandler = new AlarmTimeClickHandler(this, savedState, mAlarmUpdateHandler);
        mLayoutManager = new LinearLayoutManager(mContext) {
            @Override
            protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State state,
                                                     @NonNull int[] extraLayoutSpace) {
                // We need enough space so after expand/collapse, other items are still
                // shown properly. The multiplier was chosen after tests
                extraLayoutSpace[0] = 2 * getHeight();
                extraLayoutSpace[1] = extraLayoutSpace[0];

            }
        };
        final GestureDetector gestureDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                hideSideButtonsWithFabAnimation();
                return false;
            }
        });

        mMainLayout.setOnClickListener(view -> hideSideButtonsWithFabAnimation());

        mRecyclerView.setOnTouchListener((view, event) -> {
            gestureDetector.onTouchEvent(event);
            view.performClick();
            return false;
        });

        mRecyclerView.setLayoutManager(mLayoutManager);
        // Due to the ViewPager and the location of FAB, set a bottom padding and/or a right padding
        // to prevent the alarm list from being hidden by the FAB (e.g. when scrolling down).
        final int rightPadding = ThemeUtils.convertDpToPixels(mIsPhoneInLandscape ? 85 : 0, mContext);
        final int bottomPadding = ThemeUtils.convertDpToPixels(mIsTablet ? 110 : mIsPhoneInLandscape ? 5 : 95, mContext);
        mRecyclerView.setPadding(0, 0, rightPadding, bottomPadding);

        mItemAdapter.setHasStableIds();
        mItemAdapter.withViewTypes(new CollapsedAlarmViewHolder.Factory(inflater),
                null, null, CollapsedAlarmViewHolder.VIEW_TYPE);
        mItemAdapter.withViewTypes(new ExpandedAlarmViewHolder.Factory(mContext),
                null, null, ExpandedAlarmViewHolder.VIEW_TYPE);
        mItemAdapter.setOnItemChangedListener(holder -> {
            hideSideButtonsWithFabAnimation();

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
                        smoothScrollTo(viewHolder.getBindingAdapterPosition());
                    }
                }
            } else if (mExpandedAlarmId == holder.itemId) {
                // The expanded alarm is now collapsed so update the tracking id.
                mExpandedAlarmId = Alarm.INVALID_ID;
            }
        });

        mRecyclerView.setAdapter(mItemAdapter);

        final ItemAnimator itemAnimator = new ItemAnimator();
        itemAnimator.setChangeDuration(300L);
        itemAnimator.setMoveDuration(300L);
        mRecyclerView.setItemAnimator(itemAnimator);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

           @Override
           public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                 @NonNull RecyclerView.ViewHolder target) {

               return false;
           }

           @Override
           public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                   @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                   int actionState, boolean isCurrentlyActive) {

               super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

               // Swiping Right
               if (dX > 0) {
                   hideSideButtonsWithFabAnimation();

                   // Background
                   c.clipRect(
                           viewHolder.itemView.getLeft(),
                           viewHolder.itemView.getTop(),
                           viewHolder.itemView.getLeft() + (int) dX,
                           viewHolder.itemView.getBottom()
                   );

                   final GradientDrawable background = new GradientDrawable();
                   background.setColor(mContext.getColor(R.color.colorAlert));
                   background.setBounds(
                           viewHolder.itemView.getLeft(),
                           viewHolder.itemView.getTop(),
                           viewHolder.itemView.getLeft() + (int) dX,
                           viewHolder.itemView.getBottom()
                   );
                   background.setCornerRadius(ThemeUtils.convertDpToPixels(12, mContext));
                   background.draw(c);

                   // Delete icon
                   int deleteIconSize = 0;
                   int deleteIconHorizontalMargin = ThemeUtils.convertDpToPixels(16, mContext);

                   if (dX > deleteIconHorizontalMargin) {
                       Drawable deleteIcon = AppCompatResources.getDrawable(mContext, R.drawable.ic_delete);
                       if (deleteIcon != null) {
                           DrawableCompat.setTint(deleteIcon, MaterialColors.getColor(
                                   mContext, com.google.android.material.R.attr.colorOnError, Color.BLACK));
                           deleteIconSize = deleteIcon.getIntrinsicHeight();
                           int halfIcon = deleteIconSize / 2;
                           int top = viewHolder.itemView.getTop()
                                   + ((viewHolder.itemView.getBottom()
                                   - viewHolder.itemView.getTop()) / 2 - halfIcon);

                           deleteIcon.setBounds(
                                   viewHolder.itemView.getLeft() + deleteIconHorizontalMargin,
                                   top,
                                   viewHolder.itemView.getLeft()
                                           + deleteIconHorizontalMargin
                                           + deleteIcon.getIntrinsicWidth(),
                                   top + deleteIcon.getIntrinsicHeight()
                           );

                           deleteIcon.draw(c);
                       }
                   }

                   // Delete text
                   final String deleteText = mContext.getString(R.string.delete);
                   if (dX > deleteIconHorizontalMargin + deleteIconSize) {
                       TextPaint textPaint = new TextPaint();
                       textPaint.setAntiAlias(true);
                       textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16,
                               mContext.getResources().getDisplayMetrics()));
                       textPaint.setColor(MaterialColors.getColor(
                               mContext, com.google.android.material.R.attr.colorOnError, Color.BLACK));
                       textPaint.setTypeface(Typeface.DEFAULT_BOLD);

                       int textMarginLeft = (int) (viewHolder.itemView.getLeft()
                               + 1.5 * deleteIconHorizontalMargin + deleteIconSize);

                       int textMarginTop = (int) (viewHolder.itemView.getTop()
                               + ((viewHolder.itemView.getBottom()
                               - viewHolder.itemView.getTop()) / 2.0)
                               + (textPaint.getTextSize() - textPaint.getFontMetrics().descent) / 2);

                       c.drawText(deleteText, textMarginLeft, textMarginTop, textPaint);
                   }
               }
           }

           @Override
           public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
               AlarmItemViewHolder alarmHolder = (AlarmItemViewHolder) viewHolder;
               AlarmItemHolder itemHolder = alarmHolder.getItemHolder();

               removeItem(itemHolder);
               final Alarm alarm = itemHolder.item;
               Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);
               mAlarmUpdateHandler.asyncDeleteAlarm(alarm);
           }
        });

        if (ThemeUtils.areSystemAnimationsDisabled(mContext)) {
            itemTouchHelper.attachToRecyclerView(null);
        } else {
            itemTouchHelper.attachToRecyclerView(mRecyclerView);
        }

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // We use Fragment Result API instead of a direct listener to safely pass data back from
        // DialogFragment. This approach allows the result to survive configuration changes and
        // fragment recreation.
        // Direct interface callbacks would be lost after a configuration change because
        // the original Fragment instance (and its listener) would be destroyed.

        getParentFragmentManager().setFragmentResultListener(SpinnerTimePickerDialogFragment.REQUEST_KEY,
                this, (requestKey, result) -> {
            if (SpinnerTimePickerDialogFragment.REQUEST_KEY.equals(requestKey)) {
                int hours = result.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int minutes = result.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_MINUTES);
                mAlarmTimeClickHandler.setAlarm(hours, minutes);
            }
        });

        getParentFragmentManager().setFragmentResultListener(AlarmDelayPickerDialogFragment.REQUEST_KEY,
                this, (requestKey, result) -> {
            if (AlarmDelayPickerDialogFragment.REQUEST_KEY.equals(requestKey)) {
                int hours = result.getInt(AlarmDelayPickerDialogFragment.BUNDLE_KEY_HOURS);
                int minutes = result.getInt(AlarmDelayPickerDialogFragment.BUNDLE_KEY_MINUTES);
                mAlarmTimeClickHandler.setAlarmWithDelay(hours, minutes);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Schedule a runnable to update the "Today/Tomorrow" values displayed for non-repeating
        // alarms when midnight passes.
        UiDataModel.getUiDataModel().addMidnightCallback(mMidnightUpdater, 100);

        // Check if another app asked us to create a blank new alarm.
        final Intent intent = requireActivity().getIntent();
        if (intent == null) {
            return;
        }

        // If the sort order has changed since last time, reload the alarms
        String currentSortOrder = SettingsDAO.getAlarmSorting(mPrefs) +
                "_enabledFirst=" + SettingsDAO.areEnabledAlarmsDisplayedFirst(mPrefs);
        if (!currentSortOrder.equals(mLastSortOrder)) {
            mLastSortOrder = currentSortOrder;
            LoaderManager.getInstance(this).restartLoader(0, null, this);
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

        // Hide side buttons only if we're leaving the activity (not rotating the screen)
        if (!requireActivity().isChangingConfigurations()) {
            resetFabAndButtonsState();
        }
    }

    /**
     * Perform smooth scroll to position.
     */
    public void smoothScrollTo(int position) {
        mLayoutManager.scrollToPositionWithOffset(position, 0);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_EXPANDED_ID, mExpandedAlarmId);
        outState.putBoolean(KEY_SIDE_BUTTONS_VISIBLE, sideButtonsVisible);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ToastManager.cancelToast();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return Alarm.getAlarmsCursorLoader(getActivity());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor data) {
        final List<AlarmItemHolder> itemHolders = new ArrayList<>(data.getCount());

        // Convert each row in the cursor into an AlarmItemHolder
        for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
            final Alarm alarm = new Alarm(data);
            final AlarmInstance alarmInstance = new AlarmInstance(data, true);
            final AlarmItemHolder itemHolder = new AlarmItemHolder(alarm, alarmInstance, mAlarmTimeClickHandler);
            itemHolders.add(itemHolder);
        }

        final boolean wantsSortByNextAlarmTime = SettingsDAO.getAlarmSorting(mPrefs).equals(SORT_ALARM_BY_NEXT_ALARM_TIME);
        final boolean wantsSortByName = SettingsDAO.getAlarmSorting(mPrefs).equals(SORT_ALARM_BY_NAME);
        final boolean areEnabledAlarmsFirst = SettingsDAO.areEnabledAlarmsDisplayedFirst(mPrefs);
        final boolean isEditingAlarm = mExpandedAlarmId != Alarm.INVALID_ID;
        final boolean shouldBlockSorting = isEditingAlarm || mBlockSortingUntilCollapse;

        // Sort by name if requested and not blocked
        if (wantsSortByName && !shouldBlockSorting) {
            final Collator collator = Collator.getInstance();
            // Ignore case and respect accents
            collator.setStrength(Collator.SECONDARY);

            Collections.sort(itemHolders, (h1, h2) -> {
                if (areEnabledAlarmsFirst && h1.item.enabled != h2.item.enabled) {
                    return h1.item.enabled ? -1 : 1;
                }

                String l1 = h1.item.label != null ? h1.item.label : "";
                String l2 = h2.item.label != null ? h2.item.label : "";
                return collator.compare(l1, l2);
            });
        } else if (wantsSortByNextAlarmTime && !shouldBlockSorting) {
            // Sort by next alarm time if requested and not blocked
            Calendar now = Calendar.getInstance();
            Collections.sort(itemHolders, (h1, h2) -> {
                if (areEnabledAlarmsFirst && h1.item.enabled != h2.item.enabled) {
                    // Sort enabled alarms before disabled ones: true comes before false
                    return h1.item.enabled ? -1 : 1;
                }

                // Get the next scheduled alarm time for each item
                Calendar t1 = h1.item.getSortableNextAlarmTime(h1.item, now);
                Calendar t2 = h2.item.getSortableNextAlarmTime(h2.item, now);

                // Both alarms have valid upcoming times: compare them chronologically
                return Long.compare(t1.getTimeInMillis(), t2.getTimeInMillis());
            });
        } else if (shouldBlockSorting && mItemAdapter.getItemCount() > 0) {
            // Preserve current visual order while alarm is expanded
            List<AlarmItemHolder> stable = reorderToMatchCurrentVisualOrder(itemHolders);
            itemHolders.clear();
            itemHolders.addAll(stable);
        }

        // Apply the final list to the adapter
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

        if (Objects.requireNonNull(mRecyclerView.getItemAnimator()).isRunning()) {
            // RecyclerView is currently animating -> defer update.
            mRecyclerView.getItemAnimator().isRunning(() -> setAdapterItems(items, updateToken));
        } else if (mRecyclerView.isComputingLayout()) {
            // RecyclerView is currently computing a layout -> defer update.
            mRecyclerView.post(() -> setAdapterItems(items, updateToken));
        } else {
            mCurrentUpdateToken = updateToken;

            mItemAdapter.setItems(items);

            attachCollapseListeners(items);

            // Show or hide the empty view as appropriate.
            final boolean noAlarms = items.isEmpty();
            mEmptyViewController.setEmpty(noAlarms);

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
     * Reorders the given list of alarm holders to match the current visual order
     * displayed in the adapter. This is used to preserve UI stability during edits.
     *
     * @param freshItems the newly loaded alarm items
     * @return a reordered list matching the adapter's current visual order
     */
    private List<AlarmItemHolder> reorderToMatchCurrentVisualOrder(List<AlarmItemHolder> freshItems) {
        // Map each item by its stable ID
        Map<Long, AlarmItemHolder> byId = new HashMap<>(freshItems.size());
        for (AlarmItemHolder h : freshItems) {
            byId.put(h.getStableId(), h);
        }

        // Get the current visual order from the adapter
        List<Long> currentOrder = new ArrayList<>(mItemAdapter.getItemCount());
        for (int i = 0; i < mItemAdapter.getItemCount(); i++) {
            currentOrder.add(mItemAdapter.getItemId(i));
        }

        // Rebuild the list in the same order
        List<AlarmItemHolder> reordered = new ArrayList<>(freshItems.size());
        for (Long id : currentOrder) {
            AlarmItemHolder h = byId.remove(id);
            if (h != null) {
                reordered.add(h);
            }
        }

        // Append any new items not previously displayed
        if (!byId.isEmpty()) {
            reordered.addAll(byId.values());
        }

        return reordered;
    }

    /**
     * Attaches a collapse listener to each alarm item.
     * When an alarm is collapsed, sorting is unblocked and the loader is restarted.
     * <p>
     * This ensures sorting is reapplied after collapsing, in cases where the alarm
     * was modified while expanded (e.g. enabled/disabled).
     * </p>
     * @param items the list of alarm item holders to attach listeners to
     */
    private void attachCollapseListeners(List<AlarmItemHolder> items) {
        for (AlarmItemHolder holder : items) {
            holder.setOnAlarmCollapseListener(() -> {
                mExpandedAlarmId = Alarm.INVALID_ID;
                mBlockSortingUntilCollapse = false;

                LoaderManager.getInstance(AlarmClockFragment.this)
                        .restartLoader(0, null, AlarmClockFragment.this);
            });

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
    public void onAlarmUpdateStarted() {
        // Blocks sorting if an alarm is currently expanded to prevent visual reordering.
        mBlockSortingUntilCollapse = (mExpandedAlarmId != Alarm.INVALID_ID);
    }

    @Override
    public void onAlarmUpdateFinished() {
        // Restart sorting immediately if no alarm is expanded at the time of update
        // (e.g. when an alarm is enabled/disabled from the collapsed view).

        // If the alarm is still expanded, sorting will be postponed until collapse,
        // and will be restarted by the collapse listener attached in attachCollapseListeners(...).
        if (mExpandedAlarmId == Alarm.INVALID_ID) {
            // We are in collapsed view: do not block sorting
            mBlockSortingUntilCollapse = false;
            new Handler(Looper.getMainLooper()).post(() ->
                    LoaderManager.getInstance(AlarmClockFragment.this)
                            .restartLoader(0, null, AlarmClockFragment.this)
            );
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
    }

    @Override
    public void setSmoothScrollStableId(long stableId) {
        mScrollToAlarmId = stableId;
    }

    @Override
    public void onFabClick() {
        mAlarmUpdateHandler.hideUndoBar();

        if (SettingsDAO.isAlarmFabLongPressEnabled(mPrefs)) {
            startCreatingAlarm();
        } else {
            sideButtonsVisible = true;
            updateFab(FAB_AND_BUTTONS_SHRINK_AND_EXPAND);
        }
    }

    @Override
    public void onFabLongClick(@NonNull ImageView fab) {
        if (SettingsDAO.isAlarmFabLongPressEnabled(mPrefs)) {
            fab.setHapticFeedbackEnabled(true);
            mAlarmUpdateHandler.hideUndoBar();
            startCreatingAlarmWithDelay();
        } else {
            fab.setHapticFeedbackEnabled(false);
        }
    }

    @Override
    public void onUpdateFab(@NonNull ImageView fab) {
        fab.setImageResource(R.drawable.ic_add);
        fab.setContentDescription(fab.getResources().getString(R.string.button_alarms));

        if (SettingsDAO.isAlarmFabLongPressEnabled(mPrefs)) {
            fab.setVisibility(VISIBLE);
        } else {
            if (sideButtonsVisible) {
                fab.setVisibility(INVISIBLE);
            } else {
                fab.setVisibility(VISIBLE);
            }
        }
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        if (SettingsDAO.isAlarmFabLongPressEnabled(mPrefs)) {
            left.setVisibility(INVISIBLE);
            right.setVisibility(INVISIBLE);
        } else {
            if (sideButtonsVisible) {
                left.setVisibility(VISIBLE);
                right.setVisibility(VISIBLE);

                left.setClickable(true);
                left.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_av_timer));
                left.setContentDescription(mContext.getString(R.string.button_alarms));
                left.setOnClickListener(v -> {
                    startCreatingAlarmWithDelay();
                    hideSideButtonsWithFabAnimation();
                });

                right.setClickable(true);
                right.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_alarm_add));
                right.setContentDescription(mContext.getString(R.string.button_alarms));
                right.setOnClickListener(v -> {
                    startCreatingAlarm();
                    hideSideButtonsWithFabAnimation();
                });
            } else {
                left.setVisibility(INVISIBLE);
                right.setVisibility(INVISIBLE);
            }
        }
    }

    private void hideSideButtonsWithFabAnimation() {
        if (sideButtonsVisible) {
            sideButtonsVisible = false;
            updateFab(FAB_AND_BUTTONS_SHRINK_AND_EXPAND);
        }
    }

    public void hideSideButtonsOnlyAnimated() {
        if (sideButtonsVisible) {
            sideButtonsVisible = false;
            updateFab(BUTTONS_SHRINK_AND_EXPAND);
        }
    }

    public void resetFabAndButtonsState() {
        if (sideButtonsVisible) {
            sideButtonsVisible = false;
            updateFab(FAB_AND_BUTTONS_IMMEDIATE);
        }
    }

    private void startCreatingAlarm() {
        // Clear the currently selected alarm.
        mAlarmTimeClickHandler.setSelectedAlarm(null);

        final Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        if (SettingsDAO.getMaterialTimePickerStyle(mPrefs).equals(SPINNER_TIME_PICKER_STYLE)) {
            mAlarmTimeClickHandler.showSpinnerTimePickerDialog(currentHour, currentMinute);
        } else {
            mAlarmTimeClickHandler.showMaterialTimePicker(currentHour, currentMinute);
        }
    }

    private void startCreatingAlarmWithDelay() {
        // Clear the currently selected alarm.
        mAlarmTimeClickHandler.setSelectedAlarm(null);
        mAlarmTimeClickHandler.showAlarmDelayPickerDialog();
    }

    public void removeItem(AlarmItemHolder itemHolder) {
        mItemAdapter.removeItem(itemHolder);
    }

    public void setLabel(Alarm alarm, String label) {
        alarm.label = label;
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
    }

    public void setAutoSilenceDuration(Alarm alarm, int autoSilenceDuration) {
        alarm.autoSilenceDuration = autoSilenceDuration;
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
    }

    public void setSnoozeDuration(Alarm alarm, int snoozeDuration) {
        alarm.snoozeDuration = snoozeDuration;
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
    }

    public void setMissedAlarmRepeatLimit(Alarm alarm, int missedAlarmRepeatLimit) {
        alarm.missedAlarmRepeatLimit = missedAlarmRepeatLimit;
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
    }

    public void setCrescendoDuration(Alarm alarm, int crescendoDuration) {
        alarm.crescendoDuration = crescendoDuration;
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
    }

    public void setAlarmVolume(Alarm alarm, int alarmVolume) {
        alarm.alarmVolume = alarmVolume;
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
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
