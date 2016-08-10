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

import android.support.annotation.ColorInt;

/**
 * The interface through which interested parties are notified of changes to the app window.
 */
public interface OnAppColorChangeListener {

    /**
     * @param oldColor the prior color of the app window
     * @param newColor the new color of the app window
     */
    void onAppColorChange(@ColorInt int oldColor, @ColorInt int newColor);
}