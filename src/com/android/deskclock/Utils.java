/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.deskclock.provider.Alarm;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.provider.DaysOfWeek;
import com.android.deskclock.stopwatch.Stopwatches;
import com.android.deskclock.timer.Timers;
import com.android.deskclock.worldclock.CityObj;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class Utils {
    private final static String PARAM_LANGUAGE_CODE = "hl";

    /**
     * Help URL query parameter key for the app version.
     */
    private final static String PARAM_VERSION = "version";

    /**
     * Cached version code to prevent repeated calls to the package manager.
     */
    private static String sCachedVersionCode = null;

    // Single-char version of day name, e.g.: 'S', 'M', 'T', 'W', 'T', 'F', 'S'
    private static String[] sShortWeekdays = null;
    private static final String DATE_FORMAT_SHORT = "ccccc";

    // Long-version of day name, e.g.: 'Sunday', 'Monday', 'Tuesday', etc
    private static String[] sLongWeekdays = null;
    private static final String DATE_FORMAT_LONG = "EEEE";

    public static final int DEFAULT_WEEK_START = Calendar.getInstance().getFirstDayOfWeek();

    private static Locale sLocaleUsedForWeekdays;

    // Used to cache the current timer ringtone name.
    private static String sTimerRingtoneName;

    /** Types that may be used for clock displays. **/
    public static final String CLOCK_TYPE_DIGITAL = "digital";
    public static final String CLOCK_TYPE_ANALOG = "analog";

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

    // Path to timer_expire.ogg
    // In order to keep currently satisfied users happy, default to using
    // the current timer_expire.ogg ringtone. If the user opens the
    // timer ringtone preference, they'll be able to pick from
    // RingtoneManager's alarm selection. This also means that they will
    // never be able to get timer_expire back unless they clear their
    // SharedPreferences, because timer_expire is not one of
    // RingtoneManager's provided ringtones.
    private static String sDefaultTimerRingtone;

    /**
     * @return {@code true} if the device is prior to {@link Build.VERSION_CODES#LOLLIPOP}
     */
    public static boolean isPreL() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    }

    /**
     * @return {@code true} if the device is {@link Build.VERSION_CODES#LOLLIPOP} or
     *      {@link Build.VERSION_CODES#LOLLIPOP_MR1}
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

    public static void prepareHelpMenuItem(Context context, MenuItem helpMenuItem) {
        String helpUrlString = context.getResources().getString(R.string.desk_clock_help_url);
        if (TextUtils.isEmpty(helpUrlString)) {
            // The help url string is empty or null, so set the help menu item to be invisible.
            helpMenuItem.setVisible(false);
            return;
        }
        // The help url string exists, so first add in some extra query parameters.  87
        final Uri fullUri = uriWithAddedParameters(context, Uri.parse(helpUrlString));

        // Then, create an intent that will be fired when the user
        // selects this help menu item.
        Intent intent = new Intent(Intent.ACTION_VIEW, fullUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        // Set the intent to the help menu item, show the help menu item in the overflow
        // menu, and make it visible.
        helpMenuItem.setIntent(intent);
        helpMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        helpMenuItem.setVisible(true);
    }

    /**
     * Adds two query parameters into the Uri, namely the language code and the version code
     * of the application's package as gotten via the context.
     * @return the uri with added query parameters
     */
    private static Uri uriWithAddedParameters(Context context, Uri baseUri) {
        Uri.Builder builder = baseUri.buildUpon();

        // Add in the preferred language
        builder.appendQueryParameter(PARAM_LANGUAGE_CODE, Locale.getDefault().toString());

        // Add in the package version code
        if (sCachedVersionCode == null) {
            // There is no cached version code, so try to get it from the package manager.
            try {
                // cache the version code
                PackageInfo info = context.getPackageManager().getPackageInfo(
                        context.getPackageName(), 0);
                sCachedVersionCode = Integer.toString(info.versionCode);

                // append the version code to the uri
                builder.appendQueryParameter(PARAM_VERSION, sCachedVersionCode);
            } catch (NameNotFoundException e) {
                // Cannot find the package name, so don't add in the version parameter
                // This shouldn't happen.
                LogUtils.wtf("Invalid package name for context " + e);
            }
        } else {
            builder.appendQueryParameter(PARAM_VERSION, sCachedVersionCode);
        }

        // Build the full uri and return it
        return builder.build();
    }

    public static long getTimeNow() {
        return SystemClock.elapsedRealtime();
    }

    /**
     * Calculate the amount by which the radius of a CircleTimerView should be offset by the any
     * of the extra painted objects.
     */
    public static float calculateRadiusOffset(
            float strokeSize, float dotStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(dotStrokeSize, markerStrokeSize));
    }

    /**
     * Uses {@link Utils#calculateRadiusOffset(float, float, float)} after fetching the values
     * from the resources just as {@link CircleTimerView#init(android.content.Context)} does.
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
     * Clears the persistent data of stopwatch (start time, state, laps, etc...).
     */
    public static void clearSwSharedPref(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove (Stopwatches.PREF_START_TIME);
        editor.remove (Stopwatches.PREF_ACCUM_TIME);
        editor.remove (Stopwatches.PREF_STATE);
        int lapNum = prefs.getInt(Stopwatches.PREF_LAP_NUM, Stopwatches.STOPWATCH_RESET);
        for (int i = 0; i < lapNum; i++) {
            String key = Stopwatches.PREF_LAP_TIME + Integer.toString(i);
            editor.remove(key);
        }
        editor.remove(Stopwatches.PREF_LAP_NUM);
        editor.apply();
    }

    /**
     * Broadcast a message to show the in-use timers in the notifications
     */
    public static void showInUseNotifications(Context context) {
        Intent timerIntent = new Intent();
        timerIntent.setAction(Timers.NOTIF_IN_USE_SHOW);
        context.sendBroadcast(timerIntent);
    }

    /**
     * Broadcast a message to update the timer times-up HUN
     */
    public static void updateTimesUpNotification(Context context) {
        final Intent timerHUNIntent = new Intent();
        timerHUNIntent.setAction(Timers.NOTIF_UPDATE);
        context.sendBroadcast(timerHUNIntent);
    }

    /**
     * Broadcast a message to show the in-use timers in the notifications
     */
    public static void showTimesUpNotifications(Context context) {
        Intent timerIntent = new Intent();
        timerIntent.setAction(Timers.NOTIF_TIMES_UP_SHOW);
        context.sendBroadcast(timerIntent);
    }

    /**
     * Broadcast a message to cancel the in-use timers in the notifications
     */
    public static void cancelTimesUpNotifications(Context context) {
        Intent timerIntent = new Intent();
        timerIntent.setAction(Timers.NOTIF_TIMES_UP_CANCEL);
        context.sendBroadcast(timerIntent);
    }

    /**
     * @return {@code true} iff the user has granted permission to read the ringtone at the given
     *      uri or no permission is required to read the ringtone
     */
    public static boolean hasPermissionToDisplayRingtoneTitle(Context context, Uri ringtoneUri) {
        final PackageManager pm = context.getPackageManager();
        final String packageName = context.getPackageName();

        // If the default alarm alert ringtone URI is given, resolve it to the actual URI.
        if (Settings.System.DEFAULT_ALARM_ALERT_URI.equals(ringtoneUri)) {
            ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context,
                    RingtoneManager.TYPE_ALARM);
        }

        // If no ringtone is specified, return true.
        if (ringtoneUri == null || ringtoneUri == Alarm.NO_RINGTONE_URI) {
            return true;
        }

        // If the permission is already granted, return true.
        if (pm.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, packageName)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        // If the ringtone is internal, return true;
        // external ringtones require the permission to see their title
        return ringtoneUri.toString().startsWith("content://media/internal/");
    }

    /** Runnable for use with screensaver and dream, to move the clock every minute.
     *  registerViews() must be called prior to posting.
     */
    public static class ScreensaverMoveSaverRunnable implements Runnable {
        static final long MOVE_DELAY = 60000; // DeskClock.SCREEN_SAVER_MOVE_DELAY;
        static final long SLIDE_TIME = 10000;
        static final long FADE_TIME = 3000;

        static final boolean SLIDE = false;

        private View mContentView, mSaverView;
        private final Handler mHandler;

        private static TimeInterpolator mSlowStartWithBrakes;


        public ScreensaverMoveSaverRunnable(Handler handler) {
            mHandler = handler;
            mSlowStartWithBrakes = new TimeInterpolator() {
                @Override
                public float getInterpolation(float x) {
                    return (float)(Math.cos((Math.pow(x,3) + 1) * Math.PI) / 2.0f) + 0.5f;
                }
            };
        }

        public void registerViews(View contentView, View saverView) {
            mContentView = contentView;
            mSaverView = saverView;
        }

        @Override
        public void run() {
            long delay = MOVE_DELAY;
            if (mContentView == null || mSaverView == null) {
                mHandler.removeCallbacks(this);
                mHandler.postDelayed(this, delay);
                return;
            }

            final float xrange = mContentView.getWidth() - mSaverView.getWidth();
            final float yrange = mContentView.getHeight() - mSaverView.getHeight();

            if (xrange == 0 && yrange == 0) {
                delay = 500; // back in a split second
            } else {
                final int nextx = (int) (Math.random() * xrange);
                final int nexty = (int) (Math.random() * yrange);

                if (mSaverView.getAlpha() == 0f) {
                    // jump right there
                    mSaverView.setX(nextx);
                    mSaverView.setY(nexty);
                    ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, 1f)
                        .setDuration(FADE_TIME)
                        .start();
                } else {
                    AnimatorSet s = new AnimatorSet();
                    Animator xMove   = ObjectAnimator.ofFloat(mSaverView,
                                         "x", mSaverView.getX(), nextx);
                    Animator yMove   = ObjectAnimator.ofFloat(mSaverView,
                                         "y", mSaverView.getY(), nexty);

                    Animator xShrink = ObjectAnimator.ofFloat(mSaverView, "scaleX", 1f, 0.85f);
                    Animator xGrow   = ObjectAnimator.ofFloat(mSaverView, "scaleX", 0.85f, 1f);

                    Animator yShrink = ObjectAnimator.ofFloat(mSaverView, "scaleY", 1f, 0.85f);
                    Animator yGrow   = ObjectAnimator.ofFloat(mSaverView, "scaleY", 0.85f, 1f);
                    AnimatorSet shrink = new AnimatorSet(); shrink.play(xShrink).with(yShrink);
                    AnimatorSet grow = new AnimatorSet(); grow.play(xGrow).with(yGrow);

                    Animator fadeout = ObjectAnimator.ofFloat(mSaverView, "alpha", 1f, 0f);
                    Animator fadein = ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, 1f);


                    if (SLIDE) {
                        s.play(xMove).with(yMove);
                        s.setDuration(SLIDE_TIME);

                        s.play(shrink.setDuration(SLIDE_TIME/2));
                        s.play(grow.setDuration(SLIDE_TIME/2)).after(shrink);
                        s.setInterpolator(mSlowStartWithBrakes);
                    } else {
                        AccelerateInterpolator accel = new AccelerateInterpolator();
                        DecelerateInterpolator decel = new DecelerateInterpolator();

                        shrink.setDuration(FADE_TIME).setInterpolator(accel);
                        fadeout.setDuration(FADE_TIME).setInterpolator(accel);
                        grow.setDuration(FADE_TIME).setInterpolator(decel);
                        fadein.setDuration(FADE_TIME).setInterpolator(decel);
                        s.play(shrink);
                        s.play(fadeout);
                        s.play(xMove.setDuration(0)).after(FADE_TIME);
                        s.play(yMove.setDuration(0)).after(FADE_TIME);
                        s.play(fadein).after(FADE_TIME);
                        s.play(grow).after(FADE_TIME);
                    }
                    s.start();
                }

                long now = System.currentTimeMillis();
                long adjust = (now % 60000);
                delay = delay
                        + (MOVE_DELAY - adjust) // minute aligned
                        - (SLIDE ? 0 : FADE_TIME) // start moving before the fade
                        ;
            }

            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, delay);
        }
    }

    /** Setup to find out when the quarter-hour changes (e.g. Kathmandu is GMT+5:45) **/
    public static long getAlarmOnQuarterHour() {
        final Calendar calendarInstance = Calendar.getInstance();
        final long now = System.currentTimeMillis();
        return getAlarmOnQuarterHour(calendarInstance, now);
    }

    static long getAlarmOnQuarterHour(Calendar calendar, long now) {
        //  Set 1 second to ensure quarter-hour threshold passed.
        calendar.set(Calendar.SECOND, 1);
        calendar.set(Calendar.MILLISECOND, 0);
        int minute = calendar.get(Calendar.MINUTE);
        calendar.add(Calendar.MINUTE, 15 - (minute % 15));
        long alarmOnQuarterHour = calendar.getTimeInMillis();

        // Verify that alarmOnQuarterHour is within the next 15 minutes
        long delta = alarmOnQuarterHour - now;
        if (0 >= delta || delta > 901000) {
            // Something went wrong in the calculation, schedule something that is
            // about 15 minutes. Next time , it will align with the 15 minutes border.
            alarmOnQuarterHour = now + 901000;
        }
        return alarmOnQuarterHour;
    }

    // Setup a thread that starts at midnight plus one second. The extra second is added to ensure
    // the date has changed.
    public static void setMidnightUpdater(Handler handler, Runnable runnable) {
        String timezone = TimeZone.getDefault().getID();
        if (handler == null || runnable == null || timezone == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Time time = new Time(timezone);
        time.set(now);
        long runInMillis = ((24 - time.hour) * 3600 - time.minute * 60 - time.second + 1) * 1000;
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, runInMillis);
    }

    // Stop the midnight update thread
    public static void cancelMidnightUpdater(Handler handler, Runnable runnable) {
        if (handler == null || runnable == null) {
            return;
        }
        handler.removeCallbacks(runnable);
    }

    // Setup a thread that starts at the quarter-hour plus one second. The extra second is added to
    // ensure dates have changed.
    public static void setQuarterHourUpdater(Handler handler, Runnable runnable) {
        String timezone = TimeZone.getDefault().getID();
        if (handler == null || runnable == null || timezone == null) {
            return;
        }
        long runInMillis = getAlarmOnQuarterHour() - System.currentTimeMillis();
        // Ensure the delay is at least one second.
        if (runInMillis < 1000) {
            runInMillis = 1000;
        }
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, runInMillis);
    }

    // Stop the quarter-hour update thread
    public static void cancelQuarterHourUpdater(Handler handler, Runnable runnable) {
        if (handler == null || runnable == null) {
            return;
        }
        handler.removeCallbacks(runnable);
    }

    /**
     * For screensavers to set whether the digital or analog clock should be displayed.
     * Returns the view to be displayed.
     */
    public static View setClockStyle(Context context, View digitalClock, View analogClock,
            String clockStyleKey) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultClockStyle = context.getResources().getString(R.string.default_clock_style);
        String style = sharedPref.getString(clockStyleKey, defaultClockStyle);
        View returnView;
        if (style.equals(CLOCK_TYPE_ANALOG)) {
            digitalClock.setVisibility(View.GONE);
            analogClock.setVisibility(View.VISIBLE);
            returnView = analogClock;
        } else {
            digitalClock.setVisibility(View.VISIBLE);
            analogClock.setVisibility(View.GONE);
            returnView = digitalClock;
        }

        return returnView;
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

    /** Clock views can call this to refresh their alarm to the next upcoming value. */
    public static void refreshAlarm(Context context, View clock) {
        final TextView nextAlarmView = (TextView) clock.findViewById(R.id.nextAlarm);
        if (nextAlarmView == null) {
            return;
        }

        final String alarm = getNextAlarm(context);
        if (!TextUtils.isEmpty(alarm)) {
            final String text = context.getString(R.string.control_set_alarm_with_existing, alarm);
            final String description = context.getString(R.string.next_alarm_description, alarm);
            nextAlarmView.setText(text);
            nextAlarmView.setContentDescription(description);
            nextAlarmView.setVisibility(View.VISIBLE);
        } else {
            nextAlarmView.setVisibility(View.GONE);
        }
    }

    /** Clock views can call this to refresh their date. **/
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
     * @param context - Context used to get user's locale and time preferences
     * @param clock - TextClock to format
     * @param amPmFontSize - size of the am/pm label since it is usually smaller
     */
    public static void setTimeFormat(Context context, TextClock clock, int amPmFontSize) {
        if (clock != null) {
            // Get the best format for 12 hours mode according to the locale
            clock.setFormat12Hour(get12ModeFormat(context, amPmFontSize));
            // Get the best format for 24 hours mode according to the locale
            clock.setFormat24Hour(get24ModeFormat());
        }
    }
    /***
     * @param context - context used to get time format string resource
     * @param amPmFontSize - size of am/pm label (label removed is size is 0).
     * @return format string for 12 hours mode time
     */
    public static CharSequence get12ModeFormat(Context context, int amPmFontSize) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hma");

        // Remove the am/pm
        if (amPmFontSize <= 0) {
            pattern = pattern.replaceAll("a", "").trim();
        }
        // Replace spaces with "Hair Space"
        pattern = pattern.replaceAll(" ", "\u200A");
        // Build a spannable so that the am/pm will be formatted
        int amPmPos = pattern.indexOf('a');
        if (amPmPos == -1) {
            return pattern;
        }
        Spannable sp = new SpannableString(pattern);
        sp.setSpan(new StyleSpan(Typeface.NORMAL), amPmPos, amPmPos + 1,
                Spannable.SPAN_POINT_MARK);
        sp.setSpan(new AbsoluteSizeSpan(amPmFontSize), amPmPos, amPmPos + 1,
                Spannable.SPAN_POINT_MARK);
        sp.setSpan(new TypefaceSpan("sans-serif"), amPmPos, amPmPos + 1,
                Spannable.SPAN_POINT_MARK);
        return sp;
    }

    public static CharSequence get24ModeFormat() {
        return DateFormat.getBestDateTimePattern(Locale.getDefault(), "Hm");
    }

    public static CityObj[] loadCitiesFromXml(Context c) {
        Resources r = c.getResources();
        // Read strings array of [index, name, phonetic name, timezone, id]
        // make sure the list are the same length
        String[] cityNames = r.getStringArray(R.array.cities_names);
        String[] timezones = r.getStringArray(R.array.cities_tz);
        String[] ids = r.getStringArray(R.array.cities_id);
        int minLength = cityNames.length;
        if (cityNames.length != timezones.length || ids.length != cityNames.length) {
            minLength = Math.min(cityNames.length, Math.min(timezones.length, ids.length));
            LogUtils.e("City lists sizes are not the same, truncating");
        }
        CityObj[] cities = new CityObj[minLength];
        for (int i = 0; i < cities.length; i++) {
            // Default to using the first character of the city name as the index unless one is
            // specified. The indicator for a specified index is the addition of character(s)
            // before the "=" separator.
            String parseString = cityNames[i];
            final int separatorIndex = parseString.indexOf("=");
            final String index;
            if (parseString.length() <= 1 && separatorIndex >= 0) {
                LogUtils.w("Cannot parse city name %s; skipping", parseString);
                continue;
            }
            // Parse Index.
            if (separatorIndex == 0) {
                // Default to using second character (the first character after the = separator)
                // as the index.
                index = parseString.substring(1, 2);
                parseString = parseString.substring(1);
            } else if (separatorIndex == -1) {
                // Default to using the first character as the index
                index = parseString.substring(0, 1);
                LogUtils.e("Missing expected separator character =");
            } else {
                 index = parseString.substring(0, separatorIndex);
                 parseString = parseString.substring(separatorIndex + 1);
            }
            // Separate city name and phonetic name.
            final String[] names = parseString.split(":");
            if (names.length == 2) {
                cities[i] = new CityObj(names[0], names[1], timezones[i], ids[i], index);
            } else {
                cities[i] = new CityObj(names[0], null, timezones[i], ids[i], index);
            }
        }
        return cities;
    }

    /**
     * Returns a map of cities where the key is lowercase
     */
    public static Map<String, CityObj> loadCityMapFromXml(Context c) {
        CityObj[] cities = loadCitiesFromXml(c);

        final Map<String, CityObj> map = new HashMap<>(cities.length);
        for (CityObj city : cities) {
            map.put(city.mCityName.toLowerCase(), city);
        }
        return map;
    }

    /**
     * Returns string denoting the timezone hour offset (e.g. GMT -8:00)
     * @param useShortForm Whether to return a short form of the header that rounds to the
     *                     nearest hour and excludes the "GMT" prefix
     */
    public static String getGMTHourOffset(TimeZone timezone, boolean useShortForm) {
        final int gmtOffset = timezone.getRawOffset();
        final long hour = gmtOffset / DateUtils.HOUR_IN_MILLIS;
        final long min = (Math.abs(gmtOffset) % DateUtils.HOUR_IN_MILLIS) /
                DateUtils.MINUTE_IN_MILLIS;

        if (useShortForm) {
            return String.format("%+d", hour);
        } else {
            return String.format("GMT %+d:%02d", hour, min);
        }
    }

    public static String getCityName(CityObj city, CityObj dbCity) {
        return (city.mCityId == null || dbCity == null) ? city.mCityName : dbCity.mCityName;
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
        return Integer.parseInt(PreferenceManager
                .getDefaultSharedPreferences(context)
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
        if (sShortWeekdays == null) {
            sShortWeekdays = new String[DaysOfWeek.DAYS_IN_A_WEEK];
        }
        if (sLongWeekdays == null) {
            sLongWeekdays = new String[DaysOfWeek.DAYS_IN_A_WEEK];
        }

        final SimpleDateFormat shortFormat = new SimpleDateFormat(DATE_FORMAT_SHORT);
        final SimpleDateFormat longFormat = new SimpleDateFormat(DATE_FORMAT_LONG);

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

    /**
     * @param context
     * @param id Resource id of the plural
     * @param quantity integer value
     * @return string with properly localized numbers
     */
    public static String getNumberFormattedQuantityString(Context context, int id, int quantity) {
        final String localizedQuantity = NumberFormat.getInstance().format(quantity);
        return context.getResources().getQuantityString(id, quantity, localizedQuantity);
    }

    public static Uri getTimerRingtoneUri(Context context) {
        // Need package name to initialize this, but only need to initialize once.
        if (sDefaultTimerRingtone == null) {
            sDefaultTimerRingtone =
                    "android.resource://" + context.getPackageName() + "/" + R.raw.timer_expire;
        }
        return Uri.parse(PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsActivity.KEY_TIMER_RINGTONE, sDefaultTimerRingtone));
    }

    // Update the cached timer ringtone.
    public static void setTimerRingtoneName(Context context, Uri ringtoneUri) {
        // If the user cannot read the ringtone file, insert our own name rather than the
        // ugly one returned by Ringtone.getTitle().
        if (!Utils.hasPermissionToDisplayRingtoneTitle(context, ringtoneUri)) {
            sTimerRingtoneName = context.getString(R.string.custom_ringtone);
        } else {
            // This is slow because a media player is created during Ringtone object creation.
            final Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
            if (ringtone == null) {
                LogUtils.i("No timer ringtone for uri %s", ringtoneUri);
            } else {
                sTimerRingtoneName = ringtone.getTitle(context);
            }
        }
    }

    // Return the cached ringtone name value.
    public static String getTimerRingtoneName(Context context) {
        return sTimerRingtoneName == null
                ? context.getString(R.string.custom_ringtone)
                : sTimerRingtoneName;
    }

    public static void setTimezoneLocale(Context context, Locale locale) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(SettingsActivity.TIMEZONE_LOCALE, locale.toString()).apply();
    }

    public static Locale getTimezoneLocale(Context context) {
       final String localeString = PreferenceManager.getDefaultSharedPreferences(context)
               .getString(SettingsActivity.TIMEZONE_LOCALE,
                       context.getResources().getConfiguration().locale.toString());
       return new Locale(localeString);
    }
}
