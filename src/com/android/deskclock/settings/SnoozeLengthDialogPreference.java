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

import android.content.Context;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;

import com.android.deskclock.R;
import com.android.deskclock.Utils;

public class SnoozeLengthDialogPreference extends DialogPreference {

    private static final String DEFAULT_SNOOZE_TIME = "10";

    public SnoozeLengthDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int getPersistedSnoozeLength() {
        return Integer.parseInt(getPersistedString(DEFAULT_SNOOZE_TIME));
    }

    public void persistSnoozeLength(int snoozeMinutes) {
        persistString(Integer.toString(snoozeMinutes));
    }

    public void updateSummary() {
        final int value = getPersistedSnoozeLength();
        final CharSequence summary = Utils.getNumberFormattedQuantityString(
                getContext(), R.plurals.snooze_duration, value);
        setSummary(summary);
    }
}
