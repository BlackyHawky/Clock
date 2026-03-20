/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SORT_TIMER_MANUALLY;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerListener;
import com.best.deskclock.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This adapter produces a {@link TimerViewHolder} for each timer.
 */
public class TimerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements TimerListener {

    public static final int SINGLE_TIMER = 0;
    public static final int MULTIPLE_TIMERS = 1;
    public static final int MULTIPLE_TIMERS_COMPACT = 2;

    private static final String PAYLOAD_UPDATE_BACKGROUND = "PAYLOAD_UPDATE_BACKGROUND";
    private static final String PAYLOAD_UPDATE_STATE = "PAYLOAD_UPDATE_STATE";

    private List<Timer> mCachedTimers = new ArrayList<>();
    private final TimerClickHandler mTimerClickHandler;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private RecyclerView mRecyclerView;

    public TimerAdapter(Context context, SharedPreferences sharedPreferences, TimerClickHandler timerClickHandler) {
        mContext = context;
        mPrefs = sharedPreferences;
        mTimerClickHandler = timerClickHandler;

        loadTimerList();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mRecyclerView = null;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder itemViewHolder) {
        super.onViewAttachedToWindow(itemViewHolder);
        TimerViewHolder holder = (TimerViewHolder) itemViewHolder;
        Timer timer = holder.getTimer();

        if (timer != null) {
            if (holder.mTimerItemCompact != null) {
                holder.mTimerItemCompact.updateTimeDisplay(timer);
            } else if (holder.mTimerItem != null) {
                holder.mTimerItem.updateTimeDisplay(timer);
            }

            if (!timer.isReset()) {
                holder.startUpdating();
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder itemViewHolder) {
        super.onViewDetachedFromWindow(itemViewHolder);
        TimerViewHolder holder = (TimerViewHolder) itemViewHolder;

        holder.stopUpdating();
    }

    @Override
    public int getItemCount() {
        return getTimers().size();
    }

    @Override
    public int getItemViewType(int position) {
        boolean isPortrait = ThemeUtils.isPortrait();

        if (getTimers().size() == 1) {
            return (ThemeUtils.isTablet() || isPortrait) ? SINGLE_TIMER : MULTIPLE_TIMERS;
        } else {
            if (isPortrait && SettingsDAO.isCompactTimersDisplayed(mPrefs)) {
                return MULTIPLE_TIMERS_COMPACT;
            } else {
                return MULTIPLE_TIMERS;
            }
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view;
        if (viewType == SINGLE_TIMER) {
            view = inflater.inflate(R.layout.timer_single_item, parent, false);
        } else if (viewType == MULTIPLE_TIMERS) {
            view = inflater.inflate(R.layout.timer_item, parent, false);
        } else {
            view = inflater.inflate(R.layout.timer_item_compact, parent, false);
        }

        return new TimerViewHolder(view, mTimerClickHandler, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder itemViewHolder, int position) {
        TimerViewHolder holder = (TimerViewHolder) itemViewHolder;
        Timer timer = getTimer(position);

        holder.onBind(timer.getId());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder itemViewHolder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            TimerViewHolder holder = (TimerViewHolder) itemViewHolder;

            if (payloads.contains(PAYLOAD_UPDATE_BACKGROUND)) {
                holder.updateBackground();
            }

            if (payloads.contains(PAYLOAD_UPDATE_STATE)) {
                Timer timer = getTimer(position);
                holder.onBind(timer.getId());
            }
        } else {
            super.onBindViewHolder(itemViewHolder, position, payloads);
        }
    }

    @Override
    public void timerAdded(Timer timer) {
        refreshTimersCache();
        saveTimerList();
        notifyItemInserted(getTimers().indexOf(timer));
        updateTime();
    }

    @Override
    public void timerRemoved(Timer timer) {
        int positionToRemove = getTimerPosition(timer.getId());

        refreshTimersCache();
        saveTimerList();

        if (positionToRemove != RecyclerView.NO_POSITION) {
            notifyItemRemoved(positionToRemove);

            if (getItemCount() == 1) {
                // Use notifyDataSetChanged() to avoid flickering when switching
                // from two timers to a single timer.
                notifyDataSetChanged();
            }
        } else {
            // Fallback
            notifyDataSetChanged();
        }
    }

    @Override
    public void timerUpdated(Timer before, Timer after) {
        int oldPosition = getTimerPosition(before.getId());

        refreshTimersCache();

        int newPosition = getTimerPosition(after.getId());

        if (oldPosition != RecyclerView.NO_POSITION && newPosition != RecyclerView.NO_POSITION) {
            if (oldPosition != newPosition) {
                // Dynamic sorting: Use notifyItemMoved() to automatically animate the timer to its
                // new position when its state changes (unlike swapTimers which handles manual drag-and-drop).
                notifyItemMoved(oldPosition, newPosition);

                int startPosition = Math.min(oldPosition, newPosition);
                int itemCount = Math.abs(oldPosition - newPosition) + 1;

                // Update timer backgrounds.
                notifyItemRangeChanged(startPosition, itemCount, PAYLOAD_UPDATE_BACKGROUND);

                // Update the Play/Pause button icon.
                notifyItemChanged(newPosition, PAYLOAD_UPDATE_STATE);
            }

            boolean isTablet = ThemeUtils.isTablet();
            // Returns true if the circle or the progress bar is displayed
            // (on tablets, the circle is always displayed).
            boolean isExpanding = !isTablet && before.isReset() && !after.isReset();
            // Returns true if the circle or the progress bar is gone.
            // (on tablets, the circle is always displayed).
            boolean isShrinking = !isTablet && !before.isReset() && after.isReset();

            if (isExpanding) {
                // Expanding: Use notifyDataSetChanged() to avoid overlapping with other timers when
                // the circle or progress bar appears.
                notifyDataSetChanged();
            } else if (oldPosition == newPosition) {
                // Static position: Use notifyItemChanged() to perform a lightweight, targeted update
                // when the timer shrinks or simply ticks without changing its position in the list.
                notifyItemChanged(newPosition, null);
            } else if (isShrinking) {
                // Shrinking and moving: Use notifyDataSetChanged() to avoid visual glitches when
                // the circle or progress bar disappears.
                notifyDataSetChanged();
            }
        } else {
            // Fallback
            notifyDataSetChanged();
        }

        updateTime();
    }

    private int getTimerPosition(int timerId) {
        for (int i = 0; i < mCachedTimers.size(); i++) {
            if (mCachedTimers.get(i).getId() == timerId) {
                return i;
            }
        }

        return RecyclerView.NO_POSITION;
    }

    /**
     * Iterates through all active {@link TimerViewHolder} instances and updates their state.
     * <p>
     * This method ensures that each timer view updates at the appropriate interval
     * based on its current state (e.g., paused or running).</p>
     */
    void updateTime() {
        if (mRecyclerView == null) {
            return;
        }

        for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
            View child = mRecyclerView.getChildAt(i);
            TimerViewHolder holder = (TimerViewHolder) mRecyclerView.getChildViewHolder(child);

            Timer timer = holder.getTimer();
            if (timer != null && !timer.isReset()) {
                holder.startUpdating();
            } else {
                holder.stopUpdating();
            }
        }
    }

    /**
     * Stops the update cycle for all active {@link TimerViewHolder} instances.
     * <p>
     * It should be called when the timer list is no longer visible or when the fragment
     * is paused to prevent unnecessary background updates and potential memory leaks.
     */
    public void stopAllUpdating() {
        if (mRecyclerView == null) {
            return;
        }

        for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
            View child = mRecyclerView.getChildAt(i);
            TimerViewHolder holder = (TimerViewHolder) mRecyclerView.getChildViewHolder(child);
            holder.stopUpdating();
        }
    }

    Timer getTimer(int index) {
        return getTimers().get(index);
    }

    public List<Timer> getTimers() {
        return mCachedTimers;
    }

    public void refreshTimersCache() {
        List<Timer> sourceTimers = new ArrayList<>(DataModel.getDataModel().getTimers());
        String timerSortingPreference = SettingsDAO.getTimerSortingPreference(mPrefs);

        if (!timerSortingPreference.equals(DEFAULT_SORT_TIMER_MANUALLY)) {
            Collections.sort(sourceTimers, Timer.createTimerStateComparator(mContext));
            mCachedTimers = sourceTimers;
        } else {
            String savedOrder = mPrefs.getString("timerOrder", null);

            if (savedOrder != null) {
                String[] timerIds = savedOrder.split(",");
                List<Timer> orderedList = new ArrayList<>();

                for (String id : timerIds) {
                    int timerId = Integer.parseInt(id);
                    for (Timer timer : sourceTimers) {
                        if (timer.getId() == timerId) {
                            orderedList.add(timer);
                            break;
                        }
                    }
                }

                for (Timer timer : sourceTimers) {
                    if (!orderedList.contains(timer)) {
                        orderedList.add(0, timer);
                    }
                }

                mCachedTimers = orderedList;
            } else {
                mCachedTimers = sourceTimers;
            }
        }
    }

    public void saveTimerList() {
        SharedPreferences.Editor editor = mPrefs.edit();

        if (getTimers().isEmpty()) {
            editor.remove("timerOrder");
        } else {
            // Convert list of IDs to string
            StringBuilder sb = new StringBuilder();
            for (Timer timer : getTimers()) {
                sb.append(timer.getId()).append(",");
            }

            // Delete the last comma
            if (!TextUtils.isEmpty(sb)) {
                sb.setLength(sb.length() - 1);
            }

            editor.putString("timerOrder", sb.toString());
        }

        editor.apply();
    }

    public void loadTimerList() {
        refreshTimersCache();
        notifyDataSetChanged();
    }

    public void swapTimers(int fromPosition, int toPosition) {
        Collections.swap(mCachedTimers, fromPosition, toPosition);
        Collections.swap(DataModel.getDataModel().getTimers(), fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    /**
     * Custom ItemTouchHelper.Callback for managing drag & drop of Timer items in a RecyclerView.
     *
     * <p>This implementation allows manual reordering of timers via drag gestures,
     * but disables dragging when the user initiates a touch event on the {@code + 1:00} button.
     *
     * <p>Drag directions are enabled or disabled based on user preferences and device orientation,
     * ensuring consistent UX whether on tablets, phones, portrait or landscape modes.</p>
     */
    public static class TimerItemTouchHelper extends ItemTouchHelper.Callback {

        private final TimerAdapter mAdapter;

        private boolean isTouchOnDragBlockingView = false;

        public TimerItemTouchHelper(TimerAdapter adapter, RecyclerView recyclerView) {
            mAdapter = adapter;

            // Prevent the timer from dragging if the "Add a minute" button is long-pressed
            recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
                @Override
                public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                    if (e.getAction() == MotionEvent.ACTION_DOWN) {
                        View child = rv.findChildViewUnder(e.getX(), e.getY());
                        if (child != null) {
                            View addTimeButton = child.findViewById(R.id.timer_add_time_button);
                            if (addTimeButton != null && addTimeButton.getVisibility() == View.VISIBLE) {
                                int[] loc = new int[2];
                                addTimeButton.getLocationOnScreen(loc);
                                float x = e.getRawX();
                                float y = e.getRawY();
                                isTouchOnDragBlockingView = x >= loc[0] && x <= loc[0] + addTimeButton.getWidth()
                                    && y >= loc[1] && y <= loc[1] + addTimeButton.getHeight();
                            } else {
                                isTouchOnDragBlockingView = false;
                            }
                        } else {
                            isTouchOnDragBlockingView = false;
                        }
                    }

                    return false;
                }

                @Override
                public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                }

                @Override
                public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                }
            });
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {

            int fromPosition = viewHolder.getBindingAdapterPosition();
            int toPosition = target.getBindingAdapterPosition();

            mAdapter.swapTimers(fromPosition, toPosition);

            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (isTouchOnDragBlockingView) {
                return 0;
            }

            final int dragFlags;
            String timerSortingPreference = SettingsDAO.getTimerSortingPreference(mAdapter.mPrefs);

            // Allow dragging only if timers are sorted manually
            if (timerSortingPreference.equals(DEFAULT_SORT_TIMER_MANUALLY)) {
                if (ThemeUtils.isTablet()) {
                    dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END;
                } else {
                    if (ThemeUtils.isLandscape()) {
                        dragFlags = ItemTouchHelper.START | ItemTouchHelper.END;
                    } else {
                        dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                    }
                }
            } else {
                dragFlags = 0;
            }

            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);

            // Draw a shadow under the timer card when it's dragging.
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                viewHolder.itemView.setTranslationZ(dpToPx(6, viewHolder.itemView.getContext().getResources().getDisplayMetrics()));
            }
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            // Remove the shadow under the city card when the drag is complete.
            viewHolder.itemView.setTranslationZ(0f);

            // Save the list of timers once the user interaction is complete.
            mAdapter.saveTimerList();

            recyclerView.post(() -> {
                // Notifies the adapter that all items may have changed positions,
                // which will force the system to call onBind() for each visible timer.
                mAdapter.notifyItemRangeChanged(0, mAdapter.getItemCount());
            });
        }
    }

}
