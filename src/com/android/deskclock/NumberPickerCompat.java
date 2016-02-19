/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.NumberPicker;

import java.lang.reflect.Field;

/**
 * Subclass of NumberPicker that allows customizing divider color and saves/restores its value
 * across device rotations.
 */
public class NumberPickerCompat extends NumberPicker implements NumberPicker.OnValueChangeListener {

    private static Field sSelectionDivider;
    private static boolean sTrySelectionDivider = true;

    private final Runnable mAnnounceValueRunnable = new Runnable() {
        @Override
        public void run() {
            if (mOnAnnounceValueChangedListener != null) {
                final int value = getValue();
                final String[] displayedValues = getDisplayedValues();
                final String displayedValue =
                        displayedValues == null ? null : displayedValues[value];
                mOnAnnounceValueChangedListener.onAnnounceValueChanged(
                        NumberPickerCompat.this, value, displayedValue);
            }
        }
    };
    private OnValueChangeListener mOnValueChangedListener;
    private OnAnnounceValueChangedListener mOnAnnounceValueChangedListener;

    public NumberPickerCompat(Context context) {
        this(context, null /* attrs */);
    }

    public NumberPickerCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        tintSelectionDivider(context);
        super.setOnValueChangedListener(this);
    }

    public NumberPickerCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        tintSelectionDivider(context);
        super.setOnValueChangedListener(this);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void tintSelectionDivider(Context context) {
        // Accent color in KK will stay system blue, so leave divider color matching.
        // The divider is correctly tinted to controlColorNormal in M.

        if (Utils.isLOrLMR1() && sTrySelectionDivider) {
            final TypedArray a = context.obtainStyledAttributes(
                    new int[] { android.R.attr.colorControlNormal });
             // White is default color if colorControlNormal is not defined.
            final int color = a.getColor(0, Color.WHITE);
            a.recycle();

            try {
                if (sSelectionDivider == null) {
                    sSelectionDivider = NumberPicker.class.getDeclaredField("mSelectionDivider");
                    sSelectionDivider.setAccessible(true);
                }
                final Drawable selectionDivider = (Drawable) sSelectionDivider.get(this);
                if (selectionDivider != null) {
                    // setTint is API21+, but this will only be called in API21
                    selectionDivider.setTint(color);
                }
            } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
                LogUtils.e("Unable to set selection divider", e);
                sTrySelectionDivider = false;
            }
        }
    }

    /**
     * @return the state of this NumberPicker including the currently selected value
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        return new State(super.onSaveInstanceState(), getValue());
    }

    /**
     * @param state the state of this NumberPicker including the value to select
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        final State instanceState = (State) state;
        super.onRestoreInstanceState(instanceState.getSuperState());
        setValue(instanceState.mValue);
    }

    @Override
    public void setOnValueChangedListener(OnValueChangeListener onValueChangedListener) {
        mOnValueChangedListener = onValueChangedListener;
    }

    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        if (mOnValueChangedListener != null) {
            mOnValueChangedListener.onValueChange(picker, oldVal, newVal);
        }

        // Wait till we reach a value to prevent TalkBack from announcing every intermediate value
        // when scrolling fast.
        removeCallbacks(mAnnounceValueRunnable);
        postDelayed(mAnnounceValueRunnable, 200L);
    }

    /**
     * Register a callback to be invoked whenever a value change should be announced.
     */
    public void setOnAnnounceValueChangedListener(OnAnnounceValueChangedListener listener) {
        mOnAnnounceValueChangedListener = listener;
    }

    /**
     * The state of this NumberPicker including the selected value. Used to preserve values across
     * device rotation.
     */
    private static final class State extends BaseSavedState {

        private final int mValue;

        public State(Parcel source) {
            super(source);
            mValue = source.readInt();
        }

        public State(Parcelable superState, int value) {
            super(superState);
            mValue = value;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mValue);
        }

        public static final Parcelable.Creator<State> CREATOR =
                new Parcelable.Creator<State>() {
                    public State createFromParcel(Parcel in) { return new State(in); }
                    public State[] newArray(int size) { return new State[size]; }
                };
    }

    /**
     * Interface for a callback to be invoked when a value change should be announced for
     * accessibility.
     */
    public interface OnAnnounceValueChangedListener {
        /**
         * Called when a value change should be announced.
         * @param picker The number picker whose value changed.
         * @param value The new value.
         * @param displayedValue The text displayed for the value, or null if the value itself
         *     is displayed.
         */
        void onAnnounceValueChanged(NumberPicker picker, int value, String displayedValue);
    }
}