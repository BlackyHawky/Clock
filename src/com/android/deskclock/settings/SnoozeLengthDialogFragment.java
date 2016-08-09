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
import com.android.deskclock.Utils;

public class SnoozeLengthDialogFragment extends PreferenceDialogFragment {

    private NumberPickerCompat mNumberPickerView;

    public static PreferenceDialogFragment newInstance(Preference preference) {
        final PreferenceDialogFragment fragment = new SnoozeLengthDialogFragment();

        final Bundle bundle = new Bundle();
        bundle.putString(ARG_KEY, preference.getKey());
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    protected void onBindDialogView(View view) {
        final SnoozeLengthDialogPreference preference =
                (SnoozeLengthDialogPreference) getPreference();
        final int snoozeMinutes = preference.getPersistedSnoozeLength();

        final CharSequence units =
                getResources().getQuantityText(R.plurals.snooze_picker_label, snoozeMinutes);
        final TextView unitView = (TextView) view.findViewById(R.id.title);
        unitView.setText(units);

        mNumberPickerView = (NumberPickerCompat) view.findViewById(R.id.minutes_picker);
        mNumberPickerView.setMinValue(1);
        mNumberPickerView.setMaxValue(30);
        mNumberPickerView.setValue(snoozeMinutes);

        mNumberPickerView.setOnAnnounceValueChangedListener(
                new NumberPickerCompat.OnAnnounceValueChangedListener() {
                    @Override
                    public void onAnnounceValueChanged(NumberPicker picker, int value,
                            String displayedValue) {
                        final String announceString = Utils.getNumberFormattedQuantityString(
                                getActivity(), R.plurals.snooze_duration, value);
                        picker.announceForAccessibility(announceString);
                    }
                });
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            final SnoozeLengthDialogPreference preference =
                    (SnoozeLengthDialogPreference) getPreference();
            preference.persistSnoozeLength(mNumberPickerView.getValue());
            preference.updateSummary();
        }
    }
}
