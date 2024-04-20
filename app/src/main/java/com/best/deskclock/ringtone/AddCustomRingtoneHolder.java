/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.ringtone;

import static androidx.recyclerview.widget.RecyclerView.NO_ID;

import android.net.Uri;

import com.best.deskclock.ItemAdapter;

final class AddCustomRingtoneHolder extends ItemAdapter.ItemHolder<Uri> {

    AddCustomRingtoneHolder() {
        super(null, NO_ID);
    }

    @Override
    public int getItemViewType() {
        return AddCustomRingtoneViewHolder.VIEW_TYPE_ADD_NEW;
    }
}
