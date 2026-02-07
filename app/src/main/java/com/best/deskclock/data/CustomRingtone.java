/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import android.net.Uri;

import androidx.annotation.NonNull;

/**
 * A read-only domain object representing a custom ringtone chosen from the file system.
 *
 * @param mId             The unique identifier of the custom ringtone.
 * @param mUri            The uri that allows playback of the ringtone.
 * @param mTitle          The title describing the file at the given uri; typically the file name.
 * @param mHasPermissions {@code true} iff the application has permission to read the content of {@code mUri uri}.
 */
public record CustomRingtone(long mId, Uri mUri, String mTitle, boolean mHasPermissions)
        implements Comparable<CustomRingtone> {

    public long getId() {
        return mId;
    }

    public Uri getUri() {
        return mUri;
    }

    public String getTitle() {
        return mTitle;
    }

    public boolean hasPermissions() {
        return mHasPermissions;
    }

    CustomRingtone setHasPermissions(boolean hasPermissions) {
        if (mHasPermissions == hasPermissions) {
            return this;
        }

        return new CustomRingtone(mId, mUri, mTitle, hasPermissions);
    }

    @Override
    public int compareTo(@NonNull CustomRingtone other) {
        return String.CASE_INSENSITIVE_ORDER.compare(getTitle(), other.getTitle());
    }
}
