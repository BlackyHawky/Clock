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

import com.android.deskclock.Utils;

import static com.android.deskclock.Utils.enforceMainLooper;

/**
 * Interactions with Android framework components responsible for part of the user experience are
 * handled via this singleton.
 */
public final class Controller implements VoiceController {

    private static final Controller sController = new Controller();

    private Context mContext;

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
            if (Utils.isMOrLater()) {
                mVoiceController = new DefaultVoiceController();
            }
            if (Utils.isNMR1OrLater()) {
                mShortcutController = new ShortcutController(mContext);
            }
        }
    }

    //
    // Voice Interaction
    //

    /**
     * @param voiceController the new delegate to control future voice interaction sessions
     * @return the old delegate that controlled prior voice interaction sessions
     */
    public VoiceController setVoiceController(VoiceController voiceController) {
        final VoiceController oldVoiceController = mVoiceController;
        mVoiceController = voiceController;
        return oldVoiceController;
    }

    @Override
    public void notifyVoiceSuccess(Activity activity, String message) {
        if (mVoiceController != null) {
            mVoiceController.notifyVoiceSuccess(activity, message);
        }
    }

    @Override
    public void notifyVoiceFailure(Activity activity, String message) {
        if (mVoiceController != null) {
            mVoiceController.notifyVoiceFailure(activity, message);
        }
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