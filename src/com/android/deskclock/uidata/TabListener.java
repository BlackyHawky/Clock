/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * The interface through which interested parties are notified of changes to the selected tab.
 */
public interface TabListener {

    /**
     * @param oldSelectedTab an enumerated value indicating the prior selected tab
     * @param newSelectedTab an enumerated value indicating the newly selected tab
     */
    void selectedTabChanged(Tab oldSelectedTab, Tab newSelectedTab);
}