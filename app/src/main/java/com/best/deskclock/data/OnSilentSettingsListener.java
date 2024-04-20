/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

/**
 * The interface through which interested parties are notified of changes to device settings that
 * silence firing alarms.
 */
public interface OnSilentSettingsListener {
    void onSilentSettingsChange(DataModel.SilentSetting after);
}
