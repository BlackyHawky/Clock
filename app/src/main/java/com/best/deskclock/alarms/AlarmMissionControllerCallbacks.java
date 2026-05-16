/*
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.best.deskclock.alarms;

import androidx.annotation.StringRes;

interface AlarmMissionControllerCallbacks {
    void onMissionResolved(int action);

    void onMissionMessage(@StringRes int messageResId);
}