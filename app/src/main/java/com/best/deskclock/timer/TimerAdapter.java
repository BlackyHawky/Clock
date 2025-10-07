/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_SORT_TIMER_MANUALLY;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.Timer;
import com.best.deskclock.data.TimerListener;
import com.best.deskclock.R;
import com.best.deskclock.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This adapter produces a {@link TimerViewHolder} for each timer.
 */
public class TimerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements TimerListener {

    private final int SINGLE_TIMER = R.layout.timer_single_item;
    private final int MULTIPLE_TIMERS = R.layout.timer_item;

    /** Maps each timer id to the corresponding {@link TimerViewHolder} that draws it. */
    private final Map<Integer, TimerViewHolder> mHolders = new ArrayMap<>();
    private final TimerClickHandler mTimerClickHandler;
    private final Context mContext;
    private final SharedPreferences mPrefs;

    public TimerAdapter(Context context, SharedPreferences sharedPreferences, TimerClickHandler timerClickHandler) {
        mContext = context;
        mPrefs = sharedPreferences;
        mTimerClickHandler = timerClickHandler;
    }

    @Override
    public int getItemCount() {
        return getTimers().size();
    }

    @Override
    public int getItemViewType(int position) {
        if (getTimers().size() == 1) {
            return (ThemeUtils.isTablet() || ThemeUtils.isPortrait()) ? SINGLE_TIMER : MULTIPLE_TIMERS;
        } else {
            return MULTIPLE_TIMERS;
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
        } else {
            view = inflater.inflate(R.layout.timer_item, parent, false);
        }
        return new TimerViewHolder(view, mTimerClickHandler);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder itemViewHolder, int position) {
        TimerViewHolder holder = (TimerViewHolder) itemViewHolder;
        Timer timer = getTimer(position);
        mHolders.put(timer.getId(), holder);
        holder.onBind(timer.getId());

        if (!timer.isReset()) {
            holder.startUpdating();
        } else {
            holder.stopUpdating();
        }
    }

    @Override
    public void timerAdded(Timer timer) {
        saveTimerList();
        notifyDataSetChanged();
        updateTime();
    }

    @Override
    public void timerRemoved(Timer timer) {
        TimerViewHolder holder = mHolders.get(timer.getId());
        if (holder != null) {
            holder.stopUpdating();
        }
        mHolders.remove(timer.getId());
        saveTimerList();
        notifyDataSetChanged();
    }

    @Override
    public void timerUpdated(Timer before, Timer after) {
        updateTime();
        notifyDataSetChanged();
    }

    /**
     * Iterates through all active {@link TimerViewHolder} instances and updates their state.
     * <p>
     * This method ensures that each timer view updates at the appropriate interval
     * based on its current state (e.g., paused or running).</p>
     */
    void updateTime() {
        for (TimerViewHolder holder : mHolders.values()) {
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
        for (TimerViewHolder holder : mHolders.values()) {
            holder.stopUpdating();
        }
    }

    Timer getTimer(int index) {
        return getTimers().get(index);
    }

    public List<Timer> getTimers() {
        List<Timer> timers = DataModel.getDataModel().getTimers();
        String timerSortingPreference = SettingsDAO.getTimerSortingPreference(mPrefs);
        if (!timerSortingPreference.equals(DEFAULT_SORT_TIMER_MANUALLY)) {
            Collections.sort(timers, Timer.createTimerStateComparator(mContext));
        }
        return timers;
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
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
            }

            editor.putString("timerOrder", sb.toString());
        }

        editor.apply();
    }

    public void loadTimerList() {
        String savedOrder = mPrefs.getString("timerOrder", null);

        if (savedOrder != null) {
            String[] timerIds = savedOrder.split(",");

            List<Timer> tempList = new ArrayList<>(getTimers());
            getTimers().clear();

            // Fill mTimers according to the saved order
            for (String id : timerIds) {
                int timerId = Integer.parseInt(id);
                for (Timer timer : tempList) {
                    if (timer.getId() == timerId) {
                        getTimers().add(timer);
                        break;
                    }
                }
            }

            // Notify adapter
            notifyDataSetChanged();
        }
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

            Collections.swap(DataModel.getDataModel().getTimers(), fromPosition, toPosition);
            Objects.requireNonNull(recyclerView.getAdapter()).notifyItemMoved(fromPosition, toPosition);

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
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            // Draw a shadow under the timer card when it's dragging.
            viewHolder.itemView.setTranslationZ(
                    (float) ThemeUtils.convertDpToPixels(isCurrentlyActive ? 6 : 0, recyclerView.getContext()));
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            // Save the list of timers once the user interaction is complete.
            mAdapter.saveTimerList();
        }
    }

}
