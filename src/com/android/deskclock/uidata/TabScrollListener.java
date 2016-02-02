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

package com.android.deskclock.uidata;

import com.android.deskclock.uidata.UiDataModel.Tab;

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
     * @param selectedTab an enumerated value indicating the current selected tab
     * @param scrolledToTop indicates whether the current selected tab is scrolled to its top
     */
    void selectedTabScrollToTopChanged(Tab selectedTab, boolean scrolledToTop);
}