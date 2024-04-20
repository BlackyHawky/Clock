/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.controller;

import android.app.Activity;
import android.app.VoiceInteractor;
import android.app.VoiceInteractor.AbortVoiceRequest;
import android.app.VoiceInteractor.CompleteVoiceRequest;
import android.app.VoiceInteractor.Prompt;

class VoiceController {
    /**
     * If the {@code activity} is currently hosting a voice interaction session, indicate the voice
     * command was processed successfully.
     *
     * @param activity an Activity that may be hosting a voice interaction session
     * @param message  to be spoken to the user to indicate success
     */
    void notifyVoiceSuccess(Activity activity, String message) {
        final VoiceInteractor voiceInteractor = activity.getVoiceInteractor();
        if (voiceInteractor != null) {
            final Prompt prompt = new Prompt(message);
            voiceInteractor.submitRequest(new CompleteVoiceRequest(prompt, null));
        }
    }

    /**
     * If the {@code activity} is currently hosting a voice interaction session, indicate the voice
     * command failed and must be aborted.
     *
     * @param activity an Activity that may be hosting a voice interaction session
     * @param message  to be spoken to the user to indicate failure
     */
    void notifyVoiceFailure(Activity activity, String message) {
        final VoiceInteractor voiceInteractor = activity.getVoiceInteractor();
        if (voiceInteractor != null) {
            final Prompt prompt = new Prompt(message);
            voiceInteractor.submitRequest(new AbortVoiceRequest(prompt, null));
        }
    }
}
