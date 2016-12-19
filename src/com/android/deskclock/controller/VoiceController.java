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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.VoiceInteractor;
import android.app.VoiceInteractor.AbortVoiceRequest;
import android.app.VoiceInteractor.CompleteVoiceRequest;
import android.app.VoiceInteractor.Prompt;
import android.os.Build;

import com.android.deskclock.Utils;

@TargetApi(Build.VERSION_CODES.M)
class VoiceController {
    /**
     * If the {@code activity} is currently hosting a voice interaction session, indicate the voice
     * command was processed successfully.
     *
     * @param activity an Activity that may be hosting a voice interaction session
     * @param message to be spoken to the user to indicate success
     */
    void notifyVoiceSuccess(Activity activity, String message) {
        if (!Utils.isMOrLater()) {
            return;
        }

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
     * @param message to be spoken to the user to indicate failure
     */
    void notifyVoiceFailure(Activity activity, String message) {
        if (!Utils.isMOrLater()) {
            return;
        }

        final VoiceInteractor voiceInteractor = activity.getVoiceInteractor();
        if (voiceInteractor != null) {
            final Prompt prompt = new Prompt(message);
            voiceInteractor.submitRequest(new AbortVoiceRequest(prompt, null));
        }
    }
}