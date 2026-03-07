/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;

public class AlarmItemHolder {

    public final Alarm item;
    public final long itemId;
    private final AlarmTimeClickHandler mAlarmTimeClickHandler;
    private final AlarmInstance mAlarmInstance;

    public AlarmItemHolder(Alarm alarm, AlarmInstance alarmInstance, AlarmTimeClickHandler alarmTimeClickHandler) {
        this.item = alarm;
        this.itemId = alarm.id;
        mAlarmTimeClickHandler = alarmTimeClickHandler;
        mAlarmInstance = alarmInstance;
    }

    public AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return mAlarmTimeClickHandler;
    }

    public AlarmInstance getAlarmInstance() {
        return mAlarmInstance;
    }

}
