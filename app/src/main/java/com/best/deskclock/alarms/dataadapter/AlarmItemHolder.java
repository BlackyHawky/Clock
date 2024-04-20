/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms.dataadapter;

import android.os.Bundle;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.alarms.AlarmTimeClickHandler;
import com.best.deskclock.provider.Alarm;

public class AlarmItemHolder extends ItemAdapter.ItemHolder<Alarm> {

    private static final java.lang.String EXPANDED_KEY = "expanded";
    private final AlarmTimeClickHandler mAlarmTimeClickHandler;
    private boolean mExpanded;

    public AlarmItemHolder(Alarm alarm, AlarmTimeClickHandler alarmTimeClickHandler) {
        super(alarm, alarm.id);
        mAlarmTimeClickHandler = alarmTimeClickHandler;
    }

    @Override
    public int getItemViewType() {
        return isExpanded() ?
                ExpandedAlarmViewHolder.VIEW_TYPE : CollapsedAlarmViewHolder.VIEW_TYPE;
    }

    public AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return mAlarmTimeClickHandler;
    }

    public void expand() {
        if (!isExpanded()) {
            mExpanded = true;
            notifyItemChanged();
        }
    }

    public void collapse() {
        if (isExpanded()) {
            mExpanded = false;
            notifyItemChanged();
        }
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(EXPANDED_KEY, mExpanded);
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        mExpanded = bundle.getBoolean(EXPANDED_KEY);
    }
}
