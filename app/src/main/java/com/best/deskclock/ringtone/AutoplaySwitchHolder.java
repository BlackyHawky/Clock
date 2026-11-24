// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.ringtone;

public record AutoplaySwitchHolder(boolean isEnabled) implements RingtoneAdapter.RingtoneItem {

    @Override
    public int getViewType() {
        return RingtoneAdapter.VIEW_TYPE_AUTOPLAY_SWITCH;
    }

}
