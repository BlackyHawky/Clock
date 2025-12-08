// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;

import static com.best.deskclock.settings.PreferencesKeys.KEY_AUTO_HOME_CLOCK;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CLOCK_DIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CLOCK_DIAL_MATERIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CLOCK_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_CLOCK_STYLE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DATE_TIME;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_CLOCK_FONT;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DISPLAY_CLOCK_SECONDS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ENABLE_CITY_NOTE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_HOME_TIME_ZONE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_SORT_CITIES;

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
import com.best.deskclock.data.TimeZones;
import com.best.deskclock.utils.LogUtils;
import com.best.deskclock.utils.Utils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

public class ClockSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    String[] mClockStyleValues;
    String mAnalogClock;
    String mMaterialAnalogClock;
    String mDigitalClock;

    ListPreference mClockStylePref;
    ListPreference mClockDialPref;
    ListPreference mClockDialMaterialPref;
    ListPreference mClockSecondHandPref;
    SwitchPreferenceCompat mDisplayClockSecondsPref;
    Preference mDigitalClockFontPref;
    SwitchPreferenceCompat mEnableCityNotePref;
    ListPreference mSortCitiesPref;
    SwitchPreferenceCompat mAutoHomeClockPref;
    ListPreference mHomeTimeZonePref;
    Preference mDateTimePref;

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

                String safeTitle = Utils.toSafeFileName("digital_clock_font");

                // Delete the old font if it exists
                clearDigitalClockFontFile();

                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(requireContext(), sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_DIGITAL_CLOCK_FONT, copiedUri.getPath()).apply();
                    mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title_variant));

                    Toast.makeText(requireContext(), R.string.custom_font_toast_message_selected, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Error importing font", Toast.LENGTH_SHORT).show();
                    mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title));
                }
            });


    @Override
    protected String getFragmentTitle() {
        return getString(R.string.clock_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_clock);

        mClockStylePref = findPreference(KEY_CLOCK_STYLE);
        mClockDialPref = findPreference(KEY_CLOCK_DIAL);
        mClockDialMaterialPref = findPreference(KEY_CLOCK_DIAL_MATERIAL);
        mDisplayClockSecondsPref = findPreference(KEY_DISPLAY_CLOCK_SECONDS);
        mClockSecondHandPref = findPreference(KEY_CLOCK_SECOND_HAND);
        mDigitalClockFontPref = findPreference(KEY_DIGITAL_CLOCK_FONT);
        mEnableCityNotePref = findPreference(KEY_ENABLE_CITY_NOTE);
        mSortCitiesPref = findPreference(KEY_SORT_CITIES);
        mAutoHomeClockPref = findPreference(KEY_AUTO_HOME_CLOCK);
        mHomeTimeZonePref = findPreference(KEY_HOME_TIME_ZONE);
        mDateTimePref = findPreference(KEY_DATE_TIME);

        mClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
        mAnalogClock = mClockStyleValues[0];
        mMaterialAnalogClock = mClockStyleValues[1];
        mDigitalClock = mClockStyleValues[2];

        setupPreferences();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_CLOCK_STYLE -> {
                final int clockIndex = mClockStylePref.findIndexOfValue((String) newValue);
                mClockStylePref.setSummary(mClockStylePref.getEntries()[clockIndex]);
                mClockDialPref.setVisible(newValue.equals(mAnalogClock));
                mClockDialMaterialPref.setVisible(newValue.equals(mMaterialAnalogClock));
                mClockSecondHandPref.setVisible(newValue.equals(mAnalogClock)
                        && SettingsDAO.areClockSecondsDisplayed(mPrefs));
                mDigitalClockFontPref.setVisible(newValue.equals(mDigitalClock));
            }

            case KEY_CLOCK_DIAL, KEY_CLOCK_DIAL_MATERIAL, KEY_CLOCK_SECOND_HAND, KEY_HOME_TIME_ZONE,
                 KEY_SORT_CITIES -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_DISPLAY_CLOCK_SECONDS -> {
                mClockSecondHandPref.setVisible((boolean) newValue
                        && SettingsDAO.getClockStyle(mPrefs) == DataModel.ClockStyle.ANALOG);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_AUTO_HOME_CLOCK -> {
                mHomeTimeZonePref.setEnabled((boolean) newValue);

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ENABLE_CITY_NOTE -> Utils.setVibrationTime(requireContext(), 50);
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        switch (pref.getKey()) {
            case KEY_DATE_TIME -> {
                final Intent dialogIntent = new Intent(Settings.ACTION_DATE_SETTINGS);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialogIntent);
            }

            case KEY_DIGITAL_CLOCK_FONT -> {
                if (SettingsDAO.getDigitalClockFont(mPrefs) == null) {
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

    private void setupPreferences() {
        mClockStylePref.setSummary(mClockStylePref.getEntry());
        mClockStylePref.setOnPreferenceChangeListener(this);

        mClockDialPref.setVisible(mClockStylePref.getValue().equals(mAnalogClock));
        mClockDialPref.setSummary(mClockDialPref.getEntry());
        mClockDialPref.setOnPreferenceChangeListener(this);

        mClockDialMaterialPref.setVisible(mClockStylePref.getValue().equals(mMaterialAnalogClock));
        mClockDialMaterialPref.setSummary(mClockDialMaterialPref.getEntry());
        mClockDialMaterialPref.setOnPreferenceChangeListener(this);

        mDisplayClockSecondsPref.setOnPreferenceChangeListener(this);

        mClockSecondHandPref.setVisible(mClockStylePref.getValue().equals(mAnalogClock)
                && SettingsDAO.areClockSecondsDisplayed(mPrefs));
        mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
        mClockSecondHandPref.setOnPreferenceChangeListener(this);

        mDigitalClockFontPref.setVisible(mClockStylePref.getValue().equals(mDigitalClock));
        mDigitalClockFontPref.setTitle(getString(SettingsDAO.getDigitalClockFont(mPrefs) == null
                ? R.string.custom_font_title
                : R.string.custom_font_title_variant));
        mDigitalClockFontPref.setOnPreferenceClickListener(this);

        mSortCitiesPref.setSummary(mSortCitiesPref.getEntry());
        mSortCitiesPref.setOnPreferenceChangeListener(this);

        mEnableCityNotePref.setOnPreferenceChangeListener(this);

        mAutoHomeClockPref.setOnPreferenceChangeListener(this);

        mHomeTimeZonePref.setEnabled(SettingsDAO.getAutoShowHomeClock(mPrefs));
        // Reconstruct the timezone list.
        final TimeZones timezones = SettingsDAO.getTimeZones(requireContext(), System.currentTimeMillis());
        mHomeTimeZonePref.setEntryValues(timezones.getTimeZoneIds());
        mHomeTimeZonePref.setEntries(timezones.getTimeZoneNames());
        mHomeTimeZonePref.setSummary(mHomeTimeZonePref.getEntry());
        mHomeTimeZonePref.setOnPreferenceChangeListener(this);

        mDateTimePref.setOnPreferenceClickListener(this);
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

        mPrefs.edit().remove(KEY_DIGITAL_CLOCK_FONT).apply();
        mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title));

        Toast.makeText(requireContext(), R.string.custom_font_toast_message_deleted, Toast.LENGTH_SHORT).show();
    }

    private void clearDigitalClockFontFile() {
        String path = mPrefs.getString(KEY_DIGITAL_CLOCK_FONT, null);
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