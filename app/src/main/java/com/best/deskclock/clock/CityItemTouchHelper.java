// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.clock;

import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.ItemTouchHelperContract;

/**
 * Custom {@link androidx.recyclerview.widget.ItemTouchHelper.Callback} for managing drag & drop of city items in a RecyclerView.
 */
public class CityItemTouchHelper extends ItemTouchHelper.Callback {

    private final ItemTouchHelperContract mContract;
    private final boolean mIsPortrait;
    private final boolean mShowHomeClock;

    private int dragFrom = RecyclerView.NO_POSITION;
    private int dragTo = RecyclerView.NO_POSITION;

    public CityItemTouchHelper(ItemTouchHelperContract contract, boolean isPortrait, boolean showHomeClock) {
        mContract = contract;
        mIsPortrait = isPortrait;
        mShowHomeClock = showHomeClock;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getBindingAdapterPosition();
        int offset = (mIsPortrait ? 1 : 0) + (mShowHomeClock ? 1 : 0);

        if (position < offset) {
            return 0;
        }

        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {

        int from = viewHolder.getBindingAdapterPosition();
        int to = target.getBindingAdapterPosition();
        int offset = (mIsPortrait ? 1 : 0) + (mShowHomeClock ? 1 : 0);

        if (from < offset || to < offset) {
            return false;
        }

        if (dragFrom == RecyclerView.NO_POSITION) {
            dragFrom = from;
        }

        dragTo = to;

        mContract.onRowMoved(from, to);

        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        // Calculation of upper and lower limits for drag
        int position = viewHolder.getBindingAdapterPosition();
        int offset = (mIsPortrait ? 1 : 0) + (mShowHomeClock ? 1 : 0);

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && position >= offset) {
            // Upper limit
            RecyclerView.ViewHolder firstHolder = recyclerView.findViewHolderForAdapterPosition(offset);
            float minY = firstHolder != null
                ? firstHolder.itemView.getTop()
                : 0; // Fallback

            // Bottom limit
            int lastIndex = recyclerView.getAdapter() != null ? recyclerView.getAdapter().getItemCount() - 1 : 0;
            RecyclerView.ViewHolder lastHolder = recyclerView.findViewHolderForAdapterPosition(lastIndex);
            float maxY = lastHolder != null
                ? lastHolder.itemView.getBottom()
                : recyclerView.getHeight() - recyclerView.getPaddingBottom();

            // Calculation of the projection
            View movingView = viewHolder.itemView;
            float currentTop = movingView.getTop();
            float currentBottom = movingView.getBottom();

            float projectedTop = currentTop + dY;
            float projectedBottom = currentBottom + dY;

            // Adjustment
            float newDY = dY;

            if (projectedTop < minY) {
                newDY = minY - currentTop;
            } else if (projectedBottom > maxY) {
                newDY = maxY - currentBottom;
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, newDY, actionState, isCurrentlyActive);
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            mContract.onRowSelected(viewHolder);
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        mContract.onRowClear(viewHolder);

        if (dragFrom != RecyclerView.NO_POSITION && dragTo != RecyclerView.NO_POSITION && dragFrom != dragTo) {
            mContract.onRowSaved();
        }

        dragFrom = RecyclerView.NO_POSITION;
        dragTo = RecyclerView.NO_POSITION;
    }

}
