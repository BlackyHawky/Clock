// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import com.best.deskclock.BuildConfig;
import com.best.deskclock.R;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

/**
 * "About" screen.
 */
public final class AboutActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "about_fragment";

    public static final String KEY_ABOUT_TITLE = "key_about_title";
    public static final String KEY_ABOUT_VERSION = "key_about_version";
    public static final String KEY_ABOUT_WHATS_NEW = "key_about_whats_new";
    public static final String KEY_ABOUT_FEATURES = "key_about_features";
    public static final String KEY_ABOUT_VIEW_ON_GITHUB = "key_about_view_on_github";
    public static final String KEY_ABOUT_READ_LICENCE = "key_about_read_licence";
    public static final String KEY_ABOUT_BLACKYHAWKY = "key_about_blackyhawky";
    public static final String KEY_ABOUT_QW123WH = "key_about_qw123wh";
    public static final String KEY_ABOUT_ODMFL = "key_about_odmfl";
    public static final String KEY_ABOUT_NILSU11 = "key_about_nilsu11";
    public static final String KEY_ABOUT_LINEAGEOS = "key_about_lineageos";
    public static final String KEY_ABOUT_CRDROID = "key_about_crdroid";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new PrefsFragment(), PREFS_FRAGMENT_TAG)
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    public static class PrefsFragment extends ScreenFragment implements Preference.OnPreferenceClickListener {

        Preference mWhatsNewPreference;
        Preference mAboutFeatures;
        Preference mViewOnGitHub;
        Preference mReadLicence;
        Preference mContributor1;
        Preference mContributor2;
        Preference mContributor3;
        Preference mContributor4;
        Preference mCredit1;
        Preference mCredit2;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_about);

            mWhatsNewPreference = findPreference(KEY_ABOUT_WHATS_NEW);
            mAboutFeatures = findPreference(KEY_ABOUT_FEATURES);
            mViewOnGitHub = findPreference(KEY_ABOUT_VIEW_ON_GITHUB);
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
        public boolean onPreferenceClick(@NonNull Preference preference) {
            switch (preference.getKey()) {
                case KEY_ABOUT_WHATS_NEW -> {
                    String version = BuildConfig.VERSION_NAME;
                    if (BuildConfig.DEBUG) {
                        version = version.replace("-debug", "");
                    }

                    final String link = "https://github.com/BlackyHawky/Clock/releases/tag/" + version;
                    final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    final AlertDialog builder = new AlertDialog.Builder(requireContext())
                            .setIcon(R.drawable.ic_about_update)
                            .setTitle(R.string.whats_new_title)
                            .setMessage(requireContext().getString(R.string.whats_new_dialog_message, link))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .create();
                    builder.show();
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
                    final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    final AlertDialog builder = new AlertDialog.Builder(requireContext())
                            .setIcon(R.drawable.ic_about_github)
                            .setTitle(R.string.about_github_link)
                            .setMessage(requireContext().getString(R.string.github_dialog_message, link))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .create();
                    builder.show();
                }

                case KEY_ABOUT_READ_LICENCE -> {
                    final String link = "https://github.com/BlackyHawky/Clock/blob/main/LICENSE-GPL-3";
                    final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    final AlertDialog builder = new AlertDialog.Builder(requireContext())
                            .setIcon(R.drawable.ic_about_license)
                            .setTitle(R.string.license)
                            .setMessage(requireContext().getString(R.string.license_dialog_message, link))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .create();
                    builder.show();
                }

                case KEY_ABOUT_BLACKYHAWKY -> {
                    final String link = "https://github.com/BlackyHawky";
                    final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    final AlertDialog builder = new AlertDialog.Builder(requireContext())
                            .setMessage(requireContext().getString(R.string.contributors_dialog_message, "BlackyHawky", link))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .create();
                    builder.show();
                }

                case KEY_ABOUT_QW123WH -> {
                    final String link = "https://github.com/qw123wh";
                    final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    final AlertDialog builder = new AlertDialog.Builder(requireContext())
                            .setMessage(requireContext().getString(R.string.contributors_dialog_message, "qw123wh", link))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .create();
                    builder.show();
                }

                case KEY_ABOUT_ODMFL -> {
                    final String link = "https://github.com/odmfl";
                    final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    final AlertDialog builder = new AlertDialog.Builder(requireContext())
                            .setMessage(requireContext().getString(R.string.contributors_dialog_message, "odmfl", link))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .create();
                    builder.show();
                }

                case KEY_ABOUT_NILSU11 -> {
                    final String link = "https://github.com/Nilsu11";
                    final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    final AlertDialog builder = new AlertDialog.Builder(requireContext())
                            .setMessage(requireContext().getString(R.string.contributors_dialog_message, "Nilsu11", link))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .create();
                    builder.show();
                }

                case KEY_ABOUT_LINEAGEOS -> {
                    final String link = "https://github.com/LineageOS";
                    final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    final AlertDialog builder = new AlertDialog.Builder(requireContext())
                            .setMessage(requireContext().getString(R.string.contributors_dialog_message, "LineageOS", link))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .create();
                    builder.show();
                }

                case KEY_ABOUT_CRDROID -> {
                    final String link = "https://github.com/crdroidandroid";
                    final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    final AlertDialog builder = new AlertDialog.Builder(requireContext())
                            .setMessage(requireContext().getString(R.string.contributors_dialog_message, "crDroid Android", link))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> startActivity(browserIntent))
                            .setNegativeButton(android.R.string.cancel, null)
                            .create();
                    builder.show();
                }
            }

            return true;
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
            mReadLicence.setOnPreferenceClickListener(this);
            mContributor1.setOnPreferenceClickListener(this);
            mContributor2.setOnPreferenceClickListener(this);
            mContributor3.setOnPreferenceClickListener(this);
            mContributor4.setOnPreferenceClickListener(this);
            mCredit1.setOnPreferenceClickListener(this);
            mCredit2.setOnPreferenceClickListener(this);
        }
    }

}
