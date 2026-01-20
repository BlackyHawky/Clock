// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.ThemeUtils;

/**
 * Custom component to display a time selection view using spinners used when creating timers.
 */
public class CustomTimerSpinnerSetupView extends LinearLayout {

    private final NumberPicker mHourPicker;
    private final NumberPicker mMinutePicker;
    private final NumberPicker mSecondPicker;

    @Nullable
    OnValueChangeListener mOnValueChangeListener;

    public CustomTimerSpinnerSetupView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        Typeface typeFace = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        View rootView = inflate(context, R.layout.timer_spinner_setup_view, this);

        int paddingLeft = (int) dpToPx(ThemeUtils.isTablet() ? 120 : 20, displayMetrics);

        int paddingRight = (int) dpToPx(ThemeUtils.isTablet()
                ? 120
                : ThemeUtils.isLandscape()
                ? 90
                : 20, displayMetrics
        );

        int paddingBottom = (int) dpToPx(ThemeUtils.isTablet() && ThemeUtils.isLandscape()
                ? 60
                : 0, displayMetrics
        );

        rootView.setPadding(paddingLeft, 0, paddingRight, paddingBottom);

        TextView hourTitle = rootView.findViewById(R.id.hour_title);
        TextView minuteTitle = rootView.findViewById(R.id.minute_title);
        TextView secondTitle = rootView.findViewById(R.id.second_title);

        hourTitle.setTypeface(typeFace);
        minuteTitle.setTypeface(typeFace);
        secondTitle.setTypeface(typeFace);

        mHourPicker = rootView.findViewById(R.id.hour);
        mMinutePicker = rootView.findViewById(R.id.minute);
        mSecondPicker = rootView.findViewById(R.id.second);

        setupCustomSpinnerDurationPicker();
    }

    private void setupCustomSpinnerDurationPicker() {
        mHourPicker.setMinValue(0);
        mHourPicker.setMaxValue(99);

        mMinutePicker.setMinValue(0);
        mMinutePicker.setMaxValue(99);

        mSecondPicker.setMinValue(0);
        mSecondPicker.setMaxValue(99);

        mHourPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);

            if (mOnValueChangeListener != null) {
                mOnValueChangeListener.onValueChange();
            }
        });

        mMinutePicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);

            if (mOnValueChangeListener != null) {
                mOnValueChangeListener.onValueChange();
            }
        });

        mSecondPicker.setOnValueChangedListener((picker, oldVal, newVal) -> {
            picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);

            if (mOnValueChangeListener != null) {
                mOnValueChangeListener.onValueChange();
            }
        });
    }

    public void setValue(DurationObject value) {
        mHourPicker.setValue(value.hour());
        mMinutePicker.setValue(value.minute());
        mSecondPicker.setValue(value.second());
    }

    public void reset() {
        setValue(new DurationObject(0, 0, 0));
    }

    public DurationObject getValue() {
        return new DurationObject(mHourPicker.getValue(), mMinutePicker.getValue(), mSecondPicker.getValue());
    }

    public void setOnChangeListener(OnValueChangeListener onValueChangeListener) {
        mOnValueChangeListener = onValueChangeListener;
    }

    public interface OnValueChangeListener {
        void onValueChange();
    }

    public record DurationObject(int hour, int minute, int second) {
        public long toMillis() {
            return (((hour * 60L) + minute) * 60 + second) * 1000;
        }
    }

}
