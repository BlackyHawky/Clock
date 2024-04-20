/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.controller;

import static com.best.deskclock.Utils.enforceMainLooper;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.annotation.StringRes;

import com.best.deskclock.events.EventTracker;

/**
 * Interactions with Android framework components responsible for part of the user experience are
 * handled via this singleton.
 */
public final class Controller {

    private static final Controller sController = new Controller();

    private Context mContext;

    /**
     * The controller that dispatches app events to event trackers.
     */
    private EventController mEventController;

    /**
     * The controller that interacts with voice interaction sessions on M+.
     */
    private VoiceController mVoiceController;

    /**
     * The controller that creates and updates launcher shortcuts on N MR1+
     */
    private ShortcutController mShortcutController;

    private Controller() {
    }

    public static Controller getController() {
        return sController;
    }

    public void setContext(Context context) {
        if (mContext != context) {
            mContext = context.getApplicationContext();
            mEventController = new EventController();
            mVoiceController = new VoiceController();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
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
     * Tracks an event. Events have a category, action and label. This method can be used to track
     * events such as button presses or other user interactions with your application.
     *
     * @param category resource id of event category
     * @param action   resource id of event action
     * @param label    resource id of event label
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
