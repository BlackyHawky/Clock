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

import android.os.Bundle;
import android.support.v14.preference.PreferenceDialogFragment;
import android.support.v7.preference.Preference;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.android.deskclock.NumberPickerCompat;
import com.android.deskclock.R;
import com.android.deskclock.uidata.UiDataModel;

public class CrescendoLengthDialogFragment extends PreferenceDialogFragment {

    private static final int CRESCENDO_TIME_STEP = 5;

    private NumberPickerCompat mNumberPickerView;

    public static PreferenceDialogFragment newInstance(Preference preference) {
        final PreferenceDialogFragment fragment = new CrescendoLengthDialogFragment();

        final Bundle bundle = new Bundle();
        bundle.putString(ARG_KEY, preference.getKey());
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        final CrescendoLengthDialogPreference preference =
                (CrescendoLengthDialogPreference) getPreference();
        final int crescendoSeconds = preference.getPersistedCrescendoLength();

        final TextView unitView = (TextView) view.findViewById(R.id.title);
        unitView.setText(R.string.crescendo_picker_label);
        updateUnits(unitView, crescendoSeconds);

        final String[] displayedValues = new String[13];
        displayedValues[0] = getString(R.string.no_crescendo_duration);
        for (int i = 1; i < displayedValues.length; i++) {
            final int length = i * CRESCENDO_TIME_STEP;
            displayedValues[i] = UiDataModel.getUiDataModel().getFormattedNumber(length);
        }

        mNumberPickerView = (NumberPickerCompat) view.findViewById(R.id.seconds_picker);
        mNumberPickerView.setDisplayedValues(displayedValues);
        mNumberPickerView.setMinValue(0);
        mNumberPickerView.setMaxValue(displayedValues.length - 1);
        mNumberPickerView.setValue(crescendoSeconds / CRESCENDO_TIME_STEP);

        mNumberPickerView.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                updateUnits(unitView, newVal);
            }
        });
        mNumberPickerView.setOnAnnounceValueChangedListener(
                new NumberPickerCompat.OnAnnounceValueChangedListener() {
            @Override
            public void onAnnounceValueChanged(NumberPicker picker, int value,
                    String displayedValue) {
                final String announceString;
                if (value == 0) {
                    announceString = getString(R.string.no_crescendo_duration);
                } else {
                    announceString = getString(R.string.crescendo_duration, displayedValue);
                }
                picker.announceForAccessibility(announceString);
            }
        });
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            final CrescendoLengthDialogPreference preference =
                    (CrescendoLengthDialogPreference) getPreference();
            preference.persistCrescendoLength(mNumberPickerView.getValue() * CRESCENDO_TIME_STEP);
            preference.updateSummary();
        }
    }

    private void updateUnits(TextView unitView, int crescendoSeconds) {
        final int visibility = crescendoSeconds == 0 ? View.INVISIBLE : View.VISIBLE;
        unitView.setVisibility(visibility);
    }
}
