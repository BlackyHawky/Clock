/*
 * Copyright (C) 2023 The LineageOS Project
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
