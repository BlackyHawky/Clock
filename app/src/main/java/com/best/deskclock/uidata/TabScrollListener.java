/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.uidata;

/**
 * The interface through which interested parties are notified of changes to the vertical scroll
 * position of the selected tab. Callbacks to listener occur when any of these events occur:
 *
 * <ul>
 *     <li>the vertical scroll position of the selected tab is now scrolled to the top</li>
 *     <li>the vertical scroll position of the selected tab is no longer scrolled to the top</li>
 *     <li>the selected tab changed and the new tab scroll state does not match the prior tab</li>
 * </ul>
 */
public interface TabScrollListener {

    /**
     * @param scrolledToTop indicates whether the current selected tab is scrolled to its top
     */
    void selectedTabScrollToTopChanged(boolean scrolledToTop);
}
