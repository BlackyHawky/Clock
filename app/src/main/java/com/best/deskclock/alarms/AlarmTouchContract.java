// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.ItemTouchHelperContract;

/**
 * Extended contract for the Alarm ItemTouchHelper.
 *
 * <p>Inherits from {@link ItemTouchHelperContract} and adds specific callbacks
 * for swipe-to-delete actions and state validations tailored to the alarm list behavior.</p>
 */
public interface AlarmTouchContract extends ItemTouchHelperContract {

    /**
     * Checks whether the drag-and-drop operation is currently allowed.
     *
     * @return {@code true} if dragging is permitted (e.g., manual sorting is enabled and the clock view is not being touched),
     * {@code false} otherwise.
     */
    boolean canDrag();

    /**
     * Checks whether the swipe-to-delete operation is currently allowed.
     *
     * @return {@code true} if swiping is permitted (e.g., no edit bottom sheets or delay dialogs are currently open),
     * {@code false} otherwise.
     */
    boolean canSwipe();

    /**
     * Called when a ViewHolder has been fully swiped off the screen.
     * This typically triggers the removal of the alarm from the database and the adapter.
     *
     * @param viewHolder The {@link androidx.recyclerview.widget.RecyclerView.ViewHolder} that was swiped.
     */
    void onRowSwiped(RecyclerView.ViewHolder viewHolder);

    /**
     * Called when the user initially moves the item horizontally, starting the swipe animation.
     * This is generally used to trigger UI adjustments, such as smoothly hiding the Floating Action Button (FAB).
     */
    void onSwipeStarted();

}
