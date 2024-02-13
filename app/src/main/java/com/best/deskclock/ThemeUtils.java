/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.best.deskclock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableKt;

public final class ThemeUtils {

    /**
     * Temporary array used internally to resolve attributes.
     */
    private static final int[] TEMP_ATTR = new int[1];

    private ThemeUtils() {
        // Prevent instantiation.
    }

    /**
     * Convenience method for retrieving a themed color value.
     *
     * @param context the {@link Context} to resolve the theme attribute against
     * @param attr    the attribute corresponding to the color to resolve
     * @return the color value of the resolved attribute
     */
    @ColorInt
    public static int resolveColor(Context context, @AttrRes int attr) {
        return resolveColor(context, attr, null /* stateSet */);
    }

    /**
     * Convenience method for retrieving a themed color value.
     *
     * @param context  the {@link Context} to resolve the theme attribute against
     * @param attr     the attribute corresponding to the color to resolve
     * @param stateSet an array of {@link android.view.View} states
     * @return the color value of the resolved attribute
     */
    @ColorInt
    public static int resolveColor(Context context, @AttrRes int attr, @AttrRes int[] stateSet) {
        final TypedArray a;
        synchronized (TEMP_ATTR) {
            TEMP_ATTR[0] = attr;
            a = context.obtainStyledAttributes(TEMP_ATTR);
        }

        try {
            if (stateSet == null) {
                return a.getColor(0, Color.RED);
            }

            final ColorStateList colorStateList = a.getColorStateList(0);
            if (colorStateList != null) {
                return colorStateList.getColorForState(stateSet, Color.RED);
            }
            return Color.RED;
        } finally {
            a.recycle();
        }
    }

    /**
     * Convenience method for retrieving a themed drawable.
     *
     * @param context the {@link Context} to resolve the theme attribute against
     * @param attr    the attribute corresponding to the drawable to resolve
     * @return the drawable of the resolved attribute
     */
    public static Drawable resolveDrawable(Context context, @AttrRes int attr) {
        final TypedArray a;
        synchronized (TEMP_ATTR) {
            TEMP_ATTR[0] = attr;
            a = context.obtainStyledAttributes(TEMP_ATTR);
        }

        try {
            return a.getDrawable(0);
        } finally {
            a.recycle();
        }
    }

    /**
     * Convenience method for scaling Drawable.
     */
    public static BitmapDrawable toScaledBitmapDrawable(Context context, int drawableResId, float scale) {
        final Drawable drawable = AppCompatResources.getDrawable(context, drawableResId);
        if (drawable == null) return null;
        return new BitmapDrawable(context.getResources(), DrawableKt.toBitmap(drawable,
                (int) (scale * drawable.getIntrinsicHeight()), (int) (scale * drawable.getIntrinsicWidth()), null));
    }

}

