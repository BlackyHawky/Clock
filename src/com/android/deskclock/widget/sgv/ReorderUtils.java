/*
 * Copyright (C) 2012 Google Inc. All Rights Reserved.
 */

package com.android.deskclock.widget.sgv;

public class ReorderUtils {
    /**
     * Possible states a dragged view can be in.
     */
    public static final int ON_PICKED_UP = 0;
    public static final int ON_DRAG_RELEASE = 1;
    public static final int ON_DRAG_CANCELLED = 2;

    /**
     * Reordering area of a view in a StaggeredGridView.
     */
    public static final int REORDER_AREA_NONE = 0x0;
    public static final int REORDER_AREA_VALID = 0x1;

    /**
     *  Reordering states
     */
    public static final int DRAG_STATE_NONE = 0;
    public static final int DRAG_STATE_DRAGGING = 1;
    public static final int DRAG_STATE_RELEASED_REORDER= 2;
    public static final int DRAG_STATE_RELEASED_HOVER = 3;

    /**
     * Reordering directions allowed.  {@link #REORDER_DIRECTION_HORIZONTAL} direction means that
     * the user is only allowed to drag a view along the horizontal axis.  Likewise for
     * {@link #REORDER_DIRECTION_VERTICAL}.  These two flags can be OR'ed together to allow for
     * free dragging across both horizontal and vertical axes.
     */
    public static final int REORDER_DIRECTION_HORIZONTAL = 0x1;
    public static final int REORDER_DIRECTION_VERTICAL = 0x2;
}
