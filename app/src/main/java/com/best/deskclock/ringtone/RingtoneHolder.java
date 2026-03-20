/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import android.net.Uri;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.RingtoneUtils;

public abstract class RingtoneHolder implements RingtoneAdapter.RingtoneItem {

    private final Uri mUri;
    private final String mName;
    private boolean mSelected;
    private boolean mPlaying;

    RingtoneHolder(Uri uri, String name) {
        mUri = uri;
        mName = name;
    }

    Uri getUri() {
        return mUri;
    }

    boolean isSilent() {
        return RingtoneUtils.RINGTONE_SILENT.equals(getUri());
    }

    boolean isSelected() {
        return mSelected;
    }

    void setSelected(boolean selected) {
        mSelected = selected;
    }

    boolean isPlaying() {
        return mPlaying;
    }

    void setPlaying(boolean playing) {
        mPlaying = playing;
    }

    boolean isRandom() {
        return RingtoneUtils.RANDOM_RINGTONE.equals(mUri) || RingtoneUtils.RANDOM_CUSTOM_RINGTONE.equals(mUri);
    }

    String getName() {
        return mName != null ? mName : DataModel.getDataModel().getRingtoneTitle(getUri());
    }
}
