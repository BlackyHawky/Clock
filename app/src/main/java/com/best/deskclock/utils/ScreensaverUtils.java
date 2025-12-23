// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel.ClockStyle;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.AnalogClock;
import com.best.deskclock.uicomponents.AutoSizingTextClock;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ScreensaverUtils {

    /**
     * Applies brightness adjustments to a view used in the screensaver.
     *
     * <p>The brightness level is retrieved from user preferences and applied differently
     * depending on the type of view:</p>
     *
     * <ul>
     *   <li><b>ImageView (background):</b> A ColorMatrix is applied to dim the image.</li>
     *   <li><b>TextView (date, next alarm):</b> The text color is recalculated based on the
     *       brightness factor.</li>
     *   <li><b>Standard AnalogClock:</b> A PorterDuffColorFilter is applied using the tinted
     *       and brightness-adjusted clock color.</li>
     *   <li><b>Material AnalogClock:</b> Only the brightness ColorMatrix is applied.</li>
     * </ul>
     *
     * <p>This method ensures consistent brightness behavior across all screensaver elements,
     * while preserving the intended color styling of each clock type.</p>
     *
     * @param view  The view to update.
     * @param prefs User preferences containing the brightness setting.
     * @param color Optional base color used for analog clock tinting.
     */
    private static void applyBrightness(View view, SharedPreferences prefs, @Nullable Integer color) {
        int brightnessPercentage = SettingsDAO.getScreensaverBrightness(prefs);

        float factor = 0.1f + (brightnessPercentage / 100f) * 0.9f;

        ColorMatrix matrix = new ColorMatrix();
        matrix.setScale(factor, factor, factor, 1f);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

        // For background
        if (view instanceof ImageView imageView) {
            imageView.setColorFilter(filter);
            return;
        }

        // For date and next alarm
        if (view instanceof TextView textView) {
            if (color != null) {
                textView.setTextColor(applyBrightnessToColor(color, factor));
            }
            return;
        }

        // For standard analog clock
        if (view instanceof AnalogClock && color != null) {
            Paint paint = new Paint();
            paint.setColorFilter(new PorterDuffColorFilter(applyBrightnessToColor(color, factor), PorterDuff.Mode.SRC_IN));
            view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
            return;
        }

        // For Material analog clock
        Paint paint = new Paint();
        paint.setColorFilter(filter);
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * Applies a brightness factor to a given RGB color.
     *
     * <p>The method multiplies each color channel (red, green, blue) by the provided factor,
     * clamping the result to the valid 0–255 range. This is used to dim or brighten colors
     * consistently with the screensaver brightness setting.</p>
     *
     * @param color  The original RGB color.
     * @param factor The brightness multiplier (0.0–1.0).
     * @return The brightness-adjusted RGB color.
     */
    private static int applyBrightnessToColor(int color, float factor) {
        int r = Math.min(255, (int) (Color.red(color) * factor));
        int g = Math.min(255, (int) (Color.green(color) * factor));
        int b = Math.min(255, (int) (Color.blue(color) * factor));
        return Color.rgb(r, g, b);
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

        applyGeneralTypeface(prefs, date, style);
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

        applyGeneralTypeface(prefs, nextAlarm, style);
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
     * Applies the general font to the given {@link TextView}.
     */
    private static void applyGeneralTypeface(SharedPreferences prefs, TextView textView, int style) {
        Typeface base = ThemeUtils.loadFont(SettingsDAO.getGeneralFont(prefs));

        if (base == null) {
            textView.setTypeface(Typeface.create("sans-serif", style));
        } else {
            textView.setTypeface(Typeface.create(base, style));
        }
    }

    /**
     * Returns the formatted "next alarm" text for the screensaver.
     * <p>
     * This method wraps the base formatted alarm time with thin spaces when the
     * screensaver settings specify italic text for the date or the next alarm.
     * Thin spaces (\u2009) prevent the text from being visually cut off on some devices
     * and help maintain proper centering in the screensaver layout.
     *
     * @param context    the context used to access preferences and formatting utilities
     * @param alarmTime  the time of the next scheduled alarm
     * @return the formatted alarm text, optionally wrapped with thin spaces
     */
    public static String getScreensaverFormattedTime(Context context, Calendar alarmTime) {
        String base = AlarmUtils.getFormattedTime(context, alarmTime);

        SharedPreferences prefs = getDefaultSharedPreferences(context);

        boolean italicDate = SettingsDAO.isScreensaverDateInItalic(prefs);
        boolean italicAlarm = SettingsDAO.isScreensaverNextAlarmInItalic(prefs);

        if (italicDate) {
            return "\u2009" + base + "\u2009";
        } else if (italicAlarm) {
            return base + "\u2009";
        }

        return base;
    }

    /**
     * Clock views can call this to refresh their date.
     **/
    public static void updateScreensaverDate(String dateSkeleton, String descriptionSkeleton, View clock) {
        final SharedPreferences prefs = getDefaultSharedPreferences(clock.getContext());
        final TextView dateDisplay = clock.findViewById(R.id.date);
        if (dateDisplay == null) {
            return;
        }

        final Locale locale = Locale.getDefault();
        String datePattern = DateFormat.getBestDateTimePattern(locale, dateSkeleton);

        if (SettingsDAO.isScreensaverDateInItalic(prefs)) {
            // Add a "Thin Space" (\u2009) at the end of the date to prevent its display
            // from being cut off on some devices.
            datePattern = "\u2009" + datePattern + "\u2009";
        } else if (SettingsDAO.isScreensaverNextAlarmInItalic(prefs)) {
            datePattern = datePattern + "\u2009";
        }

        final String descriptionPattern = DateFormat.getBestDateTimePattern(locale, descriptionSkeleton);

        final Date now = new Date();
        dateDisplay.setText(new SimpleDateFormat(datePattern, locale).format(now));
        dateDisplay.setVisibility(View.VISIBLE);
        dateDisplay.setContentDescription(new SimpleDateFormat(descriptionPattern, locale).format(now));
    }

    /**
     * For screensaver, set the margins and the style of the clock.
     */
    public static void setScreensaverClockStyle(final Context context, SharedPreferences prefs,
                                                final View view) {

        final View mainClockView = view.findViewById(R.id.main_clock);
        final ImageView backgroundImage = view.findViewById(R.id.screensaver_background_image);
        final String imagePath = SettingsDAO.getScreensaverBackgroundImage(prefs);

        if (imagePath != null) {
            backgroundImage.setVisibility(View.VISIBLE);

            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    backgroundImage.setImageBitmap(bitmap);
                    applyBrightness(backgroundImage, prefs, null);

                    if (SdkUtils.isAtLeastAndroid12() && SettingsDAO.isScreensaverBlurEffectEnabled(prefs)) {
                        float intensity = SettingsDAO.getScreensaverBlurIntensity(prefs);
                        RenderEffect blur = RenderEffect.createBlurEffect(intensity, intensity, Shader.TileMode.CLAMP);
                        backgroundImage.setRenderEffect(blur);
                    }
                } else {
                    LogUtils.e("Bitmap null for path: " + imagePath);
                    backgroundImage.setVisibility(View.GONE);
                }
            } else {
                LogUtils.e("Image file not found: " + imagePath);
                backgroundImage.setVisibility(View.GONE);
            }
        } else {
            backgroundImage.setVisibility(View.GONE);
        }

        // Style
        final ClockStyle screensaverClockStyle = SettingsDAO.getScreensaverClockStyle(prefs);
        final AnalogClock analogClock = mainClockView.findViewById(R.id.analog_clock);
        final AutoSizingTextClock textClock = mainClockView.findViewById(R.id.digital_clock);
        final boolean areClockSecondsEnabled = SettingsDAO.areScreensaverClockSecondsDisplayed(prefs);
        final TextView date = mainClockView.findViewById(R.id.date);
        final TextView nextAlarmIcon = mainClockView.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarm = mainClockView.findViewById(R.id.nextAlarm);
        final int inversePrimaryColor = ContextCompat.getColor(context, R.color.md_theme_inversePrimary);
        final boolean isMaterialAnalogClock = screensaverClockStyle == ClockStyle.ANALOG_MATERIAL;
        final boolean isDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(prefs);

        final int screenSaverClockColorPicker = isDynamicColors
                ? inversePrimaryColor
                : SettingsDAO.getScreensaverClockColorPicker(prefs);
        final int screensaverDateColorPicker = isDynamicColors && !isMaterialAnalogClock
                ? inversePrimaryColor
                : SettingsDAO.getScreensaverDateColorPicker(prefs);
        final int screensaverNextAlarmColorPicker = isDynamicColors && !isMaterialAnalogClock
                ? inversePrimaryColor
                : SettingsDAO.getScreensaverNextAlarmColorPicker(prefs);

        ClockUtils.setClockStyle(screensaverClockStyle, textClock, analogClock);

        if (screensaverClockStyle == ClockStyle.DIGITAL) {
            textClock.setTypeface(getScreensaverClockTypeface(prefs));
            ClockUtils.setDigitalClockTimeFormat(textClock, 0.4f, areClockSecondsEnabled,
                    false, false, true);

            textClock.applyUserPreferredTextSizeSp(SettingsDAO.getScreensaverDigitalClockFontSize(prefs));

            applyBrightness(textClock, prefs, screenSaverClockColorPicker);
        } else {
            ClockUtils.adjustAnalogClockSize(analogClock, prefs, false, false, true);
            ClockUtils.setAnalogClockSecondsEnabled(screensaverClockStyle, analogClock, areClockSecondsEnabled);

            if (isMaterialAnalogClock) {
                applyBrightness(analogClock, prefs, null);
            } else {
                applyBrightness(analogClock, prefs, screenSaverClockColorPicker);
            }
        }

        setScreensaverDateFormat(prefs, date);
        ClockUtils.setClockIconTypeface(nextAlarmIcon);
        setScreensaverNextAlarmFormat(prefs, nextAlarm);

        applyBrightness(date, prefs, screensaverDateColorPicker);
        applyBrightness(nextAlarmIcon, prefs, screensaverNextAlarmColorPicker);
        applyBrightness(nextAlarm, prefs, screensaverNextAlarmColorPicker);
    }

    /**
     * Hide system bars when the screensaver is active.
     */
    public static void hideScreensaverSystemBars(Window window, View view) {
        if (SdkUtils.isAtLeastAndroid10()) {
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, view);
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            controller.hide(WindowInsetsCompat.Type.systemBars());
        } else {
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
}
