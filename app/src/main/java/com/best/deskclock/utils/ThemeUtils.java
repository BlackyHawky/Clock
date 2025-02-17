/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.graphics.Bitmap.Config.ARGB_8888;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableKt;

import com.best.deskclock.data.SettingsDAO;
import com.google.android.material.color.MaterialColors;

public class ThemeUtils {

    /**
     * @return {@code true} if the device is in dark mode.
     */
    public static boolean isNight(final Resources res) {
        return (res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * @return {@code true} if the device is currently in portrait or reverse portrait orientation
     */
    public static boolean isPortrait() {
        return Resources.getSystem().getConfiguration().orientation == ORIENTATION_PORTRAIT;
    }

    /**
     * @return {@code true} if the device is currently in landscape or reverse landscape orientation
     */
    public static boolean isLandscape() {
        return Resources.getSystem().getConfiguration().orientation == ORIENTATION_LANDSCAPE;
    }

    /**
     * @return {@code true} if the device is a tablet. {@code false} otherwise
     */
    public static boolean isTablet() {
        return Resources.getSystem().getConfiguration().smallestScreenWidthDp >= 600;
    }

    /**
     * Convenience method for creating card background.
     */
    public static Drawable cardBackground (Context context) {
        final SharedPreferences prefs = getDefaultSharedPreferences(context);
        final String darkMode = SettingsDAO.getDarkMode(prefs);
        final int radius = convertDpToPixels(18, context);
        final GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setCornerRadius(radius);

        if (SettingsDAO.isCardBackgroundDisplayed(prefs)) {
            gradientDrawable.setColor(MaterialColors.getColor(
                    context, com.google.android.material.R.attr.colorSurface, Color.BLACK));
        } else {
            if (isNight(context.getResources()) && darkMode.equals(AMOLED_DARK_MODE)) {
                gradientDrawable.setColor(Color.BLACK);
            } else {
                gradientDrawable.setColor(MaterialColors.getColor(context, android.R.attr.colorBackground, Color.BLACK));
            }
        }

        if (SettingsDAO.isCardBorderDisplayed(prefs)) {
            gradientDrawable.setShape(GradientDrawable.RECTANGLE);
            gradientDrawable.setStroke(convertDpToPixels(2, context),
                    MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.BLACK)
            );
        }

        return gradientDrawable;
    }

    /**
     * Convenience method for converting dp to pixel.
     */
    public static int convertDpToPixels(int dp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    /**
     * This method assumes the given {@code view} has already been layed out.
     *
     * @return a Bitmap containing an image of the {@code view} at its current size
     */
    public static Bitmap createBitmap(View view) {
        final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
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

    /**
     * Calculate the amount by which the radius of a CircleTimerView should be offset by any
     * of the extra painted objects.
     */
    public static float calculateRadiusOffset(float strokeSize, float dotStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(dotStrokeSize, markerStrokeSize));
    }

}
