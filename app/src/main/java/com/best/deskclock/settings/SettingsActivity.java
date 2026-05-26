/*
 * Copyright (C) 2015 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.*;
import static com.best.deskclock.utils.Utils.ACTION_LANGUAGE_CODE_CHANGED;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;

import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.base.KeepAliveService;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.tiles.AlarmTileService;
import com.best.deskclock.tiles.StopwatchTileService;
import com.best.deskclock.tiles.TimerTileService;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.BackupAndRestoreUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.PermissionUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Application settings
 */
public final class SettingsActivity extends CollapsingToolbarBaseActivity {

    private static final String KEY_APPBAR_EXPANDED = "key_appbar_expanded";
    private boolean mIsAppBarExpanded = true;

    @Override
    protected String getActivityTitle() {
        // Already defined in the fragment.
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBaseBinding.appBar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            // verticalOffset == 0 when extended, negative when collapsed
            mIsAppBarExpanded = (verticalOffset == 0);
        });

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
            mBaseBinding.appBar.setExpanded(shouldExpand, false);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_APPBAR_EXPANDED, mIsAppBarExpanded);
    }

    public static class SettingsFragment extends ScreenFragment implements Preference.OnPreferenceClickListener {

        private static final String BACKUP_JSON_FILE_NAME = "settings.json";

        private static final String KEY_SHOW_BACKUP_RESTORE_DIALOG = "show_backup_restore_dialog";

        private boolean mShowBackupRestoreDialog = false;

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

        private AlertDialog mRestartDialog;

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

                final Context appContext = requireContext().getApplicationContext();

                AppExecutors.getDiskIO().execute(() -> {
                    try {
                        backupPreferences(appContext, uri);

                        AppExecutors.getMainThread().post(() -> CustomToast.show(appContext, R.string.toast_message_for_backup));
                    } catch (Exception e) {
                        LogUtils.e("Error during backup", e);

                        AppExecutors.getMainThread().post(() -> CustomToast.show(appContext, R.string.toast_message_backup_error));
                    }
                });
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

                final Context appContext = requireContext().getApplicationContext();

                BackupAndRestoreUtils.isRestoringBackupOrIsResettingApp = true;

                AppExecutors.getDiskIO().execute(() -> {
                    try {
                        wipeCustomMediaBeforeRestore(appContext);

                        restorePreferences(appContext, uri);

                        AppExecutors.getMainThread().post(() -> {
                            applySettingsAfterRestore(appContext);

                            BackupAndRestoreUtils.appNeedsRestart = true;

                            if (isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                                mRestartDialog = restartAppDialog(appContext, false);
                                mRestartDialog.show();
                            } else {
                                // If the user has left the screen, clear the notifications and force a restart without the dialog.
                                NotificationUtils.clearAllNotifications(appContext);

                                Intent restartIntent = new Intent(appContext, DeskClock.class);
                                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                appContext.startActivity(restartIntent);
                                Runtime.getRuntime().exit(0);
                            }
                        });
                    } catch (Exception e) {
                        LogUtils.e("Error reading the restore file", e);

                        AppExecutors.getMainThread().post(() -> {
                            BackupAndRestoreUtils.isRestoringBackupOrIsResettingApp = false;

                            CustomToast.show(appContext, R.string.toast_message_restore_error);
                        });
                    }
                });
            });

        @Override
        protected String getFragmentTitle() {
            return getString(Utils.getStringResByBuildType(
                R.string.settings, R.string.setting_debug_title, R.string.setting_nightly_title));
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

            if (savedInstanceState != null) {
                mShowBackupRestoreDialog = savedInstanceState.getBoolean(KEY_SHOW_BACKUP_RESTORE_DIALOG, false);
            }

            setupPreferences();

            updateSettingsVisibility();

            requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    requireActivity().finish();
                    if (SettingsDAO.isFadeTransitionsEnabled(mPrefs)) {
                        if (SdkUtils.isAtLeastAndroid14()) {
                            requireActivity().overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.fade_in, R.anim.fade_out);
                        } else {
                            requireActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                        }
                    } else {
                        if (SdkUtils.isAtLeastAndroid14()) {
                            requireActivity().overrideActivityTransition(
                                OVERRIDE_TRANSITION_CLOSE, R.anim.activity_slide_from_left, R.anim.activity_slide_to_right);
                        } else {
                            requireActivity().overridePendingTransition(R.anim.activity_slide_from_left, R.anim.activity_slide_to_right);
                        }
                    }
                }
            });
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            super.onSaveInstanceState(outState);

            outState.putBoolean(KEY_SHOW_BACKUP_RESTORE_DIALOG, mShowBackupRestoreDialog);
        }

        @Override
        public void onResume() {
            super.onResume();

            if (BackupAndRestoreUtils.appNeedsRestart && (mRestartDialog == null || !mRestartDialog.isShowing())) {
                mRestartDialog = restartAppDialog(requireContext().getApplicationContext(), false);
                mRestartDialog.show();
            } else if (mShowBackupRestoreDialog && (mActiveDialog == null || !mActiveDialog.isShowing())) {
                showBackupRestoreDialog();
            }

            updateSettingsVisibility();

            displayWarningIfEssentialPermissionAreNotGranted();
        }

        @Override
        public void onDestroyView() {
            if (mRestartDialog != null && mRestartDialog.isShowing()) {
                mRestartDialog.dismiss();
                mRestartDialog = null;
            }

            super.onDestroyView();
        }

        @Override
        public void onDestroy() {
            nullifyPreferenceListeners(mInterfaceCustomizationPref, mClockSettingsPref, mAlarmSettingsPref, mTimerSettingsPref,
                mStopwatchSettingsPref, mScreensaverSettings, mWidgetsSettings, mPermissionsManagement, mPermissionMessage,
                mBackupRestorePref);

            nullifyAllPrefs();

            super.onDestroy();
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference pref) {
            switch (pref.getKey()) {
                case KEY_INTERFACE_CUSTOMIZATION -> animateAndShowFragment(new InterfaceCustomizationFragment());

                case KEY_CLOCK_SETTINGS -> animateAndShowFragment(new ClockSettingsFragment());

                case KEY_ALARM_SETTINGS -> animateAndShowFragment(new AlarmSettingsFragment());

                case KEY_TIMER_SETTINGS -> animateAndShowFragment(new TimerSettingsFragment());

                case KEY_STOPWATCH_SETTINGS -> animateAndShowFragment(new StopwatchSettingsFragment());

                case KEY_SCREENSAVER_SETTINGS -> animateAndShowFragment(new ScreensaverSettingsActivity.ScreensaverSettingsFragment());

                case KEY_WIDGETS_SETTINGS -> animateAndShowFragment(new WidgetSettingsFragment());

                case KEY_PERMISSION_MESSAGE, KEY_PERMISSIONS_MANAGEMENT ->
                    animateAndShowFragment(new PermissionsManagementActivity.PermissionsManagementFragment());

                case KEY_BACKUP_RESTORE_PREFERENCES -> showBackupRestoreDialog();
            }

            return true;
        }

        private void setupPreferences() {
            mPermissionMessage.setVisible(PermissionUtils.areEssentialPermissionsNotGranted(requireContext()));

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

        private void updateSettingsVisibility() {
            mClockSettingsPref.setVisible(SettingsDAO.isClockTabVisible(mPrefs));
            mAlarmSettingsPref.setVisible(SettingsDAO.isAlarmTabVisible(mPrefs));
            mTimerSettingsPref.setVisible(SettingsDAO.isTimerTabVisible(mPrefs));
            mStopwatchSettingsPref.setVisible(SettingsDAO.isStopwatchTabVisible(mPrefs));
        }

        private void displayWarningIfEssentialPermissionAreNotGranted() {
            if (PermissionUtils.areEssentialPermissionsNotGranted(requireContext())) {
                mPermissionMessage.setVisible(true);
                final SpannableStringBuilder builderPermissionMessage = new SpannableStringBuilder();
                final String messagePermission = requireContext().getString(R.string.settings_permission_message);
                final Spannable spannableMessagePermission = new SpannableString(messagePermission);
                spannableMessagePermission.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.colorAlert)),
                    0, messagePermission.length(), 0);
                spannableMessagePermission.setSpan(new StyleSpan(Typeface.BOLD), 0, messagePermission.length(), 0);
                builderPermissionMessage.append(spannableMessagePermission);
                mPermissionMessage.setTitle(builderPermissionMessage);
                mPermissionMessage.setOnPreferenceClickListener(this);
            } else {
                mPermissionMessage.setVisible(false);
            }
        }

        private void showBackupRestoreDialog() {
            mShowBackupRestoreDialog = true;

            mActiveDialog = CustomDialog.create(
                requireContext(),
                null,
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_backup_restore),
                getString(R.string.backup_restore_title),
                getString(R.string.backup_restore_dialog_message),
                null,
                getString(android.R.string.cancel),
                null,
                getString(R.string.backup_button_title),
                (d, w) -> {
                    String currentDateAndTime = DateFormat.format("yyyy_MM_dd_HH-mm-ss", new Date()).toString();
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_TITLE, requireContext().getString(R.string.app_label)
                            + "_backup_" + currentDateAndTime + ".zip")
                        .setType("application/zip");
                    backupToFile.launch(intent);
                },
                getString(R.string.restore_button_title),
                (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/zip");
                    restoreFromFile.launch(intent);
                },
                (alertDialog -> alertDialog.setOnDismissListener(d -> mShowBackupRestoreDialog = false)),
                CustomDialog.SoftInputMode.NONE
            );

            mActiveDialog.show();
        }

        private void backupPreferences(Context context, Uri uri) throws IOException, JSONException {
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                 ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                // The JSON file that contains all the settings
                ZipEntry jsonEntry = new ZipEntry(BACKUP_JSON_FILE_NAME);
                zipOutputStream.putNextEntry(jsonEntry);

                BackupAndRestoreUtils.settingsToJsonStream(context, mPrefs, mPrefs.getAll(), zipOutputStream);

                zipOutputStream.closeEntry();

                // Other files for the general font, alarm font, alarm background image, etc
                appendFileToZip(zipOutputStream, mPrefs.getString(KEY_GENERAL_FONT, null));
                appendFileToZip(zipOutputStream, mPrefs.getString(KEY_ALARM_FONT, null));
                appendFileToZip(zipOutputStream, mPrefs.getString(KEY_ALARM_BACKGROUND_IMAGE, null));
                appendFileToZip(zipOutputStream, mPrefs.getString(KEY_TIMER_DURATION_FONT, null));
                appendFileToZip(zipOutputStream, mPrefs.getString(KEY_TIMER_BACKGROUND_IMAGE, null));
                appendFileToZip(zipOutputStream, mPrefs.getString(KEY_SW_FONT, null));
                appendFileToZip(zipOutputStream, mPrefs.getString(KEY_DIGITAL_CLOCK_FONT, null));
                appendFileToZip(zipOutputStream, mPrefs.getString(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, null));
                appendFileToZip(zipOutputStream, mPrefs.getString(KEY_SCREENSAVER_BACKGROUND_IMAGE, null));
            }
        }

        private void appendFileToZip(ZipOutputStream zipOutputStream, String filePath) throws IOException {
            if (filePath == null) {
                return;
            }

            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }

            ZipEntry zipEntry = new ZipEntry(file.getName());
            zipOutputStream.putNextEntry(zipEntry);

            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = fileInputStream.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, length);
                }
            }

            zipOutputStream.closeEntry();
        }

        @SuppressLint("ApplySharedPref")
        private void wipeCustomMediaBeforeRestore(Context context) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.remove(KEY_GENERAL_FONT);
            editor.remove(KEY_ALARM_FONT);
            editor.remove(KEY_ALARM_BACKGROUND_IMAGE);
            editor.remove(KEY_TIMER_DURATION_FONT);
            editor.remove(KEY_TIMER_BACKGROUND_IMAGE);
            editor.remove(KEY_SW_FONT);
            editor.remove(KEY_DIGITAL_CLOCK_FONT);
            editor.remove(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT);
            editor.remove(KEY_SCREENSAVER_BACKGROUND_IMAGE);
            editor.commit();

            wipeAllCustomFiles(context);
        }

        private void restorePreferences(Context context, Uri uri) throws IOException, JSONException {
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

                ZipEntry zipEntry;

                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    if (zipEntry.getName().equals(BACKUP_JSON_FILE_NAME)) {
                        BackupAndRestoreUtils.readJson(context, mPrefs, zipInputStream);
                    } else {
                        restoreFileFromZip(context, zipInputStream, zipEntry.getName());
                    }

                    zipInputStream.closeEntry();
                }
            }
        }

        private void restoreFileFromZip(Context context, ZipInputStream zipInputStream, String fileName)
            throws IOException {

            File outputFile = new File(context.getFilesDir(), fileName);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = zipInputStream.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
            }

            final String prefKey = getCustomFilePrefKey(fileName);

            if (prefKey != null) {
                String oldFilePath = mPrefs.getString(prefKey, null);

                if (oldFilePath != null && !oldFilePath.equals(outputFile.getAbsolutePath())) {
                    clearFile(oldFilePath);
                }

                mPrefs.edit().putString(prefKey, outputFile.getAbsolutePath()).apply();
            }
        }

        private void applySettingsAfterRestore(Context context) {
            // Required to update Locale.
            context.sendBroadcast(new Intent(ACTION_LANGUAGE_CODE_CHANGED));

            // Required to update widgets.
            WidgetUtils.updateAllWidgets(context);

            // Required to update the timer list.
            DataModel.getDataModel().loadTimers();

            // Required to update the tab to display.
            if (SettingsDAO.getTabToDisplay(mPrefs) != -1) {
                UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.values()[SettingsDAO.getTabToDisplay(mPrefs)]);
            }

            // Required to start/stop the foreground notification
            if (SettingsDAO.isForegroundServiceEnabled(mPrefs)) {
                Utils.startService(context, KeepAliveService.class);
            } else {
                Utils.stopService(context, KeepAliveService.class);
            }

            // Required to update the tiles
            if (SdkUtils.isAtLeastAndroid7()) {
                TileService.requestListeningState(context, new ComponentName(context, AlarmTileService.class));
                TileService.requestListeningState(context, new ComponentName(context, TimerTileService.class));
                TileService.requestListeningState(context, new ComponentName(context, StopwatchTileService.class));
            }
        }

        private void nullifyAllPrefs() {
            mInterfaceCustomizationPref = null;
            mClockSettingsPref = null;
            mAlarmSettingsPref = null;
            mTimerSettingsPref = null;
            mStopwatchSettingsPref = null;
            mScreensaverSettings = null;
            mWidgetsSettings = null;
            mPermissionsManagement = null;
            mPermissionMessage = null;
            mBackupRestorePref = null;
        }
    }

}
