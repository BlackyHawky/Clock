/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static com.best.deskclock.DeskClock.REQUEST_CHANGE_PERMISSIONS;
import static com.best.deskclock.DeskClock.REQUEST_CHANGE_SETTINGS;
import static com.best.deskclock.DeskClockApplication.getDefaultSharedPreferences;
import static com.best.deskclock.data.WidgetModel.ACTION_UPDATE_WIDGETS_AFTER_RESTORE;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.DARK_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.LIGHT_THEME;
import static com.best.deskclock.settings.InterfaceCustomizationActivity.SYSTEM_THEME;
import static com.best.deskclock.settings.ThemeController.Setting.CHANGED;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Application settings
 */
public final class SettingsActivity extends CollapsingToolbarBaseActivity {

    public static final String PREFS_FRAGMENT_TAG = "settings_prefs_fragment";

    public static final String KEY_PERMISSION_MESSAGE = "key_permission_message";
    public static final String KEY_INTERFACE_CUSTOMIZATION = "key_interface_customization";
    public static final String KEY_CLOCK_SETTINGS = "key_clock_settings";
    public static final String KEY_ALARM_SETTINGS = "key_alarm_settings";
    public static final String KEY_TIMER_SETTINGS = "key_timer_settings";
    public static final String KEY_STOPWATCH_SETTINGS = "key_stopwatch_settings";
    public static final String KEY_SCREENSAVER_SETTINGS = "key_screensaver_settings";
    public static final String KEY_WIDGETS_SETTINGS = "key_widgets_settings";
    public static final String KEY_PERMISSIONS_MANAGEMENT = "key_permissions_management";
    public static final String KEY_BACKUP_RESTORE_PREFERENCES = "key_backup_restore_preferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new PrefsFragment(), PREFS_FRAGMENT_TAG)
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    public static class PrefsFragment extends ScreenFragment implements Preference.OnPreferenceClickListener {

        Preference mInterfaceCustomizationPref;
        Preference mClockSettingsPref;
        Preference mAlarmSettingsPref;
        Preference mTimerSettingsPref;
        Preference mStopwatchSettingsPref;
        Preference mScreensaverSettings;
        Preference mWidgetsSettings;
        Preference mPermissionsManagement;
        Preference mPermissionMessage;
        Preference mBackupRestorePref;

        /**
         * Callback for getting the result from the settings sub-activities.
         */
        private final ActivityResultLauncher<Intent> getActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), (result) -> {
                    if (result.getResultCode() != RESULT_OK) {
                        return;
                    }

                    requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
                });

        /**
         * Callback for getting the result from the Permission Management activity.
         */
        private final ActivityResultLauncher<Intent> getPermissionManagementActivity = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), (result) -> {
                    if (result.getResultCode() != REQUEST_CHANGE_PERMISSIONS) {
                        return;
                    }

                    requireActivity().setResult(REQUEST_CHANGE_SETTINGS);
                });

        /**
         * Callback for getting the backup result.
         */
        private final ActivityResultLauncher<Intent> backupToFile = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), (result) -> {
                    if (result.getResultCode() != RESULT_OK) {
                        return;
                    }

                    Intent intent = result.getData();
                    final Uri uri = intent == null ? null : intent.getData();
                    if (uri == null) {
                        return;
                    }

                    backupPreferences(requireContext(), uri);
                    Toast.makeText(requireContext(),
                            requireContext().getString(R.string.backup_restore_toast_message_for_backup),
                            Toast.LENGTH_SHORT)
                            .show();
                });

        /**
         * Callback for getting the restoration result.
         */
        private final ActivityResultLauncher<Intent> restoreFromFile = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), (result) -> {
                    if (result.getResultCode() != RESULT_OK) {
                        return;
                    }

                    Intent intent = result.getData();
                    final Uri uri = intent == null ? null : intent.getData();
                    if (uri == null) {
                        return;
                    }

                    try {
                        restorePreferences(requireContext(), uri);
                        // This is to ensure that the interface theme loads correctly after the restore.
                        String getTheme = DataModel.getDataModel().getTheme();
                        switch (getTheme) {
                            case SYSTEM_THEME ->
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            case LIGHT_THEME ->
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            case DARK_THEME ->
                                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        }
                        requireContext().sendBroadcast(new Intent(ACTION_UPDATE_WIDGETS_AFTER_RESTORE));
                        ThemeController.setNewSetting(CHANGED);
                        Toast.makeText(requireContext(),
                                requireContext().getString(R.string.backup_restore_toast_message_for_restore),
                                Toast.LENGTH_SHORT)
                                .show();
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings);

            mInterfaceCustomizationPref = findPreference(KEY_INTERFACE_CUSTOMIZATION);
            mClockSettingsPref = findPreference(KEY_CLOCK_SETTINGS);
            mAlarmSettingsPref = findPreference(KEY_ALARM_SETTINGS);
            mTimerSettingsPref = findPreference(KEY_TIMER_SETTINGS);
            mStopwatchSettingsPref = findPreference(KEY_STOPWATCH_SETTINGS);
            mScreensaverSettings = findPreference(KEY_SCREENSAVER_SETTINGS);
            mWidgetsSettings = findPreference(KEY_WIDGETS_SETTINGS);
            mPermissionsManagement = findPreference(KEY_PERMISSIONS_MANAGEMENT);
            mPermissionMessage = findPreference(KEY_PERMISSION_MESSAGE);
            mBackupRestorePref = findPreference(KEY_BACKUP_RESTORE_PREFERENCES);

            hidePreferences();
        }

        @Override
        public void onResume() {
            super.onResume();

            refresh();
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference pref) {
            final Context context = getActivity();
            if (context == null) {
                return false;
            }

            switch (pref.getKey()) {
                case KEY_INTERFACE_CUSTOMIZATION -> {
                    final Intent interfaceCustomizationIntent =
                            new Intent(context, InterfaceCustomizationActivity.class);
                    getActivity.launch(interfaceCustomizationIntent);
                    return true;
                }

                case KEY_CLOCK_SETTINGS -> {
                    final Intent clockSettingsIntent = new Intent(context, ClockSettingsActivity.class);
                    getActivity.launch(clockSettingsIntent);
                    return true;
                }

                case KEY_ALARM_SETTINGS -> {
                    final Intent alarmSettingsIntent = new Intent(context, AlarmSettingsActivity.class);
                    getActivity.launch(alarmSettingsIntent);
                    return true;
                }

                case KEY_TIMER_SETTINGS -> {
                    final Intent timerSettingsIntent = new Intent(context, TimerSettingsActivity.class);
                    getActivity.launch(timerSettingsIntent);
                    return true;
                }

                case KEY_STOPWATCH_SETTINGS -> {
                    final Intent stopwatchSettingsIntent = new Intent(context, StopwatchSettingsActivity.class);
                    getActivity.launch(stopwatchSettingsIntent);
                    return true;
                }

                case KEY_SCREENSAVER_SETTINGS -> {
                    final Intent screensaverSettingsIntent =
                            new Intent(context, ScreensaverSettingsActivity.class);
                    startActivity(screensaverSettingsIntent);
                    return true;
                }

                case KEY_WIDGETS_SETTINGS -> {
                    final Intent widgetsSettingsIntent = new Intent(context, WidgetsSettingsActivity.class);
                    startActivity(widgetsSettingsIntent);
                    return true;
                }

                case KEY_PERMISSION_MESSAGE, KEY_PERMISSIONS_MANAGEMENT -> {
                    final Intent permissionsManagementIntent =
                            new Intent(context, PermissionsManagementActivity.class);
                    getPermissionManagementActivity.launch(permissionsManagementIntent);
                    return true;
                }

                case KEY_BACKUP_RESTORE_PREFERENCES -> {
                    final AlertDialog builder = new AlertDialog.Builder(requireContext())
                            .setIcon(R.drawable.ic_backup_restore)
                            .setTitle(R.string.backup_restore_settings_title)
                            .setMessage(R.string.backup_restore_dialog_message)
                            .setPositiveButton(android.R.string.cancel, null)
                            .setNegativeButton(R.string.backup_restore_backup_button_title, (dialog, which) -> {
                                String currentDateAndTime = DateFormat.format("yyyy_MM_dd_HH-mm-ss", new Date()).toString();
                                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                                        .addCategory(Intent.CATEGORY_OPENABLE)
                                        .putExtra(Intent.EXTRA_TITLE, requireContext().getString(R.string.app_label)
                                                + "_backup_" + currentDateAndTime + ".json")
                                        .setType("application/json");
                                backupToFile.launch(intent);
                            })
                            .setNeutralButton(R.string.backup_restore_restore_button_title, (dialog, which) -> {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                                        .addCategory(Intent.CATEGORY_OPENABLE)
                                        .setType("application/json");
                                restoreFromFile.launch(intent);
                            })
                            .create();
                    builder.show();
                    return true;
                }
            }

            return false;
        }

        private void hidePreferences() {
            mPermissionMessage.setVisible(
                        PermissionsManagementActivity.areEssentialPermissionsNotGranted(requireContext())
            );
        }

        private void refresh() {
            mPermissionMessage.setVisible(
                    PermissionsManagementActivity.areEssentialPermissionsNotGranted(requireContext())
            );
            if (mPermissionMessage.isShown()) {
                final SpannableStringBuilder builderPermissionMessage = new SpannableStringBuilder();
                final String messagePermission = requireContext().getString(R.string.settings_permission_message);
                final Spannable spannableMessagePermission = new SpannableString(messagePermission);
                spannableMessagePermission.setSpan(
                        new ForegroundColorSpan(requireContext().getColor(R.color.colorAlert)),
                        0, messagePermission.length(), 0);
                spannableMessagePermission.setSpan(
                        new StyleSpan(Typeface.BOLD), 0, messagePermission.length(), 0);
                builderPermissionMessage.append(spannableMessagePermission);
                mPermissionMessage.setTitle(builderPermissionMessage);
                mPermissionMessage.setOnPreferenceClickListener(this);
            }

            mInterfaceCustomizationPref.setOnPreferenceClickListener(this);

            mClockSettingsPref.setOnPreferenceClickListener(this);

            mAlarmSettingsPref.setOnPreferenceClickListener(this);

            mTimerSettingsPref.setOnPreferenceClickListener(this);

            mStopwatchSettingsPref.setOnPreferenceClickListener(this);

            mScreensaverSettings.setOnPreferenceClickListener(this);

            mWidgetsSettings.setOnPreferenceClickListener(this);

            mPermissionsManagement.setOnPreferenceClickListener(this);

            mBackupRestorePref.setOnPreferenceClickListener(this);
        }

        private void backupPreferences(Context context, Uri uri) {
            SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);

            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
                BackupAndRestoreUtils.settingsToJsonStream(sharedPreferences.getAll(), outputStream, sharedPreferences, context);
            } catch (IOException e) {
                Log.w(PREFS_FRAGMENT_TAG, "error during backup");
            }
        }

        private void restorePreferences(Context context, Uri uri) throws FileNotFoundException {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);

            BackupAndRestoreUtils.readJsonLines(inputStream, sharedPreferences);
        }
    }
}
