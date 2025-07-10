/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
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
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.alarms.AlarmTimeClickHandler;
import com.best.deskclock.alarms.AlarmUpdateHandler;
import com.best.deskclock.alarms.CustomSpinnerTimePickerDialog;
import com.best.deskclock.alarms.MaterialTimePickerDialog;
import com.best.deskclock.alarms.OnTimeSetListener;
import com.best.deskclock.alarms.ScrollHandler;
import com.best.deskclock.alarms.dataadapter.AlarmItemHolder;
import com.best.deskclock.alarms.dataadapter.AlarmItemViewHolder;
import com.best.deskclock.alarms.dataadapter.CollapsedAlarmViewHolder;
import com.best.deskclock.alarms.dataadapter.ExpandedAlarmViewHolder;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.widget.EmptyViewController;
import com.best.deskclock.widget.toast.SnackbarManager;
import com.best.deskclock.widget.toast.ToastManager;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

/**
 * A fragment that displays a list of alarm time and allows interaction with them.
 */
public final class AlarmClockFragment extends DeskClockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, ScrollHandler, OnTimeSetListener {

    private static final String TAG = "AlarmClockFragment";

    // This extra is used when receiving an intent to create an alarm, but no alarm details
    // have been passed in, so the alarm page should start the process of creating a new alarm.
    public static final String ALARM_CREATE_NEW_INTENT_EXTRA = "deskclock.create.new";

    // This extra is used when receiving an intent to scroll to specific alarm. If alarm
    // can not be found, and toast message will pop up that the alarm has be deleted.
    public static final String SCROLL_TO_ALARM_INTENT_EXTRA = "deskclock.scroll.to.alarm";

    private static final String KEY_EXPANDED_ID = "expandedId";

    private Context mContext;
    private SharedPreferences mPrefs;

    // Updates "Today/Tomorrow" in the UI when midnight passes.
    private final Runnable mMidnightUpdater = new MidnightRunnable();

    // Views
    private ViewGroup mMainLayout;
    private AlarmRecyclerView mRecyclerView;

    // Data
    private Loader<?> mCursorLoader;
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

        mPrefs = getDefaultSharedPreferences(requireContext());

        mCursorLoader = LoaderManager.getInstance(this).initLoader(0, null, this);
        if (savedState != null) {
            mExpandedAlarmId = savedState.getLong(KEY_EXPANDED_ID, Alarm.INVALID_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.alarm_clock, container, false);
        mContext = requireContext();
        mMainLayout = v.findViewById(R.id.main);
        mRecyclerView = v.findViewById(R.id.alarms_recycler_view);
        TextView alarmsEmptyView = v.findViewById(R.id.alarms_empty_view);
        final boolean isTablet = ThemeUtils.isTablet();
        final boolean isPhoneInLandscapeMode = !isTablet && ThemeUtils.isLandscape();
        final Drawable noAlarmsIcon = ThemeUtils.toScaledBitmapDrawable(mContext, R.drawable.ic_alarm_off, 2.5f);
        if (noAlarmsIcon != null) {
            noAlarmsIcon.setTint(MaterialColors.getColor(
                    mContext, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.BLACK));
        }
        alarmsEmptyView.setCompoundDrawablesWithIntrinsicBounds(null, noAlarmsIcon, null, null);
        alarmsEmptyView.setCompoundDrawablePadding(ThemeUtils.convertDpToPixels(30, mContext));
        // Set a bottom padding for phones in portrait mode and tablets to center correctly
        // the alarms empty view between the FAB and the top of the screen
        if (!isPhoneInLandscapeMode) {
            alarmsEmptyView.setPadding(0, 0, 0, ThemeUtils.convertDpToPixels(80, mContext));
        }
        mEmptyViewController = new EmptyViewController(mMainLayout, mRecyclerView, alarmsEmptyView);
        mAlarmUpdateHandler = new AlarmUpdateHandler(mContext, this, mMainLayout);
        mAlarmTimeClickHandler = new AlarmTimeClickHandler(this, savedState, mAlarmUpdateHandler);
        mItemAdapter = new ItemAdapter<>();
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

        mRecyclerView.setLayoutManager(mLayoutManager);
        // Due to the ViewPager and the location of FAB, set a bottom padding and/or a right padding
        // to prevent the alarm list from being hidden by the FAB (e.g. when scrolling down).
        final int rightPadding = ThemeUtils.convertDpToPixels(isPhoneInLandscapeMode ? 85 : 0, mContext);
        final int bottomPadding = ThemeUtils.convertDpToPixels(isTablet ? 110 : isPhoneInLandscapeMode ? 5 : 95, mContext);
        mRecyclerView.setPadding(0, 0, rightPadding, bottomPadding);

        mItemAdapter.setHasStableIds();
        mItemAdapter.withViewTypes(new CollapsedAlarmViewHolder.Factory(inflater),
                null, null, CollapsedAlarmViewHolder.VIEW_TYPE);
        mItemAdapter.withViewTypes(new ExpandedAlarmViewHolder.Factory(mContext),
                null, null, ExpandedAlarmViewHolder.VIEW_TYPE);
        mItemAdapter.setOnItemChangedListener(holder -> {
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

        final ScrollPositionWatcher scrollPositionWatcher = new ScrollPositionWatcher();
        mRecyclerView.addOnLayoutChangeListener(scrollPositionWatcher);
        mRecyclerView.addOnScrollListener(scrollPositionWatcher);
        mRecyclerView.setAdapter(mItemAdapter);

        final ItemAnimator itemAnimator = new ItemAnimator();
        itemAnimator.setChangeDuration(300L);
        itemAnimator.setMoveDuration(300L);
        mRecyclerView.setItemAnimator(itemAnimator);

        if (!ThemeUtils.areSystemAnimationsDisabled(requireContext())) {
            new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView,
                                      @NonNull RecyclerView.ViewHolder viewHolder,
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
            }).attachToRecyclerView(mRecyclerView);
        }

        return v;
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
        for (data.moveToFirst(); !data.isAfterLast(); data.moveToNext()) {
            final Alarm alarm = new Alarm(data);
            final AlarmInstance alarmInstance = alarm.canPreemptivelyDismiss()
                    ? new AlarmInstance(data, true)
                    : null;
            final AlarmItemHolder itemHolder = new AlarmItemHolder(alarm, alarmInstance, mAlarmTimeClickHandler);
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

        if (Objects.requireNonNull(mRecyclerView.getItemAnimator()).isRunning()) {
            // RecyclerView is currently animating -> defer update.
            mRecyclerView.getItemAnimator().isRunning(() -> setAdapterItems(items, updateToken));
        } else if (mRecyclerView.isComputingLayout()) {
            // RecyclerView is currently computing a layout -> defer update.
            mRecyclerView.post(() -> setAdapterItems(items, updateToken));
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
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
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
        fab.setImageResource(R.drawable.ic_add);
        fab.setContentDescription(fab.getResources().getString(R.string.button_alarms));
    }

    @Override
    public void onUpdateFabButtons(@NonNull ImageView left, @NonNull ImageView right) {
        left.setVisibility(View.INVISIBLE);
        right.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onTimeSet(int hour, int minute) {
        mAlarmTimeClickHandler.onTimeSet(hour, minute);
    }

    public void startCreatingAlarm() {
        // Clear the currently selected alarm.
        mAlarmTimeClickHandler.setSelectedAlarm(null);
        if (SettingsDAO.getMaterialTimePickerStyle(mPrefs).equals(SPINNER_TIME_PICKER_STYLE)) {
            showCustomSpinnerTimePicker();
        } else {
            showMaterialTimePicker();
        }
    }

    private void showCustomSpinnerTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        CustomSpinnerTimePickerDialog.show(mContext, this, currentHour, currentMinute, this);
    }

    private void showMaterialTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int currentMinute = calendar.get(Calendar.MINUTE);

        MaterialTimePickerDialog.show(mContext, ((AppCompatActivity) mContext).getSupportFragmentManager(),
                TAG, currentHour, currentMinute, mPrefs, this);
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

    public void setCrescendoDuration(Alarm alarm, int crescendoDuration) {
        alarm.crescendoDuration = crescendoDuration;
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
    }

    public void setAlarmVolume(Alarm alarm, int alarmVolume) {
        alarm.alarmVolume = alarmVolume;
        mAlarmUpdateHandler.asyncUpdateAlarm(alarm, false, true);
    }

    /**
     * Updates the vertical scroll state of this tab in the {@link UiDataModel} as the user scrolls
     * the recyclerview or when the size/position of elements within the recyclerview changes.
     */
    private final class ScrollPositionWatcher extends RecyclerView.OnScrollListener implements View.OnLayoutChangeListener {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
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
