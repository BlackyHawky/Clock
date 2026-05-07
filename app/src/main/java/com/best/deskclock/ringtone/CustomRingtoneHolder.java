/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import com.best.deskclock.data.CustomRingtone;

public class CustomRingtoneHolder extends RingtoneHolder {

    private final boolean mIsReadable;

    CustomRingtoneHolder(CustomRingtone ringtone, boolean isReadable) {
        super(ringtone.getUri(), ringtone.getTitle());
        mIsReadable = isReadable;
    }

    @Override
    public int getViewType() {
        return RingtoneAdapter.VIEW_TYPE_CUSTOM_SOUND;
    }

    public boolean isReadable() {
        return mIsReadable;
    }

}
