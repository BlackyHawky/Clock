// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

import static androidx.recyclerview.widget.RecyclerView.NO_ID;

import android.net.Uri;

import com.best.deskclock.ItemAdapter;

final class AddButtonTipHolder extends ItemAdapter.ItemHolder<Uri> {

    AddButtonTipHolder() {
        super(null, NO_ID);
    }

    @Override
    public int getItemViewType() {
        return AddButtonTipViewHolder.VIEW_TYPE_BUTTON_TIP;
    }
}
