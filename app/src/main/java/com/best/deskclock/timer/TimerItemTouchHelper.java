// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.ItemTouchHelperContract;
import com.best.deskclock.R;

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
    private boolean isTouchOnDragBlockingView = false;
    private int dragFrom = RecyclerView.NO_POSITION;
    private int dragTo = RecyclerView.NO_POSITION;
    private final boolean mIsTablet;
    private final boolean mIsLandscape;
    private final boolean mIsManualSort;

    public TimerItemTouchHelper(ItemTouchHelperContract contract, RecyclerView recyclerView, boolean isTablet, boolean isLandscape,
                                boolean isManualSort) {

        mContract = contract;
        mIsTablet = isTablet;
        mIsLandscape = isLandscape;
        mIsManualSort = isManualSort;

        // Prevent the timer from dragging if the "Add minute" button is long-pressed
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
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (isTouchOnDragBlockingView || !mIsManualSort) {
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

        // Draw a shadow under the timer card when it's dragging.
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            mContract.onRowSelected(viewHolder);
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        // Remove the shadow under the city card when the drag is complete.
        mContract.onRowClear(viewHolder);

        // Save the list of timers once the user interaction is complete.
        if (dragFrom != RecyclerView.NO_POSITION && dragTo != RecyclerView.NO_POSITION && dragFrom != dragTo) {
            mContract.onRowSaved();
        }

        dragFrom = RecyclerView.NO_POSITION;
        dragTo = RecyclerView.NO_POSITION;
    }

}
