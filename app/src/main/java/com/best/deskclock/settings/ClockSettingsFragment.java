// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

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
import com.best.deskclock.data.TimeZones;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.FileUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.DigitalAppWidgetProvider;

public class ClockSettingsFragment extends BaseSettingsScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    String[] mClockStyleValues;
    String mAnalogClock;
    String mMaterialAnalogClock;
    String mDigitalClock;

    ListPreference mClockStylePref;
    ListPreference mClockDialPref;
    ListPreference mClockDialMaterialPref;
    SwitchPreferenceCompat mDisplayClockSecondsPref;
    ListPreference mClockSecondHandPref;
    SwitchPreferenceCompat mDisplayNextAlarmPref;
    Preference mDigitalClockFontPref;
    SwitchPreferenceCompat mDisplayTextUppercasePref;
    ListPreference mSortCitiesPref;
    SwitchPreferenceCompat mEnableCityFlagPref;
    SwitchPreferenceCompat mEnableCityNotePref;
    SwitchPreferenceCompat mAutoHomeClockPref;
    ListPreference mHomeTimeZonePref;
    Preference mDateTimePref;

    private final ActivityResultLauncher<Intent> fontPickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (isAdded() && mIsSearchMode) {
                blockSearchResetForAWhile();
            }

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

            String safeTitle = FileUtils.toSafeFileName(FILE_DIGITAL_CLOCK_FONT);
            String oldFontPath = prefs.getString(KEY_DIGITAL_CLOCK_FONT, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old font if it exists
                FileUtils.clearFile(oldFontPath);

                // Copy the new font to the device's protected storage
                Uri copiedUri = FileUtils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    prefs.edit().putString(KEY_DIGITAL_CLOCK_FONT, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, style, font, R.string.custom_font_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, style, font, R.string.font_message_error);
                    }

                    if (!isAdded() || mDigitalClockFontPref == null) {
                        return;
                    }

                    if (copiedUri != null) {
                        mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title_variant));
                    } else {
                        mDigitalClockFontPref.setTitle(getString(R.string.custom_font_title));
                    }
                });
            });
        });

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.clock_settings);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_clock);

        mClockStylePref = findPreference(KEY_CLOCK_STYLE);
        mClockDialPref = findPreference(KEY_CLOCK_DIAL);
        mClockDialMaterialPref = findPreference(KEY_CLOCK_DIAL_MATERIAL);
        mDisplayClockSecondsPref = findPreference(KEY_DISPLAY_CLOCK_SECONDS);
        mClockSecondHandPref = findPreference(KEY_CLOCK_SECOND_HAND);
        mDisplayNextAlarmPref = findPreference(KEY_DISPLAY_NEXT_ALARM);
        mDigitalClockFontPref = findPreference(KEY_DIGITAL_CLOCK_FONT);
        mDisplayTextUppercasePref = findPreference(KEY_DISPLAY_TEXT_UPPERCASE);
        mSortCitiesPref = findPreference(KEY_SORT_CITIES);
        mEnableCityFlagPref = findPreference(KEY_ENABLE_CITY_FLAG);
        mEnableCityNotePref = findPreference(KEY_ENABLE_CITY_NOTE);
        mAutoHomeClockPref = findPreference(KEY_AUTO_HOME_CLOCK);
        mHomeTimeZonePref = findPreference(KEY_HOME_TIME_ZONE);
        mDateTimePref = findPreference(KEY_DATE_TIME);

        mClockStyleValues = getResources().getStringArray(R.array.clock_style_values);
        mAnalogClock = mClockStyleValues[0];
        mMaterialAnalogClock = mClockStyleValues[1];
        mDigitalClock = mClockStyleValues[2];

        setupPreferences();

        invalidatePreferencesVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();

        restoreCustomFileDialogIfNeeded(KEY_DIGITAL_CLOCK_FONT, mDigitalClockFontPref, fontPickerLauncher, null);

        invalidatePreferencesVisibility();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        switch (pref.getKey()) {
            case KEY_CLOCK_STYLE -> {
                final int clockIndex = mClockStylePref.findIndexOfValue((String) newValue);
                mClockStylePref.setSummary(mClockStylePref.getEntries()[clockIndex]);
            }

            case KEY_CLOCK_DIAL, KEY_CLOCK_DIAL_MATERIAL, KEY_CLOCK_SECOND_HAND, KEY_HOME_TIME_ZONE,
                 KEY_SORT_CITIES -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_DISPLAY_CLOCK_SECONDS, KEY_DISPLAY_NEXT_ALARM, KEY_DISPLAY_TEXT_UPPERCASE ->
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            case KEY_AUTO_HOME_CLOCK -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mHomeTimeZonePref.setEnabled((boolean) newValue);
            }

            case KEY_ENABLE_CITY_FLAG -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isFlagEnabled = (Boolean) newValue;
                final TimeZones timezones = SettingsDAO.getTimeZones(requireContext(), System.currentTimeMillis(), isFlagEnabled);
                mHomeTimeZonePref.setEntryValues(timezones.timeZoneIds());
                mHomeTimeZonePref.setEntries(timezones.timeZoneNames());

                CharSequence currentValue = mHomeTimeZonePref.getValue();
                if (currentValue != null) {
                    int index = mHomeTimeZonePref.findIndexOfValue(currentValue.toString());
                    if (index >= 0) {
                        mHomeTimeZonePref.setSummary(mHomeTimeZonePref.getEntries()[index]);
                    }
                }
            }

            case KEY_ENABLE_CITY_NOTE -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                WidgetUtils.scheduleWidgetUpdate(requireContext(), DigitalAppWidgetProvider.class);
            }
        }

        blockSearchResetForAWhile();

        if (getView() != null) {
            getView().post(() -> {
                if (isAdded()) {
                    invalidatePreferencesVisibility();
                }
            });
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

            case KEY_DIGITAL_CLOCK_FONT -> selectCustomFile(
                mDigitalClockFontPref, fontPickerLauncher, SettingsDAO.getDigitalClockFont(getPrefs()), KEY_DIGITAL_CLOCK_FONT, true,
                false, null);
        }

        return true;
    }

    @Override
    protected boolean isPreferenceVisible(@NonNull Preference pref, @NonNull String key) {
        final boolean isAnalogClock = mClockStylePref.getValue().equals(mAnalogClock);
        final boolean isMaterialAnalogClock = mClockStylePref.getValue().equals(mMaterialAnalogClock);
        final boolean isDigitalClock = mClockStylePref.getValue().equals(mDigitalClock);
        final boolean displaySeconds = mDisplayClockSecondsPref.isChecked();

        return switch (key) {
            case KEY_CLOCK_DIAL -> isAnalogClock;
            case KEY_CLOCK_DIAL_MATERIAL -> isMaterialAnalogClock;
            case KEY_ANALOG_CLOCK_SIZE -> !isDigitalClock;
            case KEY_CLOCK_SECOND_HAND -> isAnalogClock && displaySeconds;
            case KEY_DIGITAL_CLOCK_FONT, KEY_DIGITAL_CLOCK_FONT_SIZE -> isDigitalClock;
            default -> super.isPreferenceVisible(pref, key);
        };
    }

    private void setupPreferences() {
        mClockStylePref.setSummary(mClockStylePref.getEntry());
        mClockStylePref.setOnPreferenceChangeListener(this);

        mClockDialPref.setSummary(mClockDialPref.getEntry());
        mClockDialPref.setOnPreferenceChangeListener(this);

        mClockDialMaterialPref.setSummary(mClockDialMaterialPref.getEntry());
        mClockDialMaterialPref.setOnPreferenceChangeListener(this);

        mDisplayClockSecondsPref.setOnPreferenceChangeListener(this);

        mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
        mClockSecondHandPref.setOnPreferenceChangeListener(this);

        mDisplayNextAlarmPref.setOnPreferenceChangeListener(this);

        mDigitalClockFontPref.setTitle(getString(SettingsDAO.getDigitalClockFont(getPrefs()) == null
            ? R.string.custom_font_title
            : R.string.custom_font_title_variant));
        mDigitalClockFontPref.setOnPreferenceClickListener(this);

        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);

        mSortCitiesPref.setSummary(mSortCitiesPref.getEntry());
        mSortCitiesPref.setOnPreferenceChangeListener(this);

        mEnableCityFlagPref.setOnPreferenceChangeListener(this);
        mEnableCityNotePref.setOnPreferenceChangeListener(this);
        mAutoHomeClockPref.setOnPreferenceChangeListener(this);

        mHomeTimeZonePref.setEnabled(SettingsDAO.getAutoShowHomeClock(getPrefs()));
        // Reconstruct the timezone list.
        final TimeZones timezones = SettingsDAO.getTimeZones(
            requireContext(), System.currentTimeMillis(), SettingsDAO.isCityFlagEnabled(getPrefs()));
        mHomeTimeZonePref.setEntryValues(timezones.timeZoneIds());
        mHomeTimeZonePref.setEntries(timezones.timeZoneNames());
        mHomeTimeZonePref.setSummary(mHomeTimeZonePref.getEntry());
        mHomeTimeZonePref.setOnPreferenceChangeListener(this);

        mDateTimePref.setOnPreferenceClickListener(this);
    }

}
