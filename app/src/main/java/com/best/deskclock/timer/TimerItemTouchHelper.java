// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.uicomponents.ItemTouchHelperContract;

/**
 * Custom {@link androidx.recyclerview.widget.ItemTouchHelper.Callback} for managing drag & drop of timer items in a RecyclerView.
 *
 * <p>This implementation allows manual reordering of timers via drag gestures,
 * but disables dragging when the user initiates a touch event on the {@code + 1:00} button.
 *
 * <p>Drag directions are enabled or disabled based on user preferences and device orientation,
 * ensuring consistent UX whether on tablets, phones, portrait or landscape modes.</p>
 */
public class TimerItemTouchHelper extends ItemTouchHelper.Callback {

    private final ItemTouchHelperContract mContract;
    private boolean mIsTouchOnDragBlockingView = false;
    private boolean mIsTimerTimeActivated = false;
    private int dragFrom = RecyclerView.NO_POSITION;
    private int dragTo = RecyclerView.NO_POSITION;
    private final boolean mIsTablet;
    private final boolean mIsLandscape;
    private boolean mIsManualSorting;

    public TimerItemTouchHelper(ItemTouchHelperContract contract, RecyclerView recyclerView, boolean isTablet, boolean isLandscape,
                                boolean isManualSorting) {

        mContract = contract;
        mIsTablet = isTablet;
        mIsLandscape = isLandscape;
        mIsManualSorting = isManualSorting;

        // Prevent the timer from dragging if the "Add minute" button is long-pressed
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    View child = rv.findChildViewUnder(e.getX(), e.getY());
                    if (child != null) {
                        RecyclerView.ViewHolder holder = rv.getChildViewHolder(child);

                        if (holder instanceof TimerViewHolder timerViewHolder) {
                            View addTimeButton = timerViewHolder.addTimeButton;

                            if (addTimeButton != null && addTimeButton.getVisibility() == View.VISIBLE) {
                                int[] loc = new int[2];
                                addTimeButton.getLocationOnScreen(loc);
                                float x = e.getRawX();
                                float y = e.getRawY();

                                mIsTouchOnDragBlockingView = x >= loc[0] && x <= loc[0] + addTimeButton.getWidth()
                                    && y >= loc[1] && y <= loc[1] + addTimeButton.getHeight();
                            } else {
                                mIsTouchOnDragBlockingView = false;
                            }

                            View circle = timerViewHolder.circleContainer;
                            View timerTimeText = timerViewHolder.timerTimeText;

                            if (circle != null && circle.getVisibility() == View.VISIBLE) {
                                int[] loc = new int[2];
                                circle.getLocationOnScreen(loc);
                                float x = e.getRawX();
                                float y = e.getRawY();

                                mIsTimerTimeActivated = x >= loc[0] && x <= loc[0] + circle.getWidth()
                                    && y >= loc[1] && y <= loc[1] + circle.getHeight();
                            } else {
                                int[] loc = new int[2];
                                timerTimeText.getLocationOnScreen(loc);
                                float x = e.getRawX();
                                float y = e.getRawY();

                                mIsTimerTimeActivated = x >= loc[0] && x <= loc[0] + timerTimeText.getWidth()
                                    && y >= loc[1] && y <= loc[1] + timerTimeText.getHeight();
                            }
                        }
                    } else {
                        mIsTouchOnDragBlockingView = false;
                        mIsTimerTimeActivated = false;
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
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (mIsTouchOnDragBlockingView || !mIsManualSorting) {
            return 0;
        }

        final int dragFlags;
        if (mIsTablet) {
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END;
        } else if (mIsLandscape) {
            dragFlags = ItemTouchHelper.START | ItemTouchHelper.END;
        } else {
            dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        }

        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {

        int fromPosition = viewHolder.getBindingAdapterPosition();
        int toPosition = target.getBindingAdapterPosition();

        if (dragFrom == RecyclerView.NO_POSITION) {
            dragFrom = fromPosition;
        }

        dragTo = toPosition;

        mContract.onRowMoved(fromPosition, toPosition);

        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            mContract.onRowSelected(viewHolder);

            // Maintain timer text color when it's dragging.
            if (mIsTimerTimeActivated && viewHolder instanceof TimerViewHolder timerViewHolder) {
                if (timerViewHolder.timerTimeText != null) {
                    timerViewHolder.timerTimeText.setActivated(true);
                }
            }
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        mContract.onRowClear(viewHolder);

        // Clear timer text color when the drag is complete.
        if (viewHolder instanceof TimerViewHolder timerViewHolder) {
            if (timerViewHolder.timerTimeText != null) {
                timerViewHolder.timerTimeText.setActivated(false);
            }
        }

        mIsTimerTimeActivated = false;
        mIsTouchOnDragBlockingView = false;

        if (dragFrom != RecyclerView.NO_POSITION && dragTo != RecyclerView.NO_POSITION && dragFrom != dragTo) {
            mContract.onRowSaved();
        }

        dragFrom = RecyclerView.NO_POSITION;
        dragTo = RecyclerView.NO_POSITION;
    }

    public void setManualSorting(boolean isManualSorting) {
        mIsManualSorting = isManualSorting;
    }

}
