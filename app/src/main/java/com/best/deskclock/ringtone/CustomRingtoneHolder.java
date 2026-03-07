/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import com.best.deskclock.data.CustomRingtone;

public class CustomRingtoneHolder extends RingtoneHolder {

    CustomRingtoneHolder(CustomRingtone ringtone) {
        super(ringtone.getUri(), ringtone.getTitle());
    }

    @Override
    public int getViewType() {
        return RingtoneAdapter.VIEW_TYPE_CUSTOM_SOUND;
    }
}
