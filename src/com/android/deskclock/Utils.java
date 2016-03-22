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

package com.android.deskclock;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.os.BuildCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.ArraySet;
import android.view.View;
import android.widget.AbsListView;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.settings.SettingsActivity;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static android.appwidget.AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY;
import static android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
import static android.graphics.Bitmap.Config.ARGB_8888;

public class Utils {

    /**
     * {@link Uri} signifying the "silent" ringtone.
     */
    public static final Uri RINGTONE_SILENT = Uri.EMPTY;

    // Single-char version of day name, e.g.: 'S', 'M', 'T', 'W', 'T', 'F', 'S'
    private static String[] sShortWeekdays = null;
    private static final String DATE_FORMAT_SHORT = "ccccc";

    // Long-version of day name, e.g.: 'Sunday', 'Monday', 'Tuesday', etc
    private static String[] sLongWeekdays = null;
    private static final String DATE_FORMAT_LONG = "EEEE";

    public static final int DEFAULT_WEEK_START = Calendar.getInstance().getFirstDayOfWeek();

    private static Locale sLocaleUsedForWeekdays;

    /**
     * Temporary array used by {@link #obtainStyledColor(Context, int, int)}.
     */
    private static final int[] TEMP_ARRAY = new int[1];

    /**
     * The background colors of the app - it changes throughout out the day to mimic the sky.
     */
    private static final int[] BACKGROUND_SPECTRUM = {
            0xFF212121 /* 12 AM */,
            0xFF20222A /*  1 AM */,
            0xFF202233 /*  2 AM */,
            0xFF1F2242 /*  3 AM */,
            0xFF1E224F /*  4 AM */,
            0xFF1D225C /*  5 AM */,
            0xFF1B236B /*  6 AM */,
            0xFF1A237E /*  7 AM */,
            0xFF1D2783 /*  8 AM */,
            0xFF232E8B /*  9 AM */,
            0xFF283593 /* 10 AM */,
            0xFF2C3998 /* 11 AM */,
            0xFF303F9F /* 12 PM */,
            0xFF2C3998 /*  1 PM */,
            0xFF283593 /*  2 PM */,
            0xFF232E8B /*  3 PM */,
            0xFF1D2783 /*  4 PM */,
            0xFF1A237E /*  5 PM */,
            0xFF1B236B /*  6 PM */,
            0xFF1D225C /*  7 PM */,
            0xFF1E224F /*  8 PM */,
            0xFF1F2242 /*  9 PM */,
            0xFF202233 /* 10 PM */,
            0xFF20222A /* 11 PM */
    };

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
     * @return {@code true} if the device is prior to {@link Build.VERSION_CODES#LOLLIPOP}
     */
    public static boolean isPreL() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#LOLLIPOP} or
     * {@link Build.VERSION_CODES#LOLLIPOP_MR1}
     */
    public static boolean isLOrLMR1() {
        final int sdkInt = Build.VERSION.SDK_INT;
        return sdkInt == Build.VERSION_CODES.LOLLIPOP || sdkInt == Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#LOLLIPOP} or later
     */
    public static boolean isLOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#LOLLIPOP_MR1} or later
     */
    public static boolean isLMR1OrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#M} or later
     */
    public static boolean isMOrLater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
    * @return {@code true} if the device is {@link Build.VERSION_CODES#N} or later
    */
    public static boolean isNOrLater() {
       return BuildCompat.isAtLeastN();
    }

    /**
     * @param listView the scrollable list view to test
     * @return {@code true} iff the {@code listView} content is currently scrolled to the top
     */
    public static boolean isScrolledToTop(AbsListView listView) {
        return listView.getChildCount() == 0 || listView.getChildAt(0).getTop() == 0;
    }

    /**
     * Note: the {@code recyclerView} must use a {@link LinearLayoutManager} or this method throws
     * runtime exceptions.
     *
     * @param recyclerView the scrollable recycler view with a linear layout to test
     * @return {@code true} iff the {@code recyclerView} content is currently scrolled to the top
     */
    public static boolean isScrolledToTop(RecyclerView recyclerView) {
        if (recyclerView.getAdapter().getItemCount() == 0) {
            return true;
        } else {
            final LinearLayoutManager llm = (LinearLayoutManager) recyclerView.getLayoutManager();
            final int topVisibleItemPosition = llm.findFirstVisibleItemPosition();
            return topVisibleItemPosition == 0
                    && llm.findViewByPosition(topVisibleItemPosition).getTop() == 0;
        }
    }

    /**
     * Calculate the amount by which the radius of a CircleTimerView should be offset by any
     * of the extra painted objects.
     */
    public static float calculateRadiusOffset(
            float strokeSize, float dotStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(dotStrokeSize, markerStrokeSize));
    }

    /**
     * Uses {@link Utils#calculateRadiusOffset(float, float, float)} after fetching the values
     * from the resources.
     */
    public static float calculateRadiusOffset(Resources resources) {
        if (resources != null) {
            float strokeSize = resources.getDimension(R.dimen.circletimer_circle_size);
            float dotStrokeSize = resources.getDimension(R.dimen.circletimer_dot_size);
            float markerStrokeSize = resources.getDimension(R.dimen.circletimer_marker_size);
            return calculateRadiusOffset(strokeSize, dotStrokeSize, markerStrokeSize);
        } else {
            return 0f;
        }
    }

    /**
     * For screensavers to set whether the digital or analog clock should be displayed.
     * Returns the view to be displayed.
     */
    public static View setClockStyle(View digitalClock, View analogClock) {
        final DataModel.ClockStyle clockStyle = DataModel.getDataModel().getClockStyle();
        switch (clockStyle) {
            case ANALOG:
                digitalClock.setVisibility(View.GONE);
                analogClock.setVisibility(View.VISIBLE);
                return analogClock;
            case DIGITAL:
                digitalClock.setVisibility(View.VISIBLE);
                analogClock.setVisibility(View.GONE);
                return digitalClock;
        }

        throw new IllegalStateException("unexpected clock style: " + clockStyle);
    }

    /**
     * For screensavers to set whether the digital or analog clock should be displayed.
     * Returns the view to be displayed.
     */
    public static View setScreensaverClockStyle(View digitalClock, View analogClock) {
        final DataModel.ClockStyle clockStyle = DataModel.getDataModel().getScreensaverClockStyle();
        switch (clockStyle) {
            case ANALOG:
                digitalClock.setVisibility(View.GONE);
                analogClock.setVisibility(View.VISIBLE);
                return analogClock;
            case DIGITAL:
                digitalClock.setVisibility(View.VISIBLE);
                analogClock.setVisibility(View.GONE);
                return digitalClock;
        }

        throw new IllegalStateException("unexpected clock style: " + clockStyle);
    }

    /**
     * For screensavers to dim the lights if necessary.
     */
    public static void dimClockView(boolean dim, View clockView) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setColorFilter(new PorterDuffColorFilter(
                (dim ? 0x40FFFFFF : 0xC0FFFFFF),
                PorterDuff.Mode.MULTIPLY));
        clockView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /**
     * @return The next alarm from {@link AlarmManager}
     */
    public static String getNextAlarm(Context context) {
        return isPreL() ? getNextAlarmPreL(context) : getNextAlarmLOrLater(context);
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getNextAlarmPreL(Context context) {
        final ContentResolver cr = context.getContentResolver();
        return Settings.System.getString(cr, Settings.System.NEXT_ALARM_FORMATTED);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getNextAlarmLOrLater(Context context) {
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final AlarmManager.AlarmClockInfo info = am.getNextAlarmClock();
        if (info != null) {
            final long triggerTime = info.getTriggerTime();
            final Calendar alarmTime = Calendar.getInstance();
            alarmTime.setTimeInMillis(triggerTime);
            return AlarmUtils.getFormattedTime(context, alarmTime);
        }

        return null;
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
        final View nextAlarmIconView = clock.findViewById(R.id.nextAlarmIcon);
        final TextView nextAlarmView = (TextView) clock.findViewById(R.id.nextAlarm);
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

    /**
     * Clock views can call this to refresh their date.
     **/
    public static void updateDate(String dateSkeleton, String descriptionSkeleton, View clock) {
        final TextView dateDisplay = (TextView) clock.findViewById(R.id.date);
        if (dateDisplay == null) {
            return;
        }

        final Locale l = Locale.getDefault();
        final String datePattern = DateFormat.getBestDateTimePattern(l, dateSkeleton);
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
     * @param clock   - TextClock to format
     */
    public static void setTimeFormat(TextClock clock) {
        if (clock != null) {
            // Get the best format for 12 hours mode according to the locale
            clock.setFormat12Hour(get12ModeFormat(0.4f /* amPmRatio */));
            // Get the best format for 24 hours mode according to the locale
            clock.setFormat24Hour(get24ModeFormat());
        }
    }

    /**
     * @param amPmRatio a value between 0 and 1 that is the ratio of the relative size of the
     *                  am/pm string to the time string
     * @return format string for 12 hours mode time
     */
    public static CharSequence get12ModeFormat(float amPmRatio) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hma");
        if (amPmRatio <= 0) {
            pattern = pattern.replaceAll("a", "").trim();
        }

        // Replace spaces with "Hair Space"
        pattern = pattern.replaceAll(" ", "\u200A");
        // Build a spannable so that the am/pm will be formatted
        int amPmPos = pattern.indexOf('a');
        if (amPmPos == -1) {
            return pattern;
        }

        final Spannable sp = new SpannableString(pattern);
        sp.setSpan(new RelativeSizeSpan(amPmRatio), amPmPos, amPmPos + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new StyleSpan(Typeface.NORMAL), amPmPos, amPmPos + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new TypefaceSpan("sans-serif"), amPmPos, amPmPos + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return sp;
    }

    public static CharSequence get24ModeFormat() {
        return DateFormat.getBestDateTimePattern(Locale.getDefault(), "Hm");
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
        final long min = (Math.abs(gmtOffset) % DateUtils.HOUR_IN_MILLIS) /
                DateUtils.MINUTE_IN_MILLIS;

        if (useShortForm) {
            return String.format(Locale.ENGLISH, "%+d", hour);
        } else {
            return String.format(Locale.ENGLISH, "GMT %+d:%02d", hour, min);
        }
    }

    /**
     * Convenience method for retrieving a themed color value.
     *
     * @param context  the {@link Context} to resolve the theme attribute against
     * @param attr     the attribute corresponding to the color to resolve
     * @param defValue the default color value to use if the attribute cannot be resolved
     * @return the color value of the resolve attribute
     */
    public static int obtainStyledColor(Context context, int attr, int defValue) {
        TEMP_ARRAY[0] = attr;
        final TypedArray a = context.obtainStyledAttributes(TEMP_ARRAY);
        try {
            return a.getColor(0, defValue);
        } finally {
            a.recycle();
        }
    }

    /**
     * Returns the background color to use based on the current time.
     */
    public static int getCurrentHourColor() {
        return BACKGROUND_SPECTRUM[Calendar.getInstance().get(Calendar.HOUR_OF_DAY)];
    }

    /**
     * @param firstDay is the result from getZeroIndexedFirstDayOfWeek
     * @return Single-char version of day name, e.g.: 'S', 'M', 'T', 'W', 'T', 'F', 'S'
     */
    public static String getShortWeekday(int position, int firstDay) {
        generateShortAndLongWeekdaysIfNeeded();
        return sShortWeekdays[(position + firstDay) % DaysOfWeek.DAYS_IN_A_WEEK];
    }

    /**
     * @param firstDay is the result from getZeroIndexedFirstDayOfWeek
     * @return Long-version of day name, e.g.: 'Sunday', 'Monday', 'Tuesday', etc
     */
    public static String getLongWeekday(int position, int firstDay) {
        generateShortAndLongWeekdaysIfNeeded();
        return sLongWeekdays[(position + firstDay) % DaysOfWeek.DAYS_IN_A_WEEK];
    }

    // Return the first day of the week value corresponding to Calendar.<WEEKDAY> value, which is
    // 1-indexed starting with Sunday.
    public static int getFirstDayOfWeek(Context context) {
        return Integer.parseInt(getDefaultSharedPreferences(context)
                .getString(SettingsActivity.KEY_WEEK_START, String.valueOf(DEFAULT_WEEK_START)));
    }

    // Return the first day of the week value corresponding to a week with Sunday at 0 index.
    public static int getZeroIndexedFirstDayOfWeek(Context context) {
        return getFirstDayOfWeek(context) - 1;
    }

    private static boolean localeHasChanged() {
        return sLocaleUsedForWeekdays != Locale.getDefault();
    }

    /**
     * Generate arrays of short and long weekdays, starting from Sunday
     */
    private static void generateShortAndLongWeekdaysIfNeeded() {
        if (sShortWeekdays != null && sLongWeekdays != null && !localeHasChanged()) {
            // nothing to do
            return;
        }

        final Locale locale = Locale.getDefault();
        final SimpleDateFormat shortFormat = new SimpleDateFormat(DATE_FORMAT_SHORT, locale);
        final SimpleDateFormat longFormat = new SimpleDateFormat(DATE_FORMAT_LONG, locale);

        sShortWeekdays = new String[DaysOfWeek.DAYS_IN_A_WEEK];
        sLongWeekdays = new String[DaysOfWeek.DAYS_IN_A_WEEK];

        // Create a date (2014/07/20) that is a Sunday
        final long aSunday = new GregorianCalendar(2014, Calendar.JULY, 20).getTimeInMillis();
        for (int i = 0; i < DaysOfWeek.DAYS_IN_A_WEEK; i++) {
            final long dayMillis = aSunday + i * DateUtils.DAY_IN_MILLIS;
            sShortWeekdays[i] = shortFormat.format(new Date(dayMillis));
            sLongWeekdays[i] = longFormat.format(new Date(dayMillis));
        }

        // Track the Locale used to generate these weekdays
        sLocaleUsedForWeekdays = Locale.getDefault();
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
        return wo != null
                && wo.getInt(OPTION_APPWIDGET_HOST_CATEGORY, -1) != WIDGET_CATEGORY_KEYGUARD;
    }

    /**
     * @return a vector-drawable inflated from the given {@code resId}
     */
    public static VectorDrawableCompat getVectorDrawable(Context context, @DrawableRes int resId) {
        return VectorDrawableCompat.create(context.getResources(), resId, context.getTheme());
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
     * {@link ArraySet} is @hide prior to {@link Build.VERSION_CODES#M}.
     */
    @SuppressLint("NewApi")
    public static <E> ArraySet<E> newArraySet(Collection<E> collection) {
        final ArraySet<E> arraySet = new ArraySet<>(collection.size());
        arraySet.addAll(collection);
        return arraySet;
    }

    /**
     * Returns the default {@link SharedPreferences} instance from the underlying storage context.
     */
    @TargetApi(Build.VERSION_CODES.N)
    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        final Context storageContext;
        if (isNOrLater()) {
            // All N devices have split storage areas, but we may need to
            // migrate existing preferences into the new device encrypted
            // storage area, which is where our data lives from now on.
            storageContext = context.createDeviceProtectedStorageContext();
            if (!storageContext.moveSharedPreferencesFrom(context,
                    PreferenceManager.getDefaultSharedPreferencesName(context))) {
                LogUtils.wtf("Failed to migrate shared preferences");
            }
        } else {
            storageContext = context;
        }

        return PreferenceManager.getDefaultSharedPreferences(storageContext);
    }
}
