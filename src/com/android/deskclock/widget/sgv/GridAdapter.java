/*
 * Copyright (C) 2012 Google Inc. All Rights Reserved.
 */

package com.android.deskclock.widget.sgv;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.android.deskclock.widget.sgv.ReorderUtils;

/**
 * Adapter for use with {@link StaggeredGridView}. Copied from Keep.
 * TODO: Keep up to date with fixes to their code; if they move into a shared library, use that.
 */
public abstract class GridAdapter extends BaseAdapter {
    /**
     * A tag key which stores the id of the associated object. If set, this
     * allows for faster creation of views.
     */
    private static final int GRID_ID_TAG = "gridIdTag".hashCode();
    private View mHeaderView;
    private View mFooterView;

    public GridAdapter() {
        super();
    }

    /**
     * Checks to see if the child at the specified position is draggable
     * @param position The position of the child to check against
     * @return boolean If true, the child at the specified position is draggable.
     */
    public boolean isDraggable(int position) {
        return false;
    }

    /**
     * For a view at the specified position, return the region around the view that is a possible
     * reordering area.
     * @param position The adapter position
     * @param isLastColumnInGrid Flag to indicate whether the view at the specified position is
     * laid out at the last column in the grid.  Being in the last column has some UI implications
     * when it comes to reordering.  As an example, if a view has reordering area set to Left, and
     * is laid out in the last column, the grid may also choose to enable the right reordering area
     * for this view as well so that the user has the convenience have dropping views on the right
     * edge of the grid to re-order.
     * @return int The re-ordering area for the view at this adapter position.
     * Possible return values are a combination of
     * {@link ReorderUtils#REORDER_AREA_NONE}, {@link ReorderUtils#REORDER_AREA_VALID}
     */
    public int getReorderingArea(int position, boolean isLastColumnInGrid) {
        return ReorderUtils.REORDER_AREA_NONE;
    }

    /**
     * The direction for drag to reorder that is allowed.  By default, the allowed direction
     * is free across both horizontal and vertical axes.
     * @return int The allowed direction for drag to reorder.  Possible return values are a
     * combination of {@link ReorderUtils#REORDER_DIRECTION_VERTICAL} and/or
     * {@link ReorderUtils#REORDER_DIRECTION_HORIZONTAL}
     */
    public int getReorderingDirection() {
        return ReorderUtils.REORDER_DIRECTION_VERTICAL | ReorderUtils.REORDER_DIRECTION_HORIZONTAL;
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    public void setHeaderView(View view) {
        mHeaderView = view;
    }

    public View getFooterView() {
        return mFooterView;
    }

    public void setFooterView(View view) {
        mFooterView = view;
    }

    public boolean hasHeader() {
        return mHeaderView != null;
    }

    public boolean hasFooter() {
        return mFooterView != null;
    }

    /**
     * Views created via the GridAdapter or any subclasses should call this to
     * store the id of the item associated with them.
     */
    public void setItemId(View view, long id) {
        view.setTag(GRID_ID_TAG, id);
    }

    /**
     * Get the id of the item associated with this view.
     */
    public long getItemIdFromView(View view, int position) {
        final Object id = view.getTag(GRID_ID_TAG);
        if (id != null) {
            return (Long) id;
        }

        return getItemId(position);
    }

    /**
     * Get the id of the item associated with this view.  The specified Object is associated with
     * the view at this position, and can be used to optimize retrieval of the id by the adapter.
     * @param item Object associated with this view at this position.
     * @param position Position of the item.
     * @return id Id for the item at this position.
     */
    public long getItemId(Object item, int position) {
        // TODO: Rather than using Object, use BaseNode so that we're not confused between this
        // method, and the method above.
        return getItemId(position);
    }

    /**
     * Get the type of the view given the item it will display based on its
     * position in the cursor managed by the adapter. Previously, the adapter
     * would create an item based on the position and use that item to get the
     * view type. However, if the item already exists due to another call that
     * required it, it is much better to reuse the item than recreate it.
     *
     * @param item Object associated with the view at this position
     * @param position Position of the item we are verifying
     * @return int representing the type of the item at the supplied position
     */
    public int getItemViewType(Object item, int position) {
        return getItemViewType(position);
    }

    /**
     * Get the view given its associated item.
     *
     * @param item Object associated with the view at this position
     * @param position Position of the item we are verifying
     * @param scrap The old view to reuse, if possible. Note: You should check
     *            that this view is non-null and of an appropriate type before
     *            using. If it is not possible to convert this view to display
     *            the correct data, this method can create a new view.
     *            Heterogeneous lists can specify their number of view types, so
     *            that this View is always of the right type (see
     *            getViewTypeCount() and getItemViewType(int)).
     * @param parent The parent view this view will eventually be attached to
     * @param measuredWidth
     * @return View
     */
    public View getView(Object item, int position, View scrap, ViewGroup parent,
            int measuredWidth) {
        return getView(position, scrap, parent);
    }

    /**
     * Get how many columns a specific view will span in the grid.
     */
    abstract public int getItemColumnSpan(Object item, int position);

    /**
     * The first change position of all items. It will be used by
     * StaggeredGridView to optimize rendering, such as skip unchanged items. By
     * default it returns 0, so StaggeredGridView will not try to optimize.
     */
    public int getFirstChangedPosition() {
        return 0;
    }

    /**
     * StaggeredGridView only works with stable id. Any adapter which wants to
     * use staggered grid view is enforced to use stable ids.
     */
    @Override
    public boolean hasStableIds() {
        return true;
    }
}
