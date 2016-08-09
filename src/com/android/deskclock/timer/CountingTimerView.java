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

package com.android.deskclock.timer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.PluralsRes;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.uidata.UiDataModel;

import java.util.Locale;

/**
 * Class to measure and draw the time in the {@link com.android.deskclock.CircleTimerView}.
 * This class manages and sums the work of the four members mBigHours, mBigMinutes,
 * mBigSeconds and mMedHundredths. Those members are each tasked with measuring, sizing and
 * drawing digits (and optional label) of the time set in {@link #setTime(long, boolean)}
 */
public class CountingTimerView extends View {

    private static final float TEXT_SIZE_TO_WIDTH_RATIO = 0.85f;
    // This is the ratio of the font height needed to vertically offset the font for alignment
    // from the center.
    private static final float FONT_VERTICAL_OFFSET = 0.14f;
    // Ratio of the space trailing the Hours and Minutes
    private static final float HOURS_MINUTES_SPACING = 0.4f;
    // Ratio of the space leading the Hundredths
    private static final float HUNDREDTHS_SPACING = 0.5f;

    /** Reusable StringBuilder to assemble talk back announcements when the time is updated. */
    private static final StringBuilder sTalkBackBuilder = new StringBuilder(50);

    // Radial offset of the enclosing circle
    private final float mRadiusOffset;

    private String mHours, mMinutes, mSeconds, mHundredths;

    private boolean mShowTimeStr = true;
    private final Paint mPaintBigThin = new Paint();
    private final Paint mPaintMed = new Paint();
    private final float mBigFontSize, mSmallFontSize;
    // Hours and minutes are signed for when a timer goes past the set time and thus negative
    private final SignedTime mBigHours, mBigMinutes;
    // Seconds are always shown with minutes, so are never signed
    private final UnsignedTime mBigSeconds;
    private final Hundredths mMedHundredths;
    private float mTextHeight = 0;
    private float mTotalTextWidth;
    private boolean mRemeasureText = true;

    private int mDefaultColor;
    private final int mPressedColor;
    private final int mWhiteColor;
    private final int mAccentColor;
    private final AccessibilityManager mAccessibilityManager;

    // Fields for the text serving as a virtual button.
    private boolean mVirtualButtonEnabled = false;
    private boolean mVirtualButtonPressedOn = false;

    // Whether or not a bounding circle exists into which the text must be made to fit.
    // If no such circle exists, the entire width of this component is available for text display.
    private boolean mShowBoundingCircle;

    Runnable mBlinkThread = new Runnable() {
        private boolean mVisible = true;
        @Override
        public void run() {
            mVisible = !mVisible;
            CountingTimerView.this.showTime(mVisible);
            postDelayed(mBlinkThread, 500);
        }
    };

    /**
     * Class to measure and draw the digit pairs of hours, minutes, seconds or hundredths. Digits
     * may have an optional label. for hours, minutes and seconds, this label trails the digits
     * and for seconds, precedes the digits.
     */
    static class UnsignedTime {
        protected Paint mPaint;
        protected float mEm;
        protected float mWidth = 0;
        private final String mWidest;
        protected final float mSpacingRatio;
        private float mLabelWidth = 0;

        public UnsignedTime(Paint paint, float spacingRatio, String allDigits) {
            mPaint = paint;
            mSpacingRatio = spacingRatio;

            if (TextUtils.isEmpty(allDigits)) {
                LogUtils.wtf("Locale digits missing - using English");
                allDigits = "0123456789";
            }

            float widths[] = new float[allDigits.length()];
            int ll = mPaint.getTextWidths(allDigits, widths);
            int largest = 0;
            for (int ii = 1; ii < ll; ii++) {
                if (widths[ii] > widths[largest]) {
                    largest = ii;
                }
            }

            mEm = widths[largest];
            mWidest = allDigits.substring(largest, largest + 1);
        }

        public UnsignedTime(UnsignedTime unsignedTime, float spacingRatio) {
            this.mPaint = unsignedTime.mPaint;
            this.mEm = unsignedTime.mEm;
            this.mWidth = unsignedTime.mWidth;
            this.mWidest = unsignedTime.mWidest;
            this.mSpacingRatio = spacingRatio;
        }

        protected void updateWidth(final String time) {
            mEm = mPaint.measureText(mWidest);
            mLabelWidth = mSpacingRatio * mEm;
            mWidth = time.length() * mEm;
        }

        protected void resetWidth() {
            mWidth = mLabelWidth = 0;
        }

        public float calcTotalWidth(final String time) {
            if (time != null) {
                updateWidth(time);
                return mWidth + mLabelWidth;
            } else {
                resetWidth();
                return 0;
            }
        }

        public float getLabelWidth() {
            return mLabelWidth;
        }

        /**
         * Draws each character with a fixed spacing from time starting at ii.
         * @param canvas the canvas on which the time segment will be drawn
         * @param time time segment
         * @param ii what character to start the draw
         * @param x offset
         * @param y offset
         * @return X location for the next segment
         */
        protected float drawTime(Canvas canvas, final String time, int ii, float x, float y) {
            float textEm  = mEm / 2f;
            while (ii < time.length()) {
                x += textEm;
                canvas.drawText(time.substring(ii, ii + 1), x, y, mPaint);
                x += textEm;
                ii++;
            }
            return x;
        }

        /**
         * Draw this time segment and append the intra-segment spacing to the x
         * @param canvas the canvas on which the time segment will be drawn
         * @param time time segment
         * @param x offset
         * @param y offset
         * @return X location for the next segment
         */
        public float draw(Canvas canvas, final String time, float x, float y) {
            return drawTime(canvas, time, 0, x, y) + getLabelWidth();
        }
    }

    /**
     * Special derivation to handle the hundredths painting with the label in front.
     */
    static class Hundredths extends UnsignedTime {
        public Hundredths(Paint paint, float spacingRatio, final String allDigits) {
            super(paint, spacingRatio, allDigits);
        }

        /**
         * Draw this time segment after prepending the intra-segment spacing to the x location.
         * {@link UnsignedTime#draw(android.graphics.Canvas, String, float, float)}
         */
        @Override
        public float draw(Canvas canvas, final String time, float x, float y) {
            return drawTime(canvas, time, 0, x + getLabelWidth(), y);
        }
    }

    /**
     * Special derivation to handle a negative number
     */
    static class SignedTime extends UnsignedTime {
        private float mMinusWidth = 0;

        public SignedTime (UnsignedTime unsignedTime, float spacingRatio) {
            super(unsignedTime, spacingRatio);
        }

        @Override
        protected void updateWidth(final String time) {
            super.updateWidth(time);
            if (time.contains("-")) {
                mMinusWidth = mPaint.measureText("-");
                mWidth += (mMinusWidth - mEm);
            } else {
                mMinusWidth = 0;
            }
        }

        @Override
        protected void resetWidth() {
            super.resetWidth();
            mMinusWidth = 0;
        }

        /**
         * Draws each character with a fixed spacing from time, handling the special negative
         * number case.
         * {@link UnsignedTime#draw(android.graphics.Canvas, String, float, float)}
         */
        @Override
        public float draw(Canvas canvas, final String time, float x, float y) {
            int ii = 0;
            if (mMinusWidth != 0f) {
                float minusWidth = mMinusWidth / 2;
                x += minusWidth;
                //TODO:hyphen is too thick when painted
                canvas.drawText(time.substring(0, 1), x, y, mPaint);
                x += minusWidth;
                ii++;
            }
            return drawTime(canvas, time, ii, x, y) + getLabelWidth();
        }
    }

    @SuppressWarnings("unused")
    public CountingTimerView(Context context) {
        this(context, null);
    }

    public CountingTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAccessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        Resources r = context.getResources();
        mDefaultColor = mWhiteColor = Color.WHITE;
        mPressedColor = mAccentColor = Utils.obtainStyledColor(
                context, R.attr.colorAccent, Color.RED);
        mBigFontSize = r.getDimension(R.dimen.big_font_size);
        mSmallFontSize = r.getDimension(R.dimen.small_font_size);

        mPaintBigThin.setAntiAlias(true);
        mPaintBigThin.setStyle(Paint.Style.STROKE);
        mPaintBigThin.setTextAlign(Paint.Align.CENTER);
        mPaintBigThin.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));

        mPaintMed.setAntiAlias(true);
        mPaintMed.setStyle(Paint.Style.STROKE);
        mPaintMed.setTextAlign(Paint.Align.CENTER);
        mPaintMed.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

        resetTextSize();
        setTextColor(mDefaultColor);

        // allDigits will contain ten digits: "0123456789" in the default locale
        final String allDigits = String.format(Locale.getDefault(), "%010d", 123456789);
        mBigSeconds = new UnsignedTime(mPaintBigThin, 0.f, allDigits);
        mBigHours = new SignedTime(mBigSeconds, HOURS_MINUTES_SPACING);
        mBigMinutes = new SignedTime(mBigSeconds, HOURS_MINUTES_SPACING);
        mMedHundredths = new Hundredths(mPaintMed, HUNDREDTHS_SPACING, allDigits);

        mRadiusOffset = Utils.calculateRadiusOffset(r);
    }

    protected void resetTextSize() {
        mTextHeight = mBigFontSize;
        mPaintBigThin.setTextSize(mBigFontSize);
        mPaintMed.setTextSize(mSmallFontSize);
    }

    protected void setTextColor(int textColor) {
        mPaintBigThin.setColor(textColor);
        mPaintMed.setColor(textColor);
    }

    public void setShowBoundingCircle(boolean showBoundingCircle) {
        mShowBoundingCircle = showBoundingCircle;
        requestLayout();
    }

    /**
     * Update the time to display. Separates that time into the hours, minutes, seconds and
     * hundredths. If update is true, the view is invalidated so that it will draw again.
     *
     * @param time new time to display - in milliseconds
     * @param showHundredths flag to show hundredths resolution
     */
    // TODO:showHundredths S/B attribute or setter - i.e. unchanging over object life
    public void setTime(long time, boolean showHundredths) {
        final int oldLength = getDigitsLength();
        boolean neg = false, showNeg = false;
        if (time < 0) {
            time = -time;
            neg = showNeg = true;
        }

        int hours = (int) (time / DateUtils.HOUR_IN_MILLIS);
        int remainder = (int) (time % DateUtils.HOUR_IN_MILLIS);

        int minutes = (int) (remainder / DateUtils.MINUTE_IN_MILLIS);
        remainder = (int) (remainder % DateUtils.MINUTE_IN_MILLIS);

        int seconds = (int) (remainder / DateUtils.SECOND_IN_MILLIS);
        remainder = (int) (remainder % DateUtils.SECOND_IN_MILLIS);

        int hundredths = remainder / 10;

        if (hours > 999) {
            hours = 0;
        }

        // The time can be between 0 and -1 seconds, but the "truncated" equivalent time of hours
        // and minutes and seconds could be zero, so since we do not show fractions of seconds
        // when counting down, do not show the minus sign.
        // TODO:does it matter that we do not look at showHundredths?
        if (hours == 0 && minutes == 0 && seconds == 0) {
            showNeg = false;
        }

        // If not showing hundredths, round up to the next second.
        if (!showHundredths) {
            if (!neg && hundredths != 0) {
                seconds++;
                if (seconds == 60) {
                    seconds = 0;
                    minutes++;
                    if (minutes == 60) {
                        minutes = 0;
                        hours++;
                    }
                }
            }
        }

        // Hours may be empty.
        final UiDataModel uiDataModel = UiDataModel.getUiDataModel();
        if (hours > 0) {
            final int hoursLength = hours >= 10 ? 2 : 1;
            mHours = uiDataModel.getFormattedNumber(showNeg, hours, hoursLength);
        } else {
            mHours = null;
        }

        // Minutes are never empty and forced to two digits when hours exist.
        final boolean showNegMinutes = showNeg && hours == 0;
        final int minutesLength = minutes >= 10 || hours > 0 ? 2 : 1;
        mMinutes = uiDataModel.getFormattedNumber(showNegMinutes, minutes, minutesLength);

        // Seconds are always two digits
        mSeconds = uiDataModel.getFormattedNumber(seconds, 2);

        // Hundredths are optional but forced to two digits when displayed.
        if (showHundredths) {
            mHundredths = uiDataModel.getFormattedNumber(hundredths, 2);
        } else {
            mHundredths = null;
        }

        int newLength = getDigitsLength();
        if (oldLength != newLength) {
            if (oldLength > newLength) {
                resetTextSize();
            }
            mRemeasureText = true;
        }

        setContentDescription(getTimeStringForAccessibility(hours, minutes, seconds, showNeg,
                getResources()));
        postInvalidateOnAnimation();
    }

    private int getDigitsLength() {
        return ((mHours == null) ? 0 : mHours.length())
                + ((mMinutes == null) ? 0 : mMinutes.length())
                + ((mSeconds == null) ? 0 : mSeconds.length())
                + ((mHundredths == null) ? 0 : mHundredths.length());
    }

    private void calcTotalTextWidth() {
        mTotalTextWidth = mBigHours.calcTotalWidth(mHours) + mBigMinutes.calcTotalWidth(mMinutes)
                + mBigSeconds.calcTotalWidth(mSeconds)
                + mMedHundredths.calcTotalWidth(mHundredths);
    }

    /**
     * Adjust the size of the fonts to fit within the the circle and painted object in
     * {@link com.android.deskclock.CircleTimerView#onDraw(android.graphics.Canvas)}
     */
    private void setTotalTextWidth() {
        calcTotalTextWidth();

        int width;
        if (mShowBoundingCircle) {
            // A bounding circle exists, so the available width in which to fit the timer text is
            // the smaller of the width or height, which is also equal to the circle's diameter.
            width = Math.min(getWidth(), getHeight());
        } else {
            // A bounding circle does not exist, so pretend that the entire width of this component
            // is the diameter of a theoretical bounding circle.
            width = getWidth();
        }

        if (width != 0) {
            // Shrink 'width' to account for circle stroke and other painted objects.
            // Note on the "4 *": (1) To reduce divisions, using the diameter instead of the radius.
            // (2) The radius of the enclosing circle is reduced by mRadiusOffset and the
            // text needs to fit within a circle further reduced by mRadiusOffset.
            width -= (int) (4 * mRadiusOffset + 0.5f);

            final float wantDiameter2 = TEXT_SIZE_TO_WIDTH_RATIO * width * width;
            float totalDiameter2 = getHypotenuseSquared();

            // If the hypotenuse of the bounding box is too large, reduce all the paint text sizes
            while (totalDiameter2 > wantDiameter2) {
                // Convergence is slightly difficult due to quantization in the mTotalTextWidth
                // calculation. Reducing the ratio by 1% converges more quickly without excessive
                // loss of quality.
                float sizeRatio = 0.99f * (float) Math.sqrt(wantDiameter2/totalDiameter2);
                mPaintBigThin.setTextSize(mPaintBigThin.getTextSize() * sizeRatio);
                mPaintMed.setTextSize(mPaintMed.getTextSize() * sizeRatio);
                // Recalculate the new total text height and half-width
                mTextHeight = mPaintBigThin.getTextSize();
                calcTotalTextWidth();
                totalDiameter2 = getHypotenuseSquared();
            }
        }
    }

    /**
     * Calculate the square of the diameter to use in {@link CountingTimerView#setTotalTextWidth()}
     */
    private float getHypotenuseSquared() {
        return mTotalTextWidth * mTotalTextWidth + mTextHeight * mTextHeight;
    }

    public void blinkTimeStr(boolean blink) {
        if (blink) {
            removeCallbacks(mBlinkThread);
            post(mBlinkThread);
        } else {
            removeCallbacks(mBlinkThread);
            showTime(true);
        }
    }

    public void showTime(boolean visible) {
        mShowTimeStr = visible;
        invalidate();
    }

    public void setTimeStrTextColor(boolean active, boolean forceUpdate) {
        mDefaultColor = active ? mAccentColor : mWhiteColor;
        setTextColor(mDefaultColor);
        if (forceUpdate) {
            invalidate();
        }
    }

    private static String getTimeStringForAccessibility(int hours, int minutes, int seconds,
            boolean showNeg, Resources r) {
        sTalkBackBuilder.setLength(0);
        if (showNeg) {
            // This must be followed by a non-zero number or it will be audible as "hyphen"
            // instead of "minus".
            sTalkBackBuilder.append('-');
        }
        if (showNeg && hours == 0 && minutes == 0) {
            // Non-negative time will always have minutes, eg. "0 minutes 7 seconds", but negative
            // time must start with non-zero digit, eg. -0m7s will be audible as just "-7 seconds"
            sTalkBackBuilder.append(getQuantityString(r, R.plurals.Nseconds_description, seconds));
        } else if (hours == 0) {
            sTalkBackBuilder.append(getQuantityString(r, R.plurals.Nminutes_description, minutes));
            sTalkBackBuilder.append(' ');
            sTalkBackBuilder.append(getQuantityString(r, R.plurals.Nseconds_description, seconds));
        } else {
            sTalkBackBuilder.append(getQuantityString(r, R.plurals.Nhours_description, hours));
            sTalkBackBuilder.append(' ');
            sTalkBackBuilder.append(getQuantityString(r, R.plurals.Nminutes_description, minutes));
            sTalkBackBuilder.append(' ');
            sTalkBackBuilder.append(getQuantityString(r, R.plurals.Nseconds_description, seconds));
        }
        return sTalkBackBuilder.toString();
    }

    private static String getQuantityString(Resources r, @PluralsRes int resId, int quantity) {
        return r.getQuantityString(resId, quantity, quantity);
    }

    public void setVirtualButtonEnabled(boolean enabled) {
        mVirtualButtonEnabled = enabled;
    }

    private void virtualButtonPressed(boolean pressedOn) {
        mVirtualButtonPressedOn = pressedOn;
        invalidate();
    }

    private boolean withinVirtualButtonBounds(float x, float y) {
        int width = getWidth();
        int height = getHeight();
        float centerX = width / 2;
        float centerY = height / 2;
        float radius = Math.min(width, height) / 2;

        // Within the circle button if distance to the center is less than the radius.
        double distance = Math.sqrt(Math.pow(centerX - x, 2) + Math.pow(centerY - y, 2));
        return distance < radius;
    }

    public void registerVirtualButtonAction(final Runnable runnable) {
        if (!mAccessibilityManager.isEnabled()) {
            this.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (mVirtualButtonEnabled) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                if (withinVirtualButtonBounds(event.getX(), event.getY())) {
                                    virtualButtonPressed(true);
                                    return true;
                                } else {
                                    virtualButtonPressed(false);
                                    return false;
                                }
                            case MotionEvent.ACTION_CANCEL:
                                virtualButtonPressed(false);
                                return true;
                            case MotionEvent.ACTION_OUTSIDE:
                                virtualButtonPressed(false);
                                return false;
                            case MotionEvent.ACTION_UP:
                                virtualButtonPressed(false);
                                if (withinVirtualButtonBounds(event.getX(), event.getY())) {
                                    runnable.run();
                                }
                                return true;
                        }
                    }
                    return false;
                }
            });
        } else {
            this.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    runnable.run();
                }
            });
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        // Blink functionality.
        if (!mShowTimeStr && !mVirtualButtonPressedOn) {
            return;
        }

        int width = getWidth();
        if (mRemeasureText && width != 0) {
            setTotalTextWidth();
            width = getWidth();
            mRemeasureText = false;
        }

        int xCenter = width / 2;
        int yCenter = getHeight() / 2;

        float xTextStart = xCenter - mTotalTextWidth / 2;
        float yTextStart = yCenter + mTextHeight/2 - (mTextHeight * FONT_VERTICAL_OFFSET);

        // Text color differs based on pressed state.
        final int textColor = mVirtualButtonPressedOn ? mPressedColor : mDefaultColor;
        mPaintBigThin.setColor(textColor);
        mPaintMed.setColor(textColor);

        if (mHours != null) {
            xTextStart = mBigHours.draw(canvas, mHours, xTextStart, yTextStart);
        }
        if (mMinutes != null) {
            xTextStart = mBigMinutes.draw(canvas, mMinutes, xTextStart, yTextStart);
        }
        if (mSeconds != null) {
            xTextStart = mBigSeconds.draw(canvas, mSeconds, xTextStart, yTextStart);
        }
        if (mHundredths != null) {
            mMedHundredths.draw(canvas, mHundredths, xTextStart, yTextStart);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mRemeasureText = true;
        resetTextSize();
    }
}