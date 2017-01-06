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

package com.android.deskclock.ringtone;

import android.net.Uri;

import com.android.deskclock.ItemAdapter;
import com.android.deskclock.Utils;
import com.android.deskclock.data.DataModel;

import static android.support.v7.widget.RecyclerView.NO_ID;

abstract class RingtoneHolder extends ItemAdapter.ItemHolder<Uri> {

    private final String mName;
    private final boolean mHasPermissions;
    private boolean mSelected;
    private boolean mPlaying;

    RingtoneHolder(Uri uri, String name) {
        this(uri, name, true);
    }

    RingtoneHolder(Uri uri, String name, boolean hasPermissions) {
        super(uri, NO_ID);
        mName = name;
        mHasPermissions = hasPermissions;
    }

    long getId() { return itemId; }
    boolean hasPermissions() { return mHasPermissions; }
    Uri getUri() { return item; }

    boolean isSilent() { return Utils.RINGTONE_SILENT.equals(getUri()); }

    boolean isSelected() { return mSelected; }
    void setSelected(boolean selected) { mSelected = selected; }

    boolean isPlaying() { return mPlaying; }
    void setPlaying(boolean playing) { mPlaying = playing; }

    String getName() {
        return mName != null ? mName : DataModel.getDataModel().getRingtoneTitle(getUri());
    }
}