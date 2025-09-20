/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PermissionsManagementActivity.PermissionsManagementFragment.areEssentialPermissionsNotGranted;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ALARM_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_BACKUP_RESTORE_PREFERENCES;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CLOCK_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_INTERFACE_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_PERMISSIONS_MANAGEMENT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_PERMISSION_MESSAGE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_STOPWATCH_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_TIMER_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_WIDGETS_SETTINGS;
import static com.best.deskclock.utils.Utils.ACTION_LANGUAGE_CODE_CHANGED;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.BackupAndRestoreUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.WidgetUtils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Application settings
 */
public final class SettingsActivity extends CollapsingToolbarBaseActivity {

    private static final String KEY_APPBAR_EXPANDED = "key_appbar_expanded";
    private boolean mIsAppBarExpanded  = true;

    @Override
    protected String getActivityTitle() {
        // Already defined in the fragment.
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mAppBarLayout != null) {
            mAppBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
                // verticalOffset == 0 when extended, negative when collapsed
                mIsAppBarExpanded = (verticalOffset == 0);
            });
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new SettingsFragment())
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_APPBAR_EXPANDED)) {
            final boolean shouldExpand = savedInstanceState.getBoolean(KEY_APPBAR_EXPANDED);
            if (mAppBarLayout != null) {
                mAppBarLayout.setExpanded(shouldExpand, false);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_APPBAR_EXPANDED, mIsAppBarExpanded);
    }

    public static class SettingsFragment extends ScreenFragment implements Preference.OnPreferenceClickListener {

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

                    backupPreferences(uri);
                    Toast.makeText(requireContext(),
                            requireContext().getString(R.string.toast_message_for_backup),
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
                        restorePreferences(uri);
                        applySettingsAfterRestore();
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });

        @Override
        protected String getFragmentTitle() {
            return getString(R.string.settings);
        }

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

            setupPreferences();

            requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    requireActivity().finish();
                    if (SettingsDAO.isFadeTransitionsEnabled(mPrefs)) {
                        if (SdkUtils.isAtLeastAndroid14()) {
                            requireActivity().overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE,
                                    R.anim.fade_in, R.anim.fade_out);
                        } else {
                            requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        }
                    } else {
                        if (SdkUtils.isAtLeastAndroid14()) {
                            requireActivity().overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE,
                                    R.anim.activity_slide_from_left, R.anim.activity_slide_to_right);
                        } else {
                            requireActivity().overridePendingTransition(
                                    R.anim.activity_slide_from_left, R.anim.activity_slide_to_right);
                        }
                    }
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();

            displayWarningIfEssentialPermissionAreNotGranted();
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference pref) {
            switch (pref.getKey()) {
                case KEY_INTERFACE_CUSTOMIZATION -> animateAndShowFragment(new InterfaceCustomizationFragment());

                case KEY_CLOCK_SETTINGS -> animateAndShowFragment(new ClockSettingsFragment());

                case KEY_ALARM_SETTINGS -> animateAndShowFragment(new AlarmSettingsFragment());

                case KEY_TIMER_SETTINGS -> animateAndShowFragment(new TimerSettingsFragment());

                case KEY_STOPWATCH_SETTINGS -> animateAndShowFragment(new StopwatchSettingsFragment());

                case KEY_SCREENSAVER_SETTINGS ->
                    animateAndShowFragment(new ScreensaverSettingsActivity.ScreensaverSettingsFragment());

                case KEY_WIDGETS_SETTINGS -> animateAndShowFragment(new WidgetSettingsFragment());

                case KEY_PERMISSION_MESSAGE, KEY_PERMISSIONS_MANAGEMENT ->
                    animateAndShowFragment(new PermissionsManagementActivity.PermissionsManagementFragment());

                case KEY_BACKUP_RESTORE_PREFERENCES ->
                    new MaterialAlertDialogBuilder(requireContext())
                            .setIcon(R.drawable.ic_backup_restore)
                            .setTitle(R.string.backup_restore_title)
                            .setMessage(R.string.backup_restore_dialog_message)
                            .setPositiveButton(android.R.string.cancel, null)
                            .setNegativeButton(R.string.backup_button_title, (dialog, which) -> {
                                String currentDateAndTime = DateFormat.format("yyyy_MM_dd_HH-mm-ss", new Date()).toString();
                                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                                        .addCategory(Intent.CATEGORY_OPENABLE)
                                        .putExtra(Intent.EXTRA_TITLE, requireContext().getString(R.string.app_label)
                                                + "_backup_" + currentDateAndTime + ".json")
                                        .setType("application/json");
                                backupToFile.launch(intent);
                            })
                            .setNeutralButton(R.string.restore_button_title, (dialog, which) -> {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                                        .addCategory(Intent.CATEGORY_OPENABLE)
                                        .setType("application/json");
                                restoreFromFile.launch(intent);
                            })
                            .show();
            }

            return true;
        }

        private void setupPreferences() {
            mPermissionMessage.setVisible(areEssentialPermissionsNotGranted(requireContext()));

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

        private void displayWarningIfEssentialPermissionAreNotGranted() {
            if (areEssentialPermissionsNotGranted(requireContext())) {
                mPermissionMessage.setVisible(true);
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
            } else {
                mPermissionMessage.setVisible(false);
            }
        }

        private void backupPreferences(Uri uri) {
            try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
                BackupAndRestoreUtils.settingsToJsonStream(requireContext(), mPrefs, mPrefs.getAll(), outputStream);
            } catch (IOException e) {
                LogUtils.wtf("Error during backup");
            }
        }

        private void restorePreferences(Uri uri) throws FileNotFoundException {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            BackupAndRestoreUtils.readJson(requireContext(), mPrefs, inputStream);
        }

        private void applySettingsAfterRestore() {
            // Required to update Locale.
            requireContext().sendBroadcast(new Intent(ACTION_LANGUAGE_CODE_CHANGED));
            // Required to update widgets.
            WidgetUtils.updateAllWidgets(requireContext());

            // Required to update the timer list.
            DataModel.getDataModel().loadTimers();
            // Required to update the tab to display.
            if (SettingsDAO.getTabToDisplay(mPrefs) != -1) {
                UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.values()[SettingsDAO.getTabToDisplay(mPrefs)]);
            }

            Toast.makeText(requireContext(), requireContext().getString(R.string.toast_message_for_restore),
                    Toast.LENGTH_SHORT).show();
        }
    }

}
