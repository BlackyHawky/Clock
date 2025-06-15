// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.timer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.annotation.Nullable;

import com.best.deskclock.R;

import java.util.concurrent.TimeUnit;

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

        View rootView = inflate(context, R.layout.timer_spinner_setup_view, this);
        mHourPicker = rootView.findViewById(R.id.hour);
        mMinutePicker = rootView.findViewById(R.id.minute);
        mSecondPicker = rootView.findViewById(R.id.second);

        setupCustomSpinnerDurationPicker();
    }

    private void setupCustomSpinnerDurationPicker() {
        mHourPicker.setMinValue(0);
        mHourPicker.setMaxValue(24);

        mMinutePicker.setMinValue(0);
        mMinutePicker.setMaxValue(59);

        mSecondPicker.setMinValue(0);
        mSecondPicker.setMaxValue(59);

        mHourPicker.setOnValueChangedListener((_picker, _oldVal, _newVal) -> {
            if (mOnValueChangeListener != null) mOnValueChangeListener.onValueChange(getValue());
        });

        mMinutePicker.setOnValueChangedListener((_picker, _oldVal, _newVal) -> {
            if (mOnValueChangeListener != null) mOnValueChangeListener.onValueChange(getValue());
        });

        mSecondPicker.setOnValueChangedListener((_picker, _oldVal, _newVal) -> {
            if (mOnValueChangeListener != null) mOnValueChangeListener.onValueChange(getValue());
        });
    }

    public void setValue(long valueMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(valueMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(valueMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(valueMillis) % 60;
        setValue(new DurationObject((int) hours, (int) minutes, (int) seconds));
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
        this.mOnValueChangeListener = onValueChangeListener;
    }

    public interface OnValueChangeListener {
        void onValueChange(DurationObject duration);
    }

    public record DurationObject(int hour, int minute, int second) {
        public long toMillis() {
            return (((hour * 60L) + minute) * 60 + second) * 1000;
        }
    }

}
