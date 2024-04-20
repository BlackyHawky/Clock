/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import android.net.Uri;

final class SystemRingtoneHolder extends RingtoneHolder {

    SystemRingtoneHolder(Uri uri, String name) {
        super(uri, name);
    }

    @Override
    public int getItemViewType() {
        return RingtoneViewHolder.VIEW_TYPE_SYSTEM_SOUND;
    }
}
