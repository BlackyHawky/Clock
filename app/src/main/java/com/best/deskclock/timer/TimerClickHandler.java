/*
 * Copyright (C) 2023 The LineageOS Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.timer;

import com.best.deskclock.LabelDialogFragment;
import com.best.deskclock.R;

import androidx.fragment.app.Fragment;

import com.best.deskclock.data.Timer;
import com.best.deskclock.events.Events;

/**
 * Click handler for an alarm time item.
 */
public final class TimerClickHandler {

    private final Fragment mFragment;

    public TimerClickHandler(Fragment fragment) {
        mFragment = fragment;
    }

    public void onEditLabelClicked(Timer timer) {
        Events.sendAlarmEvent(R.string.action_set_label, R.string.label_deskclock);
        final LabelDialogFragment fragment = LabelDialogFragment.newInstance(timer);
        LabelDialogFragment.show(mFragment.getParentFragmentManager(), fragment);
    }
    
}
