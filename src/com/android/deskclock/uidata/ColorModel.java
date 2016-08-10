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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static java.util.Calendar.HOUR_OF_DAY;

/**
 * All globally available color data is accessed via this model.
 */
final class ColorModel {

    /** The colors of the app window - it changes throughout out the day to mimic the sky. */
    private static final @ColorInt int[] BACKGROUND_SPECTRUM = {
            0xFF212121 /* 12 AM */,
            0xFF20222A /*  1 AM */,
            0xFF202233 /*  2 AM */,
            0xFF1F2242 /*  3 AM */,
            0xFF1E224F /*  4 AM */,
            0xFF1D225C /*  5 AM */,
            0xFF1B236B /*  6 AM */,
            0xFF1A237E /*  7 AM */,
            0xFF1D2783 /*  8 AM */,
            0xFF232E8B /*  9 AM */,
            0xFF283593 /* 10 AM */,
            0xFF2C3998 /* 11 AM */,
            0xFF303F9F /* 12 PM */,
            0xFF2C3998 /*  1 PM */,
            0xFF283593 /*  2 PM */,
            0xFF232E8B /*  3 PM */,
            0xFF1D2783 /*  4 PM */,
            0xFF1A237E /*  5 PM */,
            0xFF1B236B /*  6 PM */,
            0xFF1D225C /*  7 PM */,
            0xFF1E224F /*  8 PM */,
            0xFF1F2242 /*  9 PM */,
            0xFF202233 /* 10 PM */,
            0xFF20222A /* 11 PM */
    };

    /** The listeners to notify when colors change. */
    private final List<OnAppColorChangeListener> mListeners = new ArrayList<>();

    /** The current app window color. */
    private @ColorInt int mWindowBackgroundColor;

    ColorModel(PeriodicCallbackModel periodicCallbackModel) {
        // Update the application window color when the hour changes.
        periodicCallbackModel.addHourCallback(new HourlyAppColorChanger(), 100);

        // Initialize the app window color.
        updateAppColor();
    }

    /**
     * @param colorListener to be notified when colors change
     */
    void addOnAppColorChangeListener(OnAppColorChangeListener colorListener) {
        mListeners.add(colorListener);
    }

    /**
     * @param colorListener to be notified when colors change
     */
    void removeOnAppColorChangeListener(OnAppColorChangeListener colorListener) {
        mListeners.remove(colorListener);
    }

    /**
     * @return the color of the app window
     */
    @ColorInt int getAppColor() {
        return mWindowBackgroundColor;
    }

    private void updateAppColor() {
        final @ColorInt int color = BACKGROUND_SPECTRUM[Calendar.getInstance().get(HOUR_OF_DAY)];
        if (mWindowBackgroundColor != color) {
            final @ColorInt int oldColor = mWindowBackgroundColor;
            mWindowBackgroundColor = color;

            for (OnAppColorChangeListener listener : mListeners) {
                listener.onAppColorChange(oldColor, color);
            }
        }
    }

    /**
     * Updates the app window color each time the hour changes.
     */
    private final class HourlyAppColorChanger implements Runnable {
        @Override
        public void run() {
            updateAppColor();
        }
    }
}