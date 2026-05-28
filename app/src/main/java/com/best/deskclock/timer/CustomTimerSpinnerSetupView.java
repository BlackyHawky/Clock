// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.annotation.Nullable;

import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.databinding.TimerSpinnerSetupViewBinding;
import com.best.deskclock.utils.ThemeUtils;

/**
 * Custom component to display a time selection view using spinners used when creating timers.
 */
public class CustomTimerSpinnerSetupView extends LinearLayout {

    private final TimerSpinnerSetupViewBinding mBinding;

    @Nullable
    OnValueChangeListener mOnValueChangeListener;

    public CustomTimerSpinnerSetupView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        SharedPreferences prefs = getDefaultSharedPreferences(context);
        Typeface typeFace = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        mBinding = TimerSpinnerSetupViewBinding.inflate(LayoutInflater.from(context), this, true);

        mBinding.hourTitle.setTypeface(typeFace);
        mBinding.minuteTitle.setTypeface(typeFace);
        mBinding.secondTitle.setTypeface(typeFace);

        setupCustomSpinnerDurationPicker();
    }

    private void setupCustomSpinnerDurationPicker() {
        mBinding.hourPicker.setMinValue(0);
        mBinding.hourPicker.setMaxValue(99);

        mBinding.minutePicker.setMinValue(0);
        mBinding.minutePicker.setMaxValue(99);

        mBinding.secondPicker.setMinValue(0);
        mBinding.secondPicker.setMaxValue(99);

        NumberPicker.OnValueChangeListener listener = (picker, oldVal, newVal) -> {
            picker.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            if (mOnValueChangeListener != null) {
                mOnValueChangeListener.onValueChange();
            }
        };

        mBinding.hourPicker.setOnValueChangedListener(listener);
        mBinding.minutePicker.setOnValueChangedListener(listener);
        mBinding.secondPicker.setOnValueChangedListener(listener);
    }

    public void setValue(DurationObject value) {
        mBinding.hourPicker.setValue(value.hour());
        mBinding.minutePicker.setValue(value.minute());
        mBinding.secondPicker.setValue(value.second());
    }

    public void reset() {
        setValue(new DurationObject(0, 0, 0));
    }

    public DurationObject getValue() {
        return new DurationObject(mBinding.hourPicker.getValue(), mBinding.minutePicker.getValue(), mBinding.secondPicker.getValue());
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
