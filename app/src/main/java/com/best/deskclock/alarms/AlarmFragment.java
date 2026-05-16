/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_ALARM_BY_NAME;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_ALARM_BY_NEXT_ALARM_TIME;
import static com.best.deskclock.settings.PreferencesDefaultValues.SORT_ALARM_MANUALLY;
import static com.best.deskclock.settings.PreferencesDefaultValues.SPINNER_TIME_PICKER_STYLE;
import static com.best.deskclock.uidata.UiDataModel.Tab.ALARMS;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.best.deskclock.AppExecutors;
import com.best.deskclock.DeskClockFragment;
import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.AlarmFragmentBinding;
import com.best.deskclock.dialogfragment.AlarmDelayPickerDialogFragment;
import com.best.deskclock.dialogfragment.AlarmVolumeDialogFragment;
import com.best.deskclock.dialogfragment.MaterialTimePickerDialogFragment;
import com.best.deskclock.dialogfragment.SpinnerTimePickerDialogFragment;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.uicomponents.EmptyViewController;
import com.best.deskclock.uicomponents.toast.SnackbarManager;
import com.best.deskclock.uicomponents.toast.ToastManager;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.RingtoneUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.snackbar.Snackbar;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A fragment that displays a list of alarm time and allows interaction with them.
 */
public final class AlarmFragment extends DeskClockFragment
    implements LoaderManager.LoaderCallbacks<Cursor>, ScrollHandler, AlarmTouchContract {

    // This extra is used when receiving an intent to create an alarm, but no alarm details
    // have been passed in, so the alarm page should start the process of creating a new alarm.
    public static final String ALARM_CREATE_NEW_INTENT_EXTRA = "deskclock.create.new";

    // This extra is used when receiving an intent to scroll to specific alarm. If alarm
    // can not be found, and toast message will pop up that the alarm has be deleted.
    public static final String SCROLL_TO_ALARM_INTENT_EXTRA = "deskclock.scroll.to.alarm";

    private static final String KEY_SIDE_BUTTONS_VISIBLE = "side_buttons_visible";

    private AlarmFragmentBinding mBinding;

    private SharedPreferences mPrefs;
    private DisplayMetrics mDisplayMetrics;
    private Typeface mBoldTypeface;

    // Updates "Today/Tomorrow" in the UI when midnight passes.
    private final Runnable mMidnightUpdater = new MidnightRunnable();

    private boolean mIsTablet;
    private boolean mIsLandscape;
    private boolean mIsPhoneInLandscape;
    private boolean mIsLowAlarmVolumeWarningEnabled;
    private boolean mSideButtonsVisible = false;
    private boolean mIsUiTransitioning = false;
    private boolean mIsReordering = false;

    // Data
    private Loader<?> mCursorLoader;
    private Alarm mPendingAlarmToEdit = null;
    private long mScrollToAlarmId = Alarm.INVALID_ID;
    private long mCurrentUpdateToken;
    private String mLastSortOrder = null;

    // Controllers
    private AlarmAdapter mItemAdapter;
    private AlarmUpdateHandler mAlarmUpdateHandler;
    private EmptyViewController mEmptyViewController;
    private AlarmTimeClickHandler mAlarmTimeClickHandler;

    private final BroadcastReceiver mVolumeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RingtoneUtils.VOLUME_CHANGED_ACTION.equals(intent.getAction())) {
                updateWarningBannerVisibility();
            }
        }
    };

    /**
     * The public no-arg constructor required by all fragments.
     */
    public AlarmFragment() {
        super(ALARMS);
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mPrefs = getDefaultSharedPreferences(requireContext());
        mDisplayMetrics = getResources().getDisplayMetrics();
        mBoldTypeface = ThemeUtils.boldTypeface(SettingsDAO.getGeneralFont(mPrefs));
        mCursorLoader = LoaderManager.getInstance(this).initLoader(0, null, this);
        mItemAdapter = new AlarmAdapter(requireContext());
        mIsTablet = ThemeUtils.isTablet();
        mIsLandscape = ThemeUtils.isLandscape();
        mIsPhoneInLandscape = !mIsTablet && mIsLandscape;
        mIsLowAlarmVolumeWarningEnabled = SettingsDAO.isLowAlarmVolumeWarningDisplayed(mPrefs);

        if (savedState != null) {
            mSideButtonsVisible = savedState.getBoolean(KEY_SIDE_BUTTONS_VISIBLE, false);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mBinding = AlarmFragmentBinding.inflate(inflater, container, false);

        mBinding.alarmVolumeWarningBanner.volumeWarningText.setTypeface(mBoldTypeface);
        mBinding.alarmVolumeWarningBanner.volumeWarningButton.setTypeface(mBoldTypeface);
        mBinding.alarmVolumeWarningBanner.volumeWarningButton.setOnClickListener(v ->
            RingtoneUtils.fixAlarmStreamLow(requireContext())
        );

        // Set a bottom padding for phones in portrait mode and tablets to center correctly
        // the alarms empty view between the FAB and the top of the screen
        if (!mIsPhoneInLandscape) {
            mBinding.alarmEmptyView.setPadding(0, 0, 0, (int) dpToPx(80, mDisplayMetrics));
        }

        mEmptyViewController = new EmptyViewController(mBinding.alarmRootView, mBinding.alarmRecyclerView, mBinding.alarmEmptyView);
        mAlarmUpdateHandler = new AlarmUpdateHandler(requireContext(), this, mBinding.alarmRootView);
        mAlarmTimeClickHandler = new AlarmTimeClickHandler(this, mAlarmUpdateHandler);

        final GestureDetector gestureDetector = new GestureDetector(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent e) {
                hideSideButtonsWithFabAnimation();
                return false;
            }
        });

        mBinding.alarmRootView.setOnClickListener(v -> hideSideButtonsWithFabAnimation());

        mBinding.alarmRecyclerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            mBinding.getRoot().performClick();
            return false;
        });

        mBinding.alarmRecyclerView.setLayoutManager(getLayoutManager());

        // Due to the ViewPager and the location of FAB, set a bottom padding and/or a right padding
        // to prevent the alarm list from being hidden by the FAB (e.g. when scrolling down).
        final int rightPadding = (int) dpToPx(mIsPhoneInLandscape ? 80 : 0, mDisplayMetrics);
        final int bottomPadding = (int) dpToPx(mIsTablet ? 110 : mIsPhoneInLandscape ? 0 : 100, mDisplayMetrics);
        mBinding.alarmRecyclerView.setPaddingRelative(0, 0, rightPadding, bottomPadding);

        mBinding.alarmRecyclerView.setAdapter(mItemAdapter);

        mBinding.alarmRecyclerView.addItemDecoration(new GridSpacingItemDecoration(requireContext(), mDisplayMetrics));

        RecyclerView.ItemAnimator animator = mBinding.alarmRecyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            // Disable flash/blinking during updates (notifyItemChanged)
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        AlarmItemTouchHelper callback =
            new AlarmItemTouchHelper(requireContext(), this, mBinding.alarmRecyclerView, mIsTablet, mIsLandscape);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);

        if (ThemeUtils.areSystemAnimationsDisabled(requireContext())) {
            itemTouchHelper.attachToRecyclerView(null);
        } else {
            itemTouchHelper.attachToRecyclerView(mBinding.alarmRecyclerView);
        }

        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupFragmentResultListeners();
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(RingtoneUtils.VOLUME_CHANGED_ACTION);
        if (SdkUtils.isAtLeastAndroid13()) {
            requireContext().registerReceiver(mVolumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(mVolumeReceiver, filter);
        }
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
        String currentSortOrder = SettingsDAO.getAlarmSorting(mPrefs)
            + "_enabledFirst="
            + SettingsDAO.areEnabledAlarmsDisplayedFirst(mPrefs);
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

            if (intent.hasExtra(AlarmNotifications.EXTRA_MISSED_ALARM_NOTIFICATION)) {
                if (intent.getBooleanExtra(AlarmNotifications.EXTRA_MISSED_ALARM_NOTIFICATION, false)) {
                    int notificationId = intent.getIntExtra(AlarmNotifications.EXTRA_NOTIFICATION_ID, -1);
                    long instanceId = intent.getLongExtra(AlarmNotifications.EXTRA_MISSED_ALARM_INSTANCE_ID, -1);

                    // Cancel the missed alarm notification
                    if (notificationId != -1) {
                        NotificationManagerCompat.from(requireContext()).cancel(notificationId);
                    }

                    // Update the missed alarm notifications group
                    AlarmNotifications.updateMissedAlarmGroupNotification(requireContext(), notificationId, null);

                    // Clean instance
                    if (instanceId != -1) {
                        Context appContext = requireContext().getApplicationContext();
                        AppExecutors.getDiskIO().execute(() -> {
                            AlarmInstance instance = AlarmInstance.getInstance(appContext.getContentResolver(), instanceId);
                            if (instance != null) {
                                AlarmStateManager.deleteInstanceAndUpdateParent(appContext, instance, false);
                            }
                        });
                    }

                    // Remove Extras related to missed alarms
                    intent.removeExtra(AlarmNotifications.EXTRA_MISSED_ALARM_NOTIFICATION);
                    intent.removeExtra(AlarmNotifications.EXTRA_NOTIFICATION_ID);
                    intent.removeExtra(AlarmNotifications.EXTRA_MISSED_ALARM_INSTANCE_ID);
                }
            }

            // Remove the SCROLL_TO_ALARM extra now that we've processed it.
            intent.removeExtra(SCROLL_TO_ALARM_INTENT_EXTRA);
        }

        updateWarningBannerVisibility();
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
        mBinding.alarmRecyclerView.smoothScrollToPosition(position);
    }

    @Override
    public void onStop() {
        super.onStop();

        requireContext().unregisterReceiver(mVolumeReceiver);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_SIDE_BUTTONS_VISIBLE, mSideButtonsVisible);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mBinding.alarmRecyclerView.setAdapter(null);

        mBinding = null;

        mEmptyViewController = null;
        mAlarmUpdateHandler = null;
        mAlarmTimeClickHandler = null;
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

        // Convert each row in the cursor into an AlarmItemHolder.
        // Note: If the user selected Manual Sort or Creation Date, the cursor is ALREADY correctly sorted by the SQL query
        // in getAlarmsCursorLoader().
        for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
            final Alarm alarm = new Alarm(data);
            final AlarmInstance alarmInstance = new AlarmInstance(data, true);
            final AlarmItemHolder itemHolder = new AlarmItemHolder(alarm, alarmInstance, mAlarmTimeClickHandler);

            itemHolders.add(itemHolder);
        }

        final boolean wantsSortByNextAlarmTime = SettingsDAO.getAlarmSorting(mPrefs).equals(SORT_ALARM_BY_NEXT_ALARM_TIME);
        final boolean wantsSortByName = SettingsDAO.getAlarmSorting(mPrefs).equals(SORT_ALARM_BY_NAME);
        final boolean areEnabledAlarmsFirst = SettingsDAO.areEnabledAlarmsDisplayedFirst(mPrefs);

        // We only need to manually sort in memory for complex calculations that SQL cannot handle natively
        // (like Collator accents or Calendar next alarm times).

        // Sort by name if requested
        if (wantsSortByName) {
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
        } else if (wantsSortByNextAlarmTime) {
            // Sort by next alarm time if requested
            Calendar now = Calendar.getInstance();
            Collections.sort(itemHolders, (h1, h2) -> {
                if (areEnabledAlarmsFirst && h1.item.enabled != h2.item.enabled) {
                    // Sort enabled alarms before disabled ones: true comes before false
                    return h1.item.enabled ? -1 : 1;
                }

                // Get the next scheduled alarm time for each item
                Calendar t1 = h1.item.getSortableNextAlarmTime(h1.getAlarmInstance(), now);
                Calendar t2 = h2.item.getSortableNextAlarmTime(h2.getAlarmInstance(), now);

                // Both alarms have valid upcoming times: compare them chronologically
                return Long.compare(t1.getTimeInMillis(), t2.getTimeInMillis());
            });
        }

        // Apply the final list to the adapter
        setAdapterItems(itemHolders, SystemClock.elapsedRealtime());
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
            mSideButtonsVisible = true;
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
            if (mSideButtonsVisible) {
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
            if (mSideButtonsVisible) {
                left.setVisibility(VISIBLE);
                right.setVisibility(VISIBLE);

                left.setClickable(true);
                left.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_av_timer));
                left.setContentDescription(getString(R.string.button_alarms));
                left.setOnClickListener(v -> {
                    startCreatingAlarmWithDelay();
                    hideSideButtonsWithFabAnimation();
                });

                right.setClickable(true);
                right.setImageDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_alarm_add));
                right.setContentDescription(getString(R.string.button_alarms));
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

    @Override
    public boolean canDrag() {
        return SettingsDAO.getAlarmSorting(mPrefs).equals(SORT_ALARM_MANUALLY);
    }

    @Override
    public boolean canSwipe() {
        Fragment bottomSheet = getParentFragmentManager().findFragmentByTag(AlarmEditBottomSheetFragment.TAG);
        Fragment delayDialog = getParentFragmentManager().findFragmentByTag(AlarmDelayPickerDialogFragment.TAG);

        return !(bottomSheet != null && bottomSheet.isAdded()) && !(delayDialog != null && delayDialog.isAdded());
    }

    @Override
    public void onRowMoved(int fromPosition, int toPosition) {
        mItemAdapter.swapItems(fromPosition, toPosition);
    }

    @Override
    public void onRowSelected(RecyclerView.ViewHolder viewHolder) {
        // Draw a shadow under the alarm card when it's dragging
        viewHolder.itemView.setTranslationZ(dpToPx(6, mDisplayMetrics));
    }

    @Override
    public void onRowClear(RecyclerView.ViewHolder viewHolder) {
        // Remove the shadow under the alarm card when the drag is complete.
        viewHolder.itemView.setTranslationZ(0f);
    }

    @Override
    public void onRowSaved() {
        mIsReordering = true;
        saveManualSortOrder();
    }

    @Override
    public void onRowSwiped(RecyclerView.ViewHolder viewHolder) {
        AlarmItemViewHolder alarmHolder = (AlarmItemViewHolder) viewHolder;
        AlarmItemHolder itemHolder = alarmHolder.getItemHolder();

        mItemAdapter.removeItem(itemHolder);
        final Alarm alarm = itemHolder.item;
        Events.sendAlarmEvent(R.string.action_delete, R.string.label_deskclock);
        mAlarmUpdateHandler.asyncDeleteAlarm(alarm);
    }

    @Override
    public void onSwipeStarted() {
        hideSideButtonsWithFabAnimation();
    }

    private LinearLayoutManager getLayoutManager() {
        if (mIsTablet && mIsLandscape) {
            return new GridLayoutManager(requireContext(), 3);
        }

        if (mIsTablet || mIsLandscape) {
            return new GridLayoutManager(requireContext(), 2);
        }

        return new LinearLayoutManager(requireContext());
    }

    private void setupFragmentResultListeners() {
        // We use Fragment Result API instead of a direct listener to safely pass data back from
        // DialogFragment. This approach allows the result to survive configuration changes and
        // fragment recreation.
        // Direct interface callbacks would be lost after a configuration change because
        // the original Fragment instance (and its listener) would be destroyed.

        FragmentManager parentFragmentManager = getParentFragmentManager();
        LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();

        parentFragmentManager.setFragmentResultListener(
            AlarmEditBottomSheetFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, bundle) -> {
                long alarmId = bundle.getLong(AlarmEditBottomSheetFragment.SCROLL_TO_ALARM_ID, Alarm.INVALID_ID);
                if (alarmId != Alarm.INVALID_ID) {
                    setSmoothScrollStableId(alarmId);
                }
            }
        );

        parentFragmentManager.setFragmentResultListener(MaterialTimePickerDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, result) -> {
                int hours = result.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int minutes = result.getInt(MaterialTimePickerDialogFragment.BUNDLE_KEY_MINUTES);

                Alarm selected = mAlarmTimeClickHandler.getSelectedAlarm();
                if (selected != null) {
                    setSmoothScrollStableId(selected.id);
                }

                mAlarmTimeClickHandler.setAlarm(hours, minutes);
            });

        parentFragmentManager.setFragmentResultListener(SpinnerTimePickerDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, result) -> {
                int hours = result.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_HOURS);
                int minutes = result.getInt(SpinnerTimePickerDialogFragment.BUNDLE_KEY_MINUTES);

                Alarm selectedAlarm = mAlarmTimeClickHandler.getSelectedAlarm();
                if (selectedAlarm != null) {
                    setSmoothScrollStableId(selectedAlarm.id);
                }

                mAlarmTimeClickHandler.setAlarm(hours, minutes);
            });

        parentFragmentManager.setFragmentResultListener(AlarmDelayPickerDialogFragment.REQUEST_KEY, viewLifecycleOwner,
            (requestKey, result) -> {
                int hours = result.getInt(AlarmDelayPickerDialogFragment.BUNDLE_KEY_HOURS);
                int minutes = result.getInt(AlarmDelayPickerDialogFragment.BUNDLE_KEY_MINUTES);

                Alarm selectedAlarm = mAlarmTimeClickHandler.getSelectedAlarm();
                if (selectedAlarm != null) {
                    setSmoothScrollStableId(selectedAlarm.id);
                }

                mAlarmTimeClickHandler.setAlarmWithDelay(hours, minutes);
            });
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
        if (mBinding == null || mIsReordering) {
            return;
        }

        if (updateToken < mCurrentUpdateToken) {
            LogUtils.v("Ignoring adapter update: %d < %d", updateToken, mCurrentUpdateToken);
            return;
        }

        if (Objects.requireNonNull(mBinding.alarmRecyclerView.getItemAnimator()).isRunning()) {
            // RecyclerView is currently animating -> defer update.
            mBinding.alarmRecyclerView.getItemAnimator().isRunning(() -> setAdapterItems(items, updateToken));
        } else if (mBinding.alarmRecyclerView.isComputingLayout()) {
            // RecyclerView is currently computing a layout -> defer update.
            mBinding.alarmRecyclerView.post(() -> {
                if (mBinding != null) {
                    setAdapterItems(items, updateToken);
                }
            });
        } else {
            mCurrentUpdateToken = updateToken;

            mItemAdapter.setItems(items);

            // Show or hide the empty view as appropriate.
            final boolean noAlarms = items.isEmpty();
            boolean hasActiveAlarms = false;

            for (AlarmItemHolder holder : items) {
                if (holder.item.enabled) {
                    hasActiveAlarms = true;
                    break;
                }
            }

            updateUIStatesAndAnimate(noAlarms, hasActiveAlarms);

            // Scroll to the selected alarm.
            if (mScrollToAlarmId != Alarm.INVALID_ID) {
                scrollToAlarm(mScrollToAlarmId);
                setSmoothScrollStableId(Alarm.INVALID_ID);
            }

            // Check if a newly created alarm is waiting to be edited.
            if (mPendingAlarmToEdit != null) {
                final Alarm alarmToOpen = mPendingAlarmToEdit;
                mPendingAlarmToEdit = null;

                mBinding.alarmRecyclerView.post(() -> {
                    if (mBinding != null) {
                        openBottomSheetWhenIdle(alarmToOpen);
                    }
                });
            }
        }
    }

    /**
     * @param alarmId identifies the alarm to be displayed
     */
    private void scrollToAlarm(long alarmId) {
        final int alarmCount = mItemAdapter.getItemCount();
        int alarmPosition = RecyclerView.NO_POSITION;
        for (int i = 0; i < alarmCount; i++) {
            long id = mItemAdapter.getItemId(i);
            if (id == alarmId) {
                alarmPosition = i;
                break;
            }
        }

        if (alarmPosition != RecyclerView.NO_POSITION) {
            final int finalPosition = alarmPosition;
            mBinding.alarmRecyclerView.post(() -> {
                if (mBinding != null) {
                    smoothScrollTo(finalPosition);
                }
            });
        } else {
            // Trying to display a deleted alarm should only happen from a missed notification for
            // an alarm that has been marked deleted after use.
            SnackbarManager.show(Snackbar.make(mBinding.alarmRootView, R.string.missed_alarm_has_been_deleted, Snackbar.LENGTH_LONG));
        }
    }

    private void hideSideButtonsWithFabAnimation() {
        if (mSideButtonsVisible) {
            mSideButtonsVisible = false;
            updateFab(FAB_AND_BUTTONS_SHRINK_AND_EXPAND);
        }
    }

    public void hideSideButtonsOnlyAnimated() {
        if (mSideButtonsVisible) {
            mSideButtonsVisible = false;
            updateFab(BUTTONS_SHRINK_AND_EXPAND);
        }
    }

    public void resetFabAndButtonsState() {
        if (mSideButtonsVisible) {
            mSideButtonsVisible = false;
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

    /**
     * Safely initiates the process to open the BottomSheet for a newly created alarm.
     *
     * <p>It handles potential race conditions between the database Loader and the async callback.</p>
     */
    public void setPendingAlarmToEdit(Alarm newAlarm) {
        // Check if the database Loader has already updated the adapter before this callback.
        boolean alreadyInList = false;
        for (int i = 0; i < mItemAdapter.getItemCount(); i++) {
            if (mItemAdapter.getItemId(i) == newAlarm.id) {
                alreadyInList = true;
                break;
            }
        }

        if (alreadyInList) {
            // The Loader was faster than the callback; the list is already up-to-date.
            // We can trigger the scroll and wait for the animation to open the BottomSheet.
            scrollToAlarm(newAlarm.id);
            setSmoothScrollStableId(Alarm.INVALID_ID);

            mBinding.alarmRecyclerView.post(() -> {
                if (mBinding != null) {
                    openBottomSheetWhenIdle(newAlarm);
                }
            });
        } else {
            // The callback was faster than the Loader; 'setAdapterItems' will handle it once the Loader finishes.
            mPendingAlarmToEdit = newAlarm;
            setSmoothScrollStableId(newAlarm.id);
        }
    }

    /**
     * Waits for the RecyclerView's smooth scrolling to finish before proceeding.
     */
    private void openBottomSheetWhenIdle(Alarm alarm) {
        // Check if the list scrolls to the new alarm.
        if (mBinding.alarmRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            mBinding.alarmRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        recyclerView.removeOnScrollListener(this);
                        checkAnimatorAndOpen(alarm);
                    }
                }
            });
        } else {
            // No scrolling in progress, proceed directly.
            checkAnimatorAndOpen(alarm);
        }
    }

    /**
     * Waits for both global UI transitions (EmptyView) and RecyclerView item animations to finish.
     */
    private void checkAnimatorAndOpen(Alarm alarm) {
        // Check if a global layout transition (like Fade IN/OUT of the empty view) is running.
        if (mIsUiTransitioning) {
            // Check again on the next rendering frame.
            mBinding.alarmRecyclerView.postOnAnimation(() -> checkAnimatorAndOpen(alarm));
            return;
        }

        // Check if a RecyclerView-specific item animation (insertion/expansion) is running.
        RecyclerView.ItemAnimator animator = mBinding.alarmRecyclerView.getItemAnimator();
        if (animator != null && animator.isRunning()) {
            animator.isRunning(() -> displayBottomSheet(alarm));
        } else {
            // The list is perfectly static and fully rendered.
            displayBottomSheet(alarm);
        }
    }

    /**
     * Final trigger to safely display the BottomSheet.
     */
    private void displayBottomSheet(Alarm alarm) {
        if (isAdded()) {
            mAlarmTimeClickHandler.displayBottomSheetDialog(alarm, true);
        }
    }

    public void removeItem(AlarmItemHolder itemHolder) {
        mItemAdapter.removeItem(itemHolder);
    }

    private void saveManualSortOrder() {
        List<AlarmItemHolder> currentItems = mItemAdapter.getItems();

        Context context = getContext();
        if (context == null) {
            return;
        }

        AppExecutors.getDiskIO().execute(() -> {
            ContentResolver cr = context.getContentResolver();

            for (int i = 0; i < currentItems.size(); i++) {
                Alarm alarm = currentItems.get(i).item;

                if (alarm.manualSortOrder != i) {
                    alarm.manualSortOrder = i;

                    alarm.updateAlarm(cr);
                }
            }

            AppExecutors.getMainThread().post(() -> {
                mIsReordering = false;

                if (mCursorLoader != null) {
                    mCursorLoader.forceLoad();
                }
            });
        });
    }

    /**
     * Handles the display and animation of the volume banner and the empty view, ensuring there are no visual conflicts.
     */
    private void updateUIStatesAndAnimate(boolean noAlarms, boolean hasActiveAlarms) {
        boolean shouldShowBanner = mIsLowAlarmVolumeWarningEnabled && hasActiveAlarms && RingtoneUtils.isAlarmStreamLow(requireContext());
        int targetVisibility = shouldShowBanner ? VISIBLE : GONE;
        boolean bannerWillChange = mBinding.alarmVolumeWarningBanner.volumeWarningBanner.getVisibility() != targetVisibility;

        // Check if the empty view state is about to toggle.
        boolean emptyStateWillChange = mEmptyViewController.isEmpty() != noAlarms;

        Transition transitionToTrack = null;

        if (bannerWillChange) {
            TransitionSet combinedTransition = new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(mEmptyViewController.getTransition())
                .addTransition(new ChangeBounds())
                .addTransition(new Fade().addTarget(mBinding.alarmVolumeWarningBanner.volumeWarningBanner));

            TransitionManager.beginDelayedTransition(mBinding.alarmRootView, combinedTransition);

            mBinding.alarmVolumeWarningBanner.volumeWarningBanner.setVisibility(targetVisibility);
            mEmptyViewController.setEmpty(noAlarms, false);

            transitionToTrack = combinedTransition;
        } else if (emptyStateWillChange) {
            // The state will change, the EmptyViewController will trigger its own delayed transition.
            transitionToTrack = mEmptyViewController.getTransition();
            mEmptyViewController.setEmpty(noAlarms, true);
        } else {
            mEmptyViewController.setEmpty(noAlarms, false);
        }

        // If a transition was triggered, attach a listener to block the BottomSheet until it finishes.
        if (transitionToTrack != null) {
            mIsUiTransitioning = true;
            transitionToTrack.addListener(new Transition.TransitionListener() {
                @Override public void onTransitionStart(@NonNull Transition transition) {}
                @Override public void onTransitionCancel(@NonNull Transition transition) {}
                @Override public void onTransitionPause(@NonNull Transition transition) {}
                @Override public void onTransitionResume(@NonNull Transition transition) {}

                @Override
                public void onTransitionEnd(@NonNull Transition transition) {
                    // Remove listener to prevent memory leaks and clear the flag.
                    transition.removeListener(this);
                    mIsUiTransitioning = false;
                }
            });
        }
    }

    /**
     * Updates the visibility of the volume warning banner.
     */
    public void updateWarningBannerVisibility() {
        if (!isAdded() || mBinding == null) {
            return;
        }

        Context context = getContext();
        if (context == null) {
            return;
        }

        Fragment bottomSheet = getParentFragmentManager().findFragmentByTag(AlarmEditBottomSheetFragment.TAG);

        boolean isCustomAlarmVolumePlaying = false;
        if (SettingsDAO.isPerAlarmVolumeEnabled(mPrefs) && bottomSheet != null && bottomSheet.isAdded()) {
            Fragment volumeDialog = bottomSheet.getChildFragmentManager().findFragmentByTag(AlarmVolumeDialogFragment.TAG);
            isCustomAlarmVolumePlaying = volumeDialog != null && volumeDialog.isAdded();
        }

        if (isCustomAlarmVolumePlaying) {
            return;
        }

        if (!mIsLowAlarmVolumeWarningEnabled || !RingtoneUtils.isAlarmStreamLow(context)) {
            if (mBinding.alarmVolumeWarningBanner.volumeWarningBanner.getVisibility() != GONE) {
                TransitionSet strictTransition = new TransitionSet()
                    .setOrdering(TransitionSet.ORDERING_TOGETHER)
                    .addTransition(new ChangeBounds())
                    .addTransition(new Fade(Fade.OUT).addTarget(mBinding.alarmVolumeWarningBanner.volumeWarningBanner));

                TransitionManager.beginDelayedTransition(mBinding.alarmRootView, strictTransition);

                mBinding.alarmVolumeWarningBanner.volumeWarningBanner.setVisibility(GONE);
            }

            return;
        }

        AppExecutors.getDiskIO().execute(() -> {
            List<Alarm> activeAlarms = Alarm.getEnabledAlarms(context);
            boolean shouldShow = !activeAlarms.isEmpty();

            AppExecutors.getMainThread().post(() -> {
                if (mBinding != null) {
                    mBinding.alarmVolumeWarningBanner.volumeWarningBanner.setVisibility(shouldShow ? VISIBLE : GONE);
                }
            });
        });
    }

    /**
     * A custom {@link RecyclerView.ItemDecoration} that applies consistent and even spacing
     * between items in a grid layout for tablets.
     *
     * <p>It dynamically calculates the item offsets to ensure that both the internal
     * spacing between columns/rows and the outer edges of the grid are perfectly aligned.
     * This decoration only affects the layout if the {@link RecyclerView} uses a
     * {@link GridLayoutManager}.</p>
     */
    private static class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private final int margin;
        private final int bottomMargin;
        private final boolean mIsRTL;

        public GridSpacingItemDecoration(Context context, DisplayMetrics displayMetrics) {
            this.margin = (int) dpToPx(10, displayMetrics);
            this.bottomMargin = (int) dpToPx(2, displayMetrics);
            this.mIsRTL = ThemeUtils.isRTL(context);
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {

            int position = parent.getChildAdapterPosition(view);

            if (position == RecyclerView.NO_POSITION) {
                position = parent.getChildLayoutPosition(view);
            }

            if (position < 0) {
                return;
            }

            RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager gridLayoutManager) {
                int spanCount = gridLayoutManager.getSpanCount();
                int column = position % spanCount;

                // Formula for having, for example, 10dp on the outside and 5dp on the inside
                int standardLeft = margin - column * margin / spanCount;
                int standardRight = (column + 1) * margin / spanCount;

                outRect.left = mIsRTL ? standardRight : standardLeft;
                outRect.right = mIsRTL ? standardLeft : standardRight;
                outRect.bottom = margin;
            } else {
                outRect.left = margin;
                outRect.right = margin;
                outRect.bottom = bottomMargin;
            }
        }
    }

    /**
     * This runnable executes at midnight and refreshes the display of all alarms. Collapsed alarms
     * that do no repeat will have their "Tomorrow" strings updated to say "Today".
     */
    private final class MidnightRunnable implements Runnable {
        @Override
        public void run() {
            int itemCount = mItemAdapter.getItemCount();
            if (itemCount > 0) {
                mItemAdapter.notifyItemRangeChanged(0, itemCount);
            }
        }
    }
}
