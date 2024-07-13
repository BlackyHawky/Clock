/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.data;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

import static com.best.deskclock.data.DataModel.AlarmVolumeButtonBehavior.DISMISS;
import static com.best.deskclock.data.DataModel.AlarmVolumeButtonBehavior.NOTHING;
import static com.best.deskclock.data.DataModel.AlarmVolumeButtonBehavior.SNOOZE;
import static com.best.deskclock.data.Weekdays.Order.MON_TO_SUN;
import static com.best.deskclock.data.Weekdays.Order.SAT_TO_FRI;
import static com.best.deskclock.data.Weekdays.Order.SUN_TO_SAT;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_DEFAULT_ALARM_RINGTONE;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_MATERIAL_TIME_PICKER_STYLE;
import static com.best.deskclock.settings.AlarmSettingsActivity.KEY_SWIPE_ACTION;
import static com.best.deskclock.settings.AlarmSettingsActivity.MATERIAL_TIME_PICKER_ANALOG_STYLE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.DEFAULT_ACCENT_COLOR;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.KEY_DEFAULT_DARK_MODE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.SYSTEM_THEME;

import static java.util.Calendar.MONDAY;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel.AlarmVolumeButtonBehavior;
import com.best.deskclock.data.DataModel.CitySort;
import com.best.deskclock.data.DataModel.ClockStyle;
import com.best.deskclock.settings.AlarmSettingsActivity;
import com.best.deskclock.settings.ClockSettingsActivity;
import com.best.deskclock.settings.InterfaceCustomizationActivity;
import com.best.deskclock.settings.ScreensaverSettingsActivity;
import com.best.deskclock.settings.TimerSettingsActivity;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This class encapsulates the storage of application preferences in {@link SharedPreferences}.
 */
final class SettingsDAO {

    /**
     * Key to a preference that stores the preferred sort order of world cities.
     */
    private static final String KEY_SORT_PREFERENCE = "sort_preference";

    /**
     * Key to a preference that stores the ringtone of an existing alarm.
     */
    private static final String KEY_SELECTED_ALARM_RINGTONE_URI = "selected_alarm_ringtone_uri";

    /**
     * Key to a preference that stores the global broadcast id.
     */
    private static final String KEY_ALARM_GLOBAL_ID = "intent.extra.alarm.global.id";

    /**
     * Key to a preference that indicates whether restore (of backup and restore) has completed.
     */
    private static final String KEY_RESTORE_BACKUP_FINISHED = "restore_finished";

    private SettingsDAO() {
    }

    /**
     * @return the id used to discriminate relevant AlarmManager callbacks from defunct ones
     */
    static int getGlobalIntentId(SharedPreferences prefs) {
        return prefs.getInt(KEY_ALARM_GLOBAL_ID, -1);
    }

    /**
     * Update the id used to discriminate relevant AlarmManager callbacks from defunct ones
     */
    static void updateGlobalIntentId(SharedPreferences prefs) {
        final int globalId = prefs.getInt(KEY_ALARM_GLOBAL_ID, -1) + 1;
        prefs.edit().putInt(KEY_ALARM_GLOBAL_ID, globalId).apply();
    }

    /**
     * @return an enumerated value indicating the order in which cities are ordered
     */
    static CitySort getCitySort(SharedPreferences prefs) {
        final int defaultSortOrdinal = CitySort.NAME.ordinal();
        final int citySortOrdinal = prefs.getInt(KEY_SORT_PREFERENCE, defaultSortOrdinal);
        return CitySort.values()[citySortOrdinal];
    }

    /**
     * Adjust the sort order of cities.
     */
    static void toggleCitySort(SharedPreferences prefs) {
        final CitySort oldSort = getCitySort(prefs);
        final CitySort newSort = oldSort == CitySort.NAME ? CitySort.UTC_OFFSET : CitySort.NAME;
        prefs.edit().putInt(KEY_SORT_PREFERENCE, newSort.ordinal()).apply();
    }

    /**
     * @return {@code true} if a clock for the user's home timezone should be automatically
     * displayed when it doesn't match the current timezone
     */
    static boolean getAutoShowHomeClock(SharedPreferences prefs) {
        return prefs.getBoolean(ClockSettingsActivity.KEY_AUTO_HOME_CLOCK, true);
    }

    /**
     * @return the user's home timezone
     */
    static TimeZone getHomeTimeZone(Context context, SharedPreferences prefs, TimeZone defaultTZ) {
        String timeZoneId = prefs.getString(ClockSettingsActivity.KEY_HOME_TIME_ZONE, null);

        // If the recorded home timezone is legal, use it.
        final TimeZones timeZones = getTimeZones(context, System.currentTimeMillis());
        if (timeZones.contains(timeZoneId)) {
            return TimeZone.getTimeZone(timeZoneId);
        }

        // No legal home timezone has yet been recorded, attempt to record the default.
        timeZoneId = defaultTZ.getID();
        if (timeZones.contains(timeZoneId)) {
            prefs.edit().putString(ClockSettingsActivity.KEY_HOME_TIME_ZONE, timeZoneId).apply();
        }

        // The timezone returned here may be valid or invalid. When it matches TimeZone.getDefault()
        // the Home city will not show, regardless of its validity.
        return defaultTZ;
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed in the app
     */
    public static ClockStyle getClockStyle(Context context, SharedPreferences prefs) {
        return getClockStyle(context, prefs, ClockSettingsActivity.KEY_CLOCK_STYLE);
    }

    /**
     * @return the theme applied.
     */
    static String getTheme(SharedPreferences prefs) {
        return prefs.getString(InterfaceCustomizationActivity.KEY_THEME, SYSTEM_THEME);
    }

    /**
     * @return the accent color applied.
     */
    static String getAccentColor(SharedPreferences prefs) {
        return prefs.getString(InterfaceCustomizationActivity.KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR);
    }

    /**
     * @return the dark mode of the applied theme.
     */
    static String getDarkMode(SharedPreferences prefs) {
        return prefs.getString(InterfaceCustomizationActivity.KEY_DARK_MODE, KEY_DEFAULT_DARK_MODE);
    }

    /**
     * @return whether or not the background should be displayed in a view.
     */
    static boolean isCardBackgroundDisplayed(SharedPreferences prefs) {
        return prefs.getBoolean(InterfaceCustomizationActivity.KEY_CARD_BACKGROUND, true);
    }

    /**
     * @return whether or not the background border should be displayed in a view.
     */
    static boolean isCardBackgroundBorderDisplayed(SharedPreferences prefs) {
        return prefs.getBoolean(InterfaceCustomizationActivity.KEY_CARD_BACKGROUND_BORDER, false);
    }

    /**
     * @return whether or not the vibrations are enabled for the buttons.
     */
    static boolean isVibrationsEnabled(SharedPreferences prefs) {
        return prefs.getBoolean(InterfaceCustomizationActivity.KEY_VIBRATIONS, false);
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed in the app
     */
    static boolean getDisplayClockSeconds(SharedPreferences prefs) {
        return prefs.getBoolean(ClockSettingsActivity.KEY_CLOCK_DISPLAY_SECONDS, false);
    }

    /**
     * @param displaySeconds whether or not to display seconds on main clock
     */
    static void setDisplayClockSeconds(SharedPreferences prefs, boolean displaySeconds) {
        prefs.edit().putBoolean(ClockSettingsActivity.KEY_CLOCK_DISPLAY_SECONDS, displaySeconds).apply();
    }

    /**
     * Sets the user's display seconds preference based on the currently selected clock if one has
     * not yet been manually chosen.
     */
    static void setDefaultDisplayClockSeconds(Context context, SharedPreferences prefs) {
        if (!prefs.contains(ClockSettingsActivity.KEY_CLOCK_DISPLAY_SECONDS)) {
            // If on analog clock style on upgrade, default to true. Otherwise, default to false.
            final boolean isAnalog = getClockStyle(context, prefs) == ClockStyle.ANALOG;
            setDisplayClockSeconds(prefs, isAnalog);
        }
    }

    /**
     * @return a value indicating whether analog or digital clocks are displayed on the screensaver
     */
    static ClockStyle getScreensaverClockStyle(Context context, SharedPreferences prefs) {
        return getClockStyle(context, prefs, ScreensaverSettingsActivity.KEY_CLOCK_STYLE);
    }

    /**
     * @return a value indicating whether analog or digital clock dynamic colors are displayed
     */
    static boolean getScreensaverClockDynamicColors(SharedPreferences prefs) {
        return prefs.getBoolean(ScreensaverSettingsActivity.KEY_CLOCK_DYNAMIC_COLORS, false);
    }

    /**
     * @return a value indicating the color of the clock of the screensaver
     */
    static int getPickerClockColor(SharedPreferences prefs) {
        return prefs.getInt(ScreensaverSettingsActivity.KEY_CLOCK_COLOR_PICKER, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return a value indicating the color of the date of the screensaver
     */
    static int getPickerDateColor(SharedPreferences prefs) {
        return prefs.getInt(ScreensaverSettingsActivity.KEY_DATE_COLOR_PICKER, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return a value indicating the color of the next alarm of the screensaver
     */
    static int getPickerNextAlarmColor(SharedPreferences prefs) {
        return prefs.getInt(ScreensaverSettingsActivity.KEY_NEXT_ALARM_COLOR_PICKER, Color.parseColor("#FFFFFF"));
    }

    /**
     * @return {@code int} the screen saver brightness level at night
     */
    static int getScreensaverBrightness(SharedPreferences prefs) {
        return prefs.getInt(ScreensaverSettingsActivity.KEY_SS_BRIGHTNESS, 40);
    }

    /**
     * @return a value indicating whether analog or digital clock seconds are displayed
     */
    static boolean getDisplayScreensaverClockSeconds(SharedPreferences prefs) {
        return prefs.getBoolean(ScreensaverSettingsActivity.KEY_SS_CLOCK_DISPLAY_SECONDS, false);
    }

    /**
     * @return {@code true} if the screen saver should show the clock in bold
     */
    static boolean getScreensaverBoldDigitalClock(SharedPreferences prefs) {
        return prefs.getBoolean(ScreensaverSettingsActivity.KEY_BOLD_DIGITAL_CLOCK, false);
    }

    /**
     * @return {@code true} if the screen saver should show the clock in italic
     */
    static boolean getScreensaverItalicDigitalClock(SharedPreferences prefs) {
        return prefs.getBoolean(ScreensaverSettingsActivity.KEY_ITALIC_DIGITAL_CLOCK, false);
    }

    /**
     * @return {@code true} if the screen saver should show the date in bold
     */
    static boolean getScreensaverBoldDate(SharedPreferences prefs) {
        return prefs.getBoolean(ScreensaverSettingsActivity.KEY_BOLD_DATE, true);
    }

    /**
     * @return {@code true} if the screen saver should show the date in italic
     */
    static boolean getScreensaverItalicDate(SharedPreferences prefs) {
        return prefs.getBoolean(ScreensaverSettingsActivity.KEY_ITALIC_DATE, false);
    }

    /**
     * @return {@code true} if the screen saver should show the next alarm in bold
     */
    static boolean getScreensaverBoldNextAlarm(SharedPreferences prefs) {
        return prefs.getBoolean(ScreensaverSettingsActivity.KEY_BOLD_NEXT_ALARM, true);
    }

    /**
     * @return {@code true} if the screen saver should show the next alarm in italic
     */
    static boolean getScreensaverItalicNextAlarm(SharedPreferences prefs) {
        return prefs.getBoolean(ScreensaverSettingsActivity.KEY_ITALIC_NEXT_ALARM, false);
    }

    /**
     * @return the uri of the selected ringtone or the {@code defaultUri} if no explicit selection
     * has yet been made
     */
    static Uri getTimerRingtoneUri(SharedPreferences prefs, Uri defaultUri) {
        final String uriString = prefs.getString(TimerSettingsActivity.KEY_TIMER_RINGTONE, null);
        return uriString == null ? defaultUri : Uri.parse(uriString);
    }

    /**
     * @return whether timer vibration is enabled. false by default.
     */
    static boolean getTimerVibrate(SharedPreferences prefs) {
        return prefs.getBoolean(TimerSettingsActivity.KEY_TIMER_VIBRATE, false);
    }

    /**
     * @param enabled whether vibration will be turned on for all timers.
     */
    static void setTimerVibrate(SharedPreferences prefs, boolean enabled) {
        prefs.edit().putBoolean(TimerSettingsActivity.KEY_TIMER_VIBRATE, enabled).apply();
    }

    /**
     * @return the default minutes or hour to add to timer when the "Add Minute Or Hour" button is clicked.
     */
    static int getDefaultTimeToAddToTimer(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_timer.xml
        final String string = prefs.getString(TimerSettingsActivity.KEY_DEFAULT_TIME_TO_ADD_TO_TIMER, "1");
        return Integer.parseInt(string);
    }

    /**
     * @return {@code true} if the timer display must remain on. {@code false} otherwise.
     */
    static boolean shouldTimerDisplayRemainOn(SharedPreferences pref) {
        return pref.getBoolean(TimerSettingsActivity.KEY_KEEP_TIMER_SCREEN_ON, true);
    }

    /**
     * @param uri the uri of the ringtone to play for all timers
     */
    static void setTimerRingtoneUri(SharedPreferences prefs, Uri uri) {
        prefs.edit().putString(TimerSettingsActivity.KEY_TIMER_RINGTONE, uri.toString()).apply();
    }

    /**
     * @return the uri of the ringtone from the settings to play for all alarms
     */
    static Uri getAlarmRingtoneUriFromSettings(SharedPreferences prefs, Uri defaultUri) {
        final String uriString = prefs.getString(KEY_DEFAULT_ALARM_RINGTONE, null);
        return uriString == null ? defaultUri : Uri.parse(uriString);
    }

    /**
     * @param uri the uri of the ringtone from the settings to play for all alarms
     */
    static void setAlarmRingtoneUriFromSettings(SharedPreferences prefs, Uri uri) {
        prefs.edit().putString(KEY_DEFAULT_ALARM_RINGTONE, uri.toString()).apply();
    }

    /**
     * @param uri identifies the ringtone to play of an existing alarm
     */
    static void setSelectedAlarmRingtoneUri(SharedPreferences prefs, Uri uri) {
        prefs.edit().putString(KEY_SELECTED_ALARM_RINGTONE_URI, uri.toString()).apply();
    }

    /**
     * @return the duration, in milliseconds, of the crescendo to apply to alarm ringtone playback;
     * {@code 0} implies no crescendo should be applied
     */
    static long getAlarmCrescendoDuration(SharedPreferences prefs) {
        final String crescendoSeconds = prefs.getString(AlarmSettingsActivity.KEY_ALARM_CRESCENDO, "0");
        return Integer.parseInt(crescendoSeconds) * DateUtils.SECOND_IN_MILLIS;
    }

    /**
     * @return the duration, in milliseconds, of the crescendo to apply to timer ringtone playback;
     * {@code 0} implies no crescendo should be applied
     */
    static long getTimerCrescendoDuration(SharedPreferences prefs) {
        final String crescendoSeconds = prefs.getString(TimerSettingsActivity.KEY_TIMER_CRESCENDO, "0");
        return Integer.parseInt(crescendoSeconds) * DateUtils.SECOND_IN_MILLIS;
    }

    /**
     * @return {@code true} if swipe action is enabled to dismiss or snooze alarms. {@code false} otherwise.
     */
    static boolean isSwipeActionEnabled(SharedPreferences pref) {
        return pref.getBoolean(KEY_SWIPE_ACTION, true);
    }

    /**
     * @return the display order of the weekdays, which can start with {@link Calendar#SATURDAY},
     * {@link Calendar#SUNDAY} or {@link Calendar#MONDAY}
     */
    static Weekdays.Order getWeekdayOrder(SharedPreferences prefs) {
        final String defaultValue = String.valueOf(Calendar.getInstance().getFirstDayOfWeek());
        final String value = prefs.getString(AlarmSettingsActivity.KEY_WEEK_START, defaultValue);
        final int firstCalendarDay = Integer.parseInt(value);
        return switch (firstCalendarDay) {
            case SATURDAY -> SAT_TO_FRI;
            case SUNDAY -> SUN_TO_SAT;
            case MONDAY -> MON_TO_SUN;
            default -> throw new IllegalArgumentException("Unknown weekday: " + firstCalendarDay);
        };
    }

    /**
     * @return {@code true} if the restore process (of backup and restore) has completed
     */
    static boolean isRestoreBackupFinished(SharedPreferences prefs) {
        return prefs.getBoolean(KEY_RESTORE_BACKUP_FINISHED, false);
    }

    /**
     * @param finished {@code true} means the restore process (of backup and restore) has completed
     */
    static void setRestoreBackupFinished(SharedPreferences prefs, boolean finished) {
        if (finished) {
            prefs.edit().putBoolean(KEY_RESTORE_BACKUP_FINISHED, true).apply();
        } else {
            prefs.edit().remove(KEY_RESTORE_BACKUP_FINISHED).apply();
        }
    }

    /**
     * @return the behavior to execute when volume buttons are pressed while firing an alarm
     */
    static AlarmVolumeButtonBehavior getAlarmVolumeButtonBehavior(SharedPreferences prefs) {
        final String defaultValue = AlarmSettingsActivity.DEFAULT_VOLUME_BEHAVIOR;
        final String value = prefs.getString(AlarmSettingsActivity.KEY_VOLUME_BUTTONS, defaultValue);
        return switch (value) {
            case AlarmSettingsActivity.DEFAULT_VOLUME_BEHAVIOR -> NOTHING;
            case AlarmSettingsActivity.VOLUME_BEHAVIOR_SNOOZE -> SNOOZE;
            case AlarmSettingsActivity.VOLUME_BEHAVIOR_DISMISS -> DISMISS;
            default -> throw new IllegalArgumentException("Unknown volume button behavior: " + value);
        };
    }

    /**
     * @return the behavior to execute when power buttons are pressed while firing an alarm
     */
    static AlarmVolumeButtonBehavior getAlarmPowerButtonBehavior(SharedPreferences prefs) {
        final String defaultValue = AlarmSettingsActivity.DEFAULT_POWER_BEHAVIOR;
        final String value = prefs.getString(AlarmSettingsActivity.KEY_POWER_BUTTONS, defaultValue);
        return switch (value) {
            case AlarmSettingsActivity.DEFAULT_POWER_BEHAVIOR -> NOTHING;
            case AlarmSettingsActivity.POWER_BEHAVIOR_SNOOZE -> SNOOZE;
            case AlarmSettingsActivity.POWER_BEHAVIOR_DISMISS -> DISMISS;
            default -> throw new IllegalArgumentException("Unknown power button behavior: " + value);
        };
    }

    /**
     * @return the number of minutes an alarm may ring before it has timed out and becomes missed
     */
    static int getAlarmTimeout(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(AlarmSettingsActivity.KEY_AUTO_SILENCE, "10");
        return Integer.parseInt(string);
    }

    /**
     * @return the number of minutes an alarm will remain snoozed before it rings again
     */
    static int getSnoozeLength(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(AlarmSettingsActivity.KEY_ALARM_SNOOZE, "10");
        return Integer.parseInt(string);
    }

    /**
     * @param currentTime timezone offsets created relative to this time
     * @return a description of the time zones available for selection
     */
    static TimeZones getTimeZones(Context context, long currentTime) {
        final Locale locale = Locale.getDefault();
        final Resources resources = context.getResources();
        final String[] timeZoneIds = resources.getStringArray(R.array.timezone_values);
        final String[] timeZoneNames = resources.getStringArray(R.array.timezone_labels);

        // Verify the data is consistent.
        if (timeZoneIds.length != timeZoneNames.length) {
            final String message = String.format(Locale.US,
                    "id count (%d) does not match name count (%d) for locale %s",
                    timeZoneIds.length, timeZoneNames.length, locale);
            throw new IllegalStateException(message);
        }

        // Create TimeZoneDescriptors for each TimeZone so they can be sorted.
        final TimeZoneDescriptor[] descriptors = new TimeZoneDescriptor[timeZoneIds.length];
        for (int i = 0; i < timeZoneIds.length; i++) {
            final String id = timeZoneIds[i];
            final String name = timeZoneNames[i].replaceAll("\"", "");
            descriptors[i] = new TimeZoneDescriptor(locale, id, name, currentTime);
        }
        Arrays.sort(descriptors);

        // Transfer the TimeZoneDescriptors into parallel arrays for easy consumption by the caller.
        final CharSequence[] tzIds = new CharSequence[descriptors.length];
        final CharSequence[] tzNames = new CharSequence[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            final TimeZoneDescriptor descriptor = descriptors[i];
            tzIds[i] = descriptor.mTimeZoneId;
            tzNames[i] = descriptor.mTimeZoneName;
        }

        return new TimeZones(tzIds, tzNames);
    }

    static int getFlipAction(SharedPreferences prefs) {
        final String string = prefs.getString(AlarmSettingsActivity.KEY_FLIP_ACTION, "0");
        return Integer.parseInt(string);
    }

    static int getShakeAction(SharedPreferences prefs) {
        final String string = prefs.getString(AlarmSettingsActivity.KEY_SHAKE_ACTION, "0");
        return Integer.parseInt(string);
    }

    /**
     * @return the number of minutes before the upcoming alarm notification appears
     */
    static int getAlarmNotificationReminderTime(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        final String string = prefs.getString(AlarmSettingsActivity.KEY_ALARM_NOTIFICATION_REMINDER_TIME, "30");
        return Integer.parseInt(string);
    }

    /**
     * @return {@code true} if alarm vibrations are enabled when creating alarms. {@code false} otherwise.
     */
    static boolean areAlarmVibrationsEnabledByDefault(SharedPreferences pref) {
        return pref.getBoolean(KEY_ENABLE_ALARM_VIBRATIONS_BY_DEFAULT, false);
    }

    /**
     * @return the time picker style.
     */
    static String getMaterialTimePickerStyle(SharedPreferences prefs) {
        // Default value must match the one in res/xml/settings_alarm.xml
        return prefs.getString(KEY_MATERIAL_TIME_PICKER_STYLE, MATERIAL_TIME_PICKER_ANALOG_STYLE);
    }

    private static ClockStyle getClockStyle(Context context, SharedPreferences prefs, String key) {
        final String defaultStyle = context.getString(R.string.default_clock_style);
        final String clockStyle = prefs.getString(key, defaultStyle);
        // Use hardcoded locale to perform toUpperCase, because in some languages toUpperCase adds
        // accent to character, which breaks the enum conversion.
        return ClockStyle.valueOf(clockStyle.toUpperCase(Locale.US));
    }

    /**
     * These descriptors have a natural order from furthest ahead of GMT to furthest behind GMT.
     */
    private static class TimeZoneDescriptor implements Comparable<TimeZoneDescriptor> {

        private final int mOffset;
        private final String mTimeZoneId;
        private final String mTimeZoneName;

        private TimeZoneDescriptor(Locale locale, String id, String name, long currentTime) {
            mTimeZoneId = id;

            final TimeZone tz = TimeZone.getTimeZone(id);
            mOffset = tz.getOffset(currentTime);

            final char sign = mOffset < 0 ? '-' : '+';
            final int absoluteGMTOffset = Math.abs(mOffset);
            final long hour = absoluteGMTOffset / HOUR_IN_MILLIS;
            final long minute = (absoluteGMTOffset / MINUTE_IN_MILLIS) % 60;
            mTimeZoneName = String.format(locale, "(GMT%s%d:%02d) %s", sign, hour, minute, name);
        }

        @Override
        public int compareTo(@NonNull TimeZoneDescriptor other) {
            return mOffset - other.mOffset;
        }
    }

}
