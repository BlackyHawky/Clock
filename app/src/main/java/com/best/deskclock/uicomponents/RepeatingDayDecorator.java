// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.uicomponents;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Parcel;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.Weekdays;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.DayViewDecorator;

import java.util.Calendar;

/**
 * A custom calendar day decorator used to visually highlight the specific days of the week when a repeating alarm is scheduled.
 */
public class RepeatingDayDecorator extends DayViewDecorator {

    private final int mRepeatingBits;

    private transient Calendar mCalendar;
    private transient Weekdays mWeekdays;

    private Drawable mActiveDotDefault;
    private Drawable mActiveDotSelected;
    private Drawable mEmptyDot;
    private Drawable mTopSpacer;

    public RepeatingDayDecorator(int repeatingBits) {
        mRepeatingBits = repeatingBits;
        initTransients();
    }

    private void initTransients() {
        mCalendar = Calendar.getInstance();
        mWeekdays = Weekdays.fromBits(mRepeatingBits);
    }

    @Override
    public void initialize(@NonNull Context context) {
        int dotSize = (int) dpToPx(4, context.getResources().getDisplayMetrics());
        int marginBottom = (int) dpToPx(6, context.getResources().getDisplayMetrics());

        // The default green dot
        mActiveDotDefault = createIndicatorDrawable(dotSize, marginBottom, ContextCompat.getColor(context, R.color.calendarDotColor));

        // The selected dot (tinted with the text color on the primary circle)
        int colorOnPrimary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE);
        mActiveDotSelected = createIndicatorDrawable(dotSize, marginBottom, colorOnPrimary);

        // The transparent false dot
        mEmptyDot = createSpacerDrawable(dotSize, marginBottom);

        // The space above the date
        mTopSpacer = createSpacerDrawable(dotSize, marginBottom);
    }

    @Nullable
    @Override
    public Drawable getCompoundDrawableBottom(@NonNull Context context, int year, int month, int day, boolean valid, boolean selected) {
        if (mCalendar == null || mWeekdays == null) {
            initTransients();
        }

        mCalendar.clear();
        mCalendar.set(year, month, day);
        int dayOfWeek = mCalendar.get(Calendar.DAY_OF_WEEK);

        if (mActiveDotDefault == null || mActiveDotSelected == null || mEmptyDot == null) {
            return null;
        }

        if (mWeekdays.isBitOn(dayOfWeek)) {
            return selected ? mActiveDotSelected : mActiveDotDefault;
        } else {
            return mEmptyDot;
        }
    }

    @Nullable
    @Override
    public Drawable getCompoundDrawableTop(@NonNull Context context, int year, int month, int day, boolean valid, boolean selected) {
        return mTopSpacer;
    }

    protected RepeatingDayDecorator(Parcel in) {
        mRepeatingBits = in.readInt();
        initTransients();
    }

    public static final Creator<RepeatingDayDecorator> CREATOR = new Creator<>() {
        @Override
        public RepeatingDayDecorator createFromParcel(Parcel in) {
            return new RepeatingDayDecorator(in);
        }

        @Override
        public RepeatingDayDecorator[] newArray(int size) {
            return new RepeatingDayDecorator[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mRepeatingBits);
    }

    private Drawable createIndicatorDrawable(int dotSize, int marginBottom, @ColorInt int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(color);

        InsetDrawable insetDrawable = new InsetDrawable(shape, 0, 0, 0, marginBottom);
        insetDrawable.setBounds(0, 0, dotSize, dotSize + marginBottom);
        return insetDrawable;
    }

    private Drawable createSpacerDrawable(int dotSize, int marginBottom) {
        Drawable spacer = new ColorDrawable(Color.TRANSPARENT);
        spacer.setBounds(0, 0, dotSize, dotSize + marginBottom);
        return spacer;
    }
}
