// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import static android.view.View.VISIBLE;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.text.format.DateFormat;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
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
     *   <li><b>TextView (battery text, date, next alarm):</b> The text color is recalculated based
     *       on the brightness factor.</li>
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
    private static void applyBrightness(View view, SharedPreferences prefs, @Nullable Integer color,
                                        @Nullable Drawable drawable) {

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

        // For battery text, date and next alarm
        if (view instanceof TextView textView) {
            if (color != null) {
                textView.setTextColor(applyBrightnessToColor(color, factor));

                if (drawable != null) {
                    drawable.setColorFilter(new PorterDuffColorFilter(
                            applyBrightnessToColor(color, factor), PorterDuff.Mode.SRC_IN));
                }
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
     * For screensaver, format the battery text to be bold and/or italic or not.
     *
     * @param batteryText Battery text to format
     */
    private static void setScreensaverBatteryFormat(SharedPreferences prefs, TextView batteryText) {
        int style = resolveTypefaceStyle(
                SettingsDAO.isScreensaverBatteryInBold(prefs),
                SettingsDAO.isScreensaverBatteryInItalic(prefs)
        );

        applyGeneralTypeface(prefs, batteryText, style);
    }

    /**
     * Updates the battery percentage text and icon based on the given battery intent.
     *
     * @param view   the root view containing the battery indicator TextView
     * @param intent the Intent carrying battery status information (ACTION_BATTERY_CHANGED)
     */
    @SuppressLint("SetTextI18n")
    public static void updateBatteryText(View view, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int percent = (int) ((level / (float) scale) * 100);

        TextView batteryText = view.findViewById(R.id.battery_level);
        batteryText.setText(percent + "%");

        updateBatteryIcon(view, percent);
    }

    /**
     * Updates the battery icon displayed next to the battery percentage.
     *
     * @param view      the root view containing the battery indicator TextView
     * @param percent   the current battery level as a percentage
     */
    public static void updateBatteryIcon(View view, int percent) {
        Context context = view.getContext();
        final SharedPreferences prefs = getDefaultSharedPreferences(context);

        final TextView batteryText = view.findViewById(R.id.battery_level);
        int iconRes = getBatteryIconRes(percent);
        final Drawable drawable = AppCompatResources.getDrawable(context, iconRes);

        final boolean isDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(prefs);
        final int inversePrimaryColor = ContextCompat.getColor(context, R.color.md_theme_inversePrimary);
        final int color = isDynamicColors
                ? inversePrimaryColor
                : SettingsDAO.getScreensaverBatteryColorPicker(prefs);

        applyBrightness(batteryText, prefs, color, drawable);

        batteryText.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
    }

    /**
     * Returns the appropriate battery icon resource based on the battery level.
     *
     * @param percent   the current battery level as a percentage
     * @return the drawable resource ID representing the battery state
     */
    private static int getBatteryIconRes(int percent) {
        if (percent < 10) return R.drawable.ic_battery_alert;
        if (percent < 15) return R.drawable.ic_battery_15;
        if (percent < 30) return R.drawable.ic_battery_30;
        if (percent < 45) return R.drawable.ic_battery_45;
        if (percent < 60) return R.drawable.ic_battery_60;
        if (percent < 75) return R.drawable.ic_battery_75;
        if (percent < 90) return R.drawable.ic_battery_90;
        return R.drawable.ic_battery_full;
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
     * Thin spaces (u2009) prevent the text from being visually cut off on some devices
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
        dateDisplay.setVisibility(VISIBLE);
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
            backgroundImage.setVisibility(VISIBLE);

            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap != null) {
                    backgroundImage.setImageBitmap(bitmap);
                    applyBrightness(backgroundImage, prefs, null, null);

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
        final TextView batteryText = mainClockView.findViewById(R.id.battery_level);
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

            applyBrightness(textClock, prefs, screenSaverClockColorPicker, null);
        } else {
            ClockUtils.adjustAnalogClockSize(analogClock, prefs, false, false, true);
            ClockUtils.setAnalogClockSecondsEnabled(screensaverClockStyle, analogClock, areClockSecondsEnabled);

            if (isMaterialAnalogClock) {
                applyBrightness(analogClock, prefs, null, null);
            } else {
                applyBrightness(analogClock, prefs, screenSaverClockColorPicker, null);
            }
        }

        if (SettingsDAO.isScreensaverBatteryDisplayed(prefs)) {
            batteryText.setVisibility(VISIBLE);
            setScreensaverBatteryFormat(prefs, batteryText);
        }

        setScreensaverDateFormat(prefs, date);
        ClockUtils.setClockIconTypeface(nextAlarmIcon);
        setScreensaverNextAlarmFormat(prefs, nextAlarm);

        applyBrightness(date, prefs, screensaverDateColorPicker, null);
        applyBrightness(nextAlarmIcon, prefs, screensaverNextAlarmColorPicker, null);
        applyBrightness(nextAlarm, prefs, screensaverNextAlarmColorPicker, null);
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
