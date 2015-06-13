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
package com.android.deskclock.widget.selector;

import com.android.deskclock.provider.Alarm;

public class AlarmSelection {
    private final String mLabel;
    private final Alarm mAlarm;

    /**
     * Created a new selectable item with a visual label and an id.
     * id corresponds to the Alarm id
     */
    public AlarmSelection(String label, Alarm alarm) {
        mLabel = label;
        mAlarm = alarm;
    }

    public String getLabel() {
        return mLabel;
    }

    public Alarm getAlarm() {
        return mAlarm;
    }
}
