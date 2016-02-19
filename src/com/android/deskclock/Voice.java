/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.deskclock;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.VoiceInteractor;
import android.os.Build;

/**
 * Notifies Voice Interactor about whether the action
 * was successful. Voice Interactor is called only if
 * the build version is post-Lollipop.
 */
public final class Voice {

    private static Delegate sDelegate = new VoiceInteractorDelegate();

    private Voice() { }

    public static void setDelegate(Delegate delegate) {
        sDelegate = delegate;
    }

    public static Delegate getDelegate() {
        return sDelegate;
    }

    public static void notifySuccess(Activity activity, String message) {
        if (Utils.isMOrLater()) {
            sDelegate.notifySuccess(activity.getVoiceInteractor(), message);
        }
    }

    public static void notifyFailure(Activity activity, String message) {
        if (Utils.isMOrLater()) {
            sDelegate.notifyFailure(activity.getVoiceInteractor(), message);
        }
    }

    public interface Delegate {
        void notifySuccess(VoiceInteractor vi, String message);

        void notifyFailure(VoiceInteractor vi, String message);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static class VoiceInteractorDelegate implements Delegate {
        @Override
        public void notifySuccess(VoiceInteractor vi, String message) {
            if (vi != null)  {
                final VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt(message);
                vi.submitRequest(new VoiceInteractor.CompleteVoiceRequest(prompt, null));
            }
        }

        @Override
        public void notifyFailure(VoiceInteractor vi, String message) {
            if (vi != null)  {
                final VoiceInteractor.Prompt prompt = new VoiceInteractor.Prompt(message);
                vi.submitRequest(new VoiceInteractor.AbortVoiceRequest(prompt, null));
            }
        }
    }
}