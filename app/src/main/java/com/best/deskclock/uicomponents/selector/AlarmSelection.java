/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uicomponents.selector;

import com.best.deskclock.provider.Alarm;

public record AlarmSelection(Alarm mAlarm) {

    /**
     * Created a new selectable item with a visual label and an id.
     * id corresponds to the Alarm id
     */
    public AlarmSelection {
    }

    public Alarm getAlarm() {
        return mAlarm;
    }
}
