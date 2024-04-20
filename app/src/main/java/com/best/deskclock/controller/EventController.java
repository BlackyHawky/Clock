/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.controller;

import androidx.annotation.StringRes;

import com.best.deskclock.events.EventTracker;

import java.util.ArrayList;
import java.util.Collection;

class EventController {

    private final Collection<EventTracker> mEventTrackers = new ArrayList<>();

    void addEventTracker(EventTracker eventTracker) {
        mEventTrackers.add(eventTracker);
    }

    void sendEvent(@StringRes int category, @StringRes int action, @StringRes int label) {
        for (EventTracker eventTracker : mEventTrackers) {
            eventTracker.sendEvent(category, action, label);
        }
    }
}
