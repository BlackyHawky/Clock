/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
 */

package com.android.deskclock.widget.sgv;

import android.graphics.Point;
import android.util.Log;
import android.view.View;

import com.android.deskclock.widget.sgv.StaggeredGridView.LayoutParams;
import com.android.deskclock.widget.sgv.StaggeredGridView.ReorderListener;


/**
 * Helper class for doing reorder animations. Works out the logical position of
 * where an item would be placed as the user drags it around.
 */
public final class ReorderHelper {

    private static final String TAG = "DeskClock";

    /**
     * Constant to indicate an unsupported reordering position.
     */
    public static final int INVALID_REORDER_POS = -2;

    private final ReorderListener mReorderListener;

    // Current {@link ReorderView} that is currently being dragged over.  If drag is released here,
    // and this child supports reordering, the dragged view will be reordered to be next
    // to this child.
    private ReorderView mCurrentDraggedOverChild;

    // The current child that is being dragged for reordering.
    private ReorderView mDraggedChild;

    // The id of mDraggedChild.
    private long mDraggedChildId = -1;

    // The parent view group that dragged children are attached to.
    private final StaggeredGridView mParentView;

    private boolean mEnableUpdatesOnDrag = true;

    public ReorderHelper(ReorderListener listener, StaggeredGridView parentView) {
        mReorderListener = listener;
        mParentView = parentView;
        if (listener == null) {
            throw new IllegalArgumentException("ReorderListener cannot be null");
        }

        if (parentView == null) {
            throw new IllegalArgumentException("ParentView cannot be null");
        }
    }

    /**
     * Handle dropping the dragged child.
     * @return true if the drop results in a reordering, false otherwise.
     */
    public boolean handleDrop(Point p) {
        View reorderTarget = null;
        if (mCurrentDraggedOverChild != null) {
            reorderTarget = getReorderableChildAtCoordinate(p);
        } else {
            Log.w(TAG, "Current dragged over child does not exist");
        }

        // If reorder target is null, the drag coordinate is not over any
        // reordering areas. Don't update dragged over child if its the same as
        // it was before or is the same as the child's original item.
        if (reorderTarget != null) {
            final LayoutParams lp = (LayoutParams) reorderTarget.getLayoutParams();
            // Ensure that target position is not the same as the original,
            // since that's a no-op.
            if (lp.position != mCurrentDraggedOverChild.position) {
                updateDraggedOverChild(reorderTarget);
            }
        }

        if (mCurrentDraggedOverChild != null &&
                mDraggedChild.position != mCurrentDraggedOverChild.position) {
            return mReorderListener.onReorder(mDraggedChild.target, mDraggedChild.id,
                    mDraggedChild.position,
                    mCurrentDraggedOverChild.position);
        } else {
            // Even if the dragged child is not dropped in a reorder area, we
            // would still need to notify the listener of the drop event.
            mReorderListener.onDrop(mDraggedChild.target, mDraggedChild.position,
                    mCurrentDraggedOverChild.position);
            return false;
        }
    }

    public void handleDragCancelled(View draggedView) {
        mReorderListener.onCancelDrag(draggedView);
    }

    public void handleDragStart(View view, int pos, long id, Point p) {
        mDraggedChild = new ReorderView(view, pos, id);
        mDraggedChildId = id;
        mCurrentDraggedOverChild = new ReorderView(view, pos, id);
        mReorderListener.onPickedUp(mDraggedChild.target);
    }

    /**
     * Handles determining which child views should be moved out of the way to
     * make space for a reordered item and updates the ReorderListener when a
     * new child view's space is entered by the dragging view.
     */
    public void handleDrag(Point p) {
        if (p == null || p.y < 0 && p.y > mParentView.getHeight()) {
            // If the user drags off screen, DragEvent.ACTION_DRAG_ENDED, would be called, so we'll
            // treat it as though the user has released drag.
            handleDrop(p);
            return;
        }

        if (!mEnableUpdatesOnDrag) {
            return;
        }

        View reorderTarget = null;
        if (mCurrentDraggedOverChild != null) {
            reorderTarget = getReorderableChildAtCoordinate(p);
        } else {
            Log.w(TAG, "Current dragged over child does not exist");
        }

        // If reorder target is null, the drag coordinate is not over any
        // reordering areas. Don't update dragged over child if its the same as
        // it was before or is the same as the child's original item.
        if (reorderTarget != null) {
            final LayoutParams lp = (LayoutParams) reorderTarget.getLayoutParams();
            if (lp.position != mCurrentDraggedOverChild.position) {
                updateDraggedOverChild(reorderTarget);
                // Ensure that target position is not the same as the original,
                // since that's a no-op.
                mReorderListener.onEnterReorderArea(reorderTarget, lp.position);
            }
        }
    }

    /**
     * Enable updates on drag events. If set to false, handleDrag will not update place holder
     */
    public void enableUpdatesOnDrag(boolean enabled) {
        mEnableUpdatesOnDrag = enabled;
    }

    /**
     * Clear dragged over child info
     */
    public void clearDraggedOverChild() {
        mCurrentDraggedOverChild = null;
    }

    /**
     * Return if the currently dragged view is over a valid reordering area.
     */
    public boolean isOverReorderingArea() {
        return mCurrentDraggedOverChild != null;
    }

    /**
     * Get the position of the child that is being dragged over. If there isn't one, returns
     * {@link #INVALID_REORDER_POS}
     */
    public int getCurrentDraggedOverChildPosition() {
        if (mCurrentDraggedOverChild != null) {
            return mCurrentDraggedOverChild.position;
        }

        return INVALID_REORDER_POS;
    }

    /**
     * Get the id of the child that is being dragged. If there isn't one, returns -1
     */
    public long getDraggedChildId() {
        return mDraggedChildId;
    }

    /**
     * Get the original view of the child that is being dragged. If there isn't
     * one, returns null
     */
    public View getDraggedChild() {
        return mDraggedChild != null ? mDraggedChild.target : null;
    }

    /**
     * Clear original dragged child info
     */
    public void clearDraggedChild() {
        mDraggedChild = null;
    }

    // TODO: Consolidate clearDraggedChild() and clearDraggedChildId().
    public void clearDraggedChildId() {
        mDraggedChildId = -1;
    }

    /**
     * Get the original position of the child that is being dragged. If there isn't one, returns
     * {@link #INVALID_REORDER_POS};
     */
    public int getDraggedChildPosition() {
        return mDraggedChild != null ? mDraggedChild.position : INVALID_REORDER_POS;
    }

    public void updateDraggedChildView(View v) {
        if (mDraggedChild != null && v != mDraggedChild.target) {
            mDraggedChild.target = v;
        }
    }

    public void updateDraggedOverChildView(View v) {
        if (mCurrentDraggedOverChild != null && v != mCurrentDraggedOverChild.target) {
            mCurrentDraggedOverChild.target = v;
        }
    }

    /**
     * Update the current view that is being dragged over, and clean up all drag and hover
     * UI states from other sibling views.
     * @param child The new child that is being dragged over.
     */
    private void updateDraggedOverChild(View child) {
        final LayoutParams childLayoutParam = (LayoutParams) child.getLayoutParams();
        mCurrentDraggedOverChild = new ReorderView(
                child, childLayoutParam.position, childLayoutParam.id);
    }

    /**
     * Return the child view specified by the coordinates if
     * there exists a child there.
     *
     * @return the child in this StaggeredGridView at the coordinates, null otherwise.
     */
    public View getReorderableChildAtCoordinate(Point p) {
        if (p == null || p.y < 0) {
            // TODO: If we've dragged off the screen, return null for now until we know what
            // we'd like the experience to be like.
            return null;
        }

        final int count = mParentView.getChildCount();
        for (int i = 0; i < count; i++) {
            if (!mParentView.isChildReorderable(i)) {
                continue;
            }
            final View childView = mParentView.getChildAt(i);
            if (p.x >= childView.getLeft() && p.x < childView.getRight()
                    && p.y >= childView.getTop() && p.y < childView.getBottom()) {
                return childView;
            }
        }

        return null;
    }

    public boolean hasReorderListener() {
        return mReorderListener != null;
    }

    private class ReorderView {
        final long id;
        final int position;
        View target;
        public ReorderView(View v, int pos, long i) {
            target = v;
            position = pos;
            id = i;
        }
    }
}