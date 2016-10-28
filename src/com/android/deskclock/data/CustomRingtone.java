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

package com.android.deskclock.data;

import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * A read-only domain object representing a custom ringtone chosen from the file system.
 */
public final class CustomRingtone implements Comparable<CustomRingtone> {

    /** The unique identifier of the custom ringtone. */
    private final long mId;

    /** The uri that allows playback of the ringtone. */
    private final Uri mUri;

    /** The title describing the file at the given uri; typically the file name. */
    private final String mTitle;

    CustomRingtone(long id, Uri uri, String title) {
        mId = id;
        mUri = uri;
        mTitle = title;
    }

    public long getId() { return mId; }
    public Uri getUri() { return mUri; }
    public String getTitle() { return mTitle; }

    @Override
    public int compareTo(@NonNull CustomRingtone other) {
        return String.CASE_INSENSITIVE_ORDER.compare(getTitle(), other.getTitle());
    }
}