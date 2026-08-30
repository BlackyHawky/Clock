// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesKeys.FILE_STOPWATCH_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_DISPLAY_MILLISECONDS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_DOWN_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_DOWN_ACTION_AFTER_LONG_PRESS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_UP_ACTION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SW_VOLUME_UP_ACTION_AFTER_LONG_PRESS;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.base.BaseSettingsScreenFragment;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.ThemeUtils;
import com.best.deskclock.utils.Utils;

public class StopwatchSettingsFragment extends BaseSettingsScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    Preference mStopwatchFontPref;
    SwitchPreferenceCompat mDisplayMillisecondsPref;
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
            final int style = getAccentStyle();
            final Typeface font = getGeneralTypeface();
            final SharedPreferences prefs = getPrefs();

            // Take persistent permission
            appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String safeTitle = FileUtils.toSafeFileName(FILE_STOPWATCH_FONT);
            String oldFontPath = prefs.getString(KEY_SW_FONT, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old font if it exists
                FileUtils.clearFile(oldFontPath);

                // Clear the font cache
                ThemeUtils.removeFontFromCache(oldFontPath);

                // Copy the new font to the device's protected storage
                Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    prefs.edit().putString(KEY_SW_FONT, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, style, font, R.string.custom_font_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, style, font, R.string.font_message_error);
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_stopwatch);

        mStopwatchFontPref = findPreference(KEY_SW_FONT);
        mDisplayMillisecondsPref = findPreference(KEY_SW_DISPLAY_MILLISECONDS);
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
        nullifyPreferenceListeners(mStopwatchFontPref, mDisplayMillisecondsPref, mVolumeUpActionPref, mVolumeUpActionAfterLongPressPref,
            mVolumeDownActionPref, mVolumeDownActionAfterLongPressPref);

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        switch (pref.getKey()) {
            case KEY_SW_DISPLAY_MILLISECONDS ->
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

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
                SettingsDAO.getStopwatchFont(getPrefs()), KEY_SW_FONT, true, null);
        }

        return true;
    }

    private void setupPreferences() {
        mStopwatchFontPref.setTitle(getString(SettingsDAO.getStopwatchFont(getPrefs()) == null
            ? R.string.custom_font_title
            : R.string.custom_font_title_variant));
        mStopwatchFontPref.setOnPreferenceClickListener(this);

        mDisplayMillisecondsPref.setOnPreferenceChangeListener(this);

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
        mDisplayMillisecondsPref = null;
        mVolumeUpActionPref = null;
        mVolumeUpActionAfterLongPressPref = null;
        mVolumeDownActionPref = null;
        mVolumeDownActionAfterLongPressPref = null;
    }

}
