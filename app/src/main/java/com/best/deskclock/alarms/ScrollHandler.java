/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.alarms;

/**
 * API that handles scrolling when an alarm item is expanded/collapsed.
 */
public interface ScrollHandler {

    /**
     * Sets the stable id that view should be scrolled to. The view does not actually scroll yet.
     */
    void setSmoothScrollStableId(long stableId);

}
