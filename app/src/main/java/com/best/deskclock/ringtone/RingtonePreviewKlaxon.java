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

package com.best.deskclock.ringtone;

import android.content.Context;
import android.net.Uri;

import com.best.deskclock.LogUtils;

public final class RingtonePreviewKlaxon {

    public static void stop(Context context) {
        LogUtils.i("RingtonePreviewKlaxon.stop()");
        BaseKlaxon.stop(context);
    }

    public static void start(Context context, Uri uri, int stream) {
        stop(context);
        LogUtils.i("RingtonePreviewKlaxon.start()");
        BaseKlaxon.start(context, uri, 0, stream, -1, true);
    }
}
