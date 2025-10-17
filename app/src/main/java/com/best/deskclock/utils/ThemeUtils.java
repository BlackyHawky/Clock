/*
 * Copyright (C) 2016 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.utils;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.google.android.material.color.MaterialColors;

public class ThemeUtils {

    /**
     * Prevent the screen from turning off while activity is visible.
     * <p>
     * This method adds the WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON flag to the
     * activity's window, which keeps the screen on (prevents automatic sleep).
     *
     * @param activity The activity for which the screen should remain on.
     */
    public static void keepScreenOn(Activity activity) {
        activity.getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Allow automatic screen timeout again.
     * <p>
     * This method removes the WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON flag from the
     * activity window, allowing the screen to turn off according to the system settings.
     *
     * @param activity The activity for which the screen can be automatically turned off.
     */
    public static void releaseKeepScreenOn(Activity activity) {
        activity.getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Configure the activity to allow display in the cutout area (notch/front camera).
     *
     * @param window The activity window (via getWindow()).
     */
    public static void allowDisplayCutout(Window window) {
        if (SdkUtils.isAtLeastAndroid9()) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(layoutParams);
        }
    }

    /**
     * @return {@code true} if the device is in dark mode. {@code false} otherwise.
     */
    public static boolean isNight(final Resources res) {
        return (res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * @return {@code true} if the device is currently in portrait or reverse portrait orientation.
     * {@code false} otherwise.
     */
    public static boolean isPortrait() {
        return Resources.getSystem().getConfiguration().orientation == ORIENTATION_PORTRAIT;
    }

    /**
     * @return {@code true} if the device is currently in landscape or reverse landscape orientation.
     * {@code false} otherwise.
     */
    public static boolean isLandscape() {
        return Resources.getSystem().getConfiguration().orientation == ORIENTATION_LANDSCAPE;
    }

    /**
     * @return {@code true} if the device is a tablet. {@code false} otherwise.
     */
    public static boolean isTablet() {
        return Resources.getSystem().getConfiguration().smallestScreenWidthDp >= 600;
    }

    /**
     * @return {@code true} if the system animations are disabled. {@code false} otherwise.
     */
    public static boolean areSystemAnimationsDisabled(Context context) {
        return android.provider.Settings.Global.getFloat(context.getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
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
            gradientDrawable.setStroke(convertDpToPixels(2, context), MaterialColors.getColor(
                    context, androidx.appcompat.R.attr.colorPrimary, Color.BLACK)
            );
        }

        return gradientDrawable;
    }

    /**
     * Convenience method for creating pill background.
     */
    public static Drawable pillBackground(Context context, @AttrRes int colorAttributeResId) {
        final int radius = convertDpToPixels(50, context);
        final GradientDrawable gradientDrawable = new GradientDrawable();

        gradientDrawable.setCornerRadius(radius);

        gradientDrawable.setColor(
                MaterialColors.getColor(context, colorAttributeResId, Color.BLACK));

        return gradientDrawable;
    }

    /**
     * Convenience method to create ripple drawable.
     */
    public static RippleDrawable rippleDrawable(Context context, @ColorInt int color) {
        final GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadius(convertDpToPixels(18, context));
        gradientDrawable.setColor(color);

        int rippleColor = MaterialColors.getColor(
                context, androidx.appcompat.R.attr.colorControlHighlight, Color.BLACK);

        return new RippleDrawable(ColorStateList.valueOf(rippleColor), gradientDrawable, null);
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
     * Calculate the amount by which the radius of a CircleTimerView should be offset by any
     * of the extra painted objects.
     */
    public static float calculateRadiusOffset(float strokeSize, float dotStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(dotStrokeSize, markerStrokeSize));
    }

    /**
     * Updates the enabled state and image tint of a SeekBar-related {@link ImageView} button
     * (e.g. minus or plus) based on a given enabled flag.
     *
     * @param button   The ImageView button to update.
     * @param enabled  Whether the button should be enabled.
     */
    public static void updateSeekBarButtonEnabledState(Context context, ImageView button, boolean enabled) {
        button.setEnabled(enabled);

        if (enabled) {
            button.setImageTintList(null);
        } else {
            button.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.colorDisabled)));
        }
    }

    /**
     * Cancels any ongoing animations in the button drawable of all {@link RadioButton}s
     * within the given {@link RadioGroup}.
     * <p>
     * On some devices or Android versions, transitioning from the checked to unchecked state
     * can produce visual glitches (e.g., flickering).
     * <p>
     * This method ensures that all RadioButtons immediately jump to their current drawable state
     * without any intermediate animation, preventing such artifacts.
     *
     * @param group The {@link RadioGroup} containing the {@link RadioButton}s to update.
     */
    public static void cancelRadioButtonDrawableAnimations(@NonNull RadioGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof RadioButton) {
                Drawable buttonDrawable = ((RadioButton) child).getButtonDrawable();
                if (buttonDrawable instanceof AnimatedStateListDrawable) {
                    buttonDrawable.jumpToCurrentState();
                }
            }
        }
    }

}
