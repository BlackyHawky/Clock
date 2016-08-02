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

@TargetApi(Build.VERSION_CODES.M)
class DefaultVoiceController implements VoiceController {
    @Override
    public void notifyVoiceSuccess(Activity activity, String message) {
        final VoiceInteractor voiceInteractor = activity.getVoiceInteractor();
        if (voiceInteractor != null) {
            final Prompt prompt = new Prompt(message);
            voiceInteractor.submitRequest(new CompleteVoiceRequest(prompt, null));
        }
    }

    @Override
    public void notifyVoiceFailure(Activity activity, String message) {
        final VoiceInteractor voiceInteractor = activity.getVoiceInteractor();
        if (voiceInteractor != null) {
            final Prompt prompt = new Prompt(message);
            voiceInteractor.submitRequest(new AbortVoiceRequest(prompt, null));
        }
    }
}