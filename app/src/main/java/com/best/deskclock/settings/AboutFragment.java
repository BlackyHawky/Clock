// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;

import static com.best.deskclock.FirstLaunch.KEY_IS_FIRST_LAUNCH;
import static com.best.deskclock.data.CustomRingtoneDAO.NEXT_RINGTONE_ID;
import static com.best.deskclock.data.CustomRingtoneDAO.RINGTONE_IDS;
import static com.best.deskclock.data.CustomRingtoneDAO.RINGTONE_TITLE;
import static com.best.deskclock.data.CustomRingtoneDAO.RINGTONE_URI;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_BLACKYHAWKY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_CRDROID;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_FEATURES;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_LINEAGEOS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_NILSU11;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_ODMFL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_QW123WH;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_READ_LICENCE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_TITLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_TRANSLATE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_VERSION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_VIEW_ON_GITHUB;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ABOUT_WHATS_NEW;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DEBUG_CATEGORY;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_DEBUG_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_LOCAL_LOGGING;
import static com.best.deskclock.utils.Utils.ACTION_LANGUAGE_CODE_CHANGED;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.BuildConfig;
import com.best.deskclock.R;
import com.best.deskclock.alarms.AlarmStateManager;
import com.best.deskclock.controller.ThemeController;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.provider.Alarm;
import com.best.deskclock.uidata.UiDataModel;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AboutFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

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

                exportLogsAsZip(requireContext(), uri);

                if (!LogUtils.getSavedLocalLogs(requireContext()).isEmpty()) {
                    displayExportCompleteDialog();
                } else {
                    Toast.makeText(requireContext(), requireContext().getString(
                            R.string.toast_message_for_backup), Toast.LENGTH_SHORT).show();
                }
            });

    Preference mTitlePref;
    Preference mVersionPref;
    Preference mWhatsNewPreference;
    Preference mAboutFeatures;
    Preference mViewOnGitHub;
    Preference mTranslate;
    Preference mReadLicence;
    Preference mContributor1;
    Preference mContributor2;
    Preference mContributor3;
    Preference mContributor4;
    Preference mCredit1;
    Preference mCredit2;
    PreferenceCategory mDebugCategoryPref;
    SwitchPreferenceCompat mEnableLocalLoggingPref;

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
        mWhatsNewPreference = findPreference(KEY_ABOUT_WHATS_NEW);
        mAboutFeatures = findPreference(KEY_ABOUT_FEATURES);
        mViewOnGitHub = findPreference(KEY_ABOUT_VIEW_ON_GITHUB);
        mTranslate = findPreference(KEY_ABOUT_TRANSLATE);
        mReadLicence = findPreference(KEY_ABOUT_READ_LICENCE);
        mContributor1 = findPreference(KEY_ABOUT_BLACKYHAWKY);
        mContributor2 = findPreference(KEY_ABOUT_QW123WH);
        mContributor3 = findPreference(KEY_ABOUT_ODMFL);
        mContributor4 = findPreference(KEY_ABOUT_NILSU11);
        mCredit1 = findPreference(KEY_ABOUT_LINEAGEOS);
        mCredit2 = findPreference(KEY_ABOUT_CRDROID);
        mDebugCategoryPref = findPreference(KEY_DEBUG_CATEGORY);
        mEnableLocalLoggingPref = findPreference(KEY_ENABLE_LOCAL_LOGGING);

        setupPreferences();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (Utils.isDebugConfig() || SettingsDAO.isDebugSettingsDisplayed(mPrefs)) {
            menu.add(0, 1, 0, R.string.log_backup_icon_title)
                    .setIcon(R.drawable.ic_bug_report)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        menu.add(0, 2, 0, R.string.reset_settings_title)
                .setIcon(R.drawable.ic_reset_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            String currentDateAndTime = DateFormat.format("yyyy_MM_dd_HH-mm-ss", new Date()).toString();

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .putExtra(Intent.EXTRA_TITLE, getString(R.string.app_label_debug)
                            .replace(" ", "_") + "_log_" + currentDateAndTime)
                    .setType("application/zip");

            exportLogs.launch(intent);

            return true;
        } else if (item.getItemId() == 2) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setIcon(R.drawable.ic_reset_settings)
                    .setTitle(R.string.reset_settings_title)
                    .setMessage(R.string.reset_settings_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> resetPreferences())
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();

            return true;
        }

        return super.onOptionsItemSelected(item);
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
                    Toast.makeText(requireContext(), R.string.toast_message_debug_displayed, Toast.LENGTH_SHORT).show();
                    recreateActivity();
                }
            }

            case KEY_ABOUT_WHATS_NEW -> {
                String version = BuildConfig.VERSION_NAME;
                if (Utils.isDebugConfig()) {
                    version = version.replace("-debug", "");
                }
                final String link = "https://github.com/BlackyHawky/Clock/releases/tag/" + version;
                displayLinkDialog(R.drawable.ic_about_update, R.string.whats_new_title, R.string.whats_new_dialog_message, link);
            }

            case KEY_ABOUT_FEATURES ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setIcon(R.drawable.ic_about_features)
                        .setTitle(R.string.features_title)
                        .setMessage(R.string.about_dialog_message)
                        .setPositiveButton(R.string.dialog_close, null)
                        .show();

            case KEY_ABOUT_VIEW_ON_GITHUB -> {
                final String link = "https://github.com/BlackyHawky/Clock";
                displayLinkDialog(R.drawable.ic_about_github, R.string.about_github_link, R.string.github_dialog_message, link);
            }

            case KEY_ABOUT_TRANSLATE -> {
                final String link = "https://translate.codeberg.org/projects/clock";
                displayLinkDialog(R.drawable.ic_about_translate, R.string.about_translate_link, R.string.translate_dialog_message, link);
            }

            case KEY_ABOUT_READ_LICENCE -> {
                final String link = "https://github.com/BlackyHawky/Clock/blob/main/LICENSE-GPL-3";
                displayLinkDialog(R.drawable.ic_about_license, R.string.license, R.string.license_dialog_message, link);
            }

            case KEY_ABOUT_BLACKYHAWKY ->
                displayContributorDialog(R.drawable.ic_person, "BlackyHawky", "https://github.com/BlackyHawky");

            case KEY_ABOUT_QW123WH ->
                displayContributorDialog(R.drawable.ic_person, "qw123wh", "https://github.com/qw123wh");

            case KEY_ABOUT_ODMFL ->
                displayContributorDialog(R.drawable.ic_person, "odmfl", "https://github.com/odmfl");

            case KEY_ABOUT_NILSU11 ->
                displayContributorDialog(R.drawable.ic_person, "Nilsu11", "https://github.com/Nilsu11");

            case KEY_ABOUT_LINEAGEOS ->
                displayContributorDialog(R.drawable.ic_groups, "LineageOS", "https://github.com/LineageOS");

            case KEY_ABOUT_CRDROID ->
                displayContributorDialog(R.drawable.ic_groups, "crDroid Android", "https://github.com/crdroidandroid");

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

                Toast.makeText(requireContext(), R.string.toast_message_debug_hidden, Toast.LENGTH_SHORT).show();
            }

            recreateActivity();

            Utils.setVibrationTime(requireContext(), 50);
        }

        return true;
    }

    private void displayLinkDialog(int iconId, int titleId, int messageId, String link) {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(iconId)
                .setTitle(titleId)
                .setMessage(requireContext().getString(messageId, link))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void displayContributorDialog(int iconId, String projectName, String url) {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(iconId)
                .setTitle(R.string.contributors_dialog_title)
                .setMessage(requireContext().getString(R.string.contributors_dialog_message, projectName, url))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupPreferences() {
        if (Utils.isDebugConfig()) {
            mTitlePref.setTitle(R.string.about_debug_app_title);
            mVersionPref.setSelectable(false);
            mVersionPref.setOnPreferenceClickListener(null);
        } else {
            mTitlePref.setTitle(R.string.app_label);
            mVersionPref.setSelectable(true);
            mVersionPref.setOnPreferenceClickListener(this);
        }
        mVersionPref.setSummary(BuildConfig.VERSION_NAME);
        mWhatsNewPreference.setOnPreferenceClickListener(this);
        mAboutFeatures.setOnPreferenceClickListener(this);
        mViewOnGitHub.setOnPreferenceClickListener(this);
        mTranslate.setOnPreferenceClickListener(this);
        mReadLicence.setOnPreferenceClickListener(this);
        mContributor1.setOnPreferenceClickListener(this);
        mContributor2.setOnPreferenceClickListener(this);
        mContributor3.setOnPreferenceClickListener(this);
        mContributor4.setOnPreferenceClickListener(this);
        mCredit1.setOnPreferenceClickListener(this);
        mCredit2.setOnPreferenceClickListener(this);

        mDebugCategoryPref.setVisible(SettingsDAO.isDebugSettingsDisplayed(mPrefs));
        mEnableLocalLoggingPref.setVisible(SettingsDAO.isDebugSettingsDisplayed(mPrefs));
        mEnableLocalLoggingPref.setOnPreferenceChangeListener(this);
    }

    private void resetPreferences() {
        Fragment settingsFragment =
                requireActivity().getSupportFragmentManager().findFragmentByTag(SettingsActivity.SettingsFragment.class.getSimpleName());

        if (settingsFragment == null) {
            animateAndShowFragment(new SettingsActivity.SettingsFragment());
        }

        // Adding a Handler ensures better fluidity for animations
        new Handler(requireContext().getMainLooper()).postDelayed(() -> {
            SharedPreferences.Editor editor = mPrefs.edit();
            Map<String, ?> settings = mPrefs.getAll();

            for (Map.Entry<String, ?> entry : settings.entrySet()) {
                // Do not reset the KEY_IS_FIRST_LAUNCH key to prevent the "FirstLaunch" activity from reappearing.
                // Also, exclude keys corresponding to custom ringtones as this causes bugs for alarms.
                if (!entry.getKey().equals(KEY_IS_FIRST_LAUNCH) &&
                        !entry.getKey().startsWith(RINGTONE_URI) &&
                        !entry.getKey().equals(RINGTONE_IDS) &&
                        !entry.getKey().equals(NEXT_RINGTONE_ID) &&
                        !entry.getKey().startsWith(RINGTONE_TITLE)) {
                    editor.remove(entry.getKey());
                }
            }
            editor.apply();

            tapCountOnVersion = 0;

            LogUtils.clearSavedLocalLogs(requireContext());

            // Required to update Locale.
            requireContext().sendBroadcast(new Intent(ACTION_LANGUAGE_CODE_CHANGED));
            // Required to update widgets.
            requireContext().sendBroadcast(new Intent(ACTION_APPWIDGET_UPDATE));
            // Required to update the timer list.
            DataModel.getDataModel().loadTimers();
            // Required to update the tab to display.
            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.CLOCKS);
            // Delete all alarms.
            final List<Alarm> alarms = Alarm.getAlarms(requireContext().getContentResolver(), null);
            for (Alarm alarm : alarms) {
                AlarmStateManager.deleteAllInstances(requireContext(), alarm.id);
                Alarm.deleteAlarm(requireContext().getContentResolver(), alarm.id);
            }

            ThemeController.setNewSettingWithDelay();

            Toast.makeText(requireContext(), requireContext().getString(R.string.toast_message_for_reset), Toast.LENGTH_SHORT).show();
        }, 500);
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
            Process process = Runtime.getRuntime().exec("logcat -d -b all");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            FileWriter logcatWriter = new FileWriter(logcatFile);
            String line;
            while ((line = reader.readLine()) != null) {
                logcatWriter.write(line + "\n");
            }
            reader.close();
            logcatWriter.close();

            // 2. Save LogUtils logs
            FileWriter localLogWriter = new FileWriter(localLogFile);
            localLogWriter.write(LogUtils.getSavedLocalLogs(context));
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
    private void displayExportCompleteDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_bug_report)
                .setTitle(R.string.log_dialog_title)
                .setMessage(requireContext().getString(R.string.log_dialog_message))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    LogUtils.clearSavedLocalLogs(requireContext());
                    Toast.makeText(requireContext(), requireContext().getString(
                            R.string.toast_message_log_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

}
