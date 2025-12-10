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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.best.deskclock.R;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

public class StopwatchSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    Preference mStopwatchFontPref;
    ListPreference mVolumeUpActionPref;
    ListPreference mVolumeUpActionAfterLongPressPref;
    ListPreference mVolumeDownActionPref;
    ListPreference mVolumeDownActionAfterLongPressPref;

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
                clearStopwatchFontFile();

                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_SW_FONT, copiedUri.getPath()).apply();
                    mStopwatchFontPref.setTitle(getString(R.string.custom_font_title_variant));

                    Toast.makeText(requireContext(), R.string.custom_font_toast_message_selected, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Error importing font", Toast.LENGTH_SHORT).show();
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
            if (SettingsDAO.getStopwatchFont(mPrefs) == null) {
                selectStopwatchFont();
            } else {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.custom_font_dialog_title)
                        .setMessage(R.string.custom_font_title_variant)
                        .setPositiveButton(getString(R.string.label_new_font), (dialog, which) ->
                                selectStopwatchFont())
                        .setNeutralButton(getString(R.string.delete), (dialog, which) ->
                                deleteStopwatchFont())
                        .show();
            }
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

    private void selectStopwatchFont() {
        fontPickerLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_MIME_TYPES,
                        new String[]{"application/x-font-ttf", "application/x-font-otf", "font/ttf", "font/otf"})

        );
    }

    private void deleteStopwatchFont() {
        clearStopwatchFontFile();

        mPrefs.edit().remove(KEY_SW_FONT).apply();
        mStopwatchFontPref.setTitle(getString(R.string.custom_font_title));

        Toast.makeText(requireContext(), R.string.custom_font_toast_message_deleted, Toast.LENGTH_SHORT).show();
    }

    private void clearStopwatchFontFile() {
        String path = mPrefs.getString(KEY_SW_FONT, null);
        if (path != null) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                boolean deleted = file.delete();
                if (!deleted) {
                    LogUtils.w("Unable to delete stopwatch font: " + path);
                }
            }
        }
    }

}
