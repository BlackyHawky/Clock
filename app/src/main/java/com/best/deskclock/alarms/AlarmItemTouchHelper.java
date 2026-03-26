// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.alarms;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;
import com.google.android.material.color.MaterialColors;

/**
 * Custom {@link androidx.recyclerview.widget.ItemTouchHelper.Callback} for managing drag-and-drop and swipe-to-delete of alarm items
 * in a RecyclerView.
 *
 * <p>This implementation allows manual reordering of alarms via drag gestures,
 * but disables dragging when the user initiates a touch event on the digital clock view to prevent interaction conflicts.</p>
 *
 * <p>Drag and swipe directions are dynamically enabled or disabled based on user preferences,
 * device orientation (tablet/phone, portrait/landscape), and the current UI state (e.g., if a dialog is currently open).</p>
 */
public class AlarmItemTouchHelper extends ItemTouchHelper.SimpleCallback {

    private final AlarmTouchContract mContract;
    private final boolean mIsTablet;
    private final boolean mIsLandscape;

    private int dragFrom = RecyclerView.NO_POSITION;
    private int dragTo = RecyclerView.NO_POSITION;
    private final Rect mClipBounds = new Rect();
    float mLargeRadius;
    float mSmallRadius;
    int mDeleteIconHorizontalMargin;
    String mDeleteText;
    GradientDrawable mSwipeBackground;
    Drawable mDeleteIcon;
    int mDeleteIconSize = 0;
    TextPaint mDeleteTextPaint;
    private final float[] mTopRadii;
    private final float[] mBottomRadii;
    private final int mDeleteIconHalfSize;
    private final int mTextHorizontalOffset;
    private final float mTextVerticalOffset;
    private boolean mIsTouchOnClock = false;

    public AlarmItemTouchHelper(Context context, AlarmTouchContract contract, RecyclerView recyclerView, boolean isTablet,
                                boolean isLandscape) {

        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.END);

        mContract = contract;
        mIsTablet = isTablet;
        mIsLandscape = isLandscape;
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        mLargeRadius = dpToPx(18, displayMetrics);
        mSmallRadius = dpToPx(4, displayMetrics);
        mDeleteIconHorizontalMargin = (int) dpToPx(16, displayMetrics);
        mDeleteText = context.getString(R.string.delete);

        mSwipeBackground = new GradientDrawable();
        mSwipeBackground.setColor(ContextCompat.getColor(context, R.color.colorAlert));

        mDeleteIcon = AppCompatResources.getDrawable(context, R.drawable.ic_delete);
        if (mDeleteIcon != null) {
            DrawableCompat.setTint(mDeleteIcon, MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorOnError, Color.BLACK));
            mDeleteIconSize = mDeleteIcon.getIntrinsicHeight();
        }

        mDeleteTextPaint = new TextPaint();
        mDeleteTextPaint.setAntiAlias(true);
        mDeleteTextPaint.setTextSize(TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 16, context.getResources().getDisplayMetrics()));
        mDeleteTextPaint.setColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnError, Color.BLACK));
        mDeleteTextPaint.setTypeface(ThemeUtils.boldTypeface(SettingsDAO.getGeneralFont(getDefaultSharedPreferences(context))));
        mDeleteTextPaint.setTextAlign(ThemeUtils.isRTL(context) ? Paint.Align.RIGHT : Paint.Align.LEFT);

        mTopRadii = new float[]{
            mLargeRadius, mLargeRadius, mLargeRadius, mLargeRadius,
            mSmallRadius, mSmallRadius, mSmallRadius, mSmallRadius};

        mBottomRadii = new float[]{
            mSmallRadius, mSmallRadius, mSmallRadius, mSmallRadius,
            mLargeRadius, mLargeRadius, mLargeRadius, mLargeRadius};

        mDeleteIconHalfSize = mDeleteIconSize / 2;
        mTextHorizontalOffset = (int) (1.5 * mDeleteIconHorizontalMargin + mDeleteIconSize);

        mTextVerticalOffset = (mDeleteTextPaint.getTextSize() - mDeleteTextPaint.getFontMetrics().descent) / 2;

        // Prevent the alarm from dragging if the alarm time is long-pressed
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    View child = rv.findChildViewUnder(e.getX(), e.getY());
                    if (child != null) {
                        View digitalClock = child.findViewById(R.id.digital_clock);

                        if (digitalClock != null && digitalClock.getVisibility() == View.VISIBLE) {
                            int[] loc = new int[2];
                            digitalClock.getLocationOnScreen(loc);
                            float x = e.getRawX();
                            float y = e.getRawY();

                            mIsTouchOnClock = x >= loc[0] && x <= loc[0] + digitalClock.getWidth()
                                && y >= loc[1] && y <= loc[1] + digitalClock.getHeight();
                        } else {
                            mIsTouchOnClock = false;
                        }
                    } else {
                        mIsTouchOnClock = false;
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
    public int getDragDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (!mContract.canDrag() || mIsTouchOnClock) {
            return 0;
        } else if (mIsTablet || mIsLandscape) {
            return ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END;
        } else {
            return super.getDragDirs(recyclerView, viewHolder);
        }
    }

    @Override
    public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (!mContract.canSwipe()) {
            return 0;
        }
        return super.getSwipeDirs(recyclerView, viewHolder);
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
        mContract.onRowSwiped(viewHolder);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        View itemView = viewHolder.itemView;
        int width = itemView.getWidth();
        int height = itemView.getHeight();

        if (dX == 0) {
            itemView.setClipBounds(null);
            return;
        }

        // On prévient le Fragment qu'un swipe commence (pour cacher le FAB)
        mContract.onSwipeStarted();

        c.save();

        int itemTop = itemView.getTop();
        int itemBottom = itemView.getBottom();
        int itemLeft = itemView.getLeft();
        int itemRight = itemView.getRight();

        if (dX > 0) {
            mClipBounds.set((int) dX, 0, width, height);
        } else {
            mClipBounds.set(0, 0, width + (int) dX, height);
        }
        itemView.setClipBounds(mClipBounds);

        AlarmItemViewHolder alarmHolder = (AlarmItemViewHolder) viewHolder;
        int position = alarmHolder.mItemPosition;
        int totalCount = alarmHolder.mTotalCount;

        // Radius setting
        if (totalCount <= 1) {
            mSwipeBackground.setCornerRadius(mLargeRadius);
        } else if (position == 0) {
            mSwipeBackground.setCornerRadii(mTopRadii);
        } else if (position == totalCount - 1) {
            mSwipeBackground.setCornerRadii(mBottomRadii);
        } else {
            mSwipeBackground.setCornerRadius(mSmallRadius);
        }

        mSwipeBackground.setBounds(itemLeft, itemTop, itemRight, itemBottom);

        int topIcon = itemTop + ((itemBottom - itemTop) / 2 - mDeleteIconHalfSize);
        int textMarginTop = (int) (itemTop + ((itemBottom - itemTop) / 2.0) + mTextVerticalOffset);

        // Drawing according to the swipe direction
        if (dX > 0) {
            // Swipe right
            int rightEdge = Math.min(itemLeft + (int) dX, itemRight);
            c.clipRect(itemLeft, itemTop, rightEdge, itemBottom);
            mSwipeBackground.draw(c);

            // Icon
            if (dX > mDeleteIconHorizontalMargin && mDeleteIcon != null) {
                mDeleteIcon.setBounds(
                    itemLeft + mDeleteIconHorizontalMargin,
                    topIcon,
                    itemLeft + mDeleteIconHorizontalMargin + mDeleteIconSize,
                    topIcon + mDeleteIconSize
                );
                mDeleteIcon.draw(c);
            }

            // Text
            if (dX > mTextHorizontalOffset) {
                c.drawText(mDeleteText, itemLeft + mTextHorizontalOffset, textMarginTop, mDeleteTextPaint);
            }
        } else {
            // Swipe left (for RTL)
            int leftEdge = Math.max(itemRight + (int) dX, itemLeft);
            c.clipRect(leftEdge, itemTop, itemRight, itemBottom);
            mSwipeBackground.draw(c);

            // Icon
            if (-dX > mDeleteIconHorizontalMargin && mDeleteIcon != null) {
                mDeleteIcon.setBounds(
                    itemRight - mDeleteIconHorizontalMargin - mDeleteIconSize,
                    topIcon,
                    itemRight - mDeleteIconHorizontalMargin,
                    topIcon + mDeleteIconSize
                );
                mDeleteIcon.draw(c);
            }

            // Text
            if (-dX > mTextHorizontalOffset) {
                c.drawText(mDeleteText, itemRight - mTextHorizontalOffset, textMarginTop, mDeleteTextPaint);
            }
        }

        c.restore();
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
        viewHolder.itemView.setClipBounds(null);

        mContract.onRowClear(viewHolder);

        if (dragFrom != RecyclerView.NO_POSITION && dragTo != RecyclerView.NO_POSITION && dragFrom != dragTo) {
            mContract.onRowSaved();
        }

        dragFrom = RecyclerView.NO_POSITION;
        dragTo = RecyclerView.NO_POSITION;
    }

}
