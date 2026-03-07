/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import androidx.annotation.StringRes;

public record HeaderHolder(@StringRes int mTextResId) implements RingtoneAdapter.RingtoneItem {

    @StringRes
    int getTextResId() {
        return mTextResId;
    }

    @Override
    public int getViewType() {
        return RingtoneAdapter.VIEW_TYPE_HEADER;
    }
}
