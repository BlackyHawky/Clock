/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.deskclock.controller;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.StringRes;

import com.android.deskclock.Utils;
import com.android.deskclock.events.EventTracker;

import static com.android.deskclock.Utils.enforceMainLooper;

/**
 * Interactions with Android framework components responsible for part of the user experience are
 * handled via this singleton.
 */
public final class Controller {

    private static final Controller sController = new Controller();

    private Context mContext;

    /** The controller that dispatches app events to event trackers. */
    private EventController mEventController;

    /** The controller that interacts with voice interaction sessions on M+. */
    private VoiceController mVoiceController;

    /** The controller that creates and updates launcher shortcuts on N MR1+ */
    private ShortcutController mShortcutController;

    private Controller() {}

    public static Controller getController() {
        return sController;
    }

    public void setContext(Context context) {
        if (mContext != context) {
            mContext = context.getApplicationContext();
            mEventController = new EventController();
            mVoiceController = new VoiceController();
            if (Utils.isNMR1OrLater()) {
                mShortcutController = new ShortcutController(mContext);
            }
        }
    }

    //
    // Event Tracking
    //

    /**
     * @param eventTracker to be registered for tracking application events
     */
    public void addEventTracker(EventTracker eventTracker) {
        enforceMainLooper();
        mEventController.addEventTracker(eventTracker);
    }

    /**
     * @param eventTracker to be unregistered from tracking application events
     */
    public void removeEventTracker(EventTracker eventTracker) {
        enforceMainLooper();
        mEventController.removeEventTracker(eventTracker);
    }

    /**
     * Tracks an event. Events have a category, action and label. This method can be used to track
     * events such as button presses or other user interactions with your application.
     *
     * @param category resource id of event category
     * @param action resource id of event action
     * @param label resource id of event label
     */
    public void sendEvent(@StringRes int category, @StringRes int action, @StringRes int label) {
        mEventController.sendEvent(category, action, label);
    }

    //
    // Voice Interaction
    //

    public void notifyVoiceSuccess(Activity activity, String message) {
        mVoiceController.notifyVoiceSuccess(activity, message);
    }

    public void notifyVoiceFailure(Activity activity, String message) {
        mVoiceController.notifyVoiceFailure(activity, message);
    }

    //
    // Shortcuts
    //

    public void updateShortcuts() {
        enforceMainLooper();
        if (mShortcutController != null) {
            mShortcutController.updateShortcuts();
        }
    }
}