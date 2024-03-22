// SPDX-License-Identifier: GPL-3.0-only
package com.best.deskclock.settings;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.best.deskclock.BuildConfig;
import com.best.deskclock.R;
import com.best.deskclock.actionbarmenu.OptionsMenuManager;
import com.best.deskclock.widget.CollapsingToolbarBaseActivity;

/**
 * "About" screen.
 */
public final class AboutActivity extends CollapsingToolbarBaseActivity {

    private static final String PREFS_FRAGMENT_TAG = "about_fragment";

    private final OptionsMenuManager mOptionsMenuManager = new OptionsMenuManager();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new PrefsFragment(), PREFS_FRAGMENT_TAG)
                    .disallowAddToBackStack()
                    .commit();
        }

        getWindow().setNavigationBarColor(getColor(R.color.md_theme_background));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mOptionsMenuManager.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mOptionsMenuManager.onPrepareOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mOptionsMenuManager.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    public static class PrefsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            addPreferencesFromResource(R.xml.settings_about);
            setupTitle();
            setupVersion();
            setupMainFeatures();
        }

        private void setupTitle() {
            Preference title = findPreference("about_title");
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
            Preference version = findPreference("about_version");
            if (version == null) {
                return;
            }
            version.setSummary(BuildConfig.VERSION_NAME);
        }

        private void setupMainFeatures() {
            Preference featuresPreference = findPreference("about_features");
            if (featuresPreference == null) {
                return;
            }
            featuresPreference.setOnPreferenceClickListener(preference -> {
                final AlertDialog builder = new AlertDialog.Builder(requireContext())
                        .setIcon(R.drawable.ic_about_features)
                        .setTitle(R.string.features_title)
                        .setMessage(R.string.about_dialog_message)
                        .setPositiveButton(R.string.dialog_close, null)
                        .create();
                builder.show();
                return true;
            });
        }
    }

}
