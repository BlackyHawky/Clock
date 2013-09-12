/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.widget.sgv;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.android.deskclock.widget.sgv.SgvAnimationHelper.AnimationIn;
import com.android.deskclock.widget.sgv.SgvAnimationHelper.AnimationOut;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Temporarily copied from support v4 library so that StaggeredGridView can access
 * animation APIs on the current SDK version.
 */
/**
 * ListView and GridView just not complex enough? Try StaggeredGridView!
 *
 * <p>StaggeredGridView presents a multi-column grid with consistent column sizes
 * but varying row sizes between the columns. Each successive item from a
 * {@link android.widget.ListAdapter ListAdapter} will be arranged from top to bottom,
 * left to right. The largest vertical gap is always filled first.</p>
 *
 * <p>Item views may span multiple columns as specified by their {@link LayoutParams}.
 * The attribute <code>android:layout_span</code> may be used when inflating
 * item views from xml.</p>
 */
public class StaggeredGridView extends ViewGroup {

    private static final String TAG = "Clock-" + StaggeredGridView.class.getSimpleName();

    /*
     * There are a few things you should know if you're going to make modifications
     * to StaggeredGridView.
     *
     * Like ListView, SGV populates from an adapter and recycles views that fall out
     * of the visible boundaries of the grid. A few invariants always hold:
     *
     * - mFirstPosition is the adapter position of the View returned by getChildAt(0).
     * - Any child index can be translated to an adapter position by adding mFirstPosition.
     * - Any adapter position can be translated to a child index by subtracting mFirstPosition.
     * - Views for items in the range [mFirstPosition, mFirstPosition + getChildCount()) are
     *   currently attached to the grid as children. All other adapter positions do not have
     *   active views.
     *
     * This means a few things thanks to the staggered grid's nature. Some views may stay attached
     * long after they have scrolled offscreen if removing and recycling them would result in
     * breaking one of the invariants above.
     *
     * LayoutRecords are used to track data about a particular item's layout after the associated
     * view has been removed. These let positioning and the choice of column for an item
     * remain consistent even though the rules for filling content up vs. filling down vary.
     *
     * Whenever layout parameters for a known LayoutRecord change, other LayoutRecords before
     * or after it may need to be invalidated. e.g. if the item's height or the number
     * of columns it spans changes, all bets for other items in the same direction are off
     * since the cached information no longer applies.
     */

    private GridAdapter mAdapter;

    public static final int COLUMN_COUNT_AUTO = -1;

    /**
     * The window size to search for a specific item when restoring scroll position.
     */
    private final int SCROLL_RESTORE_WINDOW_SIZE = 10;

    private static final int CHILD_TO_REORDER_AREA_RATIO = 4;

    private static final int SINGLE_COL_REORDERING_AREA_SIZE = 30;

    // Time delay in milliseconds between posting each scroll runnables.
    private static final int SCROLL_HANDLER_DELAY = 5;

    // The default rate of pixels to scroll by when a child view is dragged towards the
    // upper and lower bound of this view.
    private static final int DRAG_SCROLL_RATE = 10;

    public static final int ANIMATION_DELAY_IN_MS = 50;

    private AnimationIn mAnimationInMode = AnimationIn.NONE;
    private AnimationOut mAnimationOutMode = AnimationOut.NONE;

    private AnimatorSet mCurrentRunningAnimatorSet = null;

    /**
     * Flag to indicate whether the current running animator set was canceled before it reaching
     * the end of the animations.  This flag is used to help indicate whether the next set of
     * animators should resume from where the last animator set left off.
     */
    boolean mIsCurrentAnimationCanceled = false;

    private int mColCountSetting = 2;
    private int mColCount = 2;
    private int mMinColWidth = 0;
    private int mItemMargin = 0;

    private int[] mItemTops;
    private int[] mItemBottoms;

    private final Rect mTempRect = new Rect();

    private boolean mFastChildLayout;
    private boolean mPopulating;
    private boolean mInLayout;

    private boolean mIsRtlLayout;

    private final RecycleBin mRecycler = new RecycleBin();

    private final AdapterDataSetObserver mObserver = new AdapterDataSetObserver();

    private boolean mDataChanged;
    private int mItemCount;

    /**
     * After data set change, we ask adapter the first view that changed.
     * Any view from 0 to mFirstChangedPosition - 1 is not changed.
     */
    private int mFirstChangedPosition;

    /**
     * If set to true, then we guard against jagged edges in the grid by doing expensive
     * computation. Otherwise if this is false, we skip the computation.
     */
    private boolean mGuardAgainstJaggedEdges;

    private boolean mHasStableIds;

    /**
     * List of all views to animate out.  This is used when we need to animate out stale views.
     */
    private final List<View> mViewsToAnimateOut = new ArrayList<View>();

    private int mFirstPosition;

    private long mFocusedChildIdToScrollIntoView;
    private ScrollState mCurrentScrollState;

    private final int mTouchSlop;
    private final int mMaximumVelocity;
    private final int mFlingVelocity;
    private float mLastTouchY = 0;
    private float mTouchRemainderY;
    private int mActivePointerId;

    private static final int TOUCH_MODE_IDLE = 0;
    private static final int TOUCH_MODE_DRAGGING = 1;
    private static final int TOUCH_MODE_FLINGING = 2;
    private static final int TOUCH_MODE_OVERFLING = 3;

    // Value used to estimate the range of scroll and scroll position
    final static int SCROLLING_ESTIMATED_ITEM_HEIGHT = 100;

    private int mTouchMode;
    private final VelocityTracker mVelocityTracker = VelocityTracker.obtain();
    private final OverScrollerSGV mScroller;

    private final EdgeEffectCompat mTopEdge;
    private final EdgeEffectCompat mBottomEdge;

    private boolean mIsDragReorderingEnabled;

    private ScrollListener mScrollListener;
    private OnSizeChangedListener mOnSizeChangedListener;

    // The view to show when the adapter is empty.
    private View mEmptyView;

    // The size of the region at location relative to the child's edges where reordering
    // can happen if another child view is dragged and dropped over it.
    private int mHorizontalReorderingAreaSize;

    // TODO: Put these states into a ReorderingParam object for maintainability.
    private ImageView mDragView;

    // X and Y positions of the touch down event that started the drag
    private int mTouchDownForDragStartX;
    private int mTouchDownForDragStartY;

    // X and Y offsets inside the item from where the user grabbed to the
    // child's left coordinate.
    // This is used to aid in the drawing of the drag shadow.
    private int mTouchOffsetToChildLeft;
    private int mTouchOffsetToChildTop;

    // Difference between screen coordinates and coordinates in this view.
    private int mOffsetToAbsoluteX;
    private int mOffsetToAbsoluteY;

    // the cached positions of the drag view when released.
    private Rect mCachedDragViewRect;

    // the current drag state
    private int mDragState;

    // the height of this view
    private int mHeight;

    // The bounds of the screen that should initiate scrolling when a view
    // is dragged past these positions.
    private int mUpperScrollBound;
    private int mLowerScrollBound;

    // The Bitmap that contains the drag shadow.
    private Bitmap mDragBitmap;
    private final int mOverscrollDistance;

    private final WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private static final int mWindowManagerLayoutFlags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

    private ReorderHelper mReorderHelper;

    /**
     * Indicates whether to use pixels-based or position-based scrollbar
     * properties.
     * This property is borrow from AbsListView
     */
    private boolean mSmoothScrollbarEnabled = false;

    private static final class LayoutRecord {
        public int column;
        public long id = -1;
        public int height;
        public int span;
        private int[] mMargins;

        private final void ensureMargins() {
            if (mMargins == null) {
                // Don't need to confirm length;
                // all layoutrecords are purged when column count changes.
                mMargins = new int[span * 2];
            }
        }

        public final int getMarginAbove(int col) {
            if (mMargins == null) {
                return 0;
            }
            return mMargins[col * 2];
        }

        public final int getMarginBelow(int col) {
            if (mMargins == null) {
                return 0;
            }
            return mMargins[col * 2 + 1];
        }

        public final void setMarginAbove(int col, int margin) {
            if (mMargins == null && margin == 0) {
                return;
            }
            ensureMargins();
            mMargins[col * 2] = margin;
        }

        public final void setMarginBelow(int col, int margin) {
            if (mMargins == null && margin == 0) {
                return;
            }
            ensureMargins();
            mMargins[col * 2 + 1] = margin;
        }

        @Override
        public String toString() {
            String result = "LayoutRecord{c=" + column + ", id=" + id + " h=" + height +
                    " s=" + span;
            if (mMargins != null) {
                result += " margins[above, below](";
                for (int i = 0; i < mMargins.length; i += 2) {
                    result += "[" + mMargins[i] + ", " + mMargins[i+1] + "]";
                }
                result += ")";
            }
            return result + "}";
        }
    }

    private final Map<Long, ViewRectPair> mChildRectsForAnimation =
            new HashMap<Long, ViewRectPair>();

    private final SparseArrayCompat<LayoutRecord> mLayoutRecords =
            new SparseArrayCompat<LayoutRecord>();

    // Handler for executing the scroll runnable
    private Handler mScrollHandler;

    // Boolean is true when the {@link #mDragScroller} scroll runanbled has been kicked off.
    // This is set back to false when it is removed from the handler.
    private boolean mIsDragScrollerRunning;

    /**
     * Scroller runnable to invoke scrolling when user is holding a dragged view over the upper
     * or lower bounds of the screen.
     */
    private final Runnable mDragScroller = new Runnable() {
        @Override
        public void run() {
            if (mDragState == ReorderUtils.DRAG_STATE_NONE) {
                return;
            }

            boolean enableUpdate = true;
            if (mLastTouchY >= mLowerScrollBound) {
                // scroll the list up a bit if we're past the lower bound, and the direction
                // of the movement is towards the bottom of the view.
                if (trackMotionScroll(-DRAG_SCROLL_RATE, false)) {
                    // Disable reordering if the view is scrolling
                    enableUpdate = false;
                }
            } else if (mLastTouchY <= mUpperScrollBound) {
                // scroll the list down a bit if we're past the upper bound, and the direction
                // of the movement is towards the top of the view.
                if (trackMotionScroll(DRAG_SCROLL_RATE, false)) {
                    // Disable reordering if the view is scrolling
                    enableUpdate = false;
                }
            }

            mReorderHelper.enableUpdatesOnDrag(enableUpdate);

            mScrollHandler.postDelayed(this, SCROLL_HANDLER_DELAY);
        }
    };

    public StaggeredGridView(Context context) {
        this(context, null);
    }

    public StaggeredGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StaggeredGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMaximumVelocity = vc.getScaledMaximumFlingVelocity();
        mFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mScroller = new OverScrollerSGV(context);

        mTopEdge = new EdgeEffectCompat(context);
        mBottomEdge = new EdgeEffectCompat(context);
        setWillNotDraw(false);
        setClipToPadding(false);

        SgvAnimationHelper.initialize(context);

        mDragState = ReorderUtils.DRAG_STATE_NONE;
        mIsDragReorderingEnabled = true;
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mOverscrollDistance = configuration.getScaledOverflingDistance();
        // Disable splitting event. Only one of the children can handle motion event.
        setMotionEventSplittingEnabled(false);
    }

    /**
     * Check to see if the current layout is Right-to-Left.  This check is only supported for
     * API 17+.  For earlier versions, this method will just return false.
     *
     * NOTE:  This is based on the private API method in {@link View} class.
     *
     * @return boolean Boolean indicating whether the currently locale is RTL.
     */
    @SuppressLint("NewApi")
    private boolean isLayoutRtl() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return View.LAYOUT_DIRECTION_RTL == getLayoutDirection();
        } else {
            return false;
        }
    }

    /**
     * Set a fixed number of columns for this grid. Space will be divided evenly
     * among all columns, respecting the item margin between columns.
     * The default is 2. (If it were 1, perhaps you should be using a
     * {@link android.widget.ListView ListView}.)
     *
     * @param colCount Number of columns to display.
     * @see #setMinColumnWidth(int)
     */
    public void setColumnCount(int colCount) {
        if (colCount < 1 && colCount != COLUMN_COUNT_AUTO) {
            throw new IllegalArgumentException("Column count must be at least 1 - received " +
                    colCount);
        }
        final boolean needsPopulate = colCount != mColCount;
        mColCount = mColCountSetting = colCount;
        if (needsPopulate) {
            // When switching column count, for now, don't restore scroll position, and just
            // start layout fresh again.
            clearAllState();

            mHorizontalReorderingAreaSize = 0;
            populate();
        }
    }

    public int getColumnCount() {
        return mColCount;
    }

    /**
     * Set whether or not to explicitly guard against "jagged edges" in the grid
     * (meaning that the top edge of the children views in the first row of the grid can be
     * horizontally misaligned).
     *
     * If guardAgainstJaggedEdges is true, then we prevent jagged edges by computing the heights of
     * all views starting at the 0th position of the adapter to figure out the proper offset of the
     * views currently on screen. This is an expensive operation and should be avoided if possible.
     *
     * If guardAgainstJaggedEdges is false, then we can skip the expensive computation that
     * guards against jagged edges and just layout views on the screen starting from mFirstPosition
     * (ignoring what came before it).
     */
    public void setGuardAgainstJaggedEdges(boolean guardAgainstJaggedEdges) {
        mGuardAgainstJaggedEdges = guardAgainstJaggedEdges;
    }

    /**
     * Set a minimum column width for
     * @param minColWidth
     */
    public void setMinColumnWidth(int minColWidth) {
        mMinColWidth = minColWidth;
        setColumnCount(COLUMN_COUNT_AUTO);
    }

    /**
     * Set the margin between items in pixels. This margin is applied
     * both vertically and horizontally.
     *
     * @param marginPixels Spacing between items in pixels
     */
    public void setItemMargin(int marginPixels) {
        // We only need to {@link #populate()} if the margin has been changed.
        if (marginPixels != mItemMargin) {
            mItemMargin = marginPixels;
            populate();
        }
    }

    public int getItemMargin() {
        return mItemMargin;
    }

    /**
     * When smooth scrollbar is enabled, the position and size of the scrollbar thumb
     * is computed based on the number of visible pixels in the visible items. This
     * however assumes that all list items have the same height. If you use a list in
     * which items have different heights, the scrollbar will change appearance as the
     * user scrolls through the list. To avoid this issue, you need to disable this
     * property.
     *
     * When smooth scrollbar is disabled, the position and size of the scrollbar thumb
     * is based solely on the number of items in the adapter and the position of the
     * visible items inside the adapter. This provides a stable scrollbar as the user
     * navigates through a list of items with varying heights.
     *
     * @param enabled Whether or not to enable smooth scrollbar.
     *
     * @see #setSmoothScrollbarEnabled(boolean)
     * @attr ref android.R.styleable#AbsListView_smoothScrollbar
     */
    public void setSmoothScrollbarEnabled(boolean enabled) {
        mSmoothScrollbarEnabled = enabled;
    }

    /**
     * Returns the current state of the fast scroll feature.
     *
     * @return True if smooth scrollbar is enabled is enabled, false otherwise.
     *
     * @see #setSmoothScrollbarEnabled(boolean)
     */
    public boolean isSmoothScrollbarEnabled() {
        return mSmoothScrollbarEnabled;
    }


    /**
     * Return the child view specified by the coordinates if
     * there exists a child there.
     *
     * @return the child in this StaggeredGridView at the coordinates, null otherwise.
     */
    private View getChildAtCoordinate(int x, int y) {
        if (y < 0) {
            // TODO: If we've dragged off the screen, return null for now until we know what
            // we'd like the experience to be like.
            return null;
        }

        final Rect frame = new Rect();
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {

            final View childView = getChildAt(i);
            childView.getHitRect(frame);
            if (frame.contains(x, y)) {
                return getChildAt(i);
            }
        }

        // No child view at this coordinate.
        return null;
    }

    /**
     * Get the last Y coordinate on this grid where the last touch was made
     */
    public float getLastTouchY() {
        return mLastTouchY;
    }

    /**
     * Enable drag reordering of child items.
     */
    public void enableDragReordering() {
        mIsDragReorderingEnabled = true;
    }

    /**
     * Disable drag reordering of child items.
     */
    public void disableDragReordering() {
        mIsDragReorderingEnabled = false;
    }

    /**
     * Check to see if drag reordering is supported.  The switch must be flipped to true, and there
     * must be a {@link ReorderListener} registered to listen for reordering events.
     *
     * @return boolean indicating whether drag reordering is currently supported.
     */
    private boolean isDragReorderingSupported() {
        return mIsDragReorderingEnabled && mReorderHelper != null &&
                mReorderHelper.hasReorderListener();
    }

    /**
     * Calculate bounds to assist in scrolling during a drag
     * @param y The y coordinate of the current drag.
     */
    private void initializeDragScrollParameters(int y) {
        // Calculate the upper and lower bound of the screen to support drag scrolling
        mHeight = getHeight();
        mUpperScrollBound = Math.min(y - mTouchSlop, mHeight / 5);
        mLowerScrollBound = Math.max(y + mTouchSlop, mHeight * 4 / 5);
    }

    /**
     * Initiate the dragging process. Create a bitmap that is displayed as the dragging event
     * happens and is moved around across the screen.  This function is called once for each time
     * that a dragging event is initiated.
     *
     * The logic to this method was borrowed from the TouchInterceptor.java class from the
     * music app.
     *
     * @param draggedChild The child view being dragged
     * @param x The x coordinate of this view where dragging began
     * @param y The y coordinate of this view where dragging began
     */
    private void startDragging(final View draggedChild, final int x, final int y) {
        if (!isDragReorderingSupported()) {
            return;
        }

        mDragBitmap = createDraggedChildBitmap(draggedChild);
        if (mDragBitmap == null) {
            // It appears that creating bitmaps for large views fail. For now, don't allow
            // dragging in this scenario.  When using the framework's drag and drop implementation,
            // drag shadow also fails with a OutofResourceException when trying to draw the drag
            // shadow onto a Surface.
            mReorderHelper.handleDragCancelled(draggedChild);
            return;
        }
        mTouchOffsetToChildLeft = x - draggedChild.getLeft();
        mTouchOffsetToChildTop = y - draggedChild.getTop();
        updateReorderStates(ReorderUtils.DRAG_STATE_DRAGGING);

        initializeDragScrollParameters(y);

        final LayoutParams params = (LayoutParams) draggedChild.getLayoutParams();
        mReorderHelper.handleDragStart(draggedChild, params.position, params.id,
                new Point(mTouchDownForDragStartX, mTouchDownForDragStartY));

        // TODO: Reconsider using the framework's DragShadow support for dragging,
        // and only draw the bitmap in onDrop for animation.
        final Context context = getContext();
        mDragView = new ImageView(context);
        mDragView.setImageBitmap(mDragBitmap);
        mDragView.setAlpha(160);

        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.gravity = Gravity.TOP | Gravity.START;

        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = mWindowManagerLayoutFlags;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        // Use WindowManager to overlay a transparent image on drag
        mWindowManager.addView(mDragView, mWindowParams);
        updateDraggedBitmapLocation(x, y);
    }

    private Bitmap createDraggedChildBitmap(View view) {
        view.setDrawingCacheEnabled(true);
        final Bitmap cache = view.getDrawingCache();

        Bitmap bitmap = null;
        if (cache != null) {
            try {
                bitmap = cache.copy(Bitmap.Config.ARGB_8888, false);
            } catch (final OutOfMemoryError e) {
                Log.w(TAG, "Failed to copy bitmap from Drawing cache", e);
                bitmap = null;
            }
        }

        view.destroyDrawingCache();
        view.setDrawingCacheEnabled(false);

        return bitmap;
    }

    /**
     * Updates the current drag state and the UI appropriately.
     * @param state the new drag state to update to.
     */
    private void updateReorderStates(int state) throws IllegalStateException {
        boolean resetDraggedChildView = false;
        boolean resetDragProperties = false;

        mDragState = state;

        switch (state) {
            case ReorderUtils.DRAG_STATE_NONE:
            case ReorderUtils.DRAG_STATE_DRAGGING:
                // reset all states when a drag is complete or when we're starting a new drag.
                resetDraggedChildView = true;
                resetDragProperties = true;
                break;

            case ReorderUtils.DRAG_STATE_RELEASED_REORDER:
                // In a release over a valid reordering zone, don't reset any UI.  Let
                // LayoutChildren() take care of doing the appropriate animation
                // based on the result
                break;

            case ReorderUtils.DRAG_STATE_RELEASED_HOVER:
                // When a dragged child is released over another child, the dragged child will
                // remain hidden.  It is up to the ReorderListener to refresh the UI state
                // of the child if it does not handle the drop.
                resetDragProperties = true;
                break;

            default:
                throw new IllegalStateException("Illegal drag state: " + mDragState);
        }

        if (resetDraggedChildView && mReorderHelper.getDraggedChild() != null) {
            // DraggedChildId and mCachedDragViewRect need to stay around longer than
            // the other properties because on the next data change, as we lay out, we'll need
            // mCachedDragViewRect to position the view's animation start position, and
            // draggedChildId to check if the current was the dragged view.
            // For the other properties - DraggedOverChildView, DraggedChildView, etc.,
            // as soon as drag is released, we can reset them because they have no impact on the
            // next layout pass.
            mReorderHelper.clearDraggedChildId();
            mCachedDragViewRect = null;
        }

        if (resetDragProperties) {
            if (mDragView != null) {
                mDragView.setVisibility(INVISIBLE);
                mWindowManager.removeView(mDragView);
                mDragView.setImageDrawable(null);
                mDragView = null;

                if (mDragBitmap != null) {
                    mDragBitmap.recycle();
                    mDragBitmap = null;
                }
            }

            // We don't reset DraggedChildId here because it may still be in used.
            // Let LayoutChildren reset it when it's done with it.
            mReorderHelper.clearDraggedChild();
            mReorderHelper.clearDraggedOverChild();
        }
    }

    /**
     * Redraw the dragged child's bitmap based on the new coordinates.  If the reordering direction
     * is {@link ReorderUtils#REORDER_DIRECTION_VERTICAL}, then ignore the x coordinate, as
     * only vertical movement is allowed.  Similarly, if reordering direction is
     * {@link ReorderUtils#REORDER_DIRECTION_HORIZONTAL}.  Even though this class does not manage
     * drag shadow directly, we need to make sure we position the dragged bitmap at where the
     * drag shadow is so that when drag ends, we can swap the shadow and the bitmap to animate
     * the view into place.
     * @param x The updated x coordinate of the drag shadow.
     * @param y THe updated y coordinate of the drag shadow.
     */
    private void updateDraggedBitmapLocation(int x, int y) {
        final int direction = mAdapter.getReorderingDirection();
        if ((direction & ReorderUtils.REORDER_DIRECTION_HORIZONTAL) ==
                ReorderUtils.REORDER_DIRECTION_HORIZONTAL) {
            if (mDragBitmap != null && mDragBitmap.getWidth() > getWidth()) {
                // If the bitmap is wider than the width of the screen, then some parts of the view
                // are off screen.  In this case, just set the drag shadow to start at x = 0
                // (adjusted to the absolute position on screen) so that at least the beginning of
                // the drag shadow is guaranteed to be within view.
                mWindowParams.x = mOffsetToAbsoluteX;
            } else {
                // WindowParams is RTL agnostic and operates on raw coordinates.  So in an RTL
                // layout, we would still want to find the view's left coordinate for the
                // drag shadow, rather than the view's start.
                mWindowParams.x = x - mTouchOffsetToChildLeft + mOffsetToAbsoluteX;
            }
        } else {
            mWindowParams.x = mOffsetToAbsoluteX;
        }

        if ((direction & ReorderUtils.REORDER_DIRECTION_VERTICAL) ==
                ReorderUtils.REORDER_DIRECTION_VERTICAL) {
            mWindowParams.y = y - mTouchOffsetToChildTop + mOffsetToAbsoluteY;
        } else {
            mWindowParams.y = mOffsetToAbsoluteY;
        }

        mWindowManager.updateViewLayout(mDragView, mWindowParams);
    }

    /**
     * Update the visual state of the drag event based on the current drag location.  If the user
     * has attempted to re-order by dragging a child over another child's drop zone, call the
     * appropriate {@link ReorderListener} callback.
     *
     * @param x The current x coordinate of the drag event
     * @param y The current y coordinate of the drag event
     */
    private void handleDrag(int x, int y) {
        if (mDragState != ReorderUtils.DRAG_STATE_DRAGGING) {
            return;
        }

        // TODO: Consider moving drag shadow management logic into mReorderHelper as well, or
        // scrap the custom logic and use the framework's drag-and-drop support now that we're not
        // doing anything special to the drag shadow.
        updateDraggedBitmapLocation(x, y);

        if (mCurrentRunningAnimatorSet == null) {
            // If the current animator set is not null, then animation is running, in which case,
            // we shouldn't do any reordering processing, as views will be moving around, and
            // interfering with drag target calculations.
            mReorderHelper.handleDrag(new Point(x, y));
        }
    }

    /**
     * Check if a view is reorderable.
     * @param i the child index in view group
     */
    public boolean isChildReorderable(int i) {
        return mAdapter.isDraggable(mFirstPosition + i);
    }

    /**
     * Handle the the release of a dragged view.
     * @param x The current x coordinate where the drag was released.
     * @param y The current y coordinate where the drag was released.
     */
    private void handleDrop(int x, int y) {
        if (!mReorderHelper.hasReorderListener()) {
            updateReorderStates(ReorderUtils.DRAG_STATE_NONE);
            return;
        }

        if (mReorderHelper.isOverReorderingArea()) {
            // Store the location of the drag shadow at where dragging stopped
            // for animation if a reordering has just happened. Since the drag
            // shadow is drawn as a WindowManager view, its coordinates are
            // absolute. However, for views inside the grid, we need to operate
            // with coordinate values that's relative to this grid, so we need
            // to subtract the offset to absolute screen coordinates that have
            // been added to mWindowParams.
            final int left = mWindowParams.x - mOffsetToAbsoluteX;
            final int top = mWindowParams.y - mOffsetToAbsoluteY;

            mCachedDragViewRect = new Rect(
                    left, top, left + mDragView.getWidth(), top + mDragView.getHeight());
            if (getChildCount() > 0) {
                final View view = getChildAt(0);
                final LayoutParams lp = (LayoutParams) view.getLayoutParams();
                if (lp.position > mReorderHelper.getDraggedChildPosition()) {
                    // If the adapter position of the first child in view is
                    // greater than the position of the original dragged child,
                    // this means that the user has scrolled the child out of
                    // view. Those off screen views would have been recycled. If
                    // mFirstPosition is currently x, after the reordering
                    // operation, the child[mFirstPosition] will be
                    // at mFirstPosition-1. We want to adjust mFirstPosition so
                    // that we render the view in the correct location after
                    // reordering completes.
                    //
                    // If the user has not scrolled the original dragged child
                    // out of view, then the view has not been recycled and is
                    // still in view.
                    // When onLayout() gets called, we'll automatically fill in
                    // the empty space that the child leaves behind from the
                    // reordering operation.
                    mFirstPosition--;
                }
            }

            // Get the current scroll position so that after reordering
            // completes, we can restore the scroll position of mFirstPosition.
            mCurrentScrollState = getScrollState();
        }

        final boolean reordered = mReorderHelper.handleDrop(new Point(x, y));
        if (reordered) {
            updateReorderStates(ReorderUtils.DRAG_STATE_RELEASED_REORDER);
        } else {
            updateReorderStates(ReorderUtils.DRAG_STATE_NONE);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mVelocityTracker.addMovement(ev);
        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mOffsetToAbsoluteX = (int)(ev.getRawX() - ev.getX());
                mOffsetToAbsoluteY = (int)(ev.getRawY() - ev.getY());

                // Per bug 7377413, event.getX() and getY() returns rawX and rawY when accessed in
                // dispatchDragEvent, so since an action down is required before a drag can be
                // initiated, initialize mTouchDownForDragStartX/Y here for the most accurate value.
                mTouchDownForDragStartX = (int) ev.getX();
                mTouchDownForDragStartY = (int) ev.getY();

                mVelocityTracker.clear();
                mScroller.abortAnimation();
                mLastTouchY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mTouchRemainderY = 0;
                if (mTouchMode == TOUCH_MODE_FLINGING) {
                    // Catch!
                    mTouchMode = TOUCH_MODE_DRAGGING;
                    return true;
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (index < 0) {
                    Log.e(TAG, "onInterceptTouchEvent could not find pointer with id " +
                            mActivePointerId + " - did StaggeredGridView receive an inconsistent " +
                            "event stream?");
                    return false;
                }
                final float y = MotionEventCompat.getY(ev, index);
                final float dy = y - mLastTouchY + mTouchRemainderY;
                final int deltaY = (int) dy;
                mTouchRemainderY = dy - deltaY;

                if (Math.abs(dy) > mTouchSlop) {
                    mTouchMode = TOUCH_MODE_DRAGGING;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mVelocityTracker.addMovement(ev);
        final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                resetScroller();
                mVelocityTracker.clear();
                mScroller.abortAnimation();
                mLastTouchY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mTouchRemainderY = 0;
                break;

            case MotionEvent.ACTION_MOVE: {
                final int index = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (index < 0) {
                    Log.e(TAG, "onInterceptTouchEvent could not find pointer with id " +
                            mActivePointerId + " - did StaggeredGridView receive an inconsistent " +
                            "event stream?");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, index);
                final float dy = y - mLastTouchY + mTouchRemainderY;
                final int deltaY = (int) dy;
                mTouchRemainderY = dy - deltaY;

                if (Math.abs(dy) > mTouchSlop) {
                    mTouchMode = TOUCH_MODE_DRAGGING;
                }

                if (mTouchMode == TOUCH_MODE_DRAGGING) {
                    mLastTouchY = y;
                    if (!trackMotionScroll(deltaY, true)) {
                        // Break fling velocity if we impacted an edge.
                        mVelocityTracker.clear();
                    }
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mTouchMode = TOUCH_MODE_IDLE;
                break;
            }

            case MotionEvent.ACTION_UP: {
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                final float velocity = VelocityTrackerCompat.getYVelocity(mVelocityTracker,
                        mActivePointerId);
                if (Math.abs(velocity) > mFlingVelocity) {
                    mTouchMode = TOUCH_MODE_FLINGING;
                    resetScroller();
                    mScroller.fling(0, 0, 0, (int) velocity, 0, 0,
                            Integer.MIN_VALUE, Integer.MAX_VALUE);
                    mLastTouchY = 0;
                    ViewCompat.postInvalidateOnAnimation(this);
                } else {
                    mTouchMode = TOUCH_MODE_IDLE;
                }
            }
            break;
        }

        return true;
    }

    private void resetScroller() {
        mTouchMode = TOUCH_MODE_IDLE;
        mTopEdge.finish();
        mBottomEdge.finish();
        mScroller.abortAnimation();
    }

    @Override
    public boolean dispatchDragEvent(DragEvent event) {
        if (!isDragReorderingSupported()) {
            // If the consumer of this StaggeredGridView has not registered a ReorderListener,
            // don't bother handling drag events.
            return super.dispatchDragEvent(event);
        }

        switch(event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                // Per bug 7071594, we won't be able to catch this event in onDragEvent,
                // so we'll handle the event as it is being dispatched on the way down.
                if (mReorderHelper.hasReorderListener() && mIsDragReorderingEnabled) {
                    final View child = getChildAtCoordinate(
                            mTouchDownForDragStartX, mTouchDownForDragStartY);
                    if (child != null) {
                        // Child can be null if the touch point is not on a child view, but is
                        // still within the bounds of this StaggeredGridView (i.e., margins
                        // between cells).
                        startDragging(child, mTouchDownForDragStartX, mTouchDownForDragStartY);
                        // We must return true in order to continue getting future
                        // {@link DragEvent}s.
                        return true;
                    }
                }
                // Be sure to return a value here instead of calling super.dispatchDragEvent()
                // which will unnecessarily dispatch to all the children (since the
                // {@link StaggeredGridView} handles all drag events for our purposes)
                return false;

            case DragEvent.ACTION_DROP:
            case DragEvent.ACTION_DRAG_ENDED:
                if (mDragState == ReorderUtils.DRAG_STATE_DRAGGING) {
                    handleDrop((int)event.getX(), (int)event.getY());
                }

                // Return early here to avoid calling super.dispatchDragEvent() which dispatches to
                // children (since this view already can handle all drag events). The super call
                // can also cause a NPE if the view hierarchy changed in the middle of a drag
                // and the {@link DragEvent} gets nulled out. This is a workaround for
                // a framework bug: 8298439.
                // Since the {@link StaggeredGridView} handles all drag events for our purposes,
                // just manually fire the drag event to ourselves.
                return onDragEvent(event);
        }

        // In all other cases, default to the superclass implementation. We need this so that
        // the drag/drop framework will fire off {@link #onDragEvent(DragEvent ev)} calls to us.
        return super.dispatchDragEvent(event);
    }

    @Override
    public boolean onDragEvent(DragEvent ev) {
        if (!isDragReorderingSupported()) {
            // If the consumer of this StaggeredGridView has not registered a ReorderListener,
            // don't bother handling drag events.
            return false;
        }

        final int x = (int)ev.getX();
        final int y = (int)ev.getY();

        switch(ev.getAction()) {
            case DragEvent.ACTION_DRAG_LOCATION:
                if (mDragState == ReorderUtils.DRAG_STATE_DRAGGING) {
                    handleDrag(x, y);
                    mLastTouchY = y;
                }

                // Kick off the scroll handler on the first drag location event,
                // if it's not already running
                if (!mIsDragScrollerRunning &&
                        // And if the distance traveled while dragging exceeds the touch slop
                        ((Math.abs(x - mTouchDownForDragStartX) >= 4 * mTouchSlop) ||
                        (Math.abs(y - mTouchDownForDragStartY) >= 4 * mTouchSlop))) {
                    // Set true because that the scroller is running now
                    mIsDragScrollerRunning = true;

                    if (mScrollHandler == null) {
                        mScrollHandler = getHandler();
                    }
                    mScrollHandler.postDelayed(mDragScroller, SCROLL_HANDLER_DELAY);
                }

                return true;

            case DragEvent.ACTION_DROP:
            case DragEvent.ACTION_DRAG_ENDED:
                // We can either expect to receive:
                // 1. Both {@link DragEvent#ACTION_DROP} and then
                //    {@link DragEvent#ACTION_DRAG_ENDED} if the drop is over this view.
                // 2. Only {@link DragEvent#ACTION_DRAG_ENDED} if the drop happened over a
                //    different view.
                // For this reason, we should always handle the drop. In case #1, if this code path
                // gets executed again then nothing will happen because we will have already
                // updated {@link #mDragState} to not be {@link ReorderUtils#DRAG_STATE_DRAGGING}.
                if (mScrollHandler != null) {
                    mScrollHandler.removeCallbacks(mDragScroller);
                    // Scroller is no longer running
                    mIsDragScrollerRunning = false;
                }

                return true;
            }

        return false;
    }

    /**
     *
     * @param deltaY Pixels that content should move by
     * @return true if the movement completed, false if it was stopped prematurely.
     */
    private boolean trackMotionScroll(int deltaY, boolean allowOverScroll) {
        final boolean contentFits = contentFits();
        final int allowOverhang = Math.abs(deltaY);
        final int overScrolledBy;
        final int movedBy;
        if (!contentFits) {
            int overhang;
            final boolean up;
            mPopulating = true;
            if (deltaY > 0) {
                overhang = fillUp(mFirstPosition - 1, allowOverhang);
                up = true;
            } else {
                overhang = fillDown(mFirstPosition + getChildCount(), allowOverhang);

                if (overhang < 0) {
                    // Overhang when filling down indicates how many pixels past the bottom of the
                    // screen has been filled in.  If this value is negative, it should be set to
                    // 0 so that we don't allow over scrolling.
                    overhang = 0;
                }

                up = false;
            }

            movedBy = Math.min(overhang, allowOverhang);
            offsetChildren(up ? movedBy : -movedBy);
            recycleOffscreenViews();
            mPopulating = false;
            overScrolledBy = allowOverhang - overhang;
        } else {
            overScrolledBy = allowOverhang;
            movedBy = 0;
        }

        if (allowOverScroll) {
            final int overScrollMode = ViewCompat.getOverScrollMode(this);

            if (overScrollMode == ViewCompat.OVER_SCROLL_ALWAYS ||
                    (overScrollMode == ViewCompat.OVER_SCROLL_IF_CONTENT_SCROLLS && !contentFits)) {

                if (overScrolledBy > 0) {
                    final EdgeEffectCompat edge = deltaY > 0 ? mTopEdge : mBottomEdge;
                    edge.onPull((float) Math.abs(deltaY) / getHeight());
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            }
        }

        awakenScrollBars(0 /* show immediately */, true /* invalidate */);
        return deltaY == 0 || movedBy != 0;
    }

    public final boolean contentFits() {
        if (mFirstPosition != 0 || getChildCount() != mItemCount) {
            return false;
        }

        int topmost = Integer.MAX_VALUE;
        int bottommost = Integer.MIN_VALUE;
        for (int i = 0; i < mColCount; i++) {
            if (mItemTops[i] < topmost) {
                topmost = mItemTops[i];
            }
            if (mItemBottoms[i] > bottommost) {
                bottommost = mItemBottoms[i];
            }
        }

        return topmost >= getPaddingTop() && bottommost <= getHeight() - getPaddingBottom();
    }

    /**
     * Recycle views within the range starting from startIndex (inclusive) until the last
     * attached child view.
     */
    private void recycleViewsInRange(int startIndex, int endIndex) {
        for (int i = endIndex; i >= startIndex; i--) {
            final View child = getChildAt(i);

            if (mInLayout) {
                removeViewsInLayout(i, 1);
            } else {
                removeViewAt(i);
            }

            mRecycler.addScrap(child);
        }
    }

    // TODO: Have other overloaded recycle methods call into this one so we would just have one
    // code path.
    private void recycleView(View view) {
        if (view == null) {
            return;
        }

        if (mInLayout) {
            removeViewInLayout(view);
            invalidate();
        } else {
            removeView(view);
        }

        mRecycler.addScrap(view);
    }

    /**
     * Important: this method will leave offscreen views attached if they
     * are required to maintain the invariant that child view with index i
     * is always the view corresponding to position mFirstPosition + i.
     */
    private void recycleOffscreenViews() {
        if (getChildCount() == 0) {
            return;
        }

        final int height = getHeight();
        final int clearAbove = -mItemMargin;
        final int clearBelow = height + mItemMargin;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            final View child = getChildAt(i);
            if (child.getTop() <= clearBelow)  {
                // There may be other offscreen views, but we need to maintain
                // the invariant documented above.
                break;
            }

            child.clearFocus();
            if (mInLayout) {
                removeViewsInLayout(i, 1);
            } else {
                removeViewAt(i);
            }

            mRecycler.addScrap(child);
        }

        while (getChildCount() > 0) {
            final View child = getChildAt(0);
            if (child.getBottom() >= clearAbove) {
                // There may be other offscreen views, but we need to maintain
                // the invariant documented above.
                break;
            }

            child.clearFocus();
            if (mInLayout) {
                removeViewsInLayout(0, 1);
            } else {
                removeViewAt(0);
            }

            mRecycler.addScrap(child);
            mFirstPosition++;
        }

        final int childCount = getChildCount();
        if (childCount > 0) {
            // Repair the top and bottom column boundaries from the views we still have
            Arrays.fill(mItemTops, Integer.MAX_VALUE);
            Arrays.fill(mItemBottoms, Integer.MIN_VALUE);
            for (int i = 0; i < childCount; i++){
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final int top = child.getTop() - mItemMargin;
                final int bottom = child.getBottom();
                LayoutRecord rec = mLayoutRecords.get(mFirstPosition + i);

                // It's possible the layout record could be null for visible views because
                // they are cleared between adapter data set changes, but the views are left
                // attached for the purpose of animations. Hence, populate the layout record again.
                if (rec == null) {
                    rec = recreateLayoutRecord(mFirstPosition + i, child, lp);
                }

                // In LTR layout, iterate across each column that this child is laid out in,
                // starting from the child's first column (lp.column).  For each column, update
                // mItemTops and mItemBottoms appropriately to take into account this child's
                // dimension.  In RTL layout, iterate in reverse, where the child's starting
                // column would start from the right-most.
                final int span = Math.min(mColCount, lp.span);
                for (int spanIndex = 0; spanIndex < span; spanIndex++) {
                    final int col = mIsRtlLayout ? lp.column - spanIndex :
                            lp.column + spanIndex;
                    final int colTop = top - rec.getMarginAbove(spanIndex);
                    final int colBottom = bottom + rec.getMarginBelow(spanIndex);
                    if (colTop < mItemTops[col]) {
                        mItemTops[col] = colTop;
                    }
                    if (colBottom > mItemBottoms[col]) {
                        mItemBottoms[col] = colBottom;
                    }
                }
            }

            for (int col = 0; col < mColCount; col++) {
                if (mItemTops[col] == Integer.MAX_VALUE) {
                    // If one was untouched, both were.
                    final int top = getPaddingTop();
                    mItemTops[col] = top;
                    mItemBottoms[col] = top;
                }
            }
        }

        mCurrentScrollState = getScrollState();
    }

    private LayoutRecord recreateLayoutRecord(int position, View child, LayoutParams lp) {
        final LayoutRecord rec = new LayoutRecord();
        mLayoutRecords.put(position, rec);
        rec.column = lp.column;
        rec.height = child.getHeight();
        rec.id = lp.id;
        rec.span = Math.min(mColCount, lp.span);
        return rec;
    }

    @Override
    public void computeScroll() {
        if (mTouchMode == TOUCH_MODE_OVERFLING) {
            handleOverfling();
        } else if (mScroller.computeScrollOffset()) {
            final int overScrollMode = ViewCompat.getOverScrollMode(this);
            final boolean supportsOverscroll = overScrollMode != ViewCompat.OVER_SCROLL_NEVER;
            final int y = mScroller.getCurrY();
            final int dy = (int) (y - mLastTouchY);
            // TODO: Figure out why mLastTouchY is being updated here. Consider using a new class
            // variable since this value does not represent the last place on the screen where a
            // touch occurred.
            mLastTouchY = y;
            // Check if the top of the motion view is where it is
            // supposed to be
            final View motionView = supportsOverscroll &&
                    getChildCount() > 0 ? getChildAt(0) : null;
            final int motionViewPrevTop = motionView != null ? motionView.getTop() : 0;
            final boolean stopped = !trackMotionScroll(dy, false);
            if (!stopped && !mScroller.isFinished()) {
                mTouchMode = TOUCH_MODE_IDLE;
                ViewCompat.postInvalidateOnAnimation(this);
            } else if (stopped && dy != 0 && supportsOverscroll) {
                    // Check to see if we have bumped into the scroll limit
                    if (motionView != null) {
                        final int motionViewRealTop = motionView.getTop();
                        // Apply overscroll
                        final int overscroll = -dy - (motionViewRealTop - motionViewPrevTop);
                        overScrollBy(0, overscroll, 0, getScrollY(), 0, 0, 0, mOverscrollDistance,
                                true);
                    }
                    final EdgeEffectCompat edge;
                    if (dy > 0) {
                        edge = mTopEdge;
                        mBottomEdge.finish();
                    } else {
                        edge = mBottomEdge;
                        mTopEdge.finish();
                    }
                    edge.onAbsorb(Math.abs((int) mScroller.getCurrVelocity()));
                    if (mScroller.computeScrollOffset()) {
                        mScroller.notifyVerticalEdgeReached(getScrollY(), 0, mOverscrollDistance);
                    }
                    mTouchMode = TOUCH_MODE_OVERFLING;
                    ViewCompat.postInvalidateOnAnimation(this);
            } else {
                mTouchMode = TOUCH_MODE_IDLE;
            }
        }
    }

    private void handleOverfling() {
        // If the animation is not finished yet, determine next steps.
        if (mScroller.computeScrollOffset()) {
            final int scrollY = getScrollY();
            final int currY = mScroller.getCurrY();
            final int deltaY = currY - scrollY;
            if (overScrollBy(0, deltaY, 0, scrollY, 0, 0, 0, mOverscrollDistance, false)) {
                final boolean crossDown = scrollY <= 0 && currY > 0;
                final boolean crossUp = scrollY >= 0 && currY < 0;
                if (crossDown || crossUp) {
                    int velocity = (int) mScroller.getCurrVelocity();
                    if (crossUp) {
                        velocity = -velocity;
                    }

                    // Don't flywheel from this; we're just continuing
                    // things.
                    mTouchMode = TOUCH_MODE_IDLE;
                    mScroller.abortAnimation();
                } else {
                    // Spring back! We are done overscrolling.
                    if (mScroller.springBack(0, scrollY, 0, 0, 0, 0)) {
                        mTouchMode = TOUCH_MODE_OVERFLING;
                        ViewCompat.postInvalidateOnAnimation(this);
                    } else {
                        // If already valid, we are done. Exit overfling mode.
                        mTouchMode = TOUCH_MODE_IDLE;
                    }
                }
            } else {
                // Still over-flinging; just post the next frame of the animation.
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            // Otherwise, exit overfling mode.
            mTouchMode = TOUCH_MODE_IDLE;
            mScroller.abortAnimation();
        }
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (getScrollY() != scrollY) {
            scrollTo(0, scrollY);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (mTopEdge != null) {
            boolean needsInvalidate = false;
            if (!mTopEdge.isFinished()) {
                final int restoreCount = canvas.save();
                canvas.translate(0, 0);
                mTopEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
                needsInvalidate = true;
            }
            if (!mBottomEdge.isFinished()) {
                final int restoreCount = canvas.save();
                final int width = getWidth();
                canvas.translate(-width, getHeight());
                canvas.rotate(180, width, 0);
                mBottomEdge.draw(canvas);
                canvas.restoreToCount(restoreCount);
                needsInvalidate = true;
            }

            if (needsInvalidate) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        }
    }

    public void beginFastChildLayout() {
        mFastChildLayout = true;
    }

    public void endFastChildLayout() {
        mFastChildLayout = false;
        populate();
    }

    @Override
    public void requestLayout() {
        if (!mPopulating && !mFastChildLayout) {
            super.requestLayout();
        }
    }

    /**
     * Sets the view to show if the adapter is empty
     */
    public void setEmptyView(View emptyView) {
        mEmptyView = emptyView;

        updateEmptyStatus();
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    /**
     * Update the status of the list based on the whether the adapter is empty.  If is it empty and
     * we have an empty view, display it.  In all the other cases, make sure that the
     * StaggeredGridView is VISIBLE and that the empty view is GONE (if it's not null).
     */
    private void updateEmptyStatus() {
        if (mAdapter == null || mAdapter.isEmpty()) {
            if (mEmptyView != null) {
                mEmptyView.setVisibility(View.VISIBLE);
                setVisibility(View.GONE);
            } else {
                setVisibility(View.VISIBLE);
            }
        } else {
            if (mEmptyView != null) {
                mEmptyView.setVisibility(View.GONE);
            }
            setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY) {
            Log.d(TAG, "onMeasure: must have an exact width or match_parent! " +
                    "Using fallback spec of EXACTLY " + widthSize);
            widthMode = MeasureSpec.EXACTLY;
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            Log.d(TAG, "onMeasure: must have an exact height or match_parent! " +
                    "Using fallback spec of EXACTLY " + heightSize);
            heightMode = MeasureSpec.EXACTLY;
        }

        setMeasuredDimension(widthSize, heightSize);

        if (mColCountSetting == COLUMN_COUNT_AUTO) {
            final int colCount = widthSize / mMinColWidth;
            if (colCount != mColCount) {
                mColCount = colCount;
            }
        }

        if (mHorizontalReorderingAreaSize == 0) {
            if (mColCount > 1) {
                final int totalMarginWidth = mItemMargin * (mColCount + 1);
                final int singleViewWidth = (widthSize - totalMarginWidth) / mColCount;
                mHorizontalReorderingAreaSize = singleViewWidth / CHILD_TO_REORDER_AREA_RATIO;
            } else {
                mHorizontalReorderingAreaSize = SINGLE_COL_REORDERING_AREA_SIZE;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mIsRtlLayout = isLayoutRtl();

        mInLayout = true;
        populate();
        mInLayout = false;
        final int width = r - l;
        final int height = b - t;
        mTopEdge.setSize(width, height);
        mBottomEdge.setSize(width, height);
    }

    private void populate() {
        if (getWidth() == 0 || getHeight() == 0 || mAdapter == null) {
            return;
        }

        if (mColCount == COLUMN_COUNT_AUTO) {
            final int colCount = getWidth() / mMinColWidth;
            if (colCount != mColCount) {
                mColCount = colCount;
            }
        }

        final int colCount = mColCount;
        if (mItemTops == null || mItemBottoms == null || mItemTops.length != colCount ||
                mItemBottoms.length != colCount) {
            mItemTops = new int[colCount];
            mItemBottoms = new int[colCount];

            mLayoutRecords.clear();
            if (mInLayout) {
                removeAllViewsInLayout();
            } else {
                removeAllViews();
            }
        }

        // Before we do layout, if there are any pending animations and data has changed,
        // cancel the animation, as layout on new data will likely trigger another animation
        // set to be run.
        if (mDataChanged && mCurrentRunningAnimatorSet != null) {
            mCurrentRunningAnimatorSet.cancel();
            mCurrentRunningAnimatorSet = null;
        }

        if (isSelectionAtTop()) {
            mCurrentScrollState = null;
        }

        if (mCurrentScrollState != null) {
            restoreScrollPosition(mCurrentScrollState);
        } else {
            calculateLayoutStartOffsets(getPaddingTop() /* layout start offset */);
        }

        mPopulating = true;

        mFocusedChildIdToScrollIntoView = -1;
        final View focusedChild = getFocusedChild();
        if (focusedChild != null) {
            final LayoutParams lp = (LayoutParams) focusedChild.getLayoutParams();
            mFocusedChildIdToScrollIntoView = lp.id;
        }

        layoutChildren(mDataChanged);
        fillDown(mFirstPosition + getChildCount(), 0);
        fillUp(mFirstPosition - 1, 0);

        if (isDragReorderingSupported() &&
                mDragState == ReorderUtils.DRAG_STATE_RELEASED_REORDER ||
                mDragState == ReorderUtils.DRAG_STATE_RELEASED_HOVER) {
            // This child was dragged and dropped with the UI likely
            // still showing.  Call updateReorderStates, to update
            // all UI appropriately.
            mReorderHelper.clearDraggedChildId();
            updateReorderStates(ReorderUtils.DRAG_STATE_NONE);
        }

        if (mDataChanged) {
            // Animation should only play if data has changed since populate() can be called
            // multiple times with the same data set (e.g., screen size changed).
            handleLayoutAnimation();
        }

        recycleOffscreenViews();

        mPopulating = false;
        mDataChanged = false;
    }

    @Override
    public void scrollBy(int x, int y) {
        if (y != 0) {
            // TODO: Implement smooth scrolling for this so that scrolling does more than just
            // jumping by y pixels.
            trackMotionScroll(y, false /* over scroll */);
        }
    }

    private void offsetChildren(int offset) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            child.offsetTopAndBottom(offset);

            // As we're scrolling, we need to make sure the children that are coming into view
            // have their reordering area set.
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            setReorderingArea(lp);
        }

        final int colCount = mColCount;
        for (int i = 0; i < colCount; i++) {
            mItemTops[i] += offset;
            mItemBottoms[i] += offset;
        }

        if (mScrollListener != null) {
            mScrollListener.onScrollChanged(offset, computeVerticalScrollOffset(),
                    computeVerticalScrollRange());
        }
    }

    /**
     * Performs layout animation of child views.
     * @throws IllegalStateException Exception is thrown of currently set animation mode is
     * not recognized.
     */
    private void handleLayoutAnimation() throws IllegalStateException {
        final List<Animator> animators = new ArrayList<Animator>();

        // b/8422632 - Without this dummy first animator, startDelays of subsequent animators won't
        // be honored correctly; all animators will block regardless of startDelay until the first
        // animator in the AnimatorSet truly starts playing.
        final ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(0);
        animators.add(anim);

        addOutAnimatorsForStaleViews(animators, mAnimationOutMode);

        // Play the In animators at a slight delay after all Out animators have started.
        final int animationInStartDelay = animators.size() > 0 ?
                (SgvAnimationHelper.getDefaultAnimationDuration() / 2) : 0;
        addInAnimators(animators, mAnimationInMode, animationInStartDelay);

        if (animators != null && animators.size() > 0) {
            final AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animators);
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mIsCurrentAnimationCanceled = false;
                    mCurrentRunningAnimatorSet = animatorSet;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    mIsCurrentAnimationCanceled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!mIsCurrentAnimationCanceled) {
                        // If this animation ended naturally, not because it was canceled, then
                        // reset the animation mode back to ANIMATION_MODE_NONE.  However, if
                        // the animation was canceled by a data change, then keep the mode as is,
                        // so that on a re-layout, we can resume animation from the views' current
                        // positions.
                        resetAnimationMode();
                    }
                    mCurrentRunningAnimatorSet = null;
                }
            });

            Log.v(TAG, "starting");
            animatorSet.start();
        } else {
            resetAnimationMode();
        }

        mViewsToAnimateOut.clear();
        mChildRectsForAnimation.clear();
    }

    /**
     * Reset the current animation mode.
     */
    private void resetAnimationMode() {
        mAnimationInMode = AnimationIn.NONE;
        mAnimationOutMode = AnimationOut.NONE;
    }

    /**
     * Add animators for animating in new views as well as updating positions of views that
     * should remain on screen.
     */
    private void addInAnimators(List<Animator> animators, AnimationIn animationInMode,
            int startDelay) {
        if (animationInMode == AnimationIn.NONE) {
            return;
        }

        switch (animationInMode) {
            case FLY_UP_ALL_VIEWS:
                addFlyInAllViewsAnimators(animators);
                break;

            case EXPAND_NEW_VIEWS:
                addUpdateViewPositionsAnimators(animators, true /* cascade animation */,
                        AnimationIn.EXPAND_NEW_VIEWS, startDelay);
                break;

            case EXPAND_NEW_VIEWS_NO_CASCADE:
                addUpdateViewPositionsAnimators(animators, false /* cascade animation */,
                        AnimationIn.EXPAND_NEW_VIEWS_NO_CASCADE, startDelay);
                break;

            case SLIDE_IN_NEW_VIEWS:
                addUpdateViewPositionsAnimators(animators, true /* cascade animation */,
                        AnimationIn.SLIDE_IN_NEW_VIEWS, startDelay);
                break;

            case FLY_IN_NEW_VIEWS:
                addUpdateViewPositionsAnimators(animators, true /* cascade animation */,
                        AnimationIn.FLY_IN_NEW_VIEWS, startDelay);
                break;

            case FADE:
                addUpdateViewPositionsAnimators(animators, true /* cascade animation */,
                        AnimationIn.FADE, startDelay);
                break;

            default:
                throw new IllegalStateException("Unknown animationInMode: " + mAnimationInMode);
        }
    }

    /**
     * Add animators for animating out stale views
     * @param animationOutMode The animation mode to play for stale views
     */
    private void addOutAnimatorsForStaleViews(List<Animator> animators,
            AnimationOut animationOutMode) {
        if (animationOutMode == AnimationOut.NONE) {
            return;
        }

        for (final View v : mViewsToAnimateOut) {
            // For each stale view to animate out, retrieve the animators for the view, then attach
            // the StaleViewAnimationEndListener which checks to see if the view should be recycled
            // at the end of the animation.
            final List<Animator> viewAnimators = new ArrayList<Animator>();

            switch (animationOutMode) {
                case SLIDE:
                    final LayoutParams lp = (LayoutParams) v.getLayoutParams();
                    // Bias towards sliding right, but depending on the column that this view
                    // is laid out in, slide towards the nearest side edge.
                    int endTranslation = (int)(v.getWidth() * 1.5);
                    if (lp.column < (mColCount / 2)) {
                        endTranslation = -endTranslation;
                    }
                    SgvAnimationHelper.addSlideOutAnimators(viewAnimators, v,
                            (int) v.getTranslationX(), endTranslation);
                    break;

                case COLLAPSE:
                    SgvAnimationHelper.addCollapseOutAnimators(viewAnimators, v);
                    break;

                case FLY_DOWN:
                    SgvAnimationHelper.addFlyOutAnimators(viewAnimators, v,
                            (int) v.getTranslationY(), getHeight());
                    break;

                case FADE:
                    SgvAnimationHelper.addFadeAnimators(viewAnimators, v, v.getAlpha(),
                            0 /* end alpha */);
                    break;

                default:
                    throw new IllegalStateException("Unknown animationOutMode: " +
                            animationOutMode);
            }

            if (viewAnimators.size() > 0) {
                addStaleViewAnimationEndListener(v, viewAnimators);
                animators.addAll(viewAnimators);
            }
        }
    }

    /**
     * Handle setting up the animators of child views when the animation is invoked by a change
     * in the adapter.  This method has a side effect of translating view positions in preparation
     * for the animations.
     */
    private List<Animator> addFlyInAllViewsAnimators(List<Animator> animators) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }

        if (animators == null) {
            animators = new ArrayList<Animator>();
        }

        for (int i = 0; i < childCount; i++) {
            final int animationDelay = i * ANIMATION_DELAY_IN_MS;
            final View childToAnimate = getChildAt(i);

            // Start all views from below the bottom of this grid and animate them upwards. This
            // is done simply by translating the current view's vertical position by the height
            // of the entire grid.
            float yTranslation = getHeight();
            float rotation = SgvAnimationHelper.ANIMATION_ROTATION_DEGREES;
            if (mIsCurrentAnimationCanceled) {
                // If mIsAnimationCanceled is true, then this is not the first time that this
                // animation is running.  For this particular case, we should resume from where
                // the previous animation left off, rather than resetting translation and rotation.
                yTranslation = childToAnimate.getTranslationY();
                rotation = childToAnimate.getRotation();
            }

            SgvAnimationHelper.addTranslationRotationAnimators(animators, childToAnimate,
                    0 /* xTranslation */, (int) yTranslation, rotation, animationDelay);
        }

        return animators;
    }

    /**
     * Animations to update the views on screen to their new positions.  For new views that aren't
     * currently on screen, animate them in using the specified animationInMode.
     */
    private List<Animator> addUpdateViewPositionsAnimators(List<Animator> animators,
            boolean cascadeAnimation, AnimationIn animationInMode, int startDelay) {
        final int childCount = getChildCount();
        if (childCount == 0) {
            return null;
        }

        if (animators == null) {
            animators = new ArrayList<Animator>();
        }

        int viewsAnimated = 0;
        for (int i = 0; i < childCount; i++) {
            final View childToAnimate = getChildAt(i);

            if (mViewsToAnimateOut.contains(childToAnimate)) {
                // If the stale views are still animating, then they are still laid out, so
                // getChildCount() would've accounted for them.  Since they have their own set
                // of animations to play, we'll skip over them in this loop.
                continue;
            }

            // Use progressive animation delay to create the staggered effect of animating
            // views.  This is done by having each view delay their animation by
            // ANIMATION_DELAY_IN_MS after the animation of the previous view.
            int animationDelay = startDelay +
                    (cascadeAnimation ? viewsAnimated * ANIMATION_DELAY_IN_MS : 0);

            // Figure out whether a view with this item ID existed before
            final LayoutParams lp = (LayoutParams) childToAnimate.getLayoutParams();

            final ViewRectPair viewRectPair = mChildRectsForAnimation.get(lp.id);

            final int xTranslation;
            final int yTranslation;

            // If there is a valid {@link Rect} for the view with this newId, then
            // setup an animation.
            if (viewRectPair != null && viewRectPair.rect != null) {
                // In the special case where the items are explicitly fading, we don't want to do
                // any of the translations.
                if (animationInMode == AnimationIn.FADE) {
                    SgvAnimationHelper.addFadeAnimators(animators, childToAnimate,
                            0 /* start alpha */, 1.0f /* end alpha */, animationDelay);
                    continue;
                }

                final Rect oldRect = viewRectPair.rect;
                // Since the view already exists, translate it to its new position.
                // Reset the child back to its previous position given by oldRect if the child
                // has not already been translated.  If the child has been translated, use the
                // current translated values, as this child may be in the middle of a previous
                // animation, so we don't want to simply force it to new location.

                xTranslation = oldRect.left - childToAnimate.getLeft();
                yTranslation = oldRect.top - childToAnimate.getTop();
                final float rotation = childToAnimate.getRotation();

                // First set the translation X and Y. The current translation might be out of date.
                childToAnimate.setTranslationX(xTranslation);
                childToAnimate.setTranslationY(yTranslation);

                if (xTranslation == 0 && yTranslation == 0 && rotation == 0) {
                    // Bail early if this view doesn't need to be translated.
                    continue;
                }

                SgvAnimationHelper.addTranslationRotationAnimators(animators, childToAnimate,
                        xTranslation, yTranslation, rotation, animationDelay);
            } else {
                // If this view was not present before the data updated, rather than just flashing
                // the view into its designated position, fly it up from the bottom.
                xTranslation = 0;
                yTranslation = (animationInMode == AnimationIn.FLY_IN_NEW_VIEWS) ? getHeight() : 0;

                // Since this is a new view coming in, add additional delays so that these IN
                // animations start after all the OUT animations have been played.
                animationDelay += SgvAnimationHelper.getDefaultAnimationDuration();

                childToAnimate.setTranslationX(xTranslation);
                childToAnimate.setTranslationY(yTranslation);

                switch (animationInMode) {
                    case FLY_IN_NEW_VIEWS:
                        SgvAnimationHelper.addTranslationRotationAnimators(animators,
                                childToAnimate, xTranslation, yTranslation,
                                SgvAnimationHelper.ANIMATION_ROTATION_DEGREES, animationDelay);
                        break;

                    case SLIDE_IN_NEW_VIEWS:
                        // Bias towards sliding right, but depending on the column that this view
                        // is laid out in, slide towards the nearest side edge.
                        int startTranslation = (int)(childToAnimate.getWidth() * 1.5);
                        if (lp.column < (mColCount / 2)) {
                            startTranslation = -startTranslation;
                        }

                        SgvAnimationHelper.addSlideInFromRightAnimators(animators,
                                childToAnimate, startTranslation,
                                animationDelay);
                        break;

                    case EXPAND_NEW_VIEWS:
                    case EXPAND_NEW_VIEWS_NO_CASCADE:
                        if (i == 0) {
                            // Initially set the alpha of this view to be invisible, then fade in.
                            childToAnimate.setAlpha(0);

                            // Create animators that translate the view back to translation = 0
                            // which would be its new layout position
                            final int offset = -1 * childToAnimate.getHeight();
                            SgvAnimationHelper.addXYTranslationAnimators(animators,
                                    childToAnimate, 0 /* xTranslation */, offset, animationDelay);

                            SgvAnimationHelper.addFadeAnimators(animators, childToAnimate,
                                    0 /* start alpha */, 1.0f /* end alpha */, animationDelay);
                        } else {
                            SgvAnimationHelper.addExpandInAnimators(animators,
                                    childToAnimate, animationDelay);
                        }
                        break;
                    case FADE:
                        SgvAnimationHelper.addFadeAnimators(animators, childToAnimate,
                                0 /* start alpha */, 1.0f /* end alpha */, animationDelay);
                        break;

                    default:
                        continue;
                }
            }

            viewsAnimated++;
        }

        return animators;
    }

    private void addStaleViewAnimationEndListener(final View view, List<Animator> viewAnimators) {
        if (viewAnimators == null) {
            return;
        }

        for (final Animator animator : viewAnimators) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // In the event that onChanged is called before this animation finishes,
                    // we would have mistakenly cached a view that would be recycled.  So
                    // check if it's there, and remove it so that obtainView() doesn't
                    // accidentally use the cached view later when it's already been
                    // moved to the recycler.
                    final LayoutParams lp = (LayoutParams) view.getLayoutParams();
                    if (mChildRectsForAnimation.containsKey(lp.id)) {
                        mChildRectsForAnimation.remove(lp.id);
                    }

                    recycleView(view);
                }
            });
        }
    }

    /**
     * Calculate and cache the {@link LayoutRecord}s for all positions up to mFirstPosition.
     * mFirstPosition is the position that layout will start from, but we need to know where all
     * views preceding it will be laid out so that mFirstPosition will be laid out at the correct
     * position.  If this is not done, mFirstPosition will be laid out at the first empty space
     * possible (i.e., top left), and this may not be the correct position in the overall layout.
     *
     * This can be optimized if we don't need to guard against jagged edges in the grid or if
     * mFirstChangedPosition is set to a non-zero value (so we can skip calculating some views).
     */
    private void calculateLayoutStartOffsets(int offset) {
        // Bail early if we don't guard against jagged edges or if nothing has changed before
        // mFirstPosition.
        // Also check that we're not at the top of the list because sometimes grid padding isn't set
        // until after mItemTops and mItemBottoms arrays have been initialized, so we should
        // go through and compute the right layout start offset for mFirstPosition = 0.
        if (mFirstPosition != 0 &&
                (!mGuardAgainstJaggedEdges || mFirstPosition < mFirstChangedPosition)) {
            // At this time, we know that mItemTops should be the same, because
            // nothing has changed before view at mFirstPosition. The only thing
            // we need to do is to reset mItemBottoms. The result should be the
            // same, if we don't bail early and execute the following code
            // again. Notice that mItemBottoms always equal to mItemTops after
            // this method.
            System.arraycopy(mItemTops, 0, mItemBottoms, 0, mColCount);
            return;
        }

        final int colWidth = (getWidth() - getPaddingLeft() - getPaddingRight() -
                mItemMargin * (mColCount - 1)) / mColCount;

        Arrays.fill(mItemTops, getPaddingTop());
        Arrays.fill(mItemBottoms, getPaddingTop());

        // Since we will be doing a pass to calculate all views up to mFirstPosition, it is likely
        // that all existing {@link LayoutRecord}s will be stale, so clear it out to avoid
        // accidentally the re-use of stale values.
        //
        // Note: We cannot just invalidate all layout records after mFirstPosition because it is
        // possible that this layout pass is caused by a down sync from the server that may affect
        // the layout of views from position 0 to mFirstPosition - 1.
        if (mDataChanged) {
            mLayoutRecords.clear();
        }

        for (int i = 0; i < mFirstPosition; i++) {
            LayoutRecord rec = mLayoutRecords.get(i);

            if (mDataChanged || rec == null) {
                final View view = obtainView(i, null);
                final LayoutParams lp = (LayoutParams) view.getLayoutParams();

                final int heightSpec;
                if (lp.height == LayoutParams.WRAP_CONTENT) {
                    heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                } else {
                    heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
                }

                final int span = Math.min(mColCount, lp.span);
                final int widthSize = colWidth * span + mItemMargin * (span - 1);
                final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);

                view.measure(widthSpec, heightSpec);
                final int height = view.getMeasuredHeight();

                if (rec == null) {
                    rec = new LayoutRecord();
                    mLayoutRecords.put(i, rec);
                }

                rec.height = height;
                rec.id = lp.id;
                rec.span = span;

                // We're not actually using this view, so add this back to the recycler.
                mRecycler.addScrap(view);
            }

            int nextColumn = getNextColumnDown();

            // Given the span, check if there's enough space to put this view at this column.
            // IMPORTANT Use the same logic in {@link #layoutChildren}.
            if (rec.span > 1) {
                if (mIsRtlLayout) {
                    if (nextColumn + 1 < rec.span) {
                        nextColumn = mColCount - 1;
                    }
                } else {
                    if (mColCount - nextColumn < rec.span) {
                        nextColumn = 0;
                    }
                }
            }
            rec.column = nextColumn;

            // Place the top of this child beneath the last by finding the lowest coordinate across
            // the columns that this child will span.  For LTR layout, we scan across from left to
            // right, and for RTL layout, we scan from right to left.
            // TODO: Consolidate this logic with getNextRecordDown() in the future, as that method
            // already calculates the margins for us.  This will keep the implementation consistent
            // with layoutChildren(), fillUp() and fillDown().
            int lowest = mItemBottoms[nextColumn] + mItemMargin;
            if (rec.span > 1) {
                for (int spanIndex = 0; spanIndex < rec.span; spanIndex++) {
                    final int index = mIsRtlLayout ? nextColumn - spanIndex :
                            nextColumn + spanIndex;
                    final int bottom = mItemBottoms[index] + mItemMargin;
                    if (bottom > lowest) {
                        lowest = bottom;
                    }
                }
            }

            for (int spanIndex = 0; spanIndex < rec.span; spanIndex++) {
                final int col = mIsRtlLayout ? nextColumn - spanIndex : nextColumn + spanIndex;
                mItemBottoms[col] = lowest + rec.height;

                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, " position: " + i + " bottoms: ");
                    for (int j = 0; j < mColCount; j++) {
                        Log.v(TAG, "    mItemBottoms["+j+"]: " + mItemBottoms[j]);
                    }
                }
            }
        }

        // mItemBottoms[] at this point contains the values of all views up to mFirstPosition.  To
        // figure out where view at mFirstPosition will be laid out, we'll need to find the column
        // that is the highest (i.e., i where mItemBottoms[i] <= mItemBottoms[j] for all j
        // from 0 to mColCount.)
        int highestValue = Integer.MAX_VALUE;
        for (int k = 0; k < mColCount; k++) {
            if (mItemBottoms[k] < highestValue) {
                highestValue = mItemBottoms[k];
            }
        }

        // Adjust the offsets in each column so that values in mItemTops[] and mItemBottoms[]
        // reflect coordinates on screen.  These offsets will be the actual values where layout
        // will start from, otherwise, we'd naively start at (leftPadding, topPadding) for
        // mFirstPosition.
        for (int k = 0; k < mColCount; k++) {
            mItemBottoms[k] = mItemBottoms[k] - highestValue + offset;
            mItemTops[k] = mItemBottoms[k];

            // Log.v(TAG, "Adjusting to offset = mItemBottoms[" + k + "]: " + mItemBottoms[k]);
        }
    }

    /**
     * Measure and layout all currently visible children.
     *
     * @param queryAdapter true to requery the adapter for view data
     */
    final void layoutChildren(boolean queryAdapter) {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int itemMargin = mItemMargin;
        final int availableWidth = (getWidth() - paddingLeft - paddingRight - itemMargin
                * (mColCount - 1));
        final int colWidth = availableWidth / mColCount;
        // The availableWidth may not be divisible by mColCount. Keep the
        // remainder. It will be added to the width of the last view in the row.
        final int remainder = availableWidth % mColCount;

        boolean viewsRemovedInLayout = false;

        // If we're animating out stale views, then we want to defer recycling of views.
        final boolean deferRecyclingForAnimation = mAnimationOutMode != AnimationOut.NONE;

        if (!deferRecyclingForAnimation) {
            final int childCount = getChildCount();
            // If the latest data set has fewer data items than mFirstPosition, don't keep any
            // views on screen, and just let the layout logic below retrieve appropriate views
            // from the recycler.
            final int viewsToKeepOnScreen = (mItemCount <= mFirstPosition) ? 0 :
                mItemCount - mFirstPosition;

            if (childCount > viewsToKeepOnScreen) {
                // If there are more views laid out than the number of data items remaining to be
                // laid out, recycle the extraneous views.
                recycleViewsInRange(viewsToKeepOnScreen, childCount - 1);
                viewsRemovedInLayout = true;
            }
        } else {
            mViewsToAnimateOut.clear();
        }

        for (int i = 0; i < getChildCount(); i++) {
            final int position = mFirstPosition + i;
            View child = getChildAt(i);

            final int highestAvailableLayoutPosition = mItemBottoms[getNextColumnDown()];
            if (deferRecyclingForAnimation &&
                    (position >= mItemCount || highestAvailableLayoutPosition >= getHeight())) {
                // For the remainder of views on screen, they should not be on screen, so we can
                // skip layout.  Add them to the list of views to animate out.
                // We should only get in this position if deferRecyclingForAnimation = true,
                // otherwise, we should've recycled all views before getting into this layout loop.
                mViewsToAnimateOut.add(child);
                continue;
            }

            LayoutParams lp = null;
            int col = -1;

            if (child != null) {
                lp = (LayoutParams) child.getLayoutParams();
                col = lp.column;
            }

            final boolean needsLayout = queryAdapter || child == null || child.isLayoutRequested();
            if (queryAdapter) {
                View newView = null;
                if (deferRecyclingForAnimation) {
                    // If we are deferring recycling for animation, then we don't want to pass the
                    // current child in to obtainView for re-use.  obtainView() in this case should
                    // try to find the view belonging to this item on screen, or populate a fresh
                    // one from the recycler.
                    newView = obtainView(position);
                } else {
                    newView = obtainView(position, child);
                }

                // Update layout params since they may have changed
                lp = (LayoutParams) newView.getLayoutParams();

                if (newView != child) {
                    if (child != null && !deferRecyclingForAnimation) {
                        mRecycler.addScrap(child);
                        removeViewInLayout(child);
                        viewsRemovedInLayout = true;
                    }

                    // If this view is already in the layout hierarchy, we can just detach it
                    // from the parent and re-attach it at the correct index.  If the view has
                    // already been removed from the layout hierarchy, getParent() == null.
                    if (newView.getParent() == this) {
                        detachViewFromParent(newView);
                        attachViewToParent(newView, i, lp);
                    } else {
                        addViewInLayout(newView, i, lp);
                    }
                }

                child = newView;

                // Since the data has changed, we need to make sure the next child is in the
                // right column. We choose the next column down (vs. next column up) because we
                // are filling from the top of the screen downwards as we iterate through
                // visible children. (We take span into account below.)
                lp.column = getNextColumnDown();
                col = lp.column;
            }

            setReorderingArea(lp);

            final int span = Math.min(mColCount, lp.span);

            // Given the span, check if there's enough space to put this view at this column.
            // IMPORTANT Propagate the same logic to {@link #calculateLayoutStartOffsets}.
            if (span > 1) {
                if (mIsRtlLayout) {
                    // For RTL layout, if the current column index is less than the span of the
                    // child, then we know that there is not enough room remaining to lay this
                    // child out (e.g., if col == 0, but span == 2, then laying this child down
                    // at column = col would put us out of bound into a negative column index.).
                    // For this scenario, reset the index back to the right-most column, and lay
                    // out the child at this position where we can ensure that we can display as
                    // much of the child as possible.
                    if (col + 1 < span) {
                        col = mColCount - 1;
                    }
                } else {
                    if (mColCount - col < span) {
                        // If not, reset the col to 0.
                        col = 0;
                    }
                }

                lp.column = col;
            }

            int widthSize = (colWidth * span + itemMargin * (span - 1));
            // If it is rtl, we layout the view from col to col - span +
            // 1. If it reaches the most left column, i.e. we added the
            // additional width. So the check it span == col +1
            if ((mIsRtlLayout && span == col + 1)
                    || (!mIsRtlLayout && span + col == mColCount)) {
                widthSize += remainder;
            }
            if (needsLayout) {
                final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);

                final int heightSpec;
                if (lp.height == LayoutParams.WRAP_CONTENT) {
                    heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                } else {
                    heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
                }

                child.measure(widthSpec, heightSpec);
            }

            // Place the top of this child beneath the last by finding the lowest coordinate across
            // the columns that this child will span.  For LTR layout, we scan across from left to
            // right, and for RTL layout, we scan from right to left.
            // TODO:  Consolidate this logic with getNextRecordDown() in the future, as that method
            // already calculates the margins for us.  This will keep the implementation consistent
            // with fillUp() and fillDown().
            int childTop = mItemBottoms[col] + mItemMargin;
            if (span > 1) {
                int lowest = childTop;
                for (int spanIndex = 0; spanIndex < span; spanIndex++) {
                    final int index = mIsRtlLayout ? col - spanIndex : col + spanIndex;
                    final int bottom = mItemBottoms[index] + mItemMargin;
                    if (bottom > lowest) {
                        lowest = bottom;
                    }
                }

                childTop = lowest;
            }

            final int childHeight = child.getMeasuredHeight();
            final int childBottom = childTop + childHeight;
            int childLeft = 0;
            int childRight = 0;
            if (mIsRtlLayout) {
                childRight = (getWidth() - paddingRight) -
                        (mColCount - col - 1) * (colWidth + itemMargin);
                childLeft = childRight - child.getMeasuredWidth();
            } else {
                childLeft = paddingLeft + col * (colWidth + itemMargin);
                childRight = childLeft + child.getMeasuredWidth();
            }

        /*    Log.v(TAG, "[layoutChildren] height: " + childHeight
                    + " top: " + childTop + " bottom: " + childBottom
                    + " left: " + childLeft
                    + " column: " + col
                    + " position: " + position
                    + " id: " + lp.id);
*/
            child.layout(childLeft, childTop, childRight, childBottom);
            if (lp.id == mFocusedChildIdToScrollIntoView) {
                child.requestFocus();
            }

            for (int spanIndex = 0; spanIndex < span; spanIndex++) {
                final int index = mIsRtlLayout ? col - spanIndex : col + spanIndex;
                mItemBottoms[index] = childBottom;
            }

            // Whether or not LayoutRecords may have already existed for the view at this position
            // on screen, we'll update it after we lay out to ensure that the LayoutRecord
            // has the most updated information about the view at this position.  We can be assured
            // that all views before those on screen (views with adapter position < mFirstPosition)
            // have the correct LayoutRecords because calculateLayoutStartOffsets() would have
            // set them appropriately.
            LayoutRecord rec = mLayoutRecords.get(position);
            if (rec == null) {
                rec = new LayoutRecord();
                mLayoutRecords.put(position, rec);
            }

            rec.column = lp.column;
            rec.height = childHeight;
            rec.id = lp.id;
            rec.span = span;
        }

        // It appears that removeViewInLayout() does not invalidate.  So if we make use of this
        // method during layout, we should invalidate explicitly.
        if (viewsRemovedInLayout || deferRecyclingForAnimation) {
            invalidate();
        }
    }

    /**
     * Set the reordering area for the child layout specified
     */
    private void setReorderingArea(LayoutParams childLayoutParams) {
        final boolean isLastColumn = childLayoutParams.column == (mColCount - 1);
        childLayoutParams.reorderingArea =
                mAdapter.getReorderingArea(childLayoutParams.position, isLastColumn);
    }

    final void invalidateLayoutRecordsBeforePosition(int position) {
        int endAt = 0;
        while (endAt < mLayoutRecords.size() && mLayoutRecords.keyAt(endAt) < position) {
            endAt++;
        }
        mLayoutRecords.removeAtRange(0, endAt);
    }

    final void invalidateLayoutRecordsAfterPosition(int position) {
        int beginAt = mLayoutRecords.size() - 1;
        while (beginAt >= 0 && mLayoutRecords.keyAt(beginAt) > position) {
            beginAt--;
        }
        beginAt++;
        mLayoutRecords.removeAtRange(beginAt + 1, mLayoutRecords.size() - beginAt);
    }

    /**
     * Before doing an animation, map the item IDs for the currently visible children to the
     * {@link Rect} that defines their position on the screen so a translation animation
     * can be applied to their new layout positions.
     */
    private void cacheChildRects() {
        final int childCount = getChildCount();
        mChildRectsForAnimation.clear();

        long originalDraggedChildId = -1;
        if (isDragReorderingSupported()) {
            originalDraggedChildId = mReorderHelper.getDraggedChildId();
            if (mCachedDragViewRect != null && originalDraggedChildId != -1) {
                // This child was dragged in a reordering operation.  Use the cached position
                // of where the drag event was released as the cached location.
                mChildRectsForAnimation.put(originalDraggedChildId,
                        new ViewRectPair(mDragView, mCachedDragViewRect));
                mCachedDragViewRect = null;
            }
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            Rect rect;
            if (lp.id != originalDraggedChildId) {
                final int childTop = (int) child.getY();
                final int childBottom = childTop + child.getHeight();
                final int childLeft = (int) child.getX();
                final int childRight = childLeft + child.getWidth();
                rect = new Rect(childLeft, childTop, childRight, childBottom);
                mChildRectsForAnimation.put(lp.id /* item id */, new ViewRectPair(child, rect));
            }
        }
    }

    /**
     * Should be called with mPopulating set to true
     *
     * @param fromPosition Position to start filling from
     * @param overhang the number of extra pixels to fill beyond the current top edge
     * @return the max overhang beyond the beginning of the view of any added items at the top
     */
    final int fillUp(int fromPosition, int overhang) {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int itemMargin = mItemMargin;
        final int availableWidth = (getWidth() - paddingLeft - paddingRight - itemMargin
                * (mColCount - 1));
        final int colWidth = availableWidth / mColCount;
        // The availableWidth may not be divisible by mColCount. Keep the
        // remainder. It will be added to the width of the last view in the row.
        final int remainder = availableWidth % mColCount;
        final int gridTop = getPaddingTop();
        final int fillTo = -overhang;
        int nextCol = getNextColumnUp();
        int position = fromPosition;

        while (nextCol >= 0 && mItemTops[nextCol] > fillTo && position >= 0) {
            final View child = obtainView(position, null);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (child.getParent() != this) {
                if (mInLayout) {
                    addViewInLayout(child, 0, lp);
                } else {
                    addView(child, 0);
                }
            }

            final int span = Math.min(mColCount, lp.span);

            LayoutRecord rec;
            if (span > 1) {
                rec = getNextRecordUp(position, span);
                nextCol = rec.column;
            } else {
                rec = mLayoutRecords.get(position);
            }

            boolean invalidateBefore = false;
            if (rec == null) {
                rec = new LayoutRecord();
                mLayoutRecords.put(position, rec);
                rec.column = nextCol;
                rec.span = span;
            } else if (span != rec.span) {
                rec.span = span;
                rec.column = nextCol;
                invalidateBefore = true;
            } else {
                nextCol = rec.column;
            }

            if (mHasStableIds) {
                rec.id = lp.id;
            }

            lp.column = nextCol;
            setReorderingArea(lp);

            int widthSize = colWidth * span + itemMargin * (span - 1);
            // If it is rtl, we layout the view from nextCol to nextCol - span +
            // 1. If it reaches the most left column, i.e. we added the
            // additional width. So the check it span == nextCol + 1
            if ((mIsRtlLayout && span == nextCol + 1)
                    || (!mIsRtlLayout && span + nextCol == mColCount)) {
                widthSize += remainder;
            }
            final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
            final int heightSpec;
            if (lp.height == LayoutParams.WRAP_CONTENT) {
                heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            } else {
                heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            }
            child.measure(widthSpec, heightSpec);

            final int childHeight = child.getMeasuredHeight();
            if (invalidateBefore || (childHeight != rec.height && rec.height > 0)) {
                invalidateLayoutRecordsBeforePosition(position);
            }
            rec.height = childHeight;

            // Iterate across each column that this child spans and add the margin calculated
            // for that column to mItemTops.  getMarginBelow() is expected to give us the correct
            // margin values at each column such that mItemTops ends up with a smooth edge across
            // the column spans.  We need to do this before actually laying down the child,
            // otherwise we risk overlapping one child over another.  mItemTops stores the top
            // index for where the next child should be laid out.  For RTL, we do the update
            // in reverse order.
            for (int i = 0; i < span; i++) {
                final int index = mIsRtlLayout ? nextCol - i : nextCol + i;
                mItemTops[index] += rec.getMarginBelow(i);
            }

            final int startFrom = mItemTops[nextCol];
            final int childBottom = startFrom;
            final int childTop = childBottom - childHeight;

            int childLeft = 0;
            int childRight = 0;
            // For LTR layout, the child's left is calculated as the
            // (column index from left) * (columnWidth plus item margins).
            // For RTL layout, the child's left is relative to its right, and its right coordinate
            // is calculated as the difference between the width of this grid and
            // (column index from right) * (columnWidth plus item margins).
            if (mIsRtlLayout) {
                childRight = (getWidth() - paddingRight) -
                        (mColCount - nextCol - 1) * (colWidth + itemMargin);
                childLeft = childRight - child.getMeasuredWidth();
            } else {
                childLeft = paddingLeft + nextCol * (colWidth + itemMargin);
                childRight = childLeft + child.getMeasuredWidth();
            }
            child.layout(childLeft, childTop, childRight, childBottom);

            Log.v(TAG, "[fillUp] position: " + position + " id: " + lp.id
                    + " childLeft: " + childLeft + " childTop: " + childTop
                    + " column: " + rec.column + " childHeight:" + childHeight);

            // Since we're filling up, once the child is laid out, update mItemTops again
            // to reflect the next available top value at this column.  This is simply the child's
            // top coordinates, minus any available margins set.  For LTR, we start at the column
            // that this child is laid out from (nextCol) and move right for span amount.  For RTL
            // layout, we start at the column that this child is laid out from and move left.
            for (int i = 0; i < span; i++) {
                final int index = mIsRtlLayout ? nextCol - i : nextCol + i;
                mItemTops[index] = childTop - rec.getMarginAbove(i) - itemMargin;
            }

            if (lp.id == mFocusedChildIdToScrollIntoView) {
                child.requestFocus();
            }

            nextCol = getNextColumnUp();
            mFirstPosition = position--;
        }

        int highestView = getHeight();
        for (int i = 0; i < mColCount; i++) {
            if (mItemTops[i] < highestView) {
                highestView = mItemTops[i];
            }
        }
        return gridTop - highestView;
    }

    /**
     * Should be called with mPopulating set to true
     *
     * @param fromPosition Position to start filling from
     * @param overhang the number of extra pixels to fill beyond the current bottom edge
     * @return the max overhang beyond the end of the view of any added items at the bottom
     */
    final int fillDown(int fromPosition, int overhang) {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int itemMargin = mItemMargin;
        final int availableWidth = (getWidth() - paddingLeft - paddingRight - itemMargin
                * (mColCount - 1));
        final int colWidth = availableWidth / mColCount;
        // The availableWidth may not be divisible by mColCount. Keep the
        // remainder. It will be added to the width of the last view in the row.
        final int remainder = availableWidth % mColCount;
        final int gridBottom = getHeight() - getPaddingBottom();
        final int fillTo = gridBottom + overhang;
        int nextCol = getNextColumnDown();
        int position = fromPosition;

        while (nextCol >= 0 && mItemBottoms[nextCol] < fillTo && position < mItemCount) {
            final View child = obtainView(position, null);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (child.getParent() != this) {
                if (mInLayout) {
                    addViewInLayout(child, -1, lp);
                } else {
                    addView(child);
                }
            }

            final int span = Math.min(mColCount, lp.span);

            LayoutRecord rec;
            if (span > 1) {
                rec = getNextRecordDown(position, span);
                nextCol = rec.column;
            } else {
                rec = mLayoutRecords.get(position);
            }

            boolean invalidateAfter = false;
            if (rec == null) {
                rec = new LayoutRecord();
                mLayoutRecords.put(position, rec);
                rec.column = nextCol;
                rec.span = span;
            } else if (span != rec.span) {
                rec.span = span;
                rec.column = nextCol;
                invalidateAfter = true;
            } else {
                nextCol = rec.column;
            }

            if (mHasStableIds) {
                rec.id = lp.id;
            }

            lp.column = nextCol;
            setReorderingArea(lp);


            int widthSize = colWidth * span + itemMargin * (span - 1);
            // If it is rtl, we layout the view from nextCol to nextCol - span +
            // 1. If it reaches the most left column, i.e. we added the
            // additional width. So the check it span == nextCol +1
            if ((mIsRtlLayout && span == nextCol + 1)
                    || (!mIsRtlLayout && span + nextCol == mColCount)) {
                widthSize += remainder;
            }
            final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
            final int heightSpec;
            if (lp.height == LayoutParams.WRAP_CONTENT) {
                heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            } else {
                heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            }
            child.measure(widthSpec, heightSpec);

            final int childHeight = child.getMeasuredHeight();
            if (invalidateAfter || (childHeight != rec.height && rec.height > 0)) {
                invalidateLayoutRecordsAfterPosition(position);
            }

            rec.height = childHeight;

            // Before laying out the child, we need to make sure mItemBottoms is updated with the
            // correct values such that there is a smooth edge across the child's span.
            // getMarginAbove() is expected to give us these values.  For LTR layout, we start at
            // nextCol, and update forward for the number of columns this child spans.  For RTL
            // layout, we start at nextCol and update backwards for the same number of columns.
            for (int i = 0; i < span; i++) {
                final int index = mIsRtlLayout ? nextCol - i : nextCol + i;
                mItemBottoms[index] += rec.getMarginAbove(i);
            }

            final int startFrom = mItemBottoms[nextCol];
            final int childTop = startFrom + itemMargin;
            final int childBottom = childTop + childHeight;
            int childLeft = 0;
            int childRight = 0;
            if (mIsRtlLayout) {
                childRight = (getWidth() - paddingRight) -
                        (mColCount - nextCol - 1) * (colWidth + itemMargin);
                childLeft = childRight - child.getMeasuredWidth();
            } else {
                childLeft = paddingLeft + nextCol * (colWidth + itemMargin);
                childRight = childLeft + child.getMeasuredWidth();
            }

            Log.v(TAG, "[fillDown] position: " + position + " id: " + lp.id
                    + " childLeft: " + childLeft + " childTop: " + childTop
                    + " column: " + rec.column + " childHeight:" + childHeight);

            child.layout(childLeft, childTop, childRight, childBottom);

            // Once we've laid down the child, update mItemBottoms again to reflect the next
            // available set of bottom values for the next child.
            for (int i = 0; i < span; i++) {
                final int index = mIsRtlLayout ? nextCol - i : nextCol + i;
                mItemBottoms[index] = childBottom + rec.getMarginBelow(i);
            }

            if (lp.id == mFocusedChildIdToScrollIntoView) {
                child.requestFocus();
            }

            nextCol = getNextColumnDown();
            position++;
        }

        int lowestView = 0;
        for (int i = 0; i < mColCount; i++) {
            final int index = mIsRtlLayout ? mColCount - (i + 1) : i;
            if (mItemBottoms[index] > lowestView) {
                lowestView = mItemBottoms[index];
            }
        }

        return lowestView - gridBottom;
    }

    /**
     * @return column that the next view filling upwards should occupy. This is the bottom-most
     *         position available for a single-column item.
     */
    final int getNextColumnUp() {
        int result = -1;
        int bottomMost = Integer.MIN_VALUE;

        final int colCount = mColCount;
        for (int i = colCount - 1; i >= 0; i--) {
            final int index = mIsRtlLayout ? colCount - (i + 1) : i;
            final int top = mItemTops[index];
            if (top > bottomMost) {
                bottomMost = top;
                result = index;
            }
        }

        return result;
    }

    /**
     * Return a LayoutRecord for the given position
     * @param position
     * @param span
     * @return
     */
    final LayoutRecord getNextRecordUp(int position, int span) {
        LayoutRecord rec = mLayoutRecords.get(position);
        if (rec == null || rec.span != span) {
            if (span > mColCount) {
                throw new IllegalStateException("Span larger than column count! Span:" + span
                        + " ColumnCount:" + mColCount);
            }
            rec = new LayoutRecord();
            rec.span = span;
            mLayoutRecords.put(position, rec);
        }
        int targetCol = -1;
        int bottomMost = Integer.MIN_VALUE;

        // For LTR layout, we start from the bottom-right corner upwards when we need to find the
        // NextRecordUp.  For RTL, we will start from bottom-left.
        final int colCount = mColCount;
        if (mIsRtlLayout) {
            for (int i = span - 1; i < colCount; i++) {
                int top = Integer.MAX_VALUE;
                for (int j = i; j > i - span; j--) {
                    final int singleTop = mItemTops[j];
                    if (singleTop < top) {
                        top = singleTop;
                    }
                }
                if (top > bottomMost) {
                    bottomMost = top;
                    targetCol = i;
                }
            }
        } else {
            for (int i = colCount - span; i >= 0; i--) {
                int top = Integer.MAX_VALUE;
                for (int j = i; j < i + span; j++) {
                    final int singleTop = mItemTops[j];
                    if (singleTop < top) {
                        top = singleTop;
                    }
                }
                if (top > bottomMost) {
                    bottomMost = top;
                    targetCol = i;
                }
            }
        }

        rec.column = targetCol;

        // Once we've found the target column for the view at this position, we update mItemTops
        // for all columns that this view will occupy.  We set the margin such that mItemTops is
        // equal for all columns in the view's span.  For LTR layout, we start at targetCol and
        // move right, and for RTL, we start at targetCol and move left.
        for (int i = 0; i < span; i++) {
            final int nextCol = mIsRtlLayout ? targetCol - i : targetCol + i;
            rec.setMarginBelow(i, mItemTops[nextCol] - bottomMost);
        }

        return rec;
    }

    /**
     * @return column that the next view filling downwards should occupy. This is the top-most
     *         position available.
     */
    final int getNextColumnDown() {
        int topMost = Integer.MAX_VALUE;
        int result = 0;
        final int colCount = mColCount;

        for (int i = 0; i < colCount; i++) {
            final int index = mIsRtlLayout ? colCount - (i + 1) : i;
            final int bottom = mItemBottoms[index];
            if (bottom < topMost) {
                topMost = bottom;
                result = index;
            }
        }

        return result;
    }

    final LayoutRecord getNextRecordDown(int position, int span) {
        LayoutRecord rec = mLayoutRecords.get(position);
        if (rec == null || rec.span != span) {
            if (span > mColCount) {
                throw new IllegalStateException("Span larger than column count! Span:" + span
                        + " ColumnCount:" + mColCount);
            }

            rec = new LayoutRecord();
            rec.span = span;
            mLayoutRecords.put(position, rec);
        }

        int targetCol = -1;
        int topMost = Integer.MAX_VALUE;

        final int colCount = mColCount;

        // For LTR layout, we start from the top-left corner and move right-downwards, when we
        // need to find the NextRecordDown.  For RTL we will start from Top-Right corner, and move
        // left-downwards.
        if (mIsRtlLayout) {
            for (int i = colCount - 1; i >= span - 1; i--) {
                int bottom = Integer.MIN_VALUE;
                for (int j = i; j > i - span; j--) {
                    final int singleBottom = mItemBottoms[j];
                    if (singleBottom > bottom) {
                        bottom = singleBottom;
                    }
                }
                if (bottom < topMost) {
                    topMost = bottom;
                    targetCol = i;
                }
            }
        } else {
            for (int i = 0; i <= colCount - span; i++) {
                int bottom = Integer.MIN_VALUE;
                for (int j = i; j < i + span; j++) {
                    final int singleBottom = mItemBottoms[j];
                    if (singleBottom > bottom) {
                        bottom = singleBottom;
                    }
                }
                if (bottom < topMost) {
                    topMost = bottom;
                    targetCol = i;
                }
            }
        }

        rec.column = targetCol;

        // Once we've found the target column for the view at this position, we update mItemBottoms
        // for all columns that this view will occupy.  We set the margins such that mItemBottoms
        // is equal for all columns in the view's span.  For LTR layout, we start at targetCol and
        // move right, and for RTL, we start at targetCol and move left.
        for (int i = 0; i < span; i++) {
            final int nextCol = mIsRtlLayout ? targetCol - i : targetCol + i;
            rec.setMarginAbove(i, topMost - mItemBottoms[nextCol]);
        }

        return rec;
    }

    private int getItemWidth(int itemColumnSpan) {
        final int colWidth = (getWidth() - getPaddingLeft() - getPaddingRight() -
                mItemMargin * (mColCount - 1)) / mColCount;
        return colWidth * itemColumnSpan + mItemMargin * (itemColumnSpan - 1);
    }

    /**
     * Obtain a populated view from the adapter.  This method checks to see if the view to populate
     * is already laid out on screen somewhere by comparing the item ids.
     *
     * If the view is already laid out, and the view type has not changed, populate the contents
     * and return.
     *
     * If the view is not laid out on screen somewhere, grab a view from the recycler and populate.
     *
     * NOTE: This method should be called during layout.
     *
     * TODO: This can probably be consolidated with the overloaded {@link #obtainView(int, View)}.
     *
     * @param position Position to get the view for.
     */
    final View obtainView(int position) {
        // TODO: This method currently does not support transient state views.

        final Object item = mAdapter.getItem(position);

        View scrap = null;
        final int positionViewType = mAdapter.getItemViewType(item, position);

        final long id = mAdapter.getItemId(item, position);
        final ViewRectPair viewRectPair = mChildRectsForAnimation.get(id);
        if (viewRectPair != null) {
            scrap = viewRectPair.view;

            // TODO: Make use of stable ids by retrieving the cached views using stable ids.  In
            // theory, we should maintain a list of active views, and then fetch the views
            // from that list.  If that fails, then we should go to the recycler.
            // For the collection holding stable ids, we must ensure that those views don't get
            // repurposed for other items at different positions.
        }

        final int scrapViewType = scrap != null &&
                (scrap.getLayoutParams() instanceof LayoutParams) ?
                ((LayoutParams) scrap.getLayoutParams()).viewType : -1;

        if (scrap == null || scrapViewType != positionViewType) {
            // If there is no cached view or the cached view's type no longer match the type
            // of the item at the specified position, retrieve a new view from the recycler and
            // recycle the cached view.
            if (scrap != null) {
                // The cached view we had is not valid, so add it to the recycler and
                // remove it from the current layout.
                recycleView(scrap);
            }

            scrap = mRecycler.getScrapView(positionViewType);
        }

        final int itemColumnSpan = mAdapter.getItemColumnSpan(item, position);
        final int itemWidth = getItemWidth(itemColumnSpan);
        final View view = mAdapter.getView(item, position, scrap, this, itemWidth);

        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (view.getParent() != this) {
            if (lp == null) {
                lp = generateDefaultLayoutParams();
            } else if (!checkLayoutParams(lp)) {
                lp = generateLayoutParams(lp);
            }

            view.setLayoutParams(lp);
        }

        final LayoutParams sglp = (LayoutParams) view.getLayoutParams();
        sglp.position = position;
        sglp.viewType = positionViewType;
        sglp.id = id;
        sglp.span = itemColumnSpan;

        // When the view at the positions we are tracking update, make sure to
        // update our views as well. That way, we have the correct
        // rectangle for comparing when the drag target enters/ leaves the
        // placeholder view.
        if (isDragReorderingSupported() && mReorderHelper.getDraggedChildId() == id) {
            mReorderHelper.updateDraggedChildView(view);
            mReorderHelper.updateDraggedOverChildView(view);
        }
        return view;
    }

    /**
     * Obtain a populated view from the adapter. If optScrap is non-null and is not
     * reused it will be placed in the recycle bin.
     *
     * @param position position to get view for
     * @param optScrap Optional scrap view; will be reused if possible
     * @return A new view, a recycled view from mRecycler, or optScrap
     */
    final View obtainView(int position, View optScrap) {
        View view = mRecycler.getTransientStateView(position);
        final Object item = mAdapter.getItem(position);
        final int positionViewType = mAdapter.getItemViewType(item, position);

        if (view == null) {
            // Reuse optScrap if it's of the right type (and not null)
            final int optType = optScrap != null ?
                    ((LayoutParams) optScrap.getLayoutParams()).viewType : -1;

            final View scrap = optType == positionViewType ?
                    optScrap : mRecycler.getScrapView(positionViewType);

            final int itemColumnSpan = mAdapter.getItemColumnSpan(item, position);
            final int itemWidth = getItemWidth(itemColumnSpan);
            view = mAdapter.getView(item, position, scrap, this, itemWidth);

            if (view != scrap && scrap != null) {
                // The adapter didn't use it; put it back.
                mRecycler.addScrap(scrap);
            }

            ViewGroup.LayoutParams lp = view.getLayoutParams();

            if (view.getParent() != this) {
                if (lp == null) {
                    lp = generateDefaultLayoutParams();
                } else if (!checkLayoutParams(lp)) {
                    lp = generateLayoutParams(lp);
                }

                view.setLayoutParams(lp);
            }
        }

        final LayoutParams sglp = (LayoutParams) view.getLayoutParams();
        sglp.position = position;
        sglp.viewType = positionViewType;
        final long id = mAdapter.getItemIdFromView(view, position);
        sglp.id = id;
        sglp.span = mAdapter.getItemColumnSpan(item, position);

        // When the view at the positions we are tracking update, make sure to
        // update our views as well. That way, we have the correct
        // rectangle for comparing when the drag target enters/ leaves the
        // placeholder view.
        if (isDragReorderingSupported() && mReorderHelper.getDraggedChildId() == id) {
            mReorderHelper.updateDraggedChildView(view);
            mReorderHelper.updateDraggedOverChildView(view);
        }

        return view;
    }

    /**
     * Animation mode to play for new data coming in as well as the stale data that should be
     * animated out.
     * @param animationIn The animation to play to introduce new or updated data into view
     * @param animationOut The animation to play to transition stale data out of view.
     */
    public void setAnimationMode(AnimationIn animationIn, AnimationOut animationOut) {
        mAnimationInMode = animationIn;
        mAnimationOutMode = animationOut;
    }

    public AnimationIn getAnimationInMode() {
        return mAnimationInMode;
    }

    public AnimationOut getAnimationOutMode() {
        return mAnimationOutMode;
    }

    public GridAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(GridAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
        }

        clearAllState();

        mAdapter = adapter;
        mDataChanged = true;
        mItemCount = adapter != null ? adapter.getCount() : 0;

        if (adapter != null) {
            adapter.registerDataSetObserver(mObserver);
            mRecycler.setViewTypeCount(adapter.getViewTypeCount());
            mHasStableIds = adapter.hasStableIds();
        } else {
            mHasStableIds = false;
        }

        if (isDragReorderingSupported()) {
            updateReorderStates(ReorderUtils.DRAG_STATE_NONE);
        }

        updateEmptyStatus();
    }

    public void setAdapter(GridAdapter adapter, ScrollState scrollState) {
        setAdapter(adapter);
        mCurrentScrollState = scrollState;
    }

    /**
     * Clear all state because the grid will be used for a completely different set of data.
     */
    private void clearAllState() {
        // Clear all layout records and views
        mLayoutRecords.clear();
        removeAllViews();

        mItemTops = null;
        mItemBottoms = null;

        setSelectionToTop();

        // Clear recycler because there could be different view types now
        mRecycler.clear();

        // Reset the last touch y coordinate so that any animation/events won't use stale values.
        mLastTouchY = 0;

        // Reset the first changed position to 0. At least we will update all views.
        mFirstChangedPosition = 0;
    }

    /**
     * Scroll the list so the first visible position in the grid is the first item in the adapter.
     */
    public void setSelectionToTop() {
        mCurrentScrollState = null;
        setFirstPositionAndOffsets(0 /* position */, getPaddingTop() /* offset */);
    }

    /**
     * Get {@link #mFirstPosition}, which is the adapter position of the View
     * returned by getChildAt(0).
     */
    public int getCurrentFirstPosition() {
        return mFirstPosition;
    }

    /**
     * Indicate whether the scrolling state is currently at the topmost of this grid
     * @return boolean Indicates whether the current view is the top most of this grid.
     */
    private boolean isSelectionAtTop() {
        if (mCurrentScrollState != null && mCurrentScrollState.getAdapterPosition() == 0) {
            // ScrollState is how far the top of the first child is from the top of the screen, and
            // does not include top padding when the adapter position is the first child. If the
            // vertical offset of the scroll state is exactly equal to {@link #mItemMargin}, then
            // the first item, and therefore the view of the grid, is at the top.
            return mCurrentScrollState.getVerticalOffset() == mItemMargin;
        }

        return false;
    }

    /**
     * Set the first position and offset so that on layout, we would start laying out starting
     * with the specified position at the top of the view.
     * @param position The child position to place at the top of this view.
     * @param offset The vertical layout offset of the view at the specified position.
     */
    public void setFirstPositionAndOffsets(int position, int offset) {
        // Reset the first visible position in the grid to be item 0
        mFirstPosition = position;
        if (mItemTops == null || mItemBottoms == null) {
            mItemTops = new int[mColCount];
            mItemBottoms = new int[mColCount];
        }

        calculateLayoutStartOffsets(offset);
    }

    /**
     * Restore the view to the states specified by the {@link ScrollState}.
     * @param scrollState {@link ScrollState} containing the scroll states to restore to.
     */
    private void restoreScrollPosition(ScrollState scrollState) {
        if (mAdapter == null || scrollState == null || mAdapter.getCount() == 0) {
            return;
        }

        Log.v(TAG, "[restoreScrollPosition] " + scrollState);

        int targetPosition = 0;
        long itemId = -1;

        final int originalPosition = scrollState.getAdapterPosition();
        final int adapterCount = mAdapter.getCount();
        // ScrollState is defined as the vertical offset of the first item that is laid out
        // on screen.  To restore scroll state, we check within a window to see if we can
        // find that original first item in this new data set.  If we can, restore that item
        // to the first position on screen, offset by its previous vertical offset.  If we
        // cannot find that item, then we'll simply layout out everything from the beginning
        // again.

        // TODO:  Perhaps it is more efficient if we check the cursor in one direction first
        // before going backwards, rather than jumping back and forth as we are doing now.
        for (int i = 0; i < SCROLL_RESTORE_WINDOW_SIZE; i++) {
            if (originalPosition + i < adapterCount) {
                itemId = mAdapter.getItemId(originalPosition + i);
                if (itemId != -1 && itemId == scrollState.getItemId()) {
                    targetPosition = originalPosition + i;
                    break;
                }
            }

            if (originalPosition - i >= 0 && originalPosition - i < adapterCount) {
                itemId = mAdapter.getItemId(originalPosition - i);
                if (itemId != -1 && itemId == scrollState.getItemId()) {
                    targetPosition = originalPosition - i;
                    break;
                }
            }
        }

        // layoutChildren(), fillDown() and fillUp() always apply mItemMargin when laying out
        // views.  Since restoring scroll position is effectively laying out a particular child
        // as the first child, we need to ensure we strip mItemMargin from the offset, as it
        // will be re-applied when the view is laid out.
        //
        // Since top padding varies with screen orientation and is not stored in the scroll
        // state when the scroll adapter position is the first child, we add it here.
        int offset = scrollState.getVerticalOffset() - mItemMargin;
        if (targetPosition == 0) {
            offset += getPaddingTop();
        }

        setFirstPositionAndOffsets(targetPosition, offset);
        mCurrentScrollState = null;
    }

    /**
     * Return the current scroll state of this view.
     * @return {@link ScrollState} The current scroll state
     */
    public ScrollState getScrollState() {
        final View v = getChildAt(0);
        if (v == null) {
            return null;
        }

        final LayoutParams lp = (LayoutParams) v.getLayoutParams();
        // Since top padding varies with screen orientation, it is not stored in the scroll state
        // when the scroll adapter position is the first child.
        final int offset = (lp.position == 0 ? v.getTop() - getPaddingTop() : v.getTop());
        return new ScrollState(lp.id, lp.position, offset);
    }

    /**
     * NOTE This method is borrowed from {@link ScrollView}.
     */
    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle,
            boolean immediate) {
        // offset into coordinate space of this scroll view
        rectangle.offset(child.getLeft() - child.getScrollX(),
                child.getTop() - child.getScrollY());

        return scrollToChildRect(rectangle, immediate);
    }

    /**
     * If rect is off screen, scroll just enough to get it (or at least the
     * first screen size chunk of it) on screen.
     * NOTE This method is borrowed from {@link ScrollView}.
     *
     * @param rect      The rectangle.
     * @param immediate True to scroll immediately without animation. Not used here.
     * @return true if scrolling was performed
     */
    private boolean scrollToChildRect(Rect rect, boolean immediate) {
        final int delta = computeScrollDeltaToGetChildRectOnScreen(rect);
        final boolean scroll = delta != 0;
        if (scroll) {
            // TODO smoothScrollBy if immediate is false.
            scrollBy(0, delta);
        }
        return scroll;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (mOnSizeChangedListener != null) {
            mOnSizeChangedListener.onSizeChanged(w, h, oldw, oldh);
        }

        // NOTE Below is borrowed from {@link ScrollView}.
        final View currentFocused = findFocus();
        if (null == currentFocused || this == currentFocused) {
            return;
        }

        // If the currently-focused view was visible on the screen when the
        // screen was at the old height, then scroll the screen to make that
        // view visible with the new screen height.
        if (isWithinDeltaOfScreen(currentFocused, 0, oldh)) {
            currentFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(currentFocused, mTempRect);
            scrollBy(0, computeScrollDeltaToGetChildRectOnScreen(mTempRect));
        }
    }

    /**
     *
     * NOTE This method is borrowed from {@link ScrollView}.
     *
     * @return whether the descendant of this scroll view is within delta
     *  pixels of being on the screen.
     */
    private boolean isWithinDeltaOfScreen(View descendant, int delta, int height) {
        descendant.getDrawingRect(mTempRect);
        offsetDescendantRectToMyCoords(descendant, mTempRect);

        return (mTempRect.bottom + delta) >= getScrollY()
                && (mTempRect.top - delta) <= (getScrollY() + height);
    }

    /**
     * NOTE: borrowed from {@link GridView}
     * Comments from {@link View}
     *
     * Compute the vertical extent of the vertical scrollbar's thumb within the vertical range.
     * This value is used to compute the length of the thumb within the scrollbar's track.
     * The range is expressed in arbitrary units that must be the same as the units used by
     * {@link #computeVerticalScrollRange} and {@link #computeVerticalScrollOffset}.
     *
     * The default extent is the drawing height of this view.
     *
     * @return the vertical extent of the scrollbar's thumb
     */
    @Override
    protected int computeVerticalScrollExtent() {

        final int count = getChildCount();
        if (count > 0) {
            if (mSmoothScrollbarEnabled) {
                final int rowCount = (count + mColCount - 1) / mColCount;
                int extent = rowCount * SCROLLING_ESTIMATED_ITEM_HEIGHT;

                View view = getChildAt(0);
                final int top = view.getTop();
                int height = view.getHeight();
                if (height > 0) {
                    extent += (top * SCROLLING_ESTIMATED_ITEM_HEIGHT) / height;
                }

                view = getChildAt(count - 1);
                final int bottom = view.getBottom();
                height = view.getHeight();
                if (height > 0) {
                    extent -= ((bottom - getHeight()) * SCROLLING_ESTIMATED_ITEM_HEIGHT) / height;
                }

                return extent;
            } else {
                return 1;
            }
        }
        return 0;
    }

    /**
     * NOTE: borrowed from {@link GridView} and altered as appropriate to accommodate for
     * {@link StaggeredGridView}
     *
     * Comments from {@link View}
     *
     * Compute the vertical offset of the vertical scrollbar's thumb within the horizontal range.
     * This value is used to compute the position of the thumb within the scrollbar's track.
     * The range is expressed in arbitrary units that must be the same as the units used by
     * {@link #computeVerticalScrollRange()} and {@link #computeVerticalScrollExtent()}.
     *
     * The default offset is the scroll offset of this view.
     *
     * @return the vertical offset of the scrollbar's thumb
     */
    @Override
    protected int computeVerticalScrollOffset() {
        final int firstPosition = mFirstPosition;
        final int childCount = getChildCount();
        final int paddingTop = getPaddingTop();

        if (firstPosition >= 0 && childCount > 0) {
            if (mSmoothScrollbarEnabled) {
                final View view = getChildAt(0);
                final int top = view.getTop();
                final int currentTopViewHeight = view.getHeight();
                if (currentTopViewHeight > 0) {
                    // In an ideal world, all items would have a fixed height that we would know
                    // a priori, calculating the scroll offset would simply be:
                    //     [A] (mFirstPosition * fixedHeight) - childView[0].top
                    //         where childView[0] is the first view on screen.
                    //
                    // However, given that we do not know the height ahead of time, and that each
                    // item in this grid can have varying heights, we'd need to assign an arbitrary
                    // item height (SCROLLING_ESTIMATED_ITEM_HEIGHT) in order to estimate the scroll
                    // offset.  The previous equation thus transforms to:
                    //     [B] (mFirstPosition * SCROLLING_ESTIMATED_ITEM_HEIGHT) -
                    //         ((childView[0].top * SCROLLING_ESTIMATED_ITEM_HEIGHT) /
                    //          childView[0].height)
                    //
                    // Equation [B] gives a pretty good calculation of the offset if this were a
                    // single column grid view, for a multi-column grid, one slight modification is
                    // needed:
                    //     [C] ((mFirstPosition * SCROLLING_ESTIMATED_ITEM_HEIGHT) / mColCount) -
                    //         ((childView[0].top * SCROLLING_ESTIMATED_ITEM_HEIGHT) /
                    //          childView[0].height)
                    final int estimatedScrollOffset =
                            ((firstPosition * SCROLLING_ESTIMATED_ITEM_HEIGHT) / mColCount) -
                            ((top * SCROLLING_ESTIMATED_ITEM_HEIGHT) / currentTopViewHeight);

                    final int rowCount = (mItemCount + mColCount - 1) / mColCount;
                    final int overScrollCompensation = (int) ((float) getScrollY() / getHeight() *
                            rowCount * SCROLLING_ESTIMATED_ITEM_HEIGHT);

                    int val = Math.max(estimatedScrollOffset + overScrollCompensation, 0);
                    // If mFirstPosition is currently the very first item in the adapter, check to
                    // see if we need to take into account any top padding.  This is so that we
                    // don't return 0 when in fact the user may still be scrolling through some
                    // top padding.
                    if (firstPosition == 0 && paddingTop > 0) {
                        val += paddingTop - top + mItemMargin;
                    }
                    return val;
                }
            } else {
                int index;
                final int count = mItemCount;
                if (firstPosition == 0) {
                    index = 0;
                } else if (firstPosition + childCount == count) {
                    index = count;
                } else {
                    index = firstPosition + childCount / 2;
                }
                return (int) (firstPosition + childCount * (index / (float) count));
            }
        }

        return paddingTop;
    }

    /**
     * NOTE: borrowed from {@link GridView} and altered as appropriate to accommodate for
     * {@link StaggeredGridView}
     *
     * Comments from {@link View}
     *
     * Compute the vertical range that the vertical scrollbar represents.
     * The range is expressed in arbitrary units that must be the same as the units used by
     * {@link #computeVerticalScrollExtent} and {@link #computeVerticalScrollOffset}.
     *
     * The default range is the drawing height of this view.
     *
     * @return the total vertical range represented by the vertical scrollbar
     */
    @Override
    protected int computeVerticalScrollRange() {
        final int rowCount = (mItemCount + mColCount - 1) / mColCount;
        int result = Math.max(rowCount * SCROLLING_ESTIMATED_ITEM_HEIGHT, 0);

        if (mSmoothScrollbarEnabled) {
            if (getScrollY() != 0) {
                // Compensate for overscroll
                result += Math.abs((int) ((float) getScrollY() / getHeight() * rowCount
                        * SCROLLING_ESTIMATED_ITEM_HEIGHT));
            }
        } else {
            result = mItemCount;
        }

        return result;
    }

    /**
     * Compute the amount to scroll in the Y direction in order to get
     * a rectangle completely on the screen (or, if taller than the screen,
     * at least the first screen size chunk of it).
     *
     * NOTE This method is borrowed from {@link ScrollView}.
     *
     * @param rect The rect.
     * @return The scroll delta.
     */
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (getChildCount() == 0) {
            return 0;
        }

        final int height = getHeight();
        final int fadingEdge = getVerticalFadingEdgeLength();

        int screenTop = getScrollY();
        int screenBottom = screenTop + height;

        // leave room for top fading edge as long as rect isn't at very top
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }

        // leave room for bottom fading edge as long as rect isn't at very bottom
        if (rect.bottom < getHeight()) {
            screenBottom -= fadingEdge;
        }

        int scrollYDelta = 0;

        if (rect.bottom > screenBottom && rect.top > screenTop) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.height() > height) {
                // just enough to get screen size chunk on
                scrollYDelta = screenTop - rect.top;
            } else {
                // get entire rect at bottom of screen
                scrollYDelta = screenBottom - rect.bottom;
            }
        } else if (rect.top < screenTop && rect.bottom < screenBottom) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.height() > height) {
                // screen size chunk
                scrollYDelta = screenBottom - rect.bottom;
            } else {
                // entire rect at top
                scrollYDelta = screenTop - rect.top;
            }
        }
        return scrollYDelta;
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LayoutParams(lp);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams lp) {
        return lp instanceof LayoutParams;
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState ss = new SavedState(superState);
        final int position = mFirstPosition;
        ss.position = position;
        if (position >= 0 && mAdapter != null && position < mAdapter.getCount()) {
            ss.firstId = mAdapter.getItemId(position);
        }
        if (getChildCount() > 0) {
            // Since top padding varies with screen orientation, it is not stored in the scroll
            // state when the scroll adapter position is the first child.
            ss.topOffset = position == 0 ?
                    getChildAt(0).getTop() - getPaddingTop() : getChildAt(0).getTop();
        }
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        final SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mDataChanged = true;
        mFirstPosition = ss.position;
        mCurrentScrollState = new ScrollState(ss.firstId, ss.position, ss.topOffset);
        requestLayout();
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        private static final int[] LAYOUT_ATTRS = new int[] {
            android.R.attr.layout_span
        };

        private static final int SPAN_INDEX = 0;

        /**
         * The number of columns this item should span
         */
        public int span = 1;

        /**
         * Item position this view represents
         */
        public int position = -1;

        /**
         * Type of this view as reported by the adapter
         */
        int viewType;

        /**
         * The column this view is occupying
         */
        int column;

        /**
         * The stable ID of the item this view displays
         */
        long id = -1;

        /**
         * The position where reordering can happen for this view
         */
        public int reorderingArea = ReorderUtils.REORDER_AREA_NONE;

        public LayoutParams(int height) {
            super(MATCH_PARENT, height);

            if (this.height == MATCH_PARENT) {
                Log.w(TAG, "Constructing LayoutParams with height FILL_PARENT - " +
                        "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            if (this.width != MATCH_PARENT) {
                Log.w(TAG, "Inflation setting LayoutParams width to " + this.width +
                        " - must be MATCH_PARENT");
                this.width = MATCH_PARENT;
            }
            if (this.height == MATCH_PARENT) {
                Log.w(TAG, "Inflation setting LayoutParams height to MATCH_PARENT - " +
                        "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }

            final TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            span = a.getInteger(SPAN_INDEX, 1);
            a.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);

            if (this.width != MATCH_PARENT) {
                Log.w(TAG, "Constructing LayoutParams with width " + this.width +
                        " - must be MATCH_PARENT");
                this.width = MATCH_PARENT;
            }
            if (this.height == MATCH_PARENT) {
                Log.w(TAG, "Constructing LayoutParams with height MATCH_PARENT - " +
                        "impossible! Falling back to WRAP_CONTENT");
                this.height = WRAP_CONTENT;
            }
        }
    }

    private class RecycleBin {
        private ArrayList<View>[] mScrapViews;
        private int mViewTypeCount;
        private int mMaxScrap;

        private SparseArray<View> mTransientStateViews;

        public void setViewTypeCount(int viewTypeCount) {
            if (viewTypeCount < 1) {
                throw new IllegalArgumentException("Must have at least one view type (" +
                        viewTypeCount + " types reported)");
            }
            if (viewTypeCount == mViewTypeCount) {
                return;
            }

            final ArrayList<View>[] scrapViews = new ArrayList[viewTypeCount];
            for (int i = 0; i < viewTypeCount; i++) {
                scrapViews[i] = new ArrayList<View>();
            }
            mViewTypeCount = viewTypeCount;
            mScrapViews = scrapViews;
        }

        public void clear() {
            final int typeCount = mViewTypeCount;
            for (int i = 0; i < typeCount; i++) {
                mScrapViews[i].clear();
            }
            if (mTransientStateViews != null) {
                mTransientStateViews.clear();
            }
        }

        public void clearTransientViews() {
            if (mTransientStateViews != null) {
                mTransientStateViews.clear();
            }
        }

        public void addScrap(View v) {
            if (!(v.getLayoutParams() instanceof LayoutParams)) {
                return;
            }

            final LayoutParams lp = (LayoutParams) v.getLayoutParams();
            if (ViewCompat.hasTransientState(v)) {
                if (mTransientStateViews == null) {
                    mTransientStateViews = new SparseArray<View>();
                }
                mTransientStateViews.put(lp.position, v);
                return;
            }

            final int childCount = getChildCount();
            if (childCount > mMaxScrap) {
                mMaxScrap = childCount;
            }

            // Clear possible modified states applied to the view when adding to the recycler.
            // This view may have been part of a cancelled animation, so clear that state so that
            // future consumer of this view won't have to deal with states from its past life.
            v.setTranslationX(0);
            v.setTranslationY(0);
            v.setRotation(0);
            v.setAlpha(1.0f);
            v.setScaleY(1.0f);

            final ArrayList<View> scrap = mScrapViews[lp.viewType];
            if (scrap.size() < mMaxScrap) {
                // The number of scraps have not yet exceeded our limit, check to see that this
                // view does not already exist in the recycler.  This can happen if a caller
                // mistakenly calls addScrap(view) multiple times for the same view.
                if (!scrap.contains(v)) {
                    scrap.add(v);
                }
            }
        }

        public View getTransientStateView(int position) {
            if (mTransientStateViews == null) {
                return null;
            }

            final View result = mTransientStateViews.get(position);
            if (result != null) {
                mTransientStateViews.remove(position);
            }
            return result;
        }

        public View getScrapView(int type) {
            final ArrayList<View> scrap = mScrapViews[type];
            if (scrap.isEmpty()) {
                return null;
            }

            final int index = scrap.size() - 1;
            final View result = scrap.remove(index);

            return result;
        }

        // TODO: Implement support to maintain a list of active views so that we can make use of
        // stable ids to retrieve the same view that is currently laid out for a particular item.
        // Currently, all views "recycled" are shoved into the same collection, this may not be
        // the most effective way.  Refer to the RecycleBin as implemented for AbsListView.
        public View getView(int type, long stableId) {
            final ArrayList<View> scrap = mScrapViews[type];
            if (scrap.isEmpty()) {
                return null;
            }

            for (int i = 0; i < scrap.size(); i++) {
                final View v = scrap.get(i);
                final LayoutParams lp = (LayoutParams) v.getLayoutParams();
                if (lp.id == stableId) {
                    scrap.remove(i);
                    return v;
                }
            }

            return null;
        }
    }

    private class AdapterDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            mDataChanged = true;

            mItemCount = mAdapter.getCount();
            mFirstChangedPosition = mAdapter.getFirstChangedPosition();
            if (mFirstPosition >= mItemCount) {
                // If the latest data set has fewer data items than mFirstPosition, we will not be
                // able to accurately restore scroll state, so just reset to the top.
                mFirstPosition = 0;
                mCurrentScrollState = null;
            }

            // TODO: Consider matching these back up if we have stable IDs.
            mRecycler.clearTransientViews();

            if (mHasStableIds) {
                // If we will animate the transition to the new layout, cache the current positions
                // of the visible children. This is before any views get removed below.
                cacheChildRects();
            } else {
                // Clear all layout records
                mLayoutRecords.clear();

                // Reset item bottoms to be equal to item tops
                final int colCount = mColCount;
                for (int i = 0; i < colCount; i++) {
                    mItemBottoms[i] = mItemTops[i];
                }
            }

            updateEmptyStatus();

            // TODO: consider repopulating in a deferred runnable instead
            // (so that successive changes may still be batched)
            requestLayout();
        }

        @Override
        public void onInvalidated() {
        }
    }

    static class SavedState extends BaseSavedState {
        long firstId = -1;
        int position;

        // topOffset is the vertical value that the view specified by position should
        // start rendering from.  If it is 0, the view would be at the top of the grid.
        int topOffset;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            firstId = in.readLong();
            position = in.readInt();
            topOffset = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeLong(firstId);
            out.writeInt(position);
            out.writeInt(topOffset);
        }

        @Override
        public String toString() {
            return "StaggereGridView.SavedState{"
                        + Integer.toHexString(System.identityHashCode(this))
                        + " firstId=" + firstId
                        + " position=" + position + "}";
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public void setDropListener(ReorderListener listener) {
        mReorderHelper = new ReorderHelper(listener, this);
    }

    public void setScrollListener(ScrollListener listener) {
        mScrollListener = listener;
    }

    public void setOnSizeChangedListener(OnSizeChangedListener listener) {
        mOnSizeChangedListener = listener;
    }

    /**
     * Helper class to store a {@link View} with its corresponding layout positions
     * as a {@link Rect}.
     */
    private static class ViewRectPair {
        public final View view;
        public final Rect rect;

        public ViewRectPair(View v, Rect r) {
            view = v;
            rect = r;
        }
    }

    public static class ScrollState implements Parcelable {
        private final long mItemId;
        private final int mAdapterPosition;

        // The offset that the view specified by mAdapterPosition should start rendering from.  If
        // this value is 0, then the view would be rendered from the very top of this grid.
        private int mVerticalOffset;

        public ScrollState(long itemId, int adapterPosition, int offset) {
            mItemId = itemId;
            mAdapterPosition = adapterPosition;
            mVerticalOffset = offset;
        }

        private ScrollState(Parcel in) {
            mItemId = in.readLong();
            mAdapterPosition = in.readInt();
            mVerticalOffset = in.readInt();
        }

        public long getItemId() {
            return mItemId;
        }

        public int getAdapterPosition() {
            return mAdapterPosition;
        }

        public void setVerticalOffset(int offset) {
            mVerticalOffset = offset;
        }

        public int getVerticalOffset() {
            return mVerticalOffset;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(mItemId);
            dest.writeInt(mAdapterPosition);
            dest.writeInt(mVerticalOffset);
        }

        public static final Parcelable.Creator<ScrollState> CREATOR =
                new Parcelable.Creator<ScrollState>() {
            @Override
            public ScrollState createFromParcel(Parcel source) {
                return new ScrollState(source);
            }

            @Override
            public ScrollState[] newArray(int size) {
                return new ScrollState[size];
            }
        };

        @Override
        public String toString() {
            return "ScrollState {mItemId=" + mItemId +
                    " mAdapterPosition=" + mAdapterPosition +
                    " mVerticalOffset=" + mVerticalOffset + "}";
        }
    }

    /**
     * Listener of {@Link StaggeredGridView} for grid size change.
     */
    public interface OnSizeChangedListener {
        void onSizeChanged(int width, int height, int oldWidth, int oldHeight);
    }

    /**
     * Listener of {@Link StaggeredGridView} for scroll change.
     */
    public interface ScrollListener {

        /**
         * Called when scroll happens on this view.
         *
         * @param offset The scroll offset amount.
         * @param currentScrollY The current y position of this view.
         * @param maxScrollY The maximum amount of scroll possible in this view.
         */
        void onScrollChanged(int offset, int currentScrollY, int maxScrollY);
    }

    /**
     * Listener of {@link StaggeredGridView} for animations.  This listener is responsible
     * for playing all animations created by this {@link StaggeredGridView}
     */
    public interface AnimationListener {
        /**
         * Called when animations are ready to be played
         * @param animationMode The current animation mode based on the state of the data.  Valid
         * animation modes are {@link ANIMATION_MODE_NONE}, {@link ANIMATION_MODE_NEW_DATA}, and
         * {@link ANIMATION_MODE_UPDATE_DATA}.
         * @param animators The list of animators to be played
         */
        void onAnimationReady(int animationMode, List<Animator> animators);
    }

    /**
     * Listener of {@link StaggeredGridView} for drag and drop reordering of child views.
     */
    public interface ReorderListener {

        /**
         * onPickedUp is called to notify listeners that an item has been picked up for reordering.
         * @param draggedChild the original child view that picked up.
         */
        void onPickedUp(View draggedChild);

        /**
         * onDrop is called to notify listeners that an intent to drop the
         * item at position "from" over the position "target"
         * @param draggedView the original child view that was dropped
         * @param sourcePosition the original position where the item was dragged from
         * @param targetPosition the target position where the item is dropped at
         */
        void onDrop(View draggedView, int sourcePosition, int targetPosition);

        /**
         * onCancelDrag is called to notify listeners that the drag event has been cancelled.
         * @param draggediew the original child view that was dragged.
         */
        void onCancelDrag(View draggediew);

        /**
         * onReorder is called to notify listeners that an intent to move the
         * item at position "from" to position "to"
         * @param draggedView the original child view that was dragged
         * @param id id of the original item that was picked up
         * @param from
         * @param to the target position where the item is dropped at
         */
        boolean onReorder(View draggedView, long id, int from, int to);

        /**
         * Event handler for a drag entering the {@link StaggeredGridView} element's
         * reordering area.
         * @param view The child view that just received an enter event on the reordering area.
         * @param position The adapter position of the view that just received an enter event.
         */
        void onEnterReorderArea(View view, int position);
    }
}
