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

import static androidx.core.util.TypedValueCompat.dpToPx;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.settings.PreferencesDefaultValues.AMOLED_DARK_MODE;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLACK_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLUE_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.BLUE_GRAY_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.BROWN_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.GREEN_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.INDIGO_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.ORANGE_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.PINK_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.PURPLE_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.RED_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.YELLOW_ACCENT_COLOR;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.CustomTooltip;
import com.google.android.material.color.MaterialColors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThemeUtils {

    private static final Map<String, Typeface> fontCache = new HashMap<>();
    private static final Map<View, List<TextView>> textViewCache = new HashMap<>();

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
     * Loads a {@link Typeface} from the given font file path.
     * <p>
     * This method attempts to create a typeface from the specified file path.
     * If the path is null, the file does not exist, or the font cannot be loaded,
     * the default system font will be used.
     * </p>
     *
     * @param fontPath the absolute path to the font file (.ttf or .otf), may be null
     * @return the loaded {@link Typeface}, or {@code null} if loading fails
     */
    public static Typeface loadFont(String fontPath) {
        if (fontPath == null) {
            return null;
        }

        if (fontCache.containsKey(fontPath)) {
            return fontCache.get(fontPath);
        }

        File file = new File(fontPath);
        if (!file.exists() || !file.isFile()) {
            LogUtils.w("Font file not found: " + fontPath);
            return null;
        }

        try {
            Typeface typeface = Typeface.createFromFile(file);
            fontCache.put(fontPath, typeface);
            return typeface;
        } catch (Exception e) {
            LogUtils.e("Error loading font: " + fontPath, e);
            return null;
        }
    }

    /**
     * Returns a bold {@link Typeface} based on the font located at the given path.
     * <p>
     * If the font cannot be loaded or the path is null, a default bold
     * sans-serif typeface is returned instead.
     *
     * @param fontPath the file path of the custom font to load, or null
     * @return a bold Typeface, either custom or default
     */
    public static Typeface boldTypeface(String fontPath) {
        Typeface baseTypeface = null;

        if (fontPath != null) {
            baseTypeface = loadFont(fontPath);
        }

        if (baseTypeface == null) {
            return Typeface.create("sans-serif", Typeface.BOLD);
        }

        return Typeface.create(baseTypeface, Typeface.BOLD);
    }

    /**
     * Applies the specified {@link Typeface} to the given view and all of its child views recursively.
     *
     * <p>If the view is a {@link TextView}, its typeface is updated. If the view is a {@link ViewGroup},
     * the method iterates through all children and applies the typeface to each one.</p>
     *
     * @param root the root view from which the typeface should be applied recursively
     */
    public static void applyTypeface(View root, Typeface typeface) {
        if (typeface == null) {
            return;
        }

        List<TextView> labels = getCachedTextViews(root);

        for (TextView tv : labels) {
            tv.setTypeface(typeface);
        }
    }

    /**
     * Recursively collects all {@link TextView} instances contained within the given view.
     *
     * @param root the root view to search
     * @return a list of all TextViews found in the view hierarchy
     */
    public static List<TextView> findAllTextViews(View root) {
        List<TextView> result = new ArrayList<>();
        if (root instanceof TextView) {
            result.add((TextView) root);
        } else if (root instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                result.addAll(findAllTextViews(group.getChildAt(i)));
            }
        }

        return result;
    }

    /**
     * Returns all {@link TextView} contained within the given view, using a cached
     * lookup when available.
     *
     * <p>If the view has not been processed before, its TextViews are discovered
     * recursively and stored for future reuse.</p>
     *
     * @param itemView the root view to inspect
     * @return a cached or newly discovered list of TextViews
     */
    private static List<TextView> getCachedTextViews(View itemView) {
        List<TextView> cached = textViewCache.get(itemView);
        if (cached != null) {
            return cached;
        }

        List<TextView> found = ThemeUtils.findAllTextViews(itemView);
        textViewCache.put(itemView, found);
        return found;
    }

    /**
     * Returns the style resource corresponding to the user's selected accent color.
     *
     * <p>If automatic night accent colors are enabled, the daytime accent is always used.
     * Otherwise, the method selects between the normal and night accent color depending
     * on whether the system is currently in night mode.</p>
     *
     * @param context the context used to check night mode
     * @param isAutoNightAccentColorEnabled true if automatic night accent colors are enabled
     * @param accentColor the accent color selected for day mode
     * @param nightAccentColor the accent color selected for night mode
     * @return the style resource ID matching the resolved accent color
     */
    public static int getAccentStyle(Context context, boolean isAutoNightAccentColorEnabled,
                                     String accentColor, String nightAccentColor) {

        String colorKey = isAutoNightAccentColorEnabled
                ? accentColor
                : (ThemeUtils.isNight(context.getResources()) ? nightAccentColor : accentColor);

        return switch (colorKey) {
            case BLACK_ACCENT_COLOR -> R.style.BlackAccentColor;
            case BLUE_ACCENT_COLOR -> R.style.BlueAccentColor;
            case BLUE_GRAY_ACCENT_COLOR -> R.style.BlueGrayAccentColor;
            case BROWN_ACCENT_COLOR -> R.style.BrownAccentColor;
            case GREEN_ACCENT_COLOR -> R.style.GreenAccentColor;
            case INDIGO_ACCENT_COLOR -> R.style.IndigoAccentColor;
            case ORANGE_ACCENT_COLOR -> R.style.OrangeAccentColor;
            case PINK_ACCENT_COLOR -> R.style.PinkAccentColor;
            case PURPLE_ACCENT_COLOR -> R.style.PurpleAccentColor;
            case RED_ACCENT_COLOR -> R.style.RedAccentColor;
            case YELLOW_ACCENT_COLOR -> R.style.YellowAccentColor;
            default -> R.style.Theme_DeskClock;
        };
    }

    /**
     * Creates a themed context applying the user's selected accent color style.
     *
     * <p>This ensures that custom toasts correctly resolve Material color attributes
     * such as {@code colorSecondary}, even when called from non-UI contexts.</p>
     *
     * @param context the base context
     * @param prefs the shared preferences containing theme settings
     * @return a ContextThemeWrapper applying the correct accent style
     */
    public static Context getThemedContext(Context context, SharedPreferences prefs) {
        int style = ThemeUtils.getAccentStyle(
                context,
                SettingsDAO.isAutoNightAccentColorEnabled(prefs),
                SettingsDAO.getAccentColor(prefs),
                SettingsDAO.getNightAccentColor(prefs)
        );

        return new ContextThemeWrapper(context, style);
    }

    /**
     * Installs custom tooltips on all action menu items inside the given {@link Toolbar}.
     *
     * <p>This method disables the default system tooltips and replaces them with
     * custom tooltips displayed below each icon when long‑pressed.</p>
     *
     * @param toolbar the Toolbar whose action items should receive custom tooltips
     */
    public static void applyToolbarTooltips(Toolbar toolbar) {
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);

            if (child instanceof ActionMenuView actionMenuView) {
                for (int j = 0; j < actionMenuView.getChildCount(); j++) {
                    View itemView = actionMenuView.getChildAt(j);

                    // Disable the system tooltip
                    ViewCompat.setTooltipText(itemView, null);

                    // Install the tooltip custom
                    itemView.setOnLongClickListener(v -> {
                        MenuItem item = ((ActionMenuItemView) v).getItemData();
                        CharSequence title = item.getTitle();
                        if (title != null) {
                            CustomTooltip.showBelow(v, title.toString());
                        }
                        return true;
                    });
                }
            }
        }
    }

    /**
     * Convenience method for creating card background.
     */
    public static Drawable cardBackground(Context context) {
        final SharedPreferences prefs = getDefaultSharedPreferences(context);
        final String darkMode = SettingsDAO.getDarkMode(prefs);
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final int radius = (int) dpToPx(18, displayMetrics);
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
            gradientDrawable.setStroke((int) dpToPx(2, displayMetrics), MaterialColors.getColor(
                    context, androidx.appcompat.R.attr.colorPrimary, Color.BLACK)
            );
        }

        return gradientDrawable;
    }

    /**
     * Convenience method for creating circle drawable.
     */
    public static Drawable circleDrawable() {
        final GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.OVAL);

        return gradientDrawable;
    }

    /**
     * Convenience method for creating pill background.
     */
    public static Drawable pillBackground(Context context, @AttrRes int colorAttributeResId) {
        final int radius = (int) dpToPx(50, context.getResources().getDisplayMetrics());
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
        gradientDrawable.setCornerRadius((int) dpToPx(18, context.getResources().getDisplayMetrics()));
        gradientDrawable.setColor(color);

        int rippleColor = MaterialColors.getColor(
                context, androidx.appcompat.R.attr.colorControlHighlight, Color.BLACK);

        return new RippleDrawable(ColorStateList.valueOf(rippleColor), gradientDrawable, null);
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
