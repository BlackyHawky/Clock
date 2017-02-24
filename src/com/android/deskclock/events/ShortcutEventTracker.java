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

package com.android.deskclock.events;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.support.annotation.StringRes;
import android.util.ArraySet;

import com.android.deskclock.R;
import com.android.deskclock.uidata.UiDataModel;

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