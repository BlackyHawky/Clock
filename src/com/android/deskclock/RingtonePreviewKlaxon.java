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

import android.content.Context;
import android.net.Uri;

public final class RingtonePreviewKlaxon {

    private static AsyncRingtonePlayer sAsyncRingtonePlayer;

    private RingtonePreviewKlaxon() {
    }

    public static void stop(Context context) {
        LogUtils.i("RingtonePreviewKlaxon.stop()");
        getAsyncRingtonePlayer(context).stop();
    }

    public static void start(Context context, Uri uri) {
        stop(context);
        LogUtils.i("RingtonePreviewKlaxon.start()");
        getAsyncRingtonePlayer(context).play(uri, 0);
    }

    private static synchronized AsyncRingtonePlayer getAsyncRingtonePlayer(Context context) {
        if (sAsyncRingtonePlayer == null) {
            sAsyncRingtonePlayer = new AsyncRingtonePlayer(context.getApplicationContext());
        }

        return sAsyncRingtonePlayer;
    }
}