/*
 * Copyright (C) 2009 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_DIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_CLOCK_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DATE_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DATE_IN_BOLD;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DATE_IN_ITALIC;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DAYDREAM_SETTINGS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DIGITAL_CLOCK_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DIGITAL_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SCREENSAVER_PREVIEW;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.events.Events;
import com.best.deskclock.screensaver.ScreensaverActivity;
import com.best.deskclock.uicomponents.CollapsingToolbarBaseActivity;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

/**
 * Settings for Clock screensaver
 */
public final class ScreensaverSettingsActivity extends CollapsingToolbarBaseActivity {

    @Override
    protected String getActivityTitle() {
        // Already defined in the fragment.
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, new ScreensaverSettingsFragment())
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    public static class ScreensaverSettingsFragment extends ScreenFragment
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        String[] mClockStyleValues;
        String mAnalogClock;
        String mMaterialAnalogClock;
        String mDigitalClock;

        ColorPickerPreference mClockColorPref;
        ColorPickerPreference mDateColorPref;
        ColorPickerPreference mNextAlarmColorPref;
        ListPreference mClockStylePref;
        ListPreference mClockDialPref;
        ListPreference mClockDialMaterialPref;
        ListPreference mClockSecondHandPref;
        SwitchPreferenceCompat mDisplaySecondsPref;
        CustomSeekbarPreference mDigitalClockFontSizePref;
        SwitchPreferenceCompat mBoldDigitalClockPref;
        SwitchPreferenceCompat mClockDynamicColorPref;
        SwitchPreferenceCompat mItalicDigitalClockPref;
        SwitchPreferenceCompat mBoldDatePref;
        SwitchPreferenceCompat mItalicDatePref;
        SwitchPreferenceCompat mBoldNextAlarmPref;
        SwitchPreferenceCompat mItalicNextAlarmPref;
        Preference mDigitalClockFontPref;
        Preference mScreensaverPreview;
        Preference mScreensaverMainSettings;

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

                    String safeTitle = Utils.toSafeFileName("screensaver_digital_clock_font");

                    // Delete the old font if it exists
                    clearDigitalClockFontFile();

                    Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                    // Save the new path
                    if (copiedUri != null) {
                        mPrefs.edit().putString(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, copiedUri.getPath()).apply();
                        mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title_variant));

                        Toast.makeText(requireContext(), R.string.custom_font_toast_message_selected, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Error importing font", Toast.LENGTH_SHORT).show();
                        mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title));
                    }
                });

        @Override
        protected String getFragmentTitle() {
            return getString(R.string.screensaver_settings_title);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings_screensaver);

            mClockStylePref = findPreference(KEY_SCREENSAVER_CLOCK_STYLE);
            mClockDialPref = findPreference(KEY_SCREENSAVER_CLOCK_DIAL);
            mClockDialMaterialPref = findPreference(KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL);
            mDigitalClockFontPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT);
            mDisplaySecondsPref = findPreference(KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS);
            mClockSecondHandPref = findPreference(KEY_SCREENSAVER_CLOCK_SECOND_HAND);
            mClockDynamicColorPref = findPreference(KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS);
            mClockColorPref = findPreference(KEY_SCREENSAVER_CLOCK_COLOR_PICKER);
            mDateColorPref = findPreference(KEY_SCREENSAVER_DATE_COLOR_PICKER);
            mNextAlarmColorPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_COLOR_PICKER);
            mDigitalClockFontSizePref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT_SIZE);
            mBoldDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD);
            mItalicDigitalClockPref = findPreference(KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC);
            mBoldDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_BOLD);
            mItalicDatePref = findPreference(KEY_SCREENSAVER_DATE_IN_ITALIC);
            mBoldNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD);
            mItalicNextAlarmPref = findPreference(KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC);
            mScreensaverPreview = findPreference(KEY_SCREENSAVER_PREVIEW);
            mScreensaverMainSettings = findPreference(KEY_SCREENSAVER_DAYDREAM_SETTINGS);

            mClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
            mAnalogClock = mClockStyleValues[0];
            mMaterialAnalogClock = mClockStyleValues[1];
            mDigitalClock = mClockStyleValues[2];

            setupPreferences();
        }

        @Override
        public boolean onPreferenceChange(Preference pref, Object newValue) {
            switch (pref.getKey()) {
                case KEY_SCREENSAVER_CLOCK_STYLE -> {
                    final int clockIndex = mClockStylePref.findIndexOfValue((String) newValue);
                    mClockStylePref.setSummary(mClockStylePref.getEntries()[clockIndex]);

                    boolean isAnalogClock = newValue.equals(mAnalogClock);
                    boolean isMaterialAnalogClock = newValue.equals(mMaterialAnalogClock);
                    boolean isDigitalClock = newValue.equals(mDigitalClock);
                    boolean areDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(mPrefs);

                    if (SdkUtils.isAtLeastAndroid12()) {
                        mClockDynamicColorPref.setVisible(!isMaterialAnalogClock);
                        mClockColorPref.setVisible(!isMaterialAnalogClock && !areDynamicColors);
                        if (areDynamicColors) {
                            mDateColorPref.setVisible(isMaterialAnalogClock);
                            mNextAlarmColorPref.setVisible(isMaterialAnalogClock);
                        }
                    } else {
                        mClockColorPref.setVisible(!isMaterialAnalogClock);
                    }

                    mClockDialPref.setVisible(isAnalogClock);
                    mClockDialMaterialPref.setVisible(isMaterialAnalogClock);
                    mClockSecondHandPref.setVisible(isAnalogClock
                            && SettingsDAO.areScreensaverClockSecondsDisplayed(mPrefs));
                    mDigitalClockFontPref.setVisible(isDigitalClock);
                    mDigitalClockFontSizePref.setVisible(isDigitalClock);
                    mBoldDigitalClockPref.setVisible(isDigitalClock);
                    mItalicDigitalClockPref.setVisible(isDigitalClock);
                }

                case KEY_SCREENSAVER_CLOCK_DIAL, KEY_SCREENSAVER_CLOCK_DIAL_MATERIAL,
                     KEY_SCREENSAVER_CLOCK_SECOND_HAND -> {
                    final ListPreference preference = (ListPreference) pref;
                    final int index = preference.findIndexOfValue((String) newValue);
                    preference.setSummary(preference.getEntries()[index]);
                }

                case KEY_DISPLAY_SCREENSAVER_CLOCK_SECONDS -> {
                    mClockSecondHandPref.setVisible((boolean) newValue
                            && SettingsDAO.getScreensaverClockStyle(mPrefs) == DataModel.ClockStyle.ANALOG);

                    Utils.setVibrationTime(requireContext(), 50);
                }

                case KEY_SCREENSAVER_DIGITAL_CLOCK_IN_BOLD, KEY_SCREENSAVER_DIGITAL_CLOCK_IN_ITALIC,
                     KEY_SCREENSAVER_DATE_IN_BOLD, KEY_SCREENSAVER_DATE_IN_ITALIC,
                     KEY_SCREENSAVER_NEXT_ALARM_IN_BOLD,
                     KEY_SCREENSAVER_NEXT_ALARM_IN_ITALIC ->
                        Utils.setVibrationTime(requireContext(), 50);

                case KEY_SCREENSAVER_CLOCK_DYNAMIC_COLORS -> {
                    boolean areNotDynamicColors = !(boolean) newValue;

                    mClockColorPref.setVisible(areNotDynamicColors);
                    mDateColorPref.setVisible(areNotDynamicColors);
                    mNextAlarmColorPref.setVisible(areNotDynamicColors);
                    Utils.setVibrationTime(requireContext(), 50);
                }
            }

            return true;
        }

        @Override
        public boolean onPreferenceClick(Preference pref) {
            final Context context = requireActivity();

            switch (pref.getKey()) {
                case KEY_SCREENSAVER_PREVIEW -> context.startActivity(
                        new Intent(context, ScreensaverActivity.class)
                                .putExtra(Events.EXTRA_EVENT_LABEL, R.string.label_deskclock));

                case KEY_SCREENSAVER_DAYDREAM_SETTINGS -> {
                    final Intent dialogSSMainSettingsIntent = new Intent(Settings.ACTION_DREAM_SETTINGS);
                    dialogSSMainSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(dialogSSMainSettingsIntent);
                }

                case KEY_SCREENSAVER_DIGITAL_CLOCK_FONT -> {
                    if (SettingsDAO.getScreensaverDigitalClockFont(mPrefs) == null) {
                        selectDigitalClockFont();
                    } else {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.custom_font_dialog_title)
                                .setMessage(R.string.custom_font_title_variant)
                                .setPositiveButton(getString(R.string.label_new_font), (dialog, which) ->
                                        selectDigitalClockFont())
                                .setNeutralButton(getString(R.string.delete), (dialog, which) ->
                                        deleteDigitalClockFont())
                                .show();
                    }
                }
            }

            return true;
        }

        @Override
        public void onDisplayPreferenceDialog(@NonNull Preference preference) {
            if (preference instanceof ColorPickerPreference colorPickerPref) {
                colorPickerPref.showDialog(this, 0);
            } else super.onDisplayPreferenceDialog(preference);
        }

        private void setupPreferences() {
            boolean isAnalogClock = mClockStylePref.getValue().equals(mAnalogClock);
            boolean isMaterialAnalogClock = mClockStylePref.getValue().equals(mMaterialAnalogClock);
            boolean isDigitalClock = mClockStylePref.getValue().equals(mDigitalClock);

            mClockStylePref.setSummary(mClockStylePref.getEntry());
            mClockStylePref.setOnPreferenceChangeListener(this);

            mClockDialPref.setVisible(isAnalogClock);
            mClockDialPref.setSummary(mClockDialPref.getEntry());
            mClockDialPref.setOnPreferenceChangeListener(this);

            mClockDialMaterialPref.setVisible(isMaterialAnalogClock);
            mClockDialMaterialPref.setSummary(mClockDialMaterialPref.getEntry());
            mClockDialMaterialPref.setOnPreferenceChangeListener(this);

            mDigitalClockFontPref.setVisible(isDigitalClock);
            mDigitalClockFontPref.setTitle(getString(SettingsDAO.getScreensaverDigitalClockFont(mPrefs) == null
                    ? R.string.custom_font_title
                    : R.string.custom_font_title_variant));
            mDigitalClockFontPref.setOnPreferenceClickListener(this);

            mDisplaySecondsPref.setOnPreferenceChangeListener(this);

            mClockSecondHandPref.setVisible(isAnalogClock
                    && SettingsDAO.areScreensaverClockSecondsDisplayed(mPrefs));
            mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
            mClockSecondHandPref.setOnPreferenceChangeListener(this);

            if (SdkUtils.isAtLeastAndroid12()) {
                final boolean areScreensaverClockDynamicColors = SettingsDAO.areScreensaverClockDynamicColors(mPrefs);
                mClockDynamicColorPref.setVisible(!isMaterialAnalogClock);
                mClockDynamicColorPref.setOnPreferenceChangeListener(this);
                mClockColorPref.setVisible(!areScreensaverClockDynamicColors && !isMaterialAnalogClock);
                mDateColorPref.setVisible(!areScreensaverClockDynamicColors || isMaterialAnalogClock);
                mNextAlarmColorPref.setVisible(!areScreensaverClockDynamicColors || isMaterialAnalogClock);
            } else {
                mClockColorPref.setVisible(!isMaterialAnalogClock);
            }

            mDigitalClockFontSizePref.setVisible(isDigitalClock);

            mBoldDigitalClockPref.setVisible(isDigitalClock);

            mItalicDigitalClockPref.setVisible(isDigitalClock);

            mBoldDigitalClockPref.setOnPreferenceChangeListener(this);

            mItalicDigitalClockPref.setOnPreferenceChangeListener(this);

            mBoldDatePref.setOnPreferenceChangeListener(this);

            mItalicDatePref.setOnPreferenceChangeListener(this);

            mBoldNextAlarmPref.setOnPreferenceChangeListener(this);

            mItalicNextAlarmPref.setOnPreferenceChangeListener(this);

            mScreensaverPreview.setOnPreferenceClickListener(this);

            mScreensaverMainSettings.setOnPreferenceClickListener(this);
        }

        private void selectDigitalClockFont() {
            fontPickerLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*")
                    .putExtra(Intent.EXTRA_MIME_TYPES,
                            new String[]{"application/x-font-ttf", "application/x-font-otf", "font/ttf", "font/otf"})

            );
        }

        private void deleteDigitalClockFont() {
            clearDigitalClockFontFile();

            mPrefs.edit().remove(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT).apply();
            mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title));

            Toast.makeText(requireContext(), R.string.custom_font_toast_message_deleted, Toast.LENGTH_SHORT).show();
        }

        private void clearDigitalClockFontFile() {
            String path = mPrefs.getString(KEY_SCREENSAVER_DIGITAL_CLOCK_FONT, null);
            if (path != null) {
                File file = new File(path);
                if (file.exists() && file.isFile()) {
                    boolean deleted = file.delete();
                    if (!deleted) {
                        LogUtils.w("Unable to delete digital clock font: " + path);
                    }
                }
            }
        }

    }
    
}
