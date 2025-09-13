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
import com.best.deskclock.provider.AlarmInstance;

public class AlarmItemHolder extends ItemAdapter.ItemHolder<Alarm> {

    private static final java.lang.String EXPANDED_KEY = "expanded";
    private final AlarmTimeClickHandler mAlarmTimeClickHandler;
    private final AlarmInstance mAlarmInstance;
    private OnAlarmCollapseListener mCollapseListener;
    private boolean mExpanded;

    public AlarmItemHolder(Alarm alarm, AlarmInstance alarmInstance, AlarmTimeClickHandler alarmTimeClickHandler) {
        super(alarm, alarm.id);
        mAlarmTimeClickHandler = alarmTimeClickHandler;
        mAlarmInstance = alarmInstance;
    }

    @Override
    public int getItemViewType() {
        return isExpanded() ?
                ExpandedAlarmViewHolder.VIEW_TYPE : CollapsedAlarmViewHolder.VIEW_TYPE;
    }

    public AlarmTimeClickHandler getAlarmTimeClickHandler() {
        return mAlarmTimeClickHandler;
    }

    public AlarmInstance getAlarmInstance() {
        return mAlarmInstance;
    }

    public long getStableId() {
        return itemId;
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
            if (mCollapseListener != null) {
                mCollapseListener.onAlarmCollapsed();
            }
        }
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    /**
     * Registers a listener to be notified when this alarm item is collapsed.
     *
     * @param listener the listener to invoke when the alarm collapses
     */
    public void setOnAlarmCollapseListener(OnAlarmCollapseListener listener) {
        this.mCollapseListener = listener;
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

    /**
     * Listener interface for receiving a callback when an alarm item is collapsed.
     */
    public interface OnAlarmCollapseListener {

        /**
         * Called when the alarm item has been collapsed.
         * Typically used to trigger UI updates or resume sorting.
         */
        void onAlarmCollapsed();
    }
}
