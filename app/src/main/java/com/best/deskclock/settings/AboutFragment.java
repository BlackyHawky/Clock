// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;

import static com.best.deskclock.FirstLaunch.KEY_IS_FIRST_LAUNCH;
import static com.best.deskclock.data.CustomRingtoneDAO.NEXT_RINGTONE_ID;
import static com.best.deskclock.data.CustomRingtoneDAO.RINGTONE_IDS;
import static com.best.deskclock.data.CustomRingtoneDAO.RINGTONE_TITLE;
import static com.best.deskclock.data.CustomRingtoneDAO.RINGTONE_URI;
import static com.best.deskclock.data.SettingsDAO.KEY_SELECTED_ALARM_RINGTONE_URI;
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
import static com.best.deskclock.utils.Utils.ACTION_LANGUAGE_CODE_CHANGED;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.best.deskclock.BuildConfig;
import com.best.deskclock.R;
import com.best.deskclock.controller.ThemeController;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.uidata.UiDataModel;

import java.util.Map;

public class AboutFragment extends ScreenFragment implements Preference.OnPreferenceClickListener {

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

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.about_title);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_about);

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

        setupTitle();
        setupVersion();
        setupPreferences();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.add(0, 1, 0, R.string.reset_settings_title)
                .setIcon(R.drawable.ic_reset_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            final AlertDialog builder = new AlertDialog.Builder(requireContext())
                    .setIcon(R.drawable.ic_reset_settings)
                    .setTitle(R.string.reset_settings_title)
                    .setMessage(R.string.reset_settings_message)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> resetPreferences())
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            builder.show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case KEY_ABOUT_WHATS_NEW -> {
                String version = BuildConfig.VERSION_NAME;
                if (BuildConfig.DEBUG) {
                    version = version.replace("-debug", "");
                }
                final String link = "https://github.com/BlackyHawky/Clock/releases/tag/" + version;
                displayLinkDialog(R.drawable.ic_about_update, R.string.whats_new_title, R.string.whats_new_dialog_message, link);
            }

            case KEY_ABOUT_FEATURES -> {
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setIcon(R.drawable.ic_about_features)
                        .setTitle(R.string.features_title)
                        .setMessage(R.string.about_dialog_message)
                        .setPositiveButton(R.string.dialog_close, null)
                        .create();
                builder.show();
            }

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

    private void displayLinkDialog(int iconId, int titleId, int messageId, String link) {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        final AlertDialog builder = new AlertDialog.Builder(requireContext())
                .setIcon(iconId)
                .setTitle(titleId)
                .setMessage(requireContext().getString(messageId, link))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        builder.show();
    }

    private void displayContributorDialog(int iconId, String projectName, String url) {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        final AlertDialog builder = new AlertDialog.Builder(requireContext())
                .setIcon(iconId)
                .setTitle(R.string.contributors_dialog_title)
                .setMessage(requireContext().getString(R.string.contributors_dialog_message, projectName, url))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        builder.show();
    }

    private void setupTitle() {
        Preference title = findPreference(KEY_ABOUT_TITLE);
        if (title == null) {
            return;
        }
        if (BuildConfig.DEBUG) {
            title.setTitle(R.string.about_debug_app_title);
        } else {
            title.setTitle(R.string.app_label);
        }
    }

    private void setupVersion() {
        Preference version = findPreference(KEY_ABOUT_VERSION);
        if (version == null) {
            return;
        }
        version.setSummary(BuildConfig.VERSION_NAME);
    }

    private void setupPreferences() {
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
                // Also, exclude keys corresponding to custom ringtones and the selected alarm ringtone,
                // as this causes bugs for alarms.
                if (!entry.getKey().equals(KEY_IS_FIRST_LAUNCH) &&
                        !entry.getKey().startsWith(RINGTONE_URI) &&
                        !entry.getKey().equals(RINGTONE_IDS) &&
                        !entry.getKey().equals(NEXT_RINGTONE_ID) &&
                        !entry.getKey().startsWith(RINGTONE_TITLE) &&
                        !entry.getKey().equals(KEY_SELECTED_ALARM_RINGTONE_URI)) {
                    editor.remove(entry.getKey());
                }
            }
            editor.apply();

            // Required to update Locale after a reset.
            requireContext().sendBroadcast(new Intent(ACTION_LANGUAGE_CODE_CHANGED));
            // Required to update widgets after a reset.
            requireContext().sendBroadcast(new Intent(ACTION_APPWIDGET_UPDATE));
            // Required to update the timer list after a reset.
            DataModel.getDataModel().loadTimers();
            // Required to update the tab to display after a reset.
            UiDataModel.getUiDataModel().setSelectedTab(UiDataModel.Tab.CLOCKS);

            ThemeController.setNewSettingWithDelay();
            Toast.makeText(requireContext(), requireContext().getString(R.string.toast_message_for_reset),
                    Toast.LENGTH_SHORT).show();
        }, 400);

    }

}
