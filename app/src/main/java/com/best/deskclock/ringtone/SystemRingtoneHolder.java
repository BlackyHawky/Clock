/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SystemRingtoneHolder extends RingtoneHolder {
    SystemRingtoneHolder(@NonNull Uri uri, @Nullable String name) {
        super(uri, name);
    }

    @Override
    public int getViewType() {
        return RingtoneAdapter.VIEW_TYPE_SYSTEM_SOUND;
    }
}
