// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;

import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_DOWN_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_UP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.settings.custompreference.CustomListPreference;
import com.best.deskclock.settings.custompreference.CustomPreference;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.Utils;

public class StopwatchSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    CustomPreference mStopwatchFontPref;
    CustomListPreference mVolumeUpActionPref;
    CustomListPreference mVolumeUpActionAfterLongPressPref;
    CustomListPreference mVolumeDownActionPref;
    CustomListPreference mVolumeDownActionAfterLongPressPref;

    private final ActivityResultLauncher<Intent> fontPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK) {
                    return;
                }

                Intent intent = result.getData();
                final Uri sourceUri = intent == null ? null : intent.getData();
                if (sourceUri == null) {
                    return;
                }

                // Take persistent permission
                requireActivity().getContentResolver().takePersistableUriPermission(
                        sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                String safeTitle = Utils.toSafeFileName("stopwatch_font");

                // Delete the old font if it exists
                clearFile(mPrefs.getString(KEY_SW_FONT, null));

                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_SW_FONT, copiedUri.getPath()).apply();
                    mStopwatchFontPref.setTitle(getString(R.string.custom_font_title_variant));

                    CustomToast.show(requireContext(), R.string.custom_font_toast_message_selected);
                } else {
                    CustomToast.show(requireContext(), "Error importing font");
                    mStopwatchFontPref.setTitle(getString(R.string.custom_font_title));
                }
            });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.stopwatch_channel);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_stopwatch);

        mStopwatchFontPref = findPreference(KEY_SW_FONT);
        mVolumeUpActionPref = findPreference(KEY_SW_VOLUME_UP_ACTION);
        mVolumeUpActionAfterLongPressPref = findPreference(KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS);
        mVolumeDownActionPref = findPreference(KEY_SW_VOLUME_DOWN_ACTION);
        mVolumeDownActionAfterLongPressPref = findPreference(KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS);

        setupPreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_SW_VOLUME_UP_ACTION, KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS,
                 KEY_SW_VOLUME_DOWN_ACTION, KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        if (pref.getKey().equals(KEY_SW_FONT)) {
            selectCustomFile(mStopwatchFontPref, fontPickerLauncher,
                    SettingsDAO.getStopwatchFont(mPrefs), KEY_SW_FONT, true, null);
        }

        return true;
    }

    private void setupPreferences() {
        mStopwatchFontPref.setTitle(getString(SettingsDAO.getStopwatchFont(mPrefs) == null
                ? R.string.custom_font_title
                : R.string.custom_font_title_variant));
        mStopwatchFontPref.setOnPreferenceClickListener(this);

        mVolumeUpActionPref.setOnPreferenceChangeListener(this);
        mVolumeUpActionPref.setSummary(mVolumeUpActionPref.getEntry());

        mVolumeUpActionAfterLongPressPref.setOnPreferenceChangeListener(this);
        mVolumeUpActionAfterLongPressPref.setSummary(mVolumeUpActionAfterLongPressPref.getEntry());

        mVolumeDownActionPref.setOnPreferenceChangeListener(this);
        mVolumeDownActionPref.setSummary(mVolumeDownActionPref.getEntry());

        mVolumeDownActionAfterLongPressPref.setOnPreferenceChangeListener(this);
        mVolumeDownActionAfterLongPressPref.setSummary(mVolumeDownActionAfterLongPressPref.getEntry());
    }

}
