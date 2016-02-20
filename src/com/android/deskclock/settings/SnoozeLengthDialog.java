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

package com.android.deskclock.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.android.deskclock.NumberPickerCompat;
import com.android.deskclock.R;
import com.android.deskclock.Utils;

/**
 * A dialog preference that shows a number picker for selecting snooze length
 */
public final class SnoozeLengthDialog extends DialogPreference {

    private static final String DEFAULT_SNOOZE_TIME = "10";

    private NumberPickerCompat mNumberPickerView;
    private TextView mNumberPickerMinutesView;
    private final Context mContext;
    private int mSnoozeMinutes;

    public SnoozeLengthDialog(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setDialogLayoutResource(R.layout.snooze_length_picker);
        setTitle(R.string.snooze_duration_title);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle(getContext().getString(R.string.snooze_duration_title))
                .setCancelable(true);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        mNumberPickerMinutesView = (TextView) view.findViewById(R.id.title);
        mNumberPickerView = (NumberPickerCompat) view.findViewById(R.id.minutes_picker);
        mNumberPickerView.setMinValue(1);
        mNumberPickerView.setMaxValue(30);
        mNumberPickerView.setValue(mSnoozeMinutes);
        updateUnits();

        mNumberPickerView.setOnAnnounceValueChangedListener(
                new NumberPickerCompat.OnAnnounceValueChangedListener() {
            @Override
            public void onAnnounceValueChanged(NumberPicker picker, int value,
                    String displayedValue) {
                final String announceString = Utils.getNumberFormattedQuantityString(
                        mContext, R.plurals.snooze_duration, value);
                picker.announceForAccessibility(announceString);
            }
        });
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        String val;
        if (restorePersistedValue) {
            val = getPersistedString(DEFAULT_SNOOZE_TIME);
            if (val != null) {
                mSnoozeMinutes = Integer.parseInt(val);
            }
        } else {
            val = (String) defaultValue;
            if (val != null) {
                mSnoozeMinutes = Integer.parseInt(val);
            }
            persistString(val);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Restore the value to the NumberPicker.
        super.onRestoreInstanceState(state);

        // Update the unit display in response to the new value.
        updateUnits();
    }

    private void updateUnits() {
        if (mNumberPickerView != null) {
            final Resources res = mContext.getResources();
            final int value = mNumberPickerView.getValue();
            final CharSequence units = res.getQuantityText(R.plurals.snooze_picker_label, value);
            mNumberPickerMinutesView.setText(units);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mNumberPickerView.clearFocus();
            mSnoozeMinutes = mNumberPickerView.getValue();
            persistString(Integer.toString(mSnoozeMinutes));
            setSummary();
        }
    }

    public void setSummary() {
        setSummary(Utils.getNumberFormattedQuantityString(mContext, R.plurals.snooze_duration,
                mSnoozeMinutes));
    }
}