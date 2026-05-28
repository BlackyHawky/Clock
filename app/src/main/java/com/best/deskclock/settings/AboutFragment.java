// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.data.CustomRingtoneDAO.RINGTONE_IDS;
import static com.best.deskclock.data.CustomRingtoneDAO.RINGTONE_URI;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEBUG_LANGUAGE_CODE;
import static com.best.deskclock.settings.PreferencesDefaultValues.PURPLE_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesDefaultValues.RED_ACCENT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.*;
import static com.best.deskclock.setup.FirstLaunch.KEY_IS_FIRST_LAUNCH;
import static com.best.deskclock.utils.Utils.ACTION_LANGUAGE_CODE_CHANGED;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.MenuProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.BuildConfig;
import com.best.deskclock.DeskClock;
import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmStateManager;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.base.KeepAliveService;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.settings.custompreference.CustomAboutTitlePreference;
import com.best.deskclock.tiles.AlarmTileService;
import com.best.deskclock.tiles.StopwatchTileService;
import com.best.deskclock.tiles.TimerTileService;
import com.best.deskclock.uicomponents.CustomDialog;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.BackupAndRestoreUtils;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.NotificationUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AboutFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String KEY_SHOW_RESET_SETTINGS_DIALOG = "show_reset_settings_dialog";
    private static final String KEY_PENDING_LINK_DIALOG = "pending_link_dialog";
    private static final String KEY_SHOW_KEEP_ANDROID_OPEN_DIALOG = "show_keep_android_open_dialog";
    private static final String KEY_SHOW_EXPORT_COMPLETE_DIALOG = "show_export_complete_dialog";

    private boolean mShowResetSettingsDialog = false;
    private String mPendingLinkDialogPrefKey = null;
    private boolean mShowKeepAndroidOpenDialog = false;
    private boolean mShowExportCompleteDialog = false;

    /**
     * Callback to get the log export result.
     */
    private final ActivityResultLauncher<Intent> exportLogs = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != Activity.RESULT_OK) {
                return;
            }

            Uri uri = result.getData() != null ? result.getData().getData() : null;
            if (uri == null) {
                return;
            }

            final Context appContext = requireContext().getApplicationContext();

            AppExecutors.getDiskIO().execute(() -> {
                exportLogsAsZip(appContext, uri);

                boolean hasLogs = !LogUtils.getSavedLocalLogs(appContext).isEmpty();

                AppExecutors.getMainThread().post(() -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (hasLogs) {
                        showExportCompleteDialog();
                    } else {
                        CustomToast.show(appContext, R.string.toast_message_for_backup);
                    }
                });
            });
        });

    CustomAboutTitlePreference mTitlePref;
    Preference mVersionPref;
    Preference mWhatsNewPref;
    Preference mAboutFeaturesPref;
    Preference mViewOnGitHubPref;
    Preference mTranslatePref;
    Preference mReadLicencePref;
    Preference mKeepAndroidOpenPref;
    PreferenceCategory mDebugCategoryPref;
    SwitchPreferenceCompat mEnableLocalLoggingPref;

    private AlertDialog mRestartDialog;

    /**
     * Used only for release versions.
     */
    int tapCountOnVersion = 0;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.about_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_about);

        mTitlePref = findPreference(KEY_ABOUT_TITLE);
        mVersionPref = findPreference(KEY_ABOUT_VERSION);
        mWhatsNewPref = findPreference(KEY_ABOUT_WHATS_NEW);
        mAboutFeaturesPref = findPreference(KEY_ABOUT_FEATURES);
        mViewOnGitHubPref = findPreference(KEY_ABOUT_VIEW_ON_GITHUB);
        mTranslatePref = findPreference(KEY_ABOUT_TRANSLATE);
        mReadLicencePref = findPreference(KEY_ABOUT_READ_LICENCE);
        mKeepAndroidOpenPref = findPreference(KEY_ABOUT_KEEP_ANDROID_OPEN);
        mDebugCategoryPref = findPreference(KEY_DEBUG_CATEGORY);
        mEnableLocalLoggingPref = findPreference(KEY_ENABLE_LOCAL_LOGGING);

        if (savedInstanceState != null) {
            mShowResetSettingsDialog = savedInstanceState.getBoolean(KEY_SHOW_RESET_SETTINGS_DIALOG, false);
            mPendingLinkDialogPrefKey = savedInstanceState.getString(KEY_PENDING_LINK_DIALOG);
            mShowKeepAndroidOpenDialog = savedInstanceState.getBoolean(KEY_SHOW_KEEP_ANDROID_OPEN_DIALOG, false);
            mShowExportCompleteDialog = savedInstanceState.getBoolean(KEY_SHOW_EXPORT_COMPLETE_DIALOG, false);
        }

        setupPreferences();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_SHOW_RESET_SETTINGS_DIALOG, mShowResetSettingsDialog);
        outState.putString(KEY_PENDING_LINK_DIALOG, mPendingLinkDialogPrefKey);
        outState.putBoolean(KEY_SHOW_KEEP_ANDROID_OPEN_DIALOG, mShowKeepAndroidOpenDialog);
        outState.putBoolean(KEY_SHOW_EXPORT_COMPLETE_DIALOG, mShowExportCompleteDialog);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().addMenuProvider(new MenuProvider() {
            @SuppressLint("AlwaysShowAction")
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();

                if (BuildConfig.DEBUG || SettingsDAO.isDebugSettingsDisplayed(mPrefs)) {
                    menu.add(0, MENU_BUG_REPORT, 0, R.string.log_backup_icon_title)
                        .setIcon(R.drawable.ic_bug_report)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }

                menu.add(0, MENU_RESET_SETTINGS, 0, R.string.reset_settings_title)
                    .setIcon(R.drawable.ic_reset_settings)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == MENU_BUG_REPORT) {
                    String currentDateAndTime = DateFormat.format("yyyy_MM_dd_HH-mm-ss", new Date()).toString();

                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .putExtra(Intent.EXTRA_TITLE, getString(R.string.app_label_debug)
                            .replace(" ", "_") + "_log_" + currentDateAndTime)
                        .setType("application/zip");

                    exportLogs.launch(intent);
                    return true;
                } else if (item.getItemId() == MENU_RESET_SETTINGS) {
                    showResetSettingsDialog();
                    return true;
                }

                return false;
            }
        }, getViewLifecycleOwner());
    }

    @Override
    public void onResume() {
        super.onResume();

        if (BackupAndRestoreUtils.appNeedsRestart) {
            if (mRestartDialog == null || !mRestartDialog.isShowing()) {
                mRestartDialog = restartAppDialog(requireContext().getApplicationContext(), false);
                mRestartDialog.show();
            }
        } else if (mShowResetSettingsDialog && (mActiveDialog == null || !mActiveDialog.isShowing())) {
            showResetSettingsDialog();
        } else if (mPendingLinkDialogPrefKey != null && (mActiveDialog == null || !mActiveDialog.isShowing())) {
            triggerLinkDialog(mPendingLinkDialogPrefKey);
        } else if (mShowKeepAndroidOpenDialog && (mActiveDialog == null || !mActiveDialog.isShowing())) {
            showKeepAndroidOpenDialog();
        } else if (mShowExportCompleteDialog && (mActiveDialog == null || !mActiveDialog.isShowing())) {
            showExportCompleteDialog();
        }
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
        nullifyPreferenceListeners(mTitlePref, mVersionPref, mWhatsNewPref, mAboutFeaturesPref, mViewOnGitHubPref, mTranslatePref,
            mReadLicencePref, mKeepAndroidOpenPref, mDebugCategoryPref, mEnableLocalLoggingPref
        );

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            // Used only for release versions.
            case KEY_ABOUT_VERSION -> {
                tapCountOnVersion++;

                if (tapCountOnVersion == 5) {
                    mPrefs.edit().putBoolean(KEY_DISPLAY_DEBUG_SETTINGS, true).apply();
                    mPrefs.edit().putBoolean(KEY_ENABLE_LOCAL_LOGGING, true).apply();

                    CustomToast.show(requireContext().getApplicationContext(), R.string.toast_message_debug_displayed);
                    requireActivity().recreate();
                }
            }

            case KEY_ABOUT_WHATS_NEW, KEY_ABOUT_FEATURES, KEY_ABOUT_VIEW_ON_GITHUB, KEY_ABOUT_TRANSLATE, KEY_ABOUT_READ_LICENCE ->
                triggerLinkDialog(preference.getKey());

            case KEY_ABOUT_KEEP_ANDROID_OPEN -> showKeepAndroidOpenDialog();
        }

        return true;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (KEY_ENABLE_LOCAL_LOGGING.equals(preference.getKey())) {
            if (newValue.equals(false)) {
                tapCountOnVersion = 0;

                LogUtils.clearSavedLocalLogs(requireContext());

                mPrefs.edit().putBoolean(KEY_DISPLAY_DEBUG_SETTINGS, false).apply();

                CustomToast.show(requireContext().getApplicationContext(), R.string.toast_message_debug_hidden);
            }

            requireActivity().recreate();

            Utils.setVibrationTime(requireContext(), 50);
        }

        return true;
    }

    private void showResetSettingsDialog() {
        mShowResetSettingsDialog = true;

        mActiveDialog = CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_reset_settings),
            getString(R.string.reset_settings_title),
            getString(R.string.reset_settings_message),
            null,
            getString(android.R.string.ok),
            (d, w) -> resetPreferences(),
            getString(android.R.string.cancel),
            null,
            null,
            null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> mShowResetSettingsDialog = false)),
            CustomDialog.SoftInputMode.NONE
        );

        mActiveDialog.show();
    }

    private void triggerLinkDialog(String prefKey) {
        mPendingLinkDialogPrefKey = prefKey;

        int iconId, titleId, messageId;
        String link;

        switch (prefKey) {
            case KEY_ABOUT_WHATS_NEW -> {
                String version = BuildConfig.VERSION_NAME;
                if (BuildConfig.IS_DEBUG_BUILD) {
                    version = version.replace("-debug", "");
                } else if (BuildConfig.IS_NIGHTLY_BUILD) {
                    version = version.replace(BuildConfig.VERSION_NAME, "nightly" + "-" + BuildConfig.COMMIT_NUMBER);
                }
                link = "https://github.com/BlackyHawky/Clock/releases/tag/" + version;
                iconId = R.drawable.ic_about_update;
                titleId = R.string.whats_new_title;
                messageId = R.string.whats_new_dialog_message;
            }
            case KEY_ABOUT_FEATURES -> {
                link = "https://github.com/BlackyHawky/Clock?tab=readme-ov-file#-features";
                iconId = R.drawable.ic_about_features;
                titleId = R.string.features_title;
                messageId = R.string.features_dialog_message;
            }
            case KEY_ABOUT_VIEW_ON_GITHUB -> {
                link = "https://github.com/BlackyHawky/Clock";
                iconId = R.drawable.ic_about_github;
                titleId = R.string.about_github_link;
                messageId = R.string.github_dialog_message;
            }
            case KEY_ABOUT_TRANSLATE -> {
                link = "https://translate.codeberg.org/projects/clock";
                iconId = R.drawable.ic_about_translate;
                titleId = R.string.about_translate_link;
                messageId = R.string.translate_dialog_message;
            }
            case KEY_ABOUT_READ_LICENCE -> {
                link = "https://github.com/BlackyHawky/Clock/blob/main/LICENSE";
                iconId = R.drawable.ic_about_license;
                titleId = R.string.license;
                messageId = R.string.license_dialog_message;
            }
            default -> {
                mPendingLinkDialogPrefKey = null;
                return;
            }
        }

        displayLinkDialog(iconId, titleId, messageId, link);
    }

    private void displayLinkDialog(int iconId, int titleId, int messageId, String link) {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));

        mActiveDialog = CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), iconId),
            getString(titleId),
            getString(messageId, link),
            null,
            getString(android.R.string.ok),
            (d, w) -> startActivity(browserIntent),
            getString(android.R.string.cancel),
            null,
            null,
            null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> mPendingLinkDialogPrefKey = null)),
            CustomDialog.SoftInputMode.NONE
        );

        mActiveDialog.show();
    }

    private void showKeepAndroidOpenDialog() {
        mShowKeepAndroidOpenDialog = true;

        mActiveDialog = Utils.displayKeepAndroidOpenDialog(requireContext(), mPrefs, true);

        mActiveDialog.setOnDismissListener(d -> mShowKeepAndroidOpenDialog = false);

        mActiveDialog.show();
    }

    private void setupPreferences() {
        mTitlePref.setTitle(Utils.getStringResByBuildType(
            R.string.app_label, R.string.app_label_debug, R.string.app_label_nightly)
        );

        if (BuildConfig.IS_DEBUG_BUILD) {
            mVersionPref.setSelectable(false);
            mVersionPref.setOnPreferenceClickListener(null);
        } else {
            mVersionPref.setSelectable(true);
            mVersionPref.setOnPreferenceClickListener(this);
        }
        mVersionPref.setSummary(BuildConfig.VERSION_NAME);
        mWhatsNewPref.setOnPreferenceClickListener(this);
        mAboutFeaturesPref.setOnPreferenceClickListener(this);
        mViewOnGitHubPref.setOnPreferenceClickListener(this);
        mTranslatePref.setOnPreferenceClickListener(this);
        mReadLicencePref.setOnPreferenceClickListener(this);
        mKeepAndroidOpenPref.setOnPreferenceClickListener(this);

        mDebugCategoryPref.setVisible(SettingsDAO.isDebugSettingsDisplayed(mPrefs));
        mEnableLocalLoggingPref.setVisible(SettingsDAO.isDebugSettingsDisplayed(mPrefs));
        mEnableLocalLoggingPref.setOnPreferenceChangeListener(this);
    }

    /**
     * Resets the application to a clean state by removing user preferences,
     * deleting custom ringtone data, clearing cached files, and resetting
     * various runtime models.
     *
     * <p>This method performs the following steps:
     * <ul>
     *   <li>Removes all SharedPreferences except essential keys.</li>
     *   <li>Releases SAF persistable permissions for custom ringtones.</li>
     *   <li>Deletes all custom ringtone audio files stored internally.</li>
     *   <li>Removes all ringtone-related preference entries.</li>
     *   <li>Deletes imported fonts and background images.</li>
     *   <li>Clears the in-memory list of custom ringtones.</li>
     *   <li>Resets logs, widgets, timers, alarms, and UI state.</li>
     * </ul>
     * </p>
     */
    @SuppressLint("ApplySharedPref")
    private void resetPreferences() {
        final Context appContext = requireContext().getApplicationContext();

        BackupAndRestoreUtils.isRestoringBackupOrIsResettingApp = true;

        AppExecutors.getDiskIO().execute(() -> {
            SharedPreferences.Editor editor = mPrefs.edit();
            Map<String, ?> settings = mPrefs.getAll();

            releaseAllCustomRingtonePermissions();
            deleteAllCustomRingtoneFiles();

            wipeAllCustomFiles(appContext);

            LogUtils.clearSavedLocalLogs(appContext);

            final List<Alarm> alarms = Alarm.getAlarms(appContext.getContentResolver(), null);
            for (Alarm alarm : alarms) {
                AlarmStateManager.deleteAllInstances(appContext, alarm.id);
                Alarm.deleteAlarm(appContext.getContentResolver(), alarm.id);
            }

            for (Map.Entry<String, ?> entry : settings.entrySet()) {
                String key = entry.getKey();
                if (!key.equals(KEY_IS_FIRST_LAUNCH)
                    && !key.equals(KEY_ESSENTIAL_PERMISSIONS_GRANTED)
                    && !key.equals(KEY_DISPLAY_KEEP_ANDROID_OPEN_DIALOG)) {
                    editor.remove(key);
                }
            }

            applyDebugAndNightlyDefaults(editor);

            editor.commit();

            AppExecutors.getMainThread().post(() -> {
                Utils.stopService(appContext, KeepAliveService.class);

                appContext.sendBroadcast(new Intent(ACTION_LANGUAGE_CODE_CHANGED));

                WidgetUtils.updateAllWidgets(appContext);

                if (SdkUtils.isAtLeastAndroid7()) {
                    TileService.requestListeningState(appContext, new ComponentName(appContext, AlarmTileService.class));
                    TileService.requestListeningState(appContext, new ComponentName(appContext, TimerTileService.class));
                    TileService.requestListeningState(appContext, new ComponentName(appContext, StopwatchTileService.class));
                }

                BackupAndRestoreUtils.appNeedsRestart = true;

                if (isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                    mRestartDialog = restartAppDialog(appContext, true);
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
        });
    }

    /**
     * Applies the default accent color and locale for debug and nightly builds.
     */
    private void applyDebugAndNightlyDefaults(SharedPreferences.Editor editor) {
        if (BuildConfig.IS_DEBUG_BUILD) {
            editor.putString(KEY_ACCENT_COLOR, RED_ACCENT_COLOR);
        } else if (BuildConfig.IS_NIGHTLY_BUILD) {
            editor.putString(KEY_ACCENT_COLOR, PURPLE_ACCENT_COLOR);
        }

        if (BuildConfig.IS_DEBUG_BUILD || BuildConfig.IS_NIGHTLY_BUILD) {
            editor.putString(KEY_CUSTOM_LANGUAGE_CODE, DEBUG_LANGUAGE_CODE);
        }
    }

    /**
     * Deletes all custom ringtone audio files stored in the app's internal storage.
     *
     * <p>The method reads the list of ringtone IDs from SharedPreferences,
     * retrieves the associated file paths, and removes each file if present.</p>
     */
    private void deleteAllCustomRingtoneFiles() {
        Set<String> ids = mPrefs.getStringSet(RINGTONE_IDS, Collections.emptySet());

        for (String id : ids) {
            String uriString = mPrefs.getString(RINGTONE_URI + id, null);
            if (uriString != null) {
                Uri uri = Uri.parse(uriString);
                clearFile(uri.getPath());
            }
        }
    }

    /**
     * Releases all persistable SAF read permissions previously granted
     * for custom ringtone URIs.
     *
     * <p>This ensures that the app no longer holds long-term access to
     * external audio files selected through the Storage Access Framework.</p>
     */
    private void releaseAllCustomRingtonePermissions() {
        ContentResolver contentResolver = requireContext().getContentResolver();
        Set<String> ids = mPrefs.getStringSet(RINGTONE_IDS, Collections.emptySet());

        for (String id : ids) {
            String uriString = mPrefs.getString(RINGTONE_URI + id, null);
            if (uriString != null) {
                try {
                    contentResolver.releasePersistableUriPermission(Uri.parse(uriString), Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignore) {
                    LogUtils.w("Unable to release permission for " + uriString);
                }
            }
        }
    }

    /**
     * Export the application logs to a ZIP archive containing two files:
     * - logcat_logs.txt: system logs retrieved via logcat
     * - local_logs.txt: custom logs saved via LogUtils
     */
    private void exportLogsAsZip(Context context, Uri zipUri) {
        try {
            // Temp files
            File logcatFile = new File(context.getCacheDir(), "logcat_logs.txt");
            File localLogFile = new File(context.getCacheDir(), "local_logs.txt");

            // 1. Save Logcat logs
            Process process = Runtime.getRuntime().exec("logcat -d -v threadtime");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            FileWriter logcatWriter = new FileWriter(logcatFile);
            String line;
            while ((line = reader.readLine()) != null) {
                logcatWriter.write(line + "\n");
            }
            reader.close();
            logcatWriter.close();

            // 2. Save LogUtils logs
            String logsWithHeader = LogUtils.generateLocalLogFileHeader() + LogUtils.getSavedLocalLogs(context);
            FileWriter localLogWriter = new FileWriter(localLogFile);
            localLogWriter.write(logsWithHeader);
            localLogWriter.close();

            // 3. Write both files into the zip
            OutputStream outputStream = context.getContentResolver().openOutputStream(zipUri);
            if (outputStream == null) {
                return;
            }

            ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream));

            addFileToZip(logcatFile, "logcat_logs.txt", zipOut);
            addFileToZip(localLogFile, "local_logs.txt", zipOut);

            zipOut.close();

            if (!logcatFile.delete()) {
                LogUtils.w("Failed to delete temporary logcat file: " + logcatFile.getAbsolutePath());
            }
            if (!localLogFile.delete()) {
                LogUtils.w("Failed to delete temporary local log file: " + localLogFile.getAbsolutePath());
            }

        } catch (IOException e) {
            LogUtils.e("Error exporting logs", e);
        }
    }

    /**
     * Helper method to add a given file into a ZIP archive under a specific entry name.
     */
    private void addFileToZip(File file, String entryName, ZipOutputStream zipOut) throws IOException {
        byte[] buffer = new byte[1024];
        FileInputStream fileInputStream = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(entryName);
        zipOut.putNextEntry(zipEntry);
        int length;
        while ((length = fileInputStream.read(buffer)) > 0) {
            zipOut.write(buffer, 0, length);
        }
        zipOut.closeEntry();
        fileInputStream.close();
    }

    /**
     * Inform that the log export was successful and allow it to delete local log after export.
     */
    private void showExportCompleteDialog() {
        mShowExportCompleteDialog = true;

        final Context appContext = requireContext().getApplicationContext();

        mActiveDialog = CustomDialog.create(
            requireContext(),
            null,
            AppCompatResources.getDrawable(requireContext(), R.drawable.ic_bug_report),
            getString(R.string.log_dialog_title),
            getString(R.string.log_dialog_message),
            null,
            getString(android.R.string.ok),
            (d, w) -> AppExecutors.getDiskIO().execute(() -> {
                LogUtils.clearSavedLocalLogs(appContext);

                AppExecutors.getMainThread().post(() -> CustomToast.show(appContext, R.string.toast_message_log_deleted));
            }),
            getString(android.R.string.cancel),
            null,
            null,
            null,
            (alertDialog -> alertDialog.setOnDismissListener(d -> mShowExportCompleteDialog = false)),
            CustomDialog.SoftInputMode.NONE
        );

        mActiveDialog.show();
    }

    private void nullifyAllPrefs() {
        mTitlePref = null;
        mVersionPref = null;
        mWhatsNewPref = null;
        mAboutFeaturesPref = null;
        mViewOnGitHubPref = null;
        mTranslatePref = null;
        mReadLicencePref = null;
        mKeepAndroidOpenPref = null;
        mDebugCategoryPref = null;
        mEnableLocalLoggingPref = null;
    }

}
