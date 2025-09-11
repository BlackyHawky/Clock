/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents.selector;

import com.best.deskclock.provider.Alarm;

public class AlarmSelection {
    private final Alarm mAlarm;

    /**
     * Created a new selectable item with a visual label and an id.
     * id corresponds to the Alarm id
     */
    public AlarmSelection(Alarm alarm) {
        mAlarm = alarm;
    }

    public Alarm getAlarm() {
        return mAlarm;
    }
}
