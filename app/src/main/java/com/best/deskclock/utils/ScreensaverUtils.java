// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.uicomponents.AutoSizingTextClock;

public class ScreensaverUtils {

    /**
     * Generic method to apply a color filter to the screensaver.
     */
    private static void applyColorFilter(View view, Context context, SharedPreferences prefs,
                                         int color, PorterDuff.Mode mode) {

        String colorFilter = getScreensaverColorFilter(context, prefs, color);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setColorFilter(new PorterDuffColorFilter(Color.parseColor(colorFilter), mode));
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * For screensaver, calculate the color filter to use to dim the color.
     *
     * @param color the color selected in the screensaver color picker
     */
    private static String getScreensaverColorFilter(Context context, SharedPreferences prefs, int color) {
        final int brightnessPercentage = SettingsDAO.getScreensaverBrightness(prefs);

        if (SettingsDAO.areScreensaverClockDynamicColors(prefs)
                && SettingsDAO.getScreensaverClockStyle(prefs) != DataModel.ClockStyle.ANALOG_MATERIAL) {
            color = context.getColor(R.color.md_theme_inversePrimary);
        }

        String colorFilter = String.format("%06X", 0xFFFFFF & color);
        // The alpha channel should range from 16 (10 hex) to 192 (C0 hex).
        String alpha = String.format("%02X", 16 + (192 * brightnessPercentage / 100));

        colorFilter = "#" + alpha + colorFilter;

        return colorFilter;
    }

    /**
     * Dim the different views that make up the screensaver.
     */
    private static void dimScreensaverView(Context context, SharedPreferences prefs, View view, int color) {
        applyColorFilter(view, context, prefs, color, PorterDuff.Mode.SRC_IN);
    }

    /**
     * Dim the screensaver Material analog clock.
     */
    private static void dimMaterialAnalogClock(Context context, SharedPreferences prefs, View materialAnalogClock) {
        applyColorFilter(materialAnalogClock, context, prefs, Color.parseColor("#FFFFFF"), PorterDuff.Mode.MULTIPLY);
    }

    /**
     * Returns the Typeface to be used for the digital clock in screensaver mode.
     *
     * <p>This method loads the user-selected font file for the screensaver clock
     * and applies the style options (bold, italic, or bold-italic) based on
     * the user's preferences stored in SharedPreferences.</p>
     *
     * @param prefs SharedPreferences containing the user's screensaver clock settings
     * @return a Typeface object representing the chosen font with the applied style
     */
    public static Typeface getScreensaverClockTypeface(SharedPreferences prefs) {
        Typeface baseTypeface = ThemeUtils.loadFont(SettingsDAO.getScreensaverDigitalClockFont(prefs));
        int style = resolveTypefaceStyle(
                SettingsDAO.isScreensaverDigitalClockInBold(prefs),
                SettingsDAO.isScreensaverDigitalClockInItalic(prefs)
        );

        if (baseTypeface == null) {
            return Typeface.create("sans-serif", style);
        }

        return Typeface.create(baseTypeface, style);
    }

    /**
     * For screensaver, format the date and the next alarm to be bold and/or italic or not.
     *
     * @param date Date to format
     */
    private static void setScreensaverDateFormat(SharedPreferences prefs, TextView date) {
        int style = resolveTypefaceStyle(
                SettingsDAO.isScreensaverDateInBold(prefs),
                SettingsDAO.isScreensaverDateInItalic(prefs)
        );

        date.setTypeface(Typeface.defaultFromStyle(style));
    }

    /**
     * For screensaver, format the date and the next alarm to be bold and/or italic or not.
     *
     * @param nextAlarm Next alarm to format
     */
    private static void setScreensaverNextAlarmFormat(SharedPreferences prefs, TextView nextAlarm) {
        int style = resolveTypefaceStyle(
                SettingsDAO.isScreensaverNextAlarmInBold(prefs),
                SettingsDAO.isScreensaverNextAlarmInItalic(prefs)
        );

        nextAlarm.setTypeface(Typeface.defaultFromStyle(style));
    }

    /**
     * Determines the appropriate Typeface style based on bold and italic flags.
     *
     * @param isBold   True if the text should be bold.
     * @param isItalic True if the text should be italic.
     * @return The corresponding Typeface style constant (NORMAL, BOLD, ITALIC, or BOLD_ITALIC).
     */
    private static int resolveTypefaceStyle(boolean isBold, boolean isItalic) {
        if (isBold && isItalic) {
            return Typeface.BOLD_ITALIC;
        } else if (isBold) {
            return Typeface.BOLD;
        } else if (isItalic) {
            return Typeface.ITALIC;
        } else {
            return Typeface.NORMAL;
        }
    }

    /**
     * For screensaver, set the margins and the style of the clock.
     */
    public static void setScreensaverMarginsAndClockStyle(final Context context, SharedPreferences prefs,
                                                          final View clock) {

        final View mainClockView = clock.findViewById(R.id.main_clock);

        applyMargins(context, clock);

        // Style
        final DataModel.ClockStyle screensaverClockStyle = SettingsDAO.getScreensaverClockStyle(prefs);
        final AnalogClock analogClock = mainClockView.findViewById(R.id.analog_clock);
        final AutoSizingTextClock textClock = mainClockView.findViewById(R.id.digital_clock);
        final boolean areClockSecondsEnabled = SettingsDAO.areScreensaverClockSecondsDisplayed(prefs);
        final TextView date = mainClockView.findViewById(R.id.date);
        final TextView nextAlarmIcon = mainClockView.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarm = mainClockView.findViewById(R.id.nextAlarm);
        final int screenSaverClockColorPicker = SettingsDAO.getScreensaverClockColorPicker(prefs);
        final int screensaverDateColorPicker = SettingsDAO.getScreensaverDateColorPicker(prefs);
        final int screensaverNextAlarmColorPicker = SettingsDAO.getScreensaverNextAlarmColorPicker(prefs);

        ClockUtils.setClockStyle(screensaverClockStyle, textClock, analogClock);

        if (screensaverClockStyle == DataModel.ClockStyle.DIGITAL) {
            textClock.setTypeface(getScreensaverClockTypeface(prefs));
            ClockUtils.setDigitalClockTimeFormat(textClock, 0.4f, areClockSecondsEnabled,
                    false, false, true);

            textClock.applyUserPreferredTextSizeSp(SettingsDAO.getScreensaverDigitalClockFontSize(prefs));

            dimScreensaverView(context, prefs, textClock, screenSaverClockColorPicker);
        } else {
            ClockUtils.adjustAnalogClockSize(analogClock, prefs, false, true);
            ClockUtils.setAnalogClockSecondsEnabled(screensaverClockStyle, analogClock, areClockSecondsEnabled);

            if (screensaverClockStyle == DataModel.ClockStyle.ANALOG_MATERIAL) {
                dimMaterialAnalogClock(context, prefs, analogClock);
            } else {
                dimScreensaverView(context, prefs, analogClock, screenSaverClockColorPicker);
            }
        }

        setScreensaverDateFormat(prefs, date);
        ClockUtils.setClockIconTypeface(nextAlarmIcon);
        setScreensaverNextAlarmFormat(prefs, nextAlarm);

        dimScreensaverView(context, prefs, date, screensaverDateColorPicker);
        dimScreensaverView(context, prefs, nextAlarmIcon, screensaverNextAlarmColorPicker);
        dimScreensaverView(context, prefs, nextAlarm, screensaverNextAlarmColorPicker);
    }

    /**
     * Calculate and apply margins.
     */
    private static void applyMargins(Context context, View clockView) {
        final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        final boolean isTablet = ThemeUtils.isTablet();
        final boolean isLandscape = ThemeUtils.isLandscape();
        final View mainClockView = clockView.findViewById(R.id.main_clock);

        int marginLeftAndRight = (int) dpToPx(isTablet ? 20 : 16, displayMetrics);
        int marginTop = (int) dpToPx(isTablet ? (isLandscape ? 32 : 48) : (isLandscape ? 16 : 24), displayMetrics);
        int marginBottom = (int) dpToPx(isTablet ? 20 : 16, displayMetrics);

        // Apply margins
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) mainClockView.getLayoutParams();
        params.setMargins(marginLeftAndRight, marginTop, marginLeftAndRight, marginBottom);
        mainClockView.setLayoutParams(params);

        // Margins for other views (e.g., digital and analog clock)
        int digitalClockMarginBottom = (int) dpToPx(isTablet ? -18 : -8, displayMetrics);
        int analogClockMarginBottom = (int) dpToPx(isLandscape ? 5 : (isTablet ? 18 : 14), displayMetrics);

        ViewGroup.MarginLayoutParams digitalClockParams = (ViewGroup.MarginLayoutParams) mainClockView.getLayoutParams();
        digitalClockParams.setMargins(0, 0, 0, digitalClockMarginBottom);
        mainClockView.setLayoutParams(digitalClockParams);

        ViewGroup.MarginLayoutParams analogClockParams = (ViewGroup.MarginLayoutParams) mainClockView.getLayoutParams();
        analogClockParams.setMargins(0, 0, 0, analogClockMarginBottom);
        mainClockView.setLayoutParams(analogClockParams);
    }

    /**
     * Hide system bars when the screensaver is active.
     */
    public static void hideScreensaverSystemBars(Window window, View view) {
        if (SdkUtils.isAtLeastAndroid10()) {
            WindowInsetsControllerCompat windowInsetsController =
                    WindowCompat.getInsetsController(window, view);
            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

            ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
                if (insets.isVisible(WindowInsetsCompat.Type.statusBars())
                        || insets.isVisible(WindowInsetsCompat.Type.navigationBars())) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
                }

                return ViewCompat.onApplyWindowInsets(v, insets);
            });
        } else {
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
}
