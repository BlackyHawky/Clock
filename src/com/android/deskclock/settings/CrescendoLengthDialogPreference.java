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
import com.android.deskclock.uidata.UiDataModel;

public class CrescendoLengthDialogPreference extends DialogPreference {

    private static final String DEFAULT_CRESCENDO_TIME = "0";

    public CrescendoLengthDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public int getPersistedCrescendoLength() {
        return Integer.parseInt(getPersistedString(DEFAULT_CRESCENDO_TIME));
    }

    public void persistCrescendoLength(int crescendoSeconds) {
        persistString(Integer.toString(crescendoSeconds));
    }

    public void updateSummary() {
        final int crescendoSeconds = getPersistedCrescendoLength();
        if (crescendoSeconds == 0) {
            setSummary(getContext().getString(R.string.no_crescendo_duration));
        } else {
            final String length = UiDataModel.getUiDataModel().getFormattedNumber(crescendoSeconds);
            setSummary(getContext().getString(R.string.crescendo_duration, length));
        }
    }
}
