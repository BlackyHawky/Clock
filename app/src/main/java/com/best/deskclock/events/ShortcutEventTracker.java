/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.events;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.util.ArraySet;

import androidx.annotation.StringRes;

import com.best.deskclock.R;
import com.best.deskclock.uidata.UiDataModel;

import java.util.Set;

@TargetApi(Build.VERSION_CODES.N_MR1)
public final class ShortcutEventTracker implements EventTracker {

    private final ShortcutManager mShortcutManager;
    private final Set<String> shortcuts = new ArraySet<>(5);

    public ShortcutEventTracker(Context context) {
        mShortcutManager = context.getSystemService(ShortcutManager.class);
        final UiDataModel uidm = UiDataModel.getUiDataModel();
        shortcuts.add(uidm.getShortcutId(R.string.category_alarm, R.string.action_create));
        shortcuts.add(uidm.getShortcutId(R.string.category_timer, R.string.action_create));
        shortcuts.add(uidm.getShortcutId(R.string.category_stopwatch, R.string.action_pause));
        shortcuts.add(uidm.getShortcutId(R.string.category_stopwatch, R.string.action_start));
        shortcuts.add(uidm.getShortcutId(R.string.category_screensaver, R.string.action_show));
    }

    @Override
    public void sendEvent(@StringRes int category, @StringRes int action, @StringRes int label) {
        final String shortcutId = UiDataModel.getUiDataModel().getShortcutId(category, action);
        if (shortcuts.contains(shortcutId)) {
            mShortcutManager.reportShortcutUsed(shortcutId);
        }
    }
}
