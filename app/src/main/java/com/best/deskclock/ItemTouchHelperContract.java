// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Interface to listen for move and state change events from an {@link androidx.recyclerview.widget.ItemTouchHelper.Callback}.
 *
 * <p>This contract allows the ItemTouchHelper to notify the adapter of drag-and-drop actions without being tightly coupled
 * to a specific adapter implementation.</p>
 */
public interface ItemTouchHelperContract {

    /**
     * Called when an item is moved.
     */
    void onRowMoved(int fromPosition, int toPosition);

    /**
     * Called when the item is released.
     */
    void onRowSelected(RecyclerView.ViewHolder myViewHolder);

    /**
     * Called to remove visual effects.
     */
    void onRowClear(RecyclerView.ViewHolder myViewHolder);

    /**
     * Called to save the list.
     */
    void onRowSaved();
}
