// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.widgets;

import static java.lang.Math.max;
import static java.lang.Math.round;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Represents the target and measured dimensions of a digital widget layout.
 * <p>
 * Used to calculate optimal font and icon sizes based on available space,
 * and to detect layout violations when rendering RemoteViews.
 * </p>
 */
public class DigitalWidgetSizes {

    public final int mTargetWidthPx;
    public final int mTargetHeightPx;
    private final int mLargestFontSizePx;
    private final int mSmallestFontSizePx;
    public Bitmap mIconBitmap;

    public int mMeasuredWidthPx;
    public int mMeasuredHeightPx;
    public int mMeasuredTextWidthPx;
    public int mMeasuredTextForCustomColorWidthPx;
    public int mMeasuredTextHeightPx;
    public int mMeasuredTextForCustomColorHeightPx;

    /**
     * The size of the font to use on the date / next alarm time fields.
     */
    public int mFontSizePx;

    /**
     * The size of the widget font.
     */
    public int mWidgetFontSizePx;

    public int mIconFontSizePx;
    public int mIconPaddingPx;

    public DigitalWidgetSizes(int targetWidthPx, int targetHeightPx, int largestClockFontSizePx) {
        mTargetWidthPx = targetWidthPx;
        mTargetHeightPx = targetHeightPx;
        mLargestFontSizePx = largestClockFontSizePx;
        mSmallestFontSizePx = 1;
    }

    private static void append(StringBuilder builder, String format, Object... args) {
        builder.append(String.format(Locale.ENGLISH, format, args));
    }

    public int getLargestFontSizePx() {
        return mLargestFontSizePx;
    }

    public int getSmallestFontSizePx() {
        return mSmallestFontSizePx;
    }

    public int getWidgetFontSizePx() {
        return mWidgetFontSizePx;
    }

    public void setWidgetFontSizePx(int widgetFontSizePx, float fontScaleFactor) {
        mWidgetFontSizePx = widgetFontSizePx;
        mFontSizePx = max(1, round(widgetFontSizePx / fontScaleFactor));
        mIconFontSizePx = (int) (mFontSizePx * 1.4f);
        mIconPaddingPx = mFontSizePx / 3;
    }

    /**
     * @return the amount of widget height available to the world cities list
     */
    public int getListHeight() {
        return mTargetHeightPx - mMeasuredHeightPx;
    }

    public boolean hasViolations() {
        return mMeasuredWidthPx > mTargetWidthPx || mMeasuredHeightPx > mTargetHeightPx;
    }

    public DigitalWidgetSizes newSize() {
        return new DigitalWidgetSizes(mTargetWidthPx, mTargetHeightPx, mLargestFontSizePx);
    }

    @NonNull
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(1000);
        builder.append("\n");
        append(builder, "Target dimensions: %dpx x %dpx\n",
                mTargetWidthPx, mTargetHeightPx);
        append(builder, "Last valid widget container measurement: %dpx x %dpx\n",
                mMeasuredWidthPx, mMeasuredHeightPx);
        append(builder, "Last text clock measurement: %dpx x %dpx\n",
                mMeasuredTextWidthPx, mMeasuredTextHeightPx);
        append(builder, "Last text clock measurement: %dpx x %dpx\n",
                mMeasuredTextForCustomColorWidthPx, mMeasuredTextForCustomColorHeightPx);

        if (mMeasuredWidthPx > mTargetWidthPx) {
            append(builder, "Measured width %dpx exceeded widget width %dpx\n",
                    mMeasuredWidthPx, mTargetWidthPx);
        }
        if (mMeasuredHeightPx > mTargetHeightPx) {
            append(builder, "Measured height %dpx exceeded widget height %dpx\n",
                    mMeasuredHeightPx, mTargetHeightPx);
        }
        append(builder, "Clock font: %dpx\n", mWidgetFontSizePx);
        return builder.toString();
    }

}
