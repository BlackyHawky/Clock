/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Inspired by Heliboard (https://github.com/Helium314/HeliBoard/blob/main/app/src/main/java/helium314/keyboard/latin/settings/AdvancedSettingsFragment.kt)
 */

package com.best.deskclock.utils;

import static com.best.deskclock.FirstLaunch.KEY_IS_FIRST_LAUNCH;
import static com.best.deskclock.data.CustomRingtoneDAO.NEXT_RINGTONE_ID;
import static com.best.deskclock.data.CustomRingtoneDAO.RINGTONE_IDS;
import static com.best.deskclock.data.CustomRingtoneDAO.RINGTONE_TITLE;
import static com.best.deskclock.data.CustomRingtoneDAO.RINGTONE_URI;
import static com.best.deskclock.data.SettingsDAO.KEY_SELECTED_ALARM_RINGTONE_URI;
import static com.best.deskclock.data.TimerDAO.TIMER_IDS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DEFAULT_ALARM_RINGTONE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_RINGTONE;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;

import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmStateManager;
import com.best.deskclock.data.Weekdays;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.provider.AlarmInstance;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class lists all settings that can be backed up or restored.
 */
public class BackupAndRestoreUtils {

    /**
     * Read and export values in SharedPreferences to a file.
     */
    public static void settingsToJsonStream(Context context, SharedPreferences prefs,
                                            Map<String, ?> settings, OutputStream out) {

        Map<String, Boolean> booleans = new HashMap<>();
        Map<String, String> strings = new HashMap<>();
        Map<String, Integer> ints = new HashMap<>();
        Map<String, Long> longs = new HashMap<>();
        Set<String> timerIds = prefs.getStringSet(TIMER_IDS, Collections.emptySet());

        for (Map.Entry<String, ?> entry : settings.entrySet()) {
            if (entry.getKey() != null) {
                String key = entry.getKey();

                // Exclude keys from custom ringtones as this causes bugs when restoring.
                // Also, exclude the selected alarm ringtone.
                if (RINGTONE_IDS.equals(key) || key.startsWith(RINGTONE_URI) || NEXT_RINGTONE_ID.equals(key)
                        || key.startsWith(RINGTONE_TITLE) || KEY_SELECTED_ALARM_RINGTONE_URI.equals(key)) {
                    continue;
                }

                if (entry.getValue() instanceof Boolean) {
                    booleans.put(entry.getKey(), prefs.getBoolean(entry.getKey(), (Boolean) entry.getValue()));
                } else if (entry.getValue() instanceof String) {
                    // Exclude these keys if the URI does not match a system ringtone
                    if (KEY_TIMER_RINGTONE.equals(key) || KEY_DEFAULT_ALARM_RINGTONE.equals(key)) {
                        String value = prefs.getString(key, (String) entry.getValue());
                        Uri uri = Uri.parse(value);
                        if (isNotSystemRingtone(uri)) {
                            continue;
                        }
                    }

                    strings.put(entry.getKey(), prefs.getString(entry.getKey(), (String) entry.getValue()));
                } else if (entry.getValue() instanceof Integer) {
                    ints.put(entry.getKey(), prefs.getInt(entry.getKey(), (Integer) entry.getValue()));
                } else if (entry.getValue() instanceof Long) {
                    longs.put(entry.getKey(), prefs.getLong(entry.getKey(), (Long) entry.getValue()));
                }
            }
        }

        try {
            JSONObject jsonObject = new JSONObject();

            // Convert the Map of booleans to a JSONObject
            jsonObject.put("Boolean settings", convertMapToJsonObject(booleans));

            // Convert the Map of strings to a JSONObject
            jsonObject.put("String settings", convertMapToJsonObject(strings));

            // Convert the Map of integers to a JSONObject
            jsonObject.put("Integer settings", convertMapToJsonObject(ints));

            // Convert the Map of longs to a JSONObject
            jsonObject.put("Long settings", convertMapToJsonObject(longs));

            // Convert the Map of timers IDs to a JSONArray
            jsonObject.put("Timers IDs", new JSONArray(timerIds));

            // Convert the alarms to a JSONArray
            JSONArray alarmsArray = new JSONArray();
            JSONArray alarmsWithDateArray = new JSONArray();

            List<Alarm> alarms = Alarm.getAlarms(context.getContentResolver(), null);
            for (Alarm alarm : alarms) {
                JSONObject alarmObject = new JSONObject();

                alarmObject.put("id", alarm.id);
                alarmObject.put("enabled", alarm.enabled);
                alarmObject.put("hour", alarm.hour);
                alarmObject.put("minutes", alarm.minutes);
                alarmObject.put("vibrate", alarm.vibrate);
                alarmObject.put("flash", alarm.flash);
                alarmObject.put("daysOfWeek", alarm.daysOfWeek.getBits());
                alarmObject.put("label", alarm.label);
                alarmObject.put("alert", alarm.alert);
                alarmObject.put("deleteAfterUse", alarm.deleteAfterUse);
                alarmObject.put("autoSilenceDuration", alarm.autoSilenceDuration);
                alarmObject.put("snoozeDuration", alarm.snoozeDuration);
                alarmObject.put("crescendoDuration", alarm.crescendoDuration);
                alarmObject.put("alarmVolume", alarm.alarmVolume);

                if (alarm.daysOfWeek.isRepeating() || !alarm.isSpecifiedDate()) {
                    alarmsArray.put(alarmObject);
                } else {
                    alarmObject.put("year", alarm.year);
                    alarmObject.put("month", alarm.month);
                    alarmObject.put("day", alarm.day);

                    alarmsWithDateArray.put(alarmObject);
                }
            }

            jsonObject.put("Alarms", alarmsArray);

            jsonObject.put("Alarms with specified date", alarmsWithDateArray);

            out.write(jsonObject.toString(4).getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (JSONException e) {
            LogUtils.e("JSON parsing error", e);
        } catch (IOException e) {
            LogUtils.e("Error writing to file", e);
        }
    }

    /**
     * Helper method to convert a Map to JSONObject.
     */
    private static JSONObject convertMapToJsonObject(Map<String, ?> map) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                jsonObject.put(entry.getKey(), value);
            } else if (value instanceof String) {
                jsonObject.put(entry.getKey(), value);
            } else if (value instanceof Integer) {
                jsonObject.put(entry.getKey(), value);
            } else if (value instanceof Long) {
                jsonObject.put(entry.getKey(), value);
            } else if (value instanceof Set<?> set) {
                if (!set.isEmpty() && set.iterator().next() instanceof String) {
                    jsonObject.put(entry.getKey(), new JSONArray(set));
                } else {
                    LogUtils.w("Expected Set<String>, but got: " + set.getClass().getName());
                }
            }
        }
        return jsonObject;
    }

    /**
     * Read and apply values to restore in SharedPreferences.
     */
    public static void readJson(Context context, SharedPreferences prefs, InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        SharedPreferences.Editor editor = prefs.edit();

        // Do not reset the KEY_IS_FIRST_LAUNCH key to prevent the "FirstLaunch" activity from reappearing.
        // Also, exclude keys corresponding to custom ringtones and the selected alarm ringtone,
        // as this causes bugs for alarms.
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();

            if (!entry.getKey().equals(KEY_IS_FIRST_LAUNCH) &&
                    !key.startsWith(RINGTONE_URI) &&
                    !RINGTONE_IDS.equals(key) &&
                    !NEXT_RINGTONE_ID.equals(key) &&
                    !key.startsWith(RINGTONE_TITLE) &&
                    !KEY_SELECTED_ALARM_RINGTONE_URI.equals(key)) {
                editor.remove(key);
                editor.apply();
            }
        }

        try {
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            JSONObject jsonObject = new JSONObject(jsonBuilder.toString());

            JSONObject booleans = jsonObject.getJSONObject("Boolean settings");
            for (Iterator<String> it = booleans.keys(); it.hasNext();) {
                String key = it.next();
                boolean value = booleans.getBoolean(key);
                editor.putBoolean(key, value);
            }

            JSONObject strings = jsonObject.getJSONObject("String settings");
            for (Iterator<String> it = strings.keys(); it.hasNext();) {
                String key = it.next();
                String value = strings.getString(key);

                if (isRingtoneKey(key)) {
                    if (!isRingtoneAvailable(context, value)) {
                        if (KEY_TIMER_RINGTONE.equals(key)) {
                            editor.putString(key, RingtoneUtils.getResourceUri(context, R.raw.timer_expire).toString());
                        } else if (KEY_DEFAULT_ALARM_RINGTONE.equals(key)) {
                            editor.putString(key, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString());
                        }
                    } else {
                        editor.putString(key, value);
                    }
                } else {
                    editor.putString(key, value);
                }
            }

            JSONObject integers = jsonObject.getJSONObject("Integer settings");
            for (Iterator<String> it = integers.keys(); it.hasNext();) {
                String key = it.next();
                int value = integers.getInt(key);
                editor.putInt(key, value);
            }

            JSONObject longs = jsonObject.getJSONObject("Long settings");
            for (Iterator<String> it = longs.keys(); it.hasNext();) {
                String key = it.next();
                long value = longs.getLong(key);
                editor.putLong(key, value);
            }

            if (jsonObject.has("Timers IDs")) {
                JSONArray timerIdsArray = jsonObject.getJSONArray("Timers IDs");
                Set<String> timerIds = new HashSet<>();
                for (int i = 0; i < timerIdsArray.length(); i++) {
                    timerIds.add(timerIdsArray.getString(i));
                }
                editor.putStringSet(TIMER_IDS, timerIds);
            }

            editor.apply();

            final ContentResolver contentResolver = context.getContentResolver();
            // Clear the alarm list before restoring to avoid adding duplicates
            final List<Alarm> alarms = Alarm.getAlarms(contentResolver, null);
            for (Alarm alarm : alarms) {
                AlarmStateManager.deleteAllInstances(context, alarm.id);
                Alarm.deleteAlarm(contentResolver, alarm.id);
            }

            if (jsonObject.has("Alarms")) {
                JSONArray alarmsArray = jsonObject.getJSONArray("Alarms");
                for (int i = 0; i < alarmsArray.length(); i++) {
                    JSONObject alarmObject = alarmsArray.getJSONObject(i);
                    restoreAlarm(context, contentResolver, alarmObject, false);
                }
            }

            if (jsonObject.has("Alarms with specified date")) {
                JSONArray alarmsWithDateArray = jsonObject.getJSONArray("Alarms with specified date");
                for (int i = 0; i < alarmsWithDateArray.length(); i++) {
                    JSONObject alarmObject = alarmsWithDateArray.getJSONObject(i);
                    restoreAlarm(context, contentResolver, alarmObject, true);
                }
            }
        } catch (IOException | JSONException e) {
            LogUtils.e("Error during restore", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                LogUtils.e("Error closing reader", e);
            }
        }
    }

    /**
     * Restore alarm data.
     * If the alarm is enabled, a future instance will be scheduled.
     */
    private static void restoreAlarm(Context context, ContentResolver contentResolver,
                                     JSONObject alarmObject, boolean hasSpecifiedDate) throws JSONException {

        long id = alarmObject.getLong("id");
        boolean enabled = alarmObject.getBoolean("enabled");
        int hour = alarmObject.getInt("hour");
        int minutes = alarmObject.getInt("minutes");
        boolean vibrate = alarmObject.getBoolean("vibrate");
        boolean flash = alarmObject.getBoolean("flash");
        int daysOfWeek = alarmObject.getInt("daysOfWeek");
        String label = alarmObject.getString("label");
        String alert = alarmObject.getString("alert");
        boolean deleteAfterUse = alarmObject.getBoolean("deleteAfterUse");
        int autoSilenceDuration = alarmObject.getInt("autoSilenceDuration");
        int snoozeDuration = alarmObject.getInt("snoozeDuration");
        int crescendoDuration = alarmObject.getInt("crescendoDuration");
        int alarmVolume = alarmObject.getInt("alarmVolume");

        String alarmRingtone;
        if (RingtoneUtils.isRandomRingtone(Uri.parse(alert))) {
            alarmRingtone = RingtoneUtils.RANDOM_RINGTONE.toString();
        } else if (RingtoneUtils.isRandomCustomRingtone(Uri.parse(alert))) {
            alarmRingtone = RingtoneUtils.RANDOM_CUSTOM_RINGTONE.toString();
        } else if (!isNotSystemRingtone(Uri.parse(alert)) && isRingtoneAvailable(context, alert)) {
            alarmRingtone = alert;
        } else {
            alarmRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
        }

        Alarm restoredAlarm;

        int year;
        int month;
        int day;

        if (hasSpecifiedDate) {
            year = alarmObject.getInt("year");
            month = alarmObject.getInt("month");
            day = alarmObject.getInt("day");
        } else {
            Calendar calendar = Calendar.getInstance();
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DAY_OF_MONTH);
        }

        restoredAlarm = new Alarm(id, enabled, year, month, day, hour, minutes,
                vibrate, flash, Weekdays.fromBits(daysOfWeek), label, alarmRingtone, deleteAfterUse,
                autoSilenceDuration, snoozeDuration, crescendoDuration, alarmVolume);

        Alarm.addAlarm(contentResolver, restoredAlarm);

        if (restoredAlarm.enabled) {
            AlarmInstance alarmInstance = restoredAlarm.createInstanceAfter(Calendar.getInstance());
            AlarmInstance.addInstance(contentResolver, alarmInstance);
            AlarmStateManager.registerInstance(context, alarmInstance, true);
            LogUtils.i("BackupAndRestoreUtils scheduled alarm instance: %s", alarmInstance);
        }
    }

    /**
     * @return {@code true} if the URI starts with one of the possible system directories for ringtones. {@code false} otherwise.
     * This excludes custom ringtones that cause problems during restoration.
     */
    private static boolean isNotSystemRingtone(Uri uri) {
        String uriString = uri.toString().toLowerCase();
        return !(uriString.startsWith("content://media/external/audio/") ||
                uriString.startsWith("content://media/internal/audio/") ||
                uriString.startsWith("content://media/") ||
                uriString.startsWith("file:///system/media/audio/") ||
                uriString.startsWith("file:///system/media/"));
    }

    /**
     * @return {@code true} if a key matches a ringtone key. {@code false} otherwise.
     */
    private static boolean isRingtoneKey(String key) {
        return KEY_TIMER_RINGTONE.equals(key) || KEY_DEFAULT_ALARM_RINGTONE.equals(key);
    }

    /**
     * @return {@code true} if a ringtone is available in the device. {@code false} otherwise.
     * Useful when restoring between different devices.
     */
    private static boolean isRingtoneAvailable(Context context, String ringtoneUriString) {
        Uri ringtoneUri = Uri.parse(ringtoneUriString);

        // Check if the URI is of type "content" or "file"
        if ("content".equals(ringtoneUri.getScheme()) || "file".equals(ringtoneUri.getScheme())) {
            try {
                if ("content".equals(ringtoneUri.getScheme())) {
                    // For URI content:// (managed by ContentResolver)
                    try (Cursor cursor = context.getContentResolver().query(ringtoneUri, null,
                            null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

                            if (columnIndex != -1) {
                                String filePath = cursor.getString(columnIndex);
                                File file = new File(filePath);
                                return file.exists();
                            }
                        }
                    }
                } else if ("file".equals(ringtoneUri.getScheme())) {
                    // For URI file:// (local files on the system)
                    String path = ringtoneUri.getPath();
                    if (path != null) {
                        File file = new File(path);
                        return file.exists();
                    }
                }
            } catch (Exception e) {
                LogUtils.e("Error checking ringtone availability", e);
            }
        }

        return false;
    }

}
