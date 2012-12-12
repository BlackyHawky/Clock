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
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import com.android.deskclock.Log;
import com.android.deskclock.R;
import com.android.deskclock.Utils;


public class CountingTimerView extends View {
    private static final String TWO_DIGITS = "%02d";
    private static final String ONE_DIGIT = "%01d";
    private static final String NEG_TWO_DIGITS = "-%02d";
    private static final String NEG_ONE_DIGIT = "-%01d";
    private static final float TEXT_SIZE_TO_WIDTH_RATIO = 0.75f;
    // This is the ratio of the font typeface we need to offset the font by vertically to align it
    // vertically center.
    private static final float FONT_VERTICAL_OFFSET = 0.14f;

    private String mHours, mMinutes, mSeconds, mHundredths;

    private boolean mShowTimeStr = true;
    private final Typeface mAndroidClockMonoThin, mAndroidClockMonoBold, mAndroidClockMonoLight;
    private final Typeface mRobotoLabel;
    private final Paint mPaintBig = new Paint();
    private final Paint mPaintBigThin = new Paint();
    private final Paint mPaintMed = new Paint();
    private final Paint mPaintLabel = new Paint();
    private final float mBigFontSize, mSmallFontSize;
    private final SignedTime mBigHours, mBigMinutes;
    private final UnsignedTime mBigThinSeconds;
    private final Hundredths mMedHundredths;
    private float mTextHeight = 0;
    private float mTotalTextWidth;
    private static final String HUNDREDTH_SEPERATOR = ".";
    private boolean mRemeasureText = true;

    private int mDefaultColor;
    private final int mPressedColor;
    private final int mWhiteColor;
    private final int mRedColor;
    private TextView mStopStartTextView;
    private final AccessibilityManager mAccessibilityManager;

    // Fields for the text serving as a virtual button.
    private boolean mVirtualButtonEnabled = false;
    private boolean mVirtualButtonPressedOn = false;

    Runnable mBlinkThread = new Runnable() {
        private boolean mVisible = true;
        @Override
        public void run() {
            mVisible = !mVisible;
            CountingTimerView.this.showTime(mVisible);
            postDelayed(mBlinkThread, 500);
        }

    };

    class UnsignedTime {
        protected Paint mPaint;
        protected float mEm;
        protected float mWidth = 0;
        private final String mWidest;
        protected String mLabel;
        private float mLabelWidth = 0;

        public UnsignedTime(Paint paint, final String label, String allDigits) {
            mPaint = paint;
            mLabel = label;

            if (TextUtils.isEmpty(allDigits)) {
                Log.wtf("Locale digits missing - using English");
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

        public UnsignedTime(UnsignedTime unsignedTime, final String label) {
            this.mPaint = unsignedTime.mPaint;
            this.mEm = unsignedTime.mEm;
            this.mWidth = unsignedTime.mWidth;
            this.mWidest = unsignedTime.mWidest;
            this.mLabel = label;
        }

        protected void updateWidth(final String time) {
            mEm = mPaint.measureText(mWidest);
            mLabelWidth = mLabel == null ? 0 : mPaintLabel.measureText(mLabel);
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

        public float getWidth() {
            return mWidth;
        }

        public float getLabelWidth() {
            return mLabelWidth;
        }

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

        public float draw(Canvas canvas, final String time, float x, float y, float yLabel) {
            x = drawTime(canvas, time, 0, x, y);
            if (mLabel != null ) {
                canvas.drawText(mLabel, x, yLabel, mPaintLabel);
            }
            return x + getLabelWidth();
        }
    }

    class Hundredths extends UnsignedTime {
        public Hundredths(Paint paint, final String label, final String allDigits) {
            super(paint, label, allDigits);
        }

        @Override
        public float draw(Canvas canvas, final String time, float x, float y, float yLabel) {
            if (mLabel != null) {
                canvas.drawText(mLabel, x, yLabel, mPaintLabel);
            }
            return drawTime(canvas, time, 0, x + getLabelWidth(), y);
        }
    }

    class SignedTime extends UnsignedTime {
        private float mMinusWidth = 0;

        public SignedTime(Paint paint, final String label, final String allDigits) {
            super(paint, label, allDigits);
        }

        public SignedTime (SignedTime signedTime, final String label) {
            super(signedTime, label);
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

        @Override
        public float draw(Canvas canvas, final String time, float x, float y, float yLabel) {
            int ii = 0;
            if (mMinusWidth != 0f) {
                float minusWidth = mMinusWidth / 2;
                x += minusWidth;
                canvas.drawText(time.substring(ii, ii + 1), x, y, mPaint);
                x += minusWidth;
                ii++;
            }
            x = drawTime(canvas, time, ii, x, y);
            if (mLabel != null) {
                canvas.drawText(mLabel, x, yLabel, mPaintLabel);
            }
            return x + getLabelWidth();
        }
    }

    public CountingTimerView(Context context) {
        this(context, null);
    }

    public CountingTimerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mAndroidClockMonoThin = Typeface.createFromAsset(
                context.getAssets(), "fonts/AndroidClockMono-Thin.ttf");
        mAndroidClockMonoBold = Typeface.createFromAsset(
                context.getAssets(), "fonts/AndroidClockMono-Bold.ttf");
        mAndroidClockMonoLight = Typeface.createFromAsset(
                context.getAssets(), "fonts/AndroidClockMono-Light.ttf");
        mAccessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        mRobotoLabel= Typeface.create("sans-serif-condensed", Typeface.BOLD);
        Resources r = context.getResources();
        mWhiteColor = r.getColor(R.color.clock_white);
        mDefaultColor = mWhiteColor;
        mPressedColor = r.getColor(Utils.getPressedColorId());
        mRedColor = r.getColor(R.color.clock_red);

        mPaintBig.setAntiAlias(true);
        mPaintBig.setStyle(Paint.Style.STROKE);
        mPaintBig.setTextAlign(Paint.Align.CENTER);
        mPaintBig.setTypeface(mAndroidClockMonoBold);
        mBigFontSize = r.getDimension(R.dimen.big_font_size);
        mSmallFontSize = r.getDimension(R.dimen.small_font_size);

        mPaintBigThin.setAntiAlias(true);
        mPaintBigThin.setStyle(Paint.Style.STROKE);
        mPaintBigThin.setTextAlign(Paint.Align.CENTER);
        mPaintBigThin.setTypeface(mAndroidClockMonoThin);

        mPaintMed.setAntiAlias(true);
        mPaintMed.setStyle(Paint.Style.STROKE);
        mPaintMed.setTextAlign(Paint.Align.CENTER);
        mPaintMed.setTypeface(mAndroidClockMonoLight);

        mPaintLabel.setAntiAlias(true);
        mPaintLabel.setStyle(Paint.Style.STROKE);
        mPaintLabel.setTextAlign(Paint.Align.LEFT);
        mPaintLabel.setTypeface(mRobotoLabel);
        mPaintLabel.setTextSize(r.getDimension(R.dimen.label_font_size));

        resetTextSize();
        setTextColor(mDefaultColor);

        // allDigits will contain ten digits: "0123456789" in the default locale
        final String allDigits = String.format("%010d", 123456789);
        mBigHours = new SignedTime(mPaintBig,
                r.getString(R.string.hours_label).toUpperCase(), allDigits);
        mBigMinutes = new SignedTime(mBigHours,
                r.getString(R.string.minutes_label).toUpperCase());
        mBigThinSeconds = new UnsignedTime(mPaintBigThin,
                r.getString(R.string.seconds_label).toUpperCase(), allDigits);
        mMedHundredths = new Hundredths(mPaintMed, HUNDREDTH_SEPERATOR, allDigits);
    }

    protected void resetTextSize() {
        mPaintBig.setTextSize(mBigFontSize);
        mTextHeight = mBigFontSize;
        mPaintBigThin.setTextSize(mBigFontSize);
        mPaintMed.setTextSize(mSmallFontSize);
    }

    protected void setTextColor(int textColor) {
        mPaintBig.setColor(textColor);
        mPaintBigThin.setColor(textColor);
        mPaintMed.setColor(textColor);
        mPaintLabel.setColor(textColor);
    }

    public void setTime(long time, boolean showHundredths, boolean update) {
        boolean neg = false, showNeg = false;
        String format = null;
        if (time < 0) {
            time = -time;
            neg = showNeg = true;
        }
        long hundreds, seconds, minutes, hours;
        seconds = time / 1000;
        hundreds = (time - seconds * 1000) / 10;
        minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 999) {
            hours = 0;
        }
        // time may less than a second below zero, since we do not show fractions of seconds
        // when counting down, do not show the minus sign.
        if (hours ==0 && minutes == 0 && seconds == 0) {
            showNeg = false;
        }

        if (!showHundredths) {
            if (!neg && hundreds != 0) {
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
            if (hundreds < 10 || hundreds > 90) {
                update = true;
            }
        }

        int oldLength = getDigitsLength();

        if (hours >= 10) {
            format = showNeg ? NEG_TWO_DIGITS : TWO_DIGITS;
            mHours = String.format(format, hours);
        } else if (hours > 0) {
            format = showNeg ? NEG_ONE_DIGIT : ONE_DIGIT;
            mHours = String.format(format, hours);
        } else {
            mHours = null;
        }

        if (minutes >= 10 || hours > 0) {
            format = (showNeg && hours == 0) ? NEG_TWO_DIGITS : TWO_DIGITS;
            mMinutes = String.format(format, minutes);
        } else {
            format = (showNeg && hours == 0) ? NEG_ONE_DIGIT : ONE_DIGIT;
            mMinutes = String.format(format, minutes);
        }

        mSeconds = String.format(TWO_DIGITS, seconds);
        if (showHundredths) {
            mHundredths = String.format(TWO_DIGITS, hundreds);
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

        if (update) {
            setContentDescription(getTimeStringForAccessibility((int) hours, (int) minutes,
                    (int) seconds, showNeg, getResources()));
            invalidate();
        }
    }

    private int getDigitsLength() {
        return ((mHours == null) ? 0 : mHours.length())
                + ((mMinutes == null) ? 0 : mMinutes.length())
                + ((mSeconds == null) ? 0 : mSeconds.length())
                + ((mHundredths == null) ? 0 : mHundredths.length());
    }

    private void calcTotalTextWidth() {
        mTotalTextWidth = mBigHours.calcTotalWidth(mHours) + mBigMinutes.calcTotalWidth(mMinutes)
                + mBigThinSeconds.calcTotalWidth(mSeconds)
                + mMedHundredths.calcTotalWidth(mHundredths);
    }

    private void setTotalTextWidth() {
        calcTotalTextWidth();
        // To determine the maximum width, we find the minimum of the height and width (since the
        // circle we are trying to fit the text into has its radius sized to the smaller of the
        // two.
        int width = Math.min(getWidth(), getHeight());
        if (width != 0) {
            float wantWidth = (int)(TEXT_SIZE_TO_WIDTH_RATIO * width);
            // If the text is too wide, reduce all the paint text sizes
            while (mTotalTextWidth > wantWidth) {
                // Get fixed and variant parts of the total size
                float fixedWidths = mBigHours.getLabelWidth() + mBigMinutes.getLabelWidth()
                        + mBigThinSeconds.getLabelWidth() + mMedHundredths.getLabelWidth();
                float varWidths = mBigHours.getWidth() + mBigMinutes.getWidth()
                        + mBigThinSeconds.getWidth() + mMedHundredths.getWidth();
                // Avoid divide by zero || sizeRatio == 1 || sizeRatio <= 0
                if (varWidths == 0 || fixedWidths == 0 || fixedWidths >= wantWidth) {
                    break;
                }
                // Variant-section reduction
                float sizeRatio = (wantWidth - fixedWidths) / varWidths;
                mPaintBig.setTextSize(mPaintBig.getTextSize() * sizeRatio);
                mPaintBigThin.setTextSize(mPaintBigThin.getTextSize() * sizeRatio);
                mPaintMed.setTextSize(mPaintMed.getTextSize() * sizeRatio);
                //recalculate the new total text width and half text height
                mTextHeight = mPaintBig.getTextSize();
                calcTotalTextWidth();
            }
        }
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

    public void redTimeStr(boolean red, boolean forceUpdate) {
        mDefaultColor = red ? mRedColor : mWhiteColor;
        setTextColor(mDefaultColor);
        if (forceUpdate) {
            invalidate();
        }
    }

    public String getTimeString() {
        // Though only called from Stopwatch Share, so hundredth are never null,
        // protect the future and check for null mHundredths
        if (mHundredths == null) {
            if (mHours == null) {
                return String.format("%s:%s", mMinutes, mSeconds);
            }
            return String.format("%s:%s:%s", mHours, mMinutes, mSeconds);
        } else if (mHours == null) {
            return String.format("%s:%s.%s", mMinutes, mSeconds, mHundredths);
        }
        return String.format("%s:%s:%s.%s", mHours, mMinutes, mSeconds, mHundredths);
    }

    private static String getTimeStringForAccessibility(int hours, int minutes, int seconds,
            boolean showNeg, Resources r) {
        StringBuilder s = new StringBuilder();
        if (showNeg) {
            // This must be followed by a non-zero number or it will be audible as "hyphen"
            // instead of "minus".
            s.append("-");
        }
        if (showNeg && hours == 0 && minutes == 0) {
            // Non-negative time will always have minutes, eg. "0 minutes 7 seconds", but negative
            // time must start with non-zero digit, eg. -0m7s will be audible as just "-7 seconds"
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nseconds_description, seconds).toString(),
                    seconds));
        } else if (hours == 0) {
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nminutes_description, minutes).toString(),
                    minutes));
            s.append(" ");
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nseconds_description, seconds).toString(),
                    seconds));
        } else {
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nhours_description, hours).toString(),
                    hours));
            s.append(" ");
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nminutes_description, minutes).toString(),
                    minutes));
            s.append(" ");
            s.append(String.format(
                    r.getQuantityText(R.plurals.Nseconds_description, seconds).toString(),
                    seconds));
        }
        return s.toString();
    }

    public void setVirtualButtonEnabled(boolean enabled) {
        mVirtualButtonEnabled = enabled;
    }

    private void virtualButtonPressed(boolean pressedOn) {
        mVirtualButtonPressedOn = pressedOn;
        mStopStartTextView.setTextColor(pressedOn ? mPressedColor : mWhiteColor);
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

        float textXstart = xCenter - mTotalTextWidth / 2;
        float textYstart = yCenter + mTextHeight/2 - (mTextHeight * FONT_VERTICAL_OFFSET);
        // align the labels vertically to the top of the rest of the text
        float labelYStart = textYstart - (mTextHeight * (1 - 2 * FONT_VERTICAL_OFFSET))
                + (1 - 2 * FONT_VERTICAL_OFFSET) * mPaintLabel.getTextSize();

        // Text color differs based on pressed state.
        int textColor;
        if (mVirtualButtonPressedOn) {
            textColor = mPressedColor;
            mStopStartTextView.setTextColor(mPressedColor);
        } else {
            textColor = mDefaultColor;
        }
        mPaintBig.setColor(textColor);
        mPaintBigThin.setColor(textColor);
        mPaintLabel.setColor(textColor);
        mPaintMed.setColor(textColor);

        if (mHours != null) {
            textXstart = mBigHours.draw(canvas, mHours, textXstart, textYstart, labelYStart);
        }
        if (mMinutes != null) {
            textXstart = mBigMinutes.draw(canvas, mMinutes, textXstart, textYstart, labelYStart);
        }
        if (mSeconds != null) {
            textXstart = mBigThinSeconds.draw(canvas, mSeconds,
                    textXstart, textYstart, labelYStart);
        }
        if (mHundredths != null) {
            textXstart = mMedHundredths.draw(canvas, mHundredths,
                    textXstart, textYstart, textYstart);
        }
    }

    public void registerStopTextView(TextView stopStartTextView) {
        mStopStartTextView = stopStartTextView;
    }
}
