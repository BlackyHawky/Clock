// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.utils;

import static android.view.View.VISIBLE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_BLUR_INTENSITY;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.DataModel.ClockStyle;
import com.best.deskclock.databinding.DeskClockSaverBinding;
import com.best.deskclock.screensaver.ScreensaverSettings;
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
     * @param view                 The view to update.
     * @param brightnessPercentage The brightness applied to the view.
     * @param color                Optional base color used for analog clock tinting.
     * @param drawable             The {@link Drawable} used for the {@link TextView}.
     */
    private static void applyBrightness(@NonNull View view, int brightnessPercentage, @Nullable Integer color,
                                        @Nullable Drawable drawable) {

        float factor = 0.1f + (brightnessPercentage / 100f) * 0.9f;

        ColorMatrix matrix = new ColorMatrix();
        matrix.setScale(factor, factor, factor, 1f);

        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

        // For background and vector icons (ImageView)
        if (view instanceof ImageView imageView) {
            if (color != null) {
                // For vector icons
                imageView.setColorFilter(new PorterDuffColorFilter(applyBrightnessToColor(color, factor), PorterDuff.Mode.SRC_IN));
            } else {
                // For background
                imageView.setColorFilter(filter);
            }

            return;
        }

        // For battery text, date and next alarm
        if (view instanceof TextView textView) {
            if (color != null) {
                textView.setTextColor(applyBrightnessToColor(color, factor));

                if (drawable != null) {
                    drawable.setColorFilter(new PorterDuffColorFilter(applyBrightnessToColor(color, factor), PorterDuff.Mode.SRC_IN));
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
     * Returns the {@link Typeface} to be used for the digital clock in screensaver mode.
     *
     * <p>This method loads the user-selected font file for the screensaver clock
     * and applies the style options (bold, italic, or bold-italic) based on
     * the user's preferences.</p>
     *
     * @param font          the {@link Typeface} applied to the screensaver
     * @param isClockBold   {@code true} if the clock is in bold; {@code false} otherwise
     * @param isClockItalic {@code true} if the clock is in italics; {@code false} otherwise
     * @return a Typeface object representing the chosen font with the applied style
     */
    public static Typeface getScreensaverClockTypeface(@Nullable Typeface font, boolean isClockBold, boolean isClockItalic) {
        int style = resolveTypefaceStyle(
            isClockBold,
            isClockItalic
        );

        if (font == null) {
            return Typeface.create("sans-serif", style);
        }

        return Typeface.create(font, style);
    }

    /**
     * For screensaver, format the battery text to be bold and/or italic or not.
     *
     * @param batteryText     Battery text to format
     * @param font            the {@link Typeface} applied to the battery text
     * @param isBatteryBold   {@code true} if the battery text is in bold; {@code false} otherwise
     * @param isBatteryItalic {@code true} if the battery text is in italics; {@code false} otherwise
     */
    private static void setScreensaverBatteryFormat(@NonNull TextView batteryText, @NonNull Typeface font, boolean isBatteryBold,
                                                    boolean isBatteryItalic) {

        int style = resolveTypefaceStyle(
            isBatteryBold,
            isBatteryItalic
        );

        batteryText.setTypeface(font, style);
    }

    /**
     * Updates the battery percentage text and icon based on the given battery intent.
     *
     * @param view                 the root view containing the battery indicator TextView
     * @param intent               the Intent carrying battery status information (ACTION_BATTERY_CHANGED)
     * @param brightnessPercentage the brightness applied to the view
     * @param batteryColor         the color applied to the view
     * @param isBatteryItalic      {@code true} if the battery text is in italics; {@code false} otherwise
     */
    @SuppressLint("SetTextI18n")
    public static void updateBatteryText(@NonNull View view, @NonNull Intent intent, int brightnessPercentage, int batteryColor,
                                         boolean isBatteryItalic) {

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int percent = (int) ((level / (float) scale) * 100);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

        TextView batteryLevel = view.findViewById(R.id.battery_level);
        CharSequence batteryText = isBatteryItalic ? percent + "%" + "\u200A" : percent + "%";

        batteryLevel.setText(batteryText);

        updateBatteryIcon(view, brightnessPercentage, batteryColor, percent, isCharging);
    }

    /**
     * Updates the battery icon displayed next to the battery percentage.
     *
     * @param view                 the root view containing the battery indicator TextView
     * @param brightnessPercentage the brightness applied to the view.
     * @param batteryColor         the color applied to the view.
     * @param percent              the current battery level as a percentage
     * @param isCharging           {@code true} if the device is charging; {@code false} otherwise
     */
    public static void updateBatteryIcon(@NonNull View view, int brightnessPercentage, int batteryColor, int percent, boolean isCharging) {
        Context context = view.getContext();

        final TextView batteryText = view.findViewById(R.id.battery_level);
        int iconRes = getBatteryIconRes(percent, isCharging);
        final Drawable drawable = AppCompatResources.getDrawable(context, iconRes);

        applyBrightness(batteryText, brightnessPercentage, batteryColor, drawable);

        batteryText.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
    }

    /**
     * Returns the appropriate battery icon resource based on the battery level.
     *
     * @param percent the current battery level as a percentage
     * @param isCharging {@code true} if the device is charging, {@code false} otherwise
     * @return the drawable resource ID representing the battery state
     */
    private static int getBatteryIconRes(int percent, boolean isCharging) {
        if (isCharging) {
            if (percent < 10) return R.drawable.ic_battery_alert_charging;
            if (percent < 20) return R.drawable.ic_battery_20_charging;
            if (percent < 35) return R.drawable.ic_battery_35_charging;
            if (percent < 50) return R.drawable.ic_battery_50_charging;
            if (percent < 70) return R.drawable.ic_battery_70_charging;
            if (percent < 90) return R.drawable.ic_battery_90_charging;
            return R.drawable.ic_battery_full_charging;
        } else {
            if (percent < 10) return R.drawable.ic_battery_alert;
            if (percent < 15) return R.drawable.ic_battery_15;
            if (percent < 30) return R.drawable.ic_battery_30;
            if (percent < 45) return R.drawable.ic_battery_45;
            if (percent < 60) return R.drawable.ic_battery_60;
            if (percent < 75) return R.drawable.ic_battery_75;
            if (percent < 90) return R.drawable.ic_battery_90;
            return R.drawable.ic_battery_full;
        }
    }

    /**
     * For screensaver, format the date and the next alarm to be bold and/or italic or not.
     *
     * @param date         Date to format
     * @param font         the {@link Typeface} applied to the date
     * @param isDateBold   {@code true} if the date is in bold; {@code false} otherwise
     * @param isDateItalic {@code true} if the date is in italics; {@code false} otherwise
     */
    private static void setScreensaverDateFormat(@NonNull TextView date, @NonNull Typeface font, boolean isDateBold, boolean isDateItalic) {
        int style = resolveTypefaceStyle(
            isDateBold,
            isDateItalic
        );

        date.setTypeface(font, style);
    }

    /**
     * For screensaver, format the date and the next alarm to be bold and/or italic or not.
     *
     * @param nextAlarm         Next alarm to format
     * @param font              the {@link Typeface} applied to the next alarm
     * @param isNextAlarmBold   {@code true} if the date is in bold; {@code false} otherwise
     * @param isNextAlarmItalic {@code true} if the date is in italics; {@code false} otherwise
     */
    private static void setScreensaverNextAlarmFormat(@NonNull TextView nextAlarm, @NonNull Typeface font, boolean isNextAlarmBold,
                                                      boolean isNextAlarmItalic) {

        int style = resolveTypefaceStyle(
            isNextAlarmBold,
            isNextAlarmItalic
        );

        nextAlarm.setTypeface(font, style);
    }

    /**
     * Determines the appropriate Typeface style based on bold and italic flags.
     *
     * @param isBold   {@code true} if the text should be bold; {@code false} otherwise
     * @param isItalic {@code true} if the text should be italic; {@code false} otherwise
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
     * Returns the formatted "next alarm" text for the screensaver.
     * <p>
     * This method wraps the base formatted alarm time with thin spaces when the
     * screensaver settings specify italic text for the date or the next alarm.
     * Thin spaces (u2009) prevent the text from being visually cut off on some devices
     * and help maintain proper centering in the screensaver layout.
     *
     * @param context           the context used to access preferences and formatting utilities
     * @param isDateItalic      {@code true} if the date is in italics; {@code false} otherwise
     * @param isNextAlarmItalic {@code true} if the next alarm is in italics; {@code false} otherwise
     * @param alarmTime         the time of the next scheduled alarm
     * @return the formatted alarm text, optionally wrapped with thin spaces
     */
    @NonNull
    public static String getScreensaverFormattedTime(@NonNull Context context, @NonNull Calendar alarmTime, boolean isDateItalic,
                                                     boolean isNextAlarmItalic) {

        String base = AlarmUtils.getFormattedTime(context, alarmTime);

        if (isDateItalic) {
            return "\u2009" + base + "\u2009";
        } else if (isNextAlarmItalic) {
            return base + "\u2009";
        }

        return base;
    }

    /**
     * Refreshes the next alarm and date displayed on the screensaver.
     * The date format automatically expands when the next alarm is hidden or unavailable.
     *
     * @param binding              The binding containing the screensaver views. If null, no action is performed.
     * @param isUppercase          {@code true} if the text should be displayed in uppercase; {@code false} otherwise.
     * @param isNextAlarmDisplayed {@code true} if the next alarm is displayed; {@code false} otherwise.
     * @param isDateItalic         {@code true} if the date is in italics; {@code false} otherwise
     * @param isNextAlarmItalic    {@code true} if the next alarm is in italics; {@code false} otherwise
     */
    public static void refreshAlarmAndDate(@Nullable DeskClockSaverBinding binding, boolean isUppercase, boolean isNextAlarmDisplayed,
                                           boolean isDateItalic, boolean isNextAlarmItalic) {

        if (binding != null) {
            Context context = binding.getRoot().getContext();
            String shortDateFormat = context.getString(R.string.abbrev_wday_month_day_no_year);
            String longDateFormat = context.getString(R.string.full_wday_month_day_no_year);

            boolean isAlarmVisible = false;

            if (isNextAlarmDisplayed) {
                isAlarmVisible = AlarmUtils.refreshAlarm(binding.saverContainer, isUppercase, true, isDateItalic, isNextAlarmItalic);
            } else {
                binding.dateAndNextAlarmTime.nextAlarmIcon.setVisibility(View.GONE);
                binding.dateAndNextAlarmTime.nextAlarm.setVisibility(View.GONE);
            }

            String datePattern = isAlarmVisible ? shortDateFormat : longDateFormat;

            updateScreensaverDate(binding.saverContainer, datePattern, longDateFormat, isUppercase, isDateItalic, isNextAlarmItalic);
        }
    }

    /**
     * Clock views can call this to refresh their date.
     **/
    public static void updateScreensaverDate(@NonNull View clock, @NonNull String dateSkeleton, @NonNull String descriptionSkeleton,
                                             boolean isUppercase, boolean isDateItalic, boolean isNextAlarmItalic) {

        final TextView dateDisplay = clock.findViewById(R.id.date);
        if (dateDisplay == null) {
            return;
        }

        final Locale locale = Locale.getDefault();
        String datePattern = DateFormat.getBestDateTimePattern(locale, dateSkeleton);

        if (isDateItalic) {
            // Add a "Thin Space" (\u2009) at the end of the date to prevent its display
            // from being cut off on some devices.
            datePattern = "\u2009" + datePattern + "\u2009";
        } else if (isNextAlarmItalic) {
            datePattern = datePattern + "\u2009";
        }

        final String descriptionPattern = DateFormat.getBestDateTimePattern(locale, descriptionSkeleton);

        final Date now = new Date();
        String formattedDate = new SimpleDateFormat(datePattern, locale).format(now);

        dateDisplay.setAllCaps(isUppercase);
        dateDisplay.setText(FormattedTextUtils.capitalizeFirstLetter(formattedDate, locale));
        dateDisplay.setVisibility(VISIBLE);
        dateDisplay.setContentDescription(new SimpleDateFormat(descriptionPattern, locale).format(now));
    }

    /**
     * Main entry point to configure the screensaver views, including background and clock styles.
     */
    public static void setupScreensaverView(@NonNull View view, @NonNull ScreensaverSettings settings,
                                            @NonNull DisplayMetrics displayMetrics, boolean isLandscape,
                                            @Nullable Runnable onImageLoaded) {

        final ImageView backgroundImage = view.findViewById(R.id.screensaver_background_image);
        final View mainClockView = view.findViewById(R.id.main_clock);

        loadBackgroundImage(backgroundImage, settings, onImageLoaded);

        configureClocks(mainClockView, settings, displayMetrics, isLandscape);

        configureSecondaryElements(mainClockView, settings);
    }

    /**
     * Loads and applies the custom background image asynchronously, including optional blur effects.
     */
    private static void loadBackgroundImage(@NonNull ImageView backgroundImage, @NonNull ScreensaverSettings settings,
                                            @Nullable Runnable onImageLoaded) {

        if (settings.backgroundImagePath != null) {
            AppExecutors.getDiskIO().execute(() -> {
                File imageFile = new File(settings.backgroundImagePath);
                if (imageFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());

                    AppExecutors.getMainThread().post(() -> {
                        if (bitmap != null) {
                            backgroundImage.setVisibility(View.VISIBLE);
                            backgroundImage.setImageBitmap(bitmap);
                            applyBrightness(backgroundImage, settings.brightnessPercentage, null, null);

                            if (SdkUtils.isAtLeastAndroid12() && settings.blurIntensity != DEFAULT_BLUR_INTENSITY) {
                                RenderEffect blur = RenderEffect.createBlurEffect(
                                    settings.blurIntensity, settings.blurIntensity, Shader.TileMode.CLAMP);
                                backgroundImage.setRenderEffect(blur);
                            }

                            if (onImageLoaded != null) {
                                onImageLoaded.run();
                            }
                        }
                    });
                }
            });
        } else {
            backgroundImage.setVisibility(View.GONE);
        }
    }

    /**
     * Sets up the analog or digital clock based on the user's selected style and preferences.
     */
    private static void configureClocks(@NonNull View mainClockView, @NonNull ScreensaverSettings settings,
                                        @NonNull DisplayMetrics displayMetrics, boolean isLandscape) {

        final AnalogClock analogClock = mainClockView.findViewById(R.id.analog_clock);
        final AutoSizingTextClock textClock = mainClockView.findViewById(R.id.digital_clock);

        analogClock.configure(
            settings.clockStyle,
            settings.clockDial,
            settings.clockDialMaterial,
            settings.clockSecondHand,
            settings.activeAccentColor,
            0, 0, false
        );

        ClockUtils.setClockStyle(settings.clockStyle, textClock, analogClock);

        if (settings.clockStyle == ClockStyle.DIGITAL) {
            textClock.setTypeface(settings.screensaverTypeface);

            int style = settings.isDigitalItalic ? Typeface.BOLD_ITALIC : Typeface.BOLD;
            Typeface amPmTypeface = Typeface.create(settings.screensaverTypeface, style);

            ClockUtils.setDigitalClockTimeFormat(textClock, settings.areClockSecondsEnabled, 0.4f, amPmTypeface, "sans-serif", style, true);
            textClock.applyUserPreferredTextSizeSp(settings.digitalFontSize);

            applyBrightness(textClock, settings.brightnessPercentage, settings.clockColor, null);
        } else {
            ClockUtils.adjustAnalogClockSize(analogClock, displayMetrics, settings.analogClockSize, isLandscape);
            ClockUtils.setAnalogClockSecondsEnabled(settings.clockStyle, analogClock, settings.areClockSecondsEnabled);

            if (settings.clockStyle == ClockStyle.ANALOG_MATERIAL) {
                applyBrightness(analogClock, settings.brightnessPercentage, null, null);
            } else {
                applyBrightness(analogClock, settings.brightnessPercentage, settings.clockColor, null);
            }
        }
    }

    /**
     * Configures the visibility, typeface, and color of the battery, date, and next alarm elements.
     */
    private static void configureSecondaryElements(@NonNull View mainClockView, @NonNull ScreensaverSettings settings) {
        final TextView batteryText = mainClockView.findViewById(R.id.battery_level);
        final TextView date = mainClockView.findViewById(R.id.date);
        final TextView nextAlarmIcon = mainClockView.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarm = mainClockView.findViewById(R.id.nextAlarm);

        if (settings.isBatteryDisplayed) {
            batteryText.setVisibility(View.VISIBLE);
            setScreensaverBatteryFormat(batteryText, settings.screensaverTypeface, settings.isBatteryBold, settings.isBatteryItalic);
        }

        setScreensaverDateFormat(date, settings.screensaverTypeface, settings.isDateBold, settings.isDateItalic);
        ClockUtils.setClockIconTypeface(nextAlarmIcon);
        setScreensaverNextAlarmFormat(nextAlarm, settings.screensaverTypeface, settings.isNextAlarmBold, settings.isNextAlarmItalic);

        applyBrightness(date, settings.brightnessPercentage, settings.dateColor, null);
        applyBrightness(nextAlarmIcon, settings.brightnessPercentage, settings.nextAlarmColor, null);
        applyBrightness(nextAlarm, settings.brightnessPercentage, settings.nextAlarmColor, null);
    }

}
