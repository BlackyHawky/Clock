/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLACK_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLACK_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLUE_GRAY_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BLUE_GRAY_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BROWN_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.BROWN_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.DARK_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.GREEN_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.GREEN_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.INDIGO_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.INDIGO_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_AMOLED_DARK_MODE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_DEFAULT_DARK_MODE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.LIGHT_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.ORANGE_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.ORANGE_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PINK_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PINK_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PURPLE_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.PURPLE_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.RED_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.RED_NIGHT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.SYSTEM_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.YELLOW_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.YELLOW_NIGHT_ACCENT_COLOR;

import android.content.Context;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableKt;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;
import com.google.android.material.color.MaterialColors;

public class ThemeUtils {

    /**
     * Apply the theme and the accent color to the activities.
     */
    public static void applyThemeAndAccentColor(final AppCompatActivity activity) {
        final String theme = DataModel.getDataModel().getTheme();
        final String darkMode = DataModel.getDataModel().getDarkMode();
        final String accentColor = DataModel.getDataModel().getAccentColor();
        final boolean isAutoNightAccentColorEnabled = DataModel.getDataModel().isAutoNightAccentColorEnabled();
        final String nightAccentColor = DataModel.getDataModel().getNightAccentColor();

        if (darkMode.equals(KEY_DEFAULT_DARK_MODE)) {
            switch (theme) {
                case SYSTEM_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                case LIGHT_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                case DARK_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        } else if (darkMode.equals(KEY_AMOLED_DARK_MODE)
                && !theme.equals(SYSTEM_THEME) || !theme.equals(LIGHT_THEME)) {
            activity.setTheme(R.style.AmoledTheme);
        }

        if (isAutoNightAccentColorEnabled) {
            switch (accentColor) {
                case BLACK_ACCENT_COLOR -> activity.setTheme(R.style.BlackAccentColor);
                case BLUE_GRAY_ACCENT_COLOR -> activity.setTheme(R.style.BlueGrayAccentColor);
                case BROWN_ACCENT_COLOR -> activity.setTheme(R.style.BrownAccentColor);
                case GREEN_ACCENT_COLOR -> activity.setTheme(R.style.GreenAccentColor);
                case INDIGO_ACCENT_COLOR -> activity.setTheme(R.style.IndigoAccentColor);
                case ORANGE_ACCENT_COLOR -> activity.setTheme(R.style.OrangeAccentColor);
                case PINK_ACCENT_COLOR -> activity.setTheme(R.style.PinkAccentColor);
                case PURPLE_ACCENT_COLOR -> activity.setTheme(R.style.PurpleAccentColor);
                case RED_ACCENT_COLOR -> activity.setTheme(R.style.RedAccentColor);
                case YELLOW_ACCENT_COLOR -> activity.setTheme(R.style.YellowAccentColor);
            }
        } else {
            if (isNight()) {
                switch (nightAccentColor) {
                    case BLACK_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.BlackAccentColor);
                    case BLUE_GRAY_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.BlueGrayAccentColor);
                    case BROWN_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.BrownAccentColor);
                    case GREEN_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.GreenAccentColor);
                    case INDIGO_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.IndigoAccentColor);
                    case ORANGE_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.OrangeAccentColor);
                    case PINK_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.PinkAccentColor);
                    case PURPLE_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.PurpleAccentColor);
                    case RED_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.RedAccentColor);
                    case YELLOW_NIGHT_ACCENT_COLOR -> activity.setTheme(R.style.YellowAccentColor);
                }
            } else {
                switch (accentColor) {
                    case BLACK_ACCENT_COLOR -> activity.setTheme(R.style.BlackAccentColor);
                    case BLUE_GRAY_ACCENT_COLOR -> activity.setTheme(R.style.BlueGrayAccentColor);
                    case BROWN_ACCENT_COLOR -> activity.setTheme(R.style.BrownAccentColor);
                    case GREEN_ACCENT_COLOR -> activity.setTheme(R.style.GreenAccentColor);
                    case INDIGO_ACCENT_COLOR -> activity.setTheme(R.style.IndigoAccentColor);
                    case ORANGE_ACCENT_COLOR -> activity.setTheme(R.style.OrangeAccentColor);
                    case PINK_ACCENT_COLOR -> activity.setTheme(R.style.PinkAccentColor);
                    case PURPLE_ACCENT_COLOR -> activity.setTheme(R.style.PurpleAccentColor);
                    case RED_ACCENT_COLOR -> activity.setTheme(R.style.RedAccentColor);
                    case YELLOW_ACCENT_COLOR -> activity.setTheme(R.style.YellowAccentColor);
                }
            }
        }

        if (activity instanceof CollapsingToolbarBaseActivity) {
            if (isNight() && darkMode.equals(KEY_AMOLED_DARK_MODE)) {
                activity.getWindow().setNavigationBarColor(Color.BLACK);
                activity.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
            } else {
                activity.getWindow().setNavigationBarColor(
                        MaterialColors.getColor(activity, android.R.attr.colorBackground, Color.BLACK));
            }
        } else {
            if (isNight() && darkMode.equals(KEY_AMOLED_DARK_MODE)) {
                activity.getWindow().setNavigationBarColor(Color.BLACK);
                activity.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
            }
        }
    }

    /**
     * @return {@code true} if the device is in dark mode.
     */
    public static boolean isNight() {
        return (Resources.getSystem().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
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
        final String darkMode = DataModel.getDataModel().getDarkMode();
        final boolean isCardBackgroundDisplayed = DataModel.getDataModel().isCardBackgroundDisplayed();
        final int radius = convertDpToPixels(18, context);
        final GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setCornerRadius(radius);

        if (isCardBackgroundDisplayed) {
            gradientDrawable.setColor(MaterialColors.getColor(
                    context, com.google.android.material.R.attr.colorSurface, Color.BLACK));
        } else {
            if (isNight() && darkMode.equals(KEY_AMOLED_DARK_MODE)) {
                gradientDrawable.setColor(Color.BLACK);
            } else {
                gradientDrawable.setColor(MaterialColors.getColor(context, android.R.attr.colorBackground, Color.BLACK));
            }
        }

        final boolean isCardBorderDisplayed = DataModel.getDataModel().isCardBorderDisplayed();
        if (isCardBorderDisplayed) {
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
