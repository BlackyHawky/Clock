/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import static androidx.recyclerview.widget.RecyclerView.NO_ID;

import android.net.Uri;

import com.best.deskclock.ItemAdapter;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.utils.RingtoneUtils;

abstract class RingtoneHolder extends ItemAdapter.ItemHolder<Uri> {

    private final String mName;
    private boolean mSelected;
    private boolean mPlaying;

    RingtoneHolder(Uri uri, String name) {
        super(uri, NO_ID);
        mName = name;
    }

    Uri getUri() {
        return item;
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

    String getName() {
        return mName != null ? mName : DataModel.getDataModel().getRingtoneTitle(getUri());
    }
}
