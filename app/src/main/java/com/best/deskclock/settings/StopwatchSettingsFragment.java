// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesKeys.FILE_STOPWATCH_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_DOWN_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_UP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

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

            final Context appContext = requireContext().getApplicationContext();

            // Take persistent permission
            appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String safeTitle = Utils.toSafeFileName(FILE_STOPWATCH_FONT);
            String oldFontPath = mPrefs.getString(KEY_SW_FONT, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old font if it exists
                clearFile(oldFontPath);

                // Clear the font cache
                ThemeUtils.removeFontFromCache(oldFontPath);

                // Copy the new font to the device's protected storage
                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_SW_FONT, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, R.string.custom_font_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, "Error importing font");
                    }

                    if (!isAdded() || mStopwatchFontPref == null) {
                        return;
                    }

                    if (copiedUri != null) {
                        mStopwatchFontPref.setTitle(getString(R.string.custom_font_title_variant));
                    } else {
                        mStopwatchFontPref.setTitle(getString(R.string.custom_font_title));
                    }
                });
            });
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
    public void onResume() {
        super.onResume();

        restoreCustomFileDialogIfNeeded(KEY_SW_FONT, mStopwatchFontPref, fontPickerLauncher, null);
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mStopwatchFontPref, mVolumeUpActionPref, mVolumeUpActionAfterLongPressPref, mVolumeDownActionPref,
            mVolumeDownActionAfterLongPressPref);

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_SW_VOLUME_UP_ACTION, KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS, KEY_SW_VOLUME_DOWN_ACTION,
                 KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS -> {
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

    private void nullifyAllPrefs() {
        mStopwatchFontPref = null;
        mVolumeUpActionPref = null;
        mVolumeUpActionAfterLongPressPref = null;
        mVolumeDownActionPref = null;
        mVolumeDownActionAfterLongPressPref = null;
    }

}
