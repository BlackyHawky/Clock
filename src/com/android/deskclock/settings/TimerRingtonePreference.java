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
import android.content.Intent;
import android.media.RingtoneManager;
import android.preference.RingtonePreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;

import com.android.deskclock.data.DataModel;

/**
 * A custom RingtonePreference that presents the application's default timer ringtone as the value
 * behind the default selection.
 */
public final class TimerRingtonePreference extends RingtonePreference {

    public TimerRingtonePreference(Context context) {
        super(context);
    }

    public TimerRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimerRingtonePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public TimerRingtonePreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onPrepareRingtonePickerIntent(@NonNull Intent ringtonePickerIntent) {
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);

        // Replace the default ringtone uri with the beeping ringtone for timers.
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                DataModel.getDataModel().getDefaultTimerRingtoneUri());
    }
}