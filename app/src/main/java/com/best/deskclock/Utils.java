/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.best.deskclock;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY;
import static android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static android.graphics.Bitmap.Config.ARGB_8888;

import static com.best.deskclock.settings.SettingsActivity.DARK_THEME;
import static com.best.deskclock.settings.SettingsActivity.KEY_AMOLED_DARK_MODE;
import static com.best.deskclock.settings.SettingsActivity.KEY_DEFAULT_DARK_MODE;
import static com.best.deskclock.settings.SettingsActivity.LIGHT_THEME;
import static com.best.deskclock.settings.SettingsActivity.SYSTEM_THEME;

import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.ArraySet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.AnyRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableKt;
import androidx.core.graphics.ColorUtils;

import com.best.deskclock.data.DataModel;
import com.best.deskclock.provider.AlarmInstance;
import com.best.deskclock.uidata.UiDataModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    /**
     * {@link Uri} signifying the "silent" ringtone.
     */
    public static final Uri RINGTONE_SILENT = Uri.EMPTY;

    public static void enforceMainLooper() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalAccessError("May only call from main thread.");
        }
    }

    public static void enforceNotMainLooper() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalAccessError("May not call from main thread.");
        }
    }

    /**
     * @param resourceId identifies an application resource
     * @return the Uri by which the application resource is accessed
     */
    public static Uri getResourceUri(Context context, @AnyRes int resourceId) {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getPackageName())
                .path(String.valueOf(resourceId))
                .build();
    }

    /**
     * @param view the scrollable view to test
     * @return {@code true} iff the {@code view} content is currently scrolled to the top
     */
    public static boolean isScrolledToTop(View view) {
        return !view.canScrollVertically(-1);
    }

    /**
     * Calculate the amount by which the radius of a CircleTimerView should be offset by any
     * of the extra painted objects.
     */
    public static float calculateRadiusOffset(float strokeSize, float dotStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(dotStrokeSize, markerStrokeSize));
    }

    /**
     * Configure the clock that is visible to display seconds. The clock that is not visible never
     * displays seconds to avoid it scheduling unnecessary ticking runnables.
     */
    public static void setClockSecondsEnabled(TextClock digitalClock, AnalogClock analogClock) {
        final boolean displaySeconds = DataModel.getDataModel().getDisplayClockSeconds();
        final DataModel.ClockStyle clockStyle = DataModel.getDataModel().getClockStyle();
        switch (clockStyle) {
            case ANALOG -> {
                setTimeFormat(digitalClock, false);
                analogClock.enableSeconds(displaySeconds);
                return;
            }
            case DIGITAL -> {
                analogClock.enableSeconds(false);
                setTimeFormat(digitalClock, displaySeconds);
                return;
            }
        }

        throw new IllegalStateException("unexpected clock style: " + clockStyle);
    }

    /**
     * Set whether the digital or analog clock should be displayed in the application.
     * Returns the view to be displayed.
     */
    public static void setClockStyle(View digitalClock, View analogClock) {
        final DataModel.ClockStyle clockStyle = DataModel.getDataModel().getClockStyle();
        switch (clockStyle) {
            case ANALOG -> {
                final Context context = analogClock.getContext();
                // Optimally adjusts the height and the width of the analog clock when displayed
                // on a tablet or phone in portrait or landscape mode
                if (isTablet(context) || isLandscape(context)) {
                    analogClock.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    analogClock.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
                } else {
                    analogClock.getLayoutParams().height = toPixel(240, context);
                    analogClock.getLayoutParams().width = toPixel(240, context);
                }

                analogClock.setVisibility(View.VISIBLE);
                digitalClock.setVisibility(View.GONE);
                return;
            }
            case DIGITAL -> {
                digitalClock.setVisibility(View.VISIBLE);
                analogClock.setVisibility(View.GONE);
                return;
            }
        }

        throw new IllegalStateException("unexpected clock style: " + clockStyle);
    }

    /**
     * Update and return the PendingIntent corresponding to the given {@code intent}.
     *
     * @param context the Context in which the PendingIntent should start the service
     * @param intent  an Intent describing the service to be started
     * @return a PendingIntent that will start a service
     */
    public static PendingIntent pendingServiceIntent(Context context, Intent intent) {
        return PendingIntent.getService(context, 0, intent, FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    /**
     * Update and return the PendingIntent corresponding to the given {@code intent}.
     *
     * @param context the Context in which the PendingIntent should start the activity
     * @param intent  an Intent describing the activity to be started
     * @return a PendingIntent that will start an activity
     */
    public static PendingIntent pendingActivityIntent(Context context, Intent intent) {
        // explicitly set the flag here, as getActivity() documentation states we must do so
        return PendingIntent.getActivity(context, 0, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);
    }

    /**
     * @return The next alarm from {@link AlarmManager}
     */
    public static String getNextAlarm(Context context) {
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final AlarmClockInfo info = getNextAlarmClock(am);
        if (info != null) {
            final long triggerTime = info.getTriggerTime();
            final Calendar alarmTime = Calendar.getInstance();
            alarmTime.setTimeInMillis(triggerTime);
            return AlarmUtils.getFormattedTime(context, alarmTime);
        }

        return null;
    }

    private static AlarmClockInfo getNextAlarmClock(AlarmManager am) {
        return am.getNextAlarmClock();
    }

    public static void updateNextAlarm(AlarmManager am, AlarmClockInfo info, PendingIntent op) {
        am.setAlarmClock(info, op);
    }

    public static boolean isAlarmWithin24Hours(AlarmInstance alarmInstance) {
        final Calendar nextAlarmTime = alarmInstance.getAlarmTime();
        final long nextAlarmTimeMillis = nextAlarmTime.getTimeInMillis();
        return nextAlarmTimeMillis - System.currentTimeMillis() <= DateUtils.DAY_IN_MILLIS;
    }

    /**
     * Clock views can call this to refresh their alarm to the next upcoming value.
     */
    public static void refreshAlarm(Context context, View clock) {
        final TextView nextAlarmIconView = clock.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarmView = clock.findViewById(R.id.nextAlarm);
        if (nextAlarmView == null) {
            return;
        }

        final String alarm = getNextAlarm(context);
        if (!TextUtils.isEmpty(alarm)) {
            final String description = context.getString(R.string.next_alarm_description, alarm);
            nextAlarmView.setText(alarm);
            nextAlarmView.setContentDescription(description);
            nextAlarmView.setVisibility(View.VISIBLE);
            nextAlarmIconView.setVisibility(View.VISIBLE);
            nextAlarmIconView.setContentDescription(description);
        } else {
            nextAlarmView.setVisibility(View.GONE);
            nextAlarmIconView.setVisibility(View.GONE);
        }
    }

    public static void setClockIconTypeface(View clock) {
        final TextView nextAlarmIconView = clock.findViewById(R.id.nextAlarmIcon);
        nextAlarmIconView.setTypeface(UiDataModel.getUiDataModel().getAlarmIconTypeface());
    }

    /**
     * Clock views can call this to refresh their date.
     **/
    public static void updateDate(String dateSkeleton, String descriptionSkeleton, View clock) {
        final TextView dateDisplay = clock.findViewById(R.id.date);
        if (dateDisplay == null) {
            return;
        }

        final Locale l = Locale.getDefault();
        String datePattern = DateFormat.getBestDateTimePattern(l, dateSkeleton);
        if (dateDisplay.getContext() instanceof ScreensaverActivity || dateDisplay.getContext() instanceof Screensaver) {
            // Add a "Thin Space" (\u2009) at the end of the date to prevent its display from being cut off on some devices.
            // (The display of the date is only cut off at the end if it is defined in italics in the screensaver settings).
            final boolean isItalicDate = DataModel.getDataModel().getScreensaverItalicDate();
            final boolean isItalicNextAlarm = DataModel.getDataModel().getScreensaverItalicNextAlarm();
            if (isItalicDate) {
                datePattern = "\u2009" + DateFormat.getBestDateTimePattern(l, dateSkeleton) + "\u2009";
            } else if (isItalicNextAlarm) {
                datePattern = "\u2009" + DateFormat.getBestDateTimePattern(l, dateSkeleton);
            }
        }

        final String descriptionPattern = DateFormat.getBestDateTimePattern(l, descriptionSkeleton);
        final Date now = new Date();
        dateDisplay.setText(new SimpleDateFormat(datePattern, l).format(now));
        dateDisplay.setVisibility(View.VISIBLE);
        dateDisplay.setContentDescription(new SimpleDateFormat(descriptionPattern, l).format(now));
    }

    /***
     * Formats the time in the TextClock according to the Locale with a special
     * formatting treatment for the am/pm label.
     *
     * @param clock          TextClock to format
     * @param includeSeconds whether or not to include seconds in the clock's time
     */
    public static void setTimeFormat(TextClock clock, boolean includeSeconds) {
        if (clock != null) {
            // Get the best format for 12 hours mode according to the locale
            clock.setFormat12Hour(get12ModeFormat(clock.getContext(), 0.4f, includeSeconds));
            // Get the best format for 24 hours mode according to the locale
            clock.setFormat24Hour(get24ModeFormat(clock.getContext(), includeSeconds));
        }
    }

    /**
     * For screensavers to set whether the digital or analog clock should be displayed.
     * Returns the view to be displayed.
     */
    public static void setScreensaverClockStyle(View digitalClock, View analogClock) {
        final DataModel.ClockStyle clockStyle = DataModel.getDataModel().getScreensaverClockStyle();
        switch (clockStyle) {
            case ANALOG -> {
                final Context context = analogClock.getContext();
                analogClock.getLayoutParams().height = toPixel(isTablet(context) ? 300 : 220, context);
                analogClock.getLayoutParams().width = toPixel(isTablet(context) ? 300 : 220, context);
                digitalClock.setVisibility(View.GONE);
                analogClock.setVisibility(View.VISIBLE);
                return;
            }
            case DIGITAL -> {
                digitalClock.setVisibility(View.VISIBLE);
                analogClock.setVisibility(View.GONE);
                return;
            }
        }

        throw new IllegalStateException("unexpected clock style: " + clockStyle);
    }

    /**
     * For screensavers, dim the clock color.
     */
    public static void dimClockView(View clockView, Context context) {
        String colorFilter = getClockColorFilter();
        Paint paint = new Paint();

        paint.setColor(Color.WHITE);

        if (dynamicColors()) {
            final int brightnessPercentage = DataModel.getDataModel().getScreensaverBrightness();
            // The alpha channel should range from 16 (10 hex) to 192 (C0 hex).
            final int dynamicColorsBrightness = 16 + (192 * brightnessPercentage / 100);
            paint.setColorFilter(new PorterDuffColorFilter(
                    ColorUtils.setAlphaComponent(context.getColor(R.color.md_theme_inversePrimary),
                            dynamicColorsBrightness), PorterDuff.Mode.SRC_IN));

        } else {
            paint.setColorFilter(new PorterDuffColorFilter(Color.parseColor(colorFilter), PorterDuff.Mode.SRC_IN));
        }

        clockView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * For screensavers, dim the date color.
     */
    public static void dimDateView(TextView dateView, Context context) {
        String colorFilter = getDateColorFilter();
        Paint paint = new Paint();

        paint.setColor(Color.WHITE);

        if (dynamicColors()) {
            final int brightnessPercentage = DataModel.getDataModel().getScreensaverBrightness();
            // The alpha channel should range from 16 (10 hex) to 192 (C0 hex).
            final int dynamicColorsBrightness = 16 + (192 * brightnessPercentage / 100);
            paint.setColorFilter(new PorterDuffColorFilter(
                    ColorUtils.setAlphaComponent(context.getColor(R.color.md_theme_inversePrimary),
                            dynamicColorsBrightness), PorterDuff.Mode.SRC_IN));
        } else {
            paint.setColorFilter(new PorterDuffColorFilter(Color.parseColor(colorFilter), PorterDuff.Mode.SRC_IN));
        }

        dateView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * For screensavers, dim the next alarm color.
     */
    public static void dimNextAlarmView(TextView nextAlarmIcon, TextView nextAlarm, Context context) {
        String colorFilter = getNextAlarmColorFilter();
        Paint paint = new Paint();

        paint.setColor(Color.WHITE);

        if (dynamicColors()) {
            final int brightnessPercentage = DataModel.getDataModel().getScreensaverBrightness();
            // The alpha channel should range from 16 (10 hex) to 192 (C0 hex).
            final int dynamicColorsBrightness = 16 + (192 * brightnessPercentage / 100);
            paint.setColorFilter(new PorterDuffColorFilter(
                    ColorUtils.setAlphaComponent(context.getColor(R.color.md_theme_inversePrimary),
                            dynamicColorsBrightness), PorterDuff.Mode.SRC_IN));
        } else {
            paint.setColorFilter(new PorterDuffColorFilter(Color.parseColor(colorFilter), PorterDuff.Mode.SRC_IN));
        }

        if (nextAlarmIcon == null || nextAlarm == null) {
            return;
        }

        nextAlarmIcon.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
        nextAlarm.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    public static boolean dynamicColors() {
        return DataModel.getDataModel().getScreensaverClockDynamicColors();
    }

    /**
     * For screensavers, calculate the color filter to use to dim/color the clock.
     */
    public static String getClockColorFilter() {
        final int brightnessPercentage = DataModel.getDataModel().getScreensaverBrightness();
        String colorFilter = DataModel.getDataModel().getScreensaverClockPresetColors();
        // The alpha channel should range from 16 (10 hex) to 192 (C0 hex).
        String alpha = String.format("%02X", 16 + (192 * brightnessPercentage / 100));

        colorFilter = "#" + alpha + colorFilter;

        return colorFilter;
    }

    /**
     * For screensavers, calculate the color filter to use to dim/color the date.
     */
    public static String getDateColorFilter() {
        final int brightnessPercentage = DataModel.getDataModel().getScreensaverBrightness();
        String colorFilter = DataModel.getDataModel().getScreensaverDatePresetColors();
        // The alpha channel should range from 16 (10 hex) to 192 (C0 hex).
        String alpha = String.format("%02X", 16 + (192 * brightnessPercentage / 100));

        colorFilter = "#" + alpha + colorFilter;

        return colorFilter;
    }

    /**
     * For screensavers, calculate the color filter to use to dim/color the date.
     */
    public static String getNextAlarmColorFilter() {
        final int brightnessPercentage = DataModel.getDataModel().getScreensaverBrightness();
        String colorFilter = DataModel.getDataModel().getScreensaverNextAlarmPresetColors();
        // The alpha channel should range from 16 (10 hex) to 192 (C0 hex).
        String alpha = String.format("%02X", 16 + (192 * brightnessPercentage / 100));

        colorFilter = "#" + alpha + colorFilter;

        return colorFilter;
    }

    /**
     * For screensavers, configure the clock that is visible to display seconds. The clock that is not visible never
     * displays seconds to avoid it scheduling unnecessary ticking runnable.
     */
    public static void setScreensaverClockSecondsEnabled(TextClock digitalClock, AnalogClock analogClock) {
        final boolean displaySeconds = DataModel.getDataModel().getDisplayScreensaverClockSeconds();
        final DataModel.ClockStyle screensaverClockStyle = DataModel.getDataModel().getScreensaverClockStyle();
        switch (screensaverClockStyle) {
            case ANALOG -> {
                setScreensaverTimeFormat(digitalClock, false);
                analogClock.enableSeconds(displaySeconds);
                return;
            }
            case DIGITAL -> {
                analogClock.enableSeconds(false);
                setScreensaverTimeFormat(digitalClock, displaySeconds);
                return;
            }
        }

        throw new IllegalStateException("unexpected clock style: " + screensaverClockStyle);
    }

    /**
     * For screensavers, format the digital clock to be bold and/or italic or not.
     *
     * @param digitalClock TextClock to format
     */
    public static void setScreensaverTimeFormat(TextClock digitalClock, boolean includeSeconds) {
        final boolean boldText = DataModel.getDataModel().getScreensaverBoldDigitalClock();
        final boolean italicText = DataModel.getDataModel().getScreensaverItalicDigitalClock();

        if (digitalClock == null) {
            return;
        }

        digitalClock.setFormat12Hour(get12ModeFormat(digitalClock.getContext(), 0.4f, includeSeconds));
        digitalClock.setFormat24Hour(get24ModeFormat(digitalClock.getContext(), includeSeconds));

        if (boldText && italicText) {
            digitalClock.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        } else if (boldText) {
            digitalClock.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (italicText) {
            digitalClock.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        } else {
            digitalClock.setTypeface(Typeface.DEFAULT);
        }
    }

    /**
     * For screensavers, format the date and the next alarm to be bold and/or italic or not.
     *
     * @param date Date to format
     */
    public static void setScreensaverDateFormat(TextView date) {
        final boolean boldText = DataModel.getDataModel().getScreensaverBoldDate();
        final boolean italicText = DataModel.getDataModel().getScreensaverItalicDate();

        if (boldText && italicText) {
            date.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        } else if (boldText) {
            date.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (italicText) {
            date.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        } else {
            date.setTypeface(Typeface.DEFAULT);
        }
    }

    /**
     * For screensavers, format the date and the next alarm to be bold and/or italic or not.
     *
     * @param nextAlarm Next alarm to format
     */
    public static void setScreensaverNextAlarmFormat(TextView nextAlarm) {
        final boolean boldText = DataModel.getDataModel().getScreensaverBoldNextAlarm();
        final boolean italicText = DataModel.getDataModel().getScreensaverItalicNextAlarm();
        if (nextAlarm == null) {
            return;
        }
        if (boldText && italicText) {
            nextAlarm.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        } else if (boldText) {
            nextAlarm.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (italicText) {
            nextAlarm.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        } else {
            nextAlarm.setTypeface(Typeface.DEFAULT);
        }
    }

    /**
     * For screensavers, set the margins and the style of the clock.
     */
    public static void setScreenSaverMarginsAndClockStyle(final Context context, final View clock) {
        final View mainClockView = clock.findViewById(R.id.main_clock);

        // Margins
        final int mainClockMarginLeft = toPixel(isTablet(context) ? 20 : 16, context);
        final int mainClockMarginRight = toPixel(isTablet(context) ? 20 : 16, context);
        final int mainClockMarginTop = toPixel(isTablet(context)
                ? isLandscape(context) ? 32 : 48
                : isLandscape(context) ? 16 : 24, context);
        final int mainClockMarginBottom = toPixel(isTablet(context) ? 20 : 16, context);
        final ViewGroup.MarginLayoutParams paramsForMainClock = (ViewGroup.MarginLayoutParams) mainClockView.getLayoutParams();
        paramsForMainClock.setMargins(mainClockMarginLeft, mainClockMarginTop, mainClockMarginRight, mainClockMarginBottom);
        mainClockView.setLayoutParams(paramsForMainClock);

        final int digitalClockMarginBottom = toPixel(isTablet(context) ? -18 : -8, context);
        final ViewGroup.MarginLayoutParams paramsForDigitalClock = (ViewGroup.MarginLayoutParams) mainClockView.getLayoutParams();
        paramsForMainClock.setMargins(0, 0, 0, digitalClockMarginBottom);
        mainClockView.setLayoutParams(paramsForDigitalClock);

        final int analogClockMarginBottom = toPixel(isLandscape(context)
                ? 5
                : isTablet(context) ? 18 : 14, context);
        final ViewGroup.MarginLayoutParams paramsForAnalogClock = (ViewGroup.MarginLayoutParams) mainClockView.getLayoutParams();
        paramsForMainClock.setMargins(0, 0, 0, analogClockMarginBottom);
        mainClockView.setLayoutParams(paramsForAnalogClock);

        // Style
        final AnalogClock analogClock = mainClockView.findViewById(R.id.analog_clock);
        final DataModel.ClockStyle clockStyle = DataModel.getDataModel().getScreensaverClockStyle();
        final TextClock textClock = mainClockView.findViewById(R.id.digital_clock);
        final TextView date = mainClockView.findViewById(R.id.date);
        final TextView nextAlarmIcon = mainClockView.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarm = mainClockView.findViewById(R.id.nextAlarm);

        setScreensaverClockStyle(textClock, analogClock);
        dimClockView(clockStyle == DataModel.ClockStyle.ANALOG ? analogClock : textClock, context);
        dimDateView(date, context);
        dimNextAlarmView(nextAlarmIcon, nextAlarm, context);
        setScreensaverClockSecondsEnabled(textClock, analogClock);
        setScreensaverDateFormat(date);
        setClockIconTypeface(nextAlarmIcon);
        setScreensaverNextAlarmFormat(nextAlarm);
    }

    /**
     * @param amPmRatio      a value between 0 and 1 that is the ratio of the relative size of the
     *                       am/pm string to the time string
     * @param includeSeconds whether or not to include seconds in the time string
     * @return format string for 12 hours mode time, not including seconds
     */
    public static CharSequence get12ModeFormat(Context context, float amPmRatio, boolean includeSeconds) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(),
                includeSeconds ? "hmsa" : "hma");

        // Replace spaces with "Hair Space"
        pattern = pattern.replaceAll("\\s", "\u200A");

        if (amPmRatio <= 0) {
            pattern = pattern.replaceAll("a", "").trim();
        } else {
            if (context instanceof ScreensaverActivity || context instanceof Screensaver) {
                final boolean isItalic = DataModel.getDataModel().getScreensaverItalicDigitalClock();
                if (isItalic) {
                    // For screensaver, add a "Hair Space" (\u200A) at the end of the AM/PM to prevent
                    // its display from being cut off on some devices when in italic.
                    pattern = pattern.replaceAll("a", "a" + "\u200A");
                }
            }
        }

        // Build a spannable so that the am/pm will be formatted
        int amPmPos = pattern.indexOf('a');
        if (amPmPos == -1) {
            return pattern;
        }

        final Spannable sp = new SpannableString(pattern);
        sp.setSpan(new RelativeSizeSpan(amPmRatio), amPmPos, amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new StyleSpan(Typeface.NORMAL), amPmPos, amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new TypefaceSpan("sans-serif-bold"), amPmPos, amPmPos + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return sp;
    }

    public static CharSequence get24ModeFormat(Context context, boolean includeSeconds) {
        if (context instanceof ScreensaverActivity || context instanceof Screensaver) {
            final boolean isItalic = DataModel.getDataModel().getScreensaverItalicDigitalClock();
            if (isItalic) {
                // For screensaver, add a "Hair Space" (\u200A) at the end of the time to prevent
                // its display from being cut off on some devices when in italic.
                return DateFormat.getBestDateTimePattern(Locale.getDefault(), includeSeconds ? "Hms" : "Hm") + "\u2009";
            } else {
                return DateFormat.getBestDateTimePattern(Locale.getDefault(), includeSeconds ? "Hms" : "Hm");
            }
        } else {
            return DateFormat.getBestDateTimePattern(Locale.getDefault(), includeSeconds ? "Hms" : "Hm");
        }
    }

    /**
     * Returns string denoting the timezone hour offset (e.g. GMT -8:00)
     *
     * @param useShortForm Whether to return a short form of the header that rounds to the
     *                     nearest hour and excludes the "GMT" prefix
     */
    public static String getGMTHourOffset(TimeZone timezone, boolean useShortForm) {
        final int gmtOffset = timezone.getRawOffset();
        final long hour = gmtOffset / DateUtils.HOUR_IN_MILLIS;
        final long min = (Math.abs(gmtOffset) % DateUtils.HOUR_IN_MILLIS) / DateUtils.MINUTE_IN_MILLIS;

        if (useShortForm) {
            return String.format(Locale.ENGLISH, "%+d", hour);
        } else {
            return String.format(Locale.ENGLISH, "GMT %+d:%02d", hour, min);
        }
    }

    /**
     * Given a point in time, return the subsequent moment any of the time zones changes days.
     * e.g. Given 8:00pm on 1/1/2016 and time zones in LA and NY this method would return a Date for
     * midnight on 1/2/2016 in the NY timezone since it changes days first.
     *
     * @param time  a point in time from which to compute midnight on the subsequent day
     * @param zones a collection of time zones
     * @return the nearest point in the future at which any of the time zones changes days
     */
    public static Date getNextDay(Date time, Collection<TimeZone> zones) {
        Calendar next = null;
        for (TimeZone tz : zones) {
            final Calendar c = Calendar.getInstance(tz);
            c.setTime(time);

            // Advance to the next day.
            c.add(Calendar.DAY_OF_YEAR, 1);

            // Reset the time to midnight.
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            if (next == null || c.compareTo(next) < 0) {
                next = c;
            }
        }

        return next == null ? null : next.getTime();
    }

    public static String getNumberFormattedQuantityString(Context context, int id, int quantity) {
        final String localizedQuantity = NumberFormat.getInstance().format(quantity);
        return context.getResources().getQuantityString(id, quantity, localizedQuantity);
    }

    /**
     * @return {@code true} iff the widget is being hosted in a container where tapping is allowed
     */
    public static boolean isWidgetClickable(AppWidgetManager widgetManager, int widgetId) {
        final Bundle wo = widgetManager.getAppWidgetOptions(widgetId);
        return wo != null && wo.getInt(OPTION_APPWIDGET_HOST_CATEGORY, -1) != WIDGET_CATEGORY_KEYGUARD;
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
     * Convenience method for creating card background.
     */
    public static Drawable cardBackground (Context context) {
        final String getDarkMode = DataModel.getDataModel().getDarkMode();
        final int color;
        // Setting transparency is necessary to avoid flickering when expanding or collapsing alarms.
        // Todo: find a way to get rid of this transparency and use the real color R.color.md_theme_surface
        if (isNight(context.getResources()) && getDarkMode.equals(KEY_AMOLED_DARK_MODE)) {
            color = ColorUtils.setAlphaComponent(context.getColor(R.color.md_theme_inversePrimary), 90);
        } else {
            color = ColorUtils.setAlphaComponent(context.getColor(R.color.md_theme_primary), 20);
        }
        final int radius = toPixel(12, context);
        final GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setCornerRadius(radius);
        gradientDrawable.setColor(color);
        return gradientDrawable;
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
     * Convenience method for converting dp to pixel.
     */
    public static int toPixel(int dp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    /**
     * {@link ArraySet} is @hide prior to {@link Build.VERSION_CODES#M}.
     */
    public static <E> ArraySet<E> newArraySet(Collection<E> collection) {
        final ArraySet<E> arraySet = new ArraySet<>(collection.size());
        arraySet.addAll(collection);
        return arraySet;
    }

    /**
     * @param context from which to query the current device configuration
     * @return {@code true} if the device is currently in portrait or reverse portrait orientation
     */
    public static boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT;
    }

    /**
     * @param context from which to query the current device configuration
     * @return {@code true} if the device is currently in landscape or reverse landscape orientation
     */
    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE;
    }

    /**
     * @param context from which to query the current device
     * @return {@code true} if the device is a tablet
     */
    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.rotateAlarmAlert);
    }

    public static long now() {
        return DataModel.getDataModel().elapsedRealtime();
    }

    public static long wallClock() {
        return DataModel.getDataModel().currentTimeMillis();
    }

    /**
     * @param context          to obtain strings.
     * @param displayMinutes   whether or not minutes should be included
     * @param isAhead          {@code true} if the time should be marked 'ahead', else 'behind'
     * @param hoursDifferent   the number of hours the time is ahead/behind
     * @param minutesDifferent the number of minutes the time is ahead/behind
     * @return String describing the hours/minutes ahead or behind
     */
    public static String createHoursDifferentString(Context context, boolean displayMinutes,
                                                    boolean isAhead, int hoursDifferent, int minutesDifferent) {

        String timeString;
        if (displayMinutes && hoursDifferent != 0) {
            // Both minutes and hours
            final String hoursShortQuantityString = Utils.getNumberFormattedQuantityString(context, R.plurals.hours_short, Math.abs(hoursDifferent));
            final String minsShortQuantityString = Utils.getNumberFormattedQuantityString(context, R.plurals.minutes_short, Math.abs(minutesDifferent));
            final @StringRes int stringType = isAhead ? R.string.world_hours_minutes_ahead : R.string.world_hours_minutes_behind;
            timeString = context.getString(stringType, hoursShortQuantityString, minsShortQuantityString);
        } else {
            // Minutes alone or hours alone
            final String hoursQuantityString = Utils.getNumberFormattedQuantityString(context, R.plurals.hours, Math.abs(hoursDifferent));
            final String minutesQuantityString = Utils.getNumberFormattedQuantityString(context, R.plurals.minutes, Math.abs(minutesDifferent));
            final @StringRes int stringType = isAhead ? R.string.world_time_ahead : R.string.world_time_behind;
            timeString = context.getString(stringType, displayMinutes ? minutesQuantityString : hoursQuantityString);
        }
        return timeString;
    }

    /**
     * @param context The context from which to obtain strings
     * @param hours   Hours to display (if any)
     * @param minutes Minutes to display (if any)
     * @param seconds Seconds to display
     * @return Provided time formatted as a String
     */
    static String getTimeString(Context context, int hours, int minutes, int seconds) {
        if (hours != 0) {
            return context.getString(R.string.hours_minutes_seconds, hours, minutes, seconds);
        }
        if (minutes != 0) {
            return context.getString(R.string.minutes_seconds, minutes, seconds);
        }
        return context.getString(R.string.seconds, seconds);
    }

    /**
     * Set the vibration duration if the device is equipped with a vibrator.
     *
     * @param context to define whether the device is equipped with a vibrator.
     * @param milliseconds Hours to display (if any)
     */
    public static void vibrationTime(Context context, long milliseconds) {
        final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(milliseconds);
        }
    }

    /**
     * @return {@code true} if the device is in dark mode.
     * @param res Access application resources.
     */
    public static boolean isNight(final Resources res) {
        return (res.getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Apply the theme to the activities.
     */
    public static void applyTheme(final AppCompatActivity activity) {
        final String getTheme = DataModel.getDataModel().getTheme();
        final String getDarkMode = DataModel.getDataModel().getDarkMode();

        if (getDarkMode.equals(KEY_DEFAULT_DARK_MODE)) {
            switch (getTheme) {
                case SYSTEM_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                case LIGHT_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                case DARK_THEME ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        } else if (getDarkMode.equals(KEY_AMOLED_DARK_MODE)
                && !getTheme.equals(SYSTEM_THEME) || !getTheme.equals(LIGHT_THEME)) {
                activity.setTheme(R.style.AmoledTheme);
        }
    }

}
