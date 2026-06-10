// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.app.Activity.RESULT_OK;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.AppExecutors;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.TimeZones;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.uicomponents.toast.CustomToast;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.DigitalAppWidgetProvider;

public class ClockSettingsFragment extends ScreenFragment
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    String[] mClockStyleValues;
    String mAnalogClock;
    String mMaterialAnalogClock;
    String mDigitalClock;

    ListPreference mClockStylePref;
    ListPreference mClockDialPref;
    ListPreference mClockDialMaterialPref;
    CustomSliderPreference mAnalogClockSizePref;
    SwitchPreferenceCompat mDisplayClockSecondsPref;
    ListPreference mClockSecondHandPref;
    Preference mDigitalClockFontPref;
    SwitchPreferenceCompat mDisplayTextUppercasePref;
    CustomSliderPreference mDigitalClockFontSizePref;
    ListPreference mSortCitiesPref;
    SwitchPreferenceCompat mEnableCityNotePref;
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

            final Context appContext = requireContext().getApplicationContext();

            // Take persistent permission
            appContext.getContentResolver().takePersistableUriPermission(sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String safeTitle = Utils.toSafeFileName(FILE_DIGITAL_CLOCK_FONT);
            String oldFontPath = mPrefs.getString(KEY_DIGITAL_CLOCK_FONT, null);

            AppExecutors.getDiskIO().execute(() -> {
                // Delete the old font if it exists
                clearFile(oldFontPath);

                // Copy the new font to the device's protected storage
                Uri copiedUri = Utils.copyFileToDeviceProtectedStorage(appContext, sourceUri, safeTitle);

                // Save the new path
                if (copiedUri != null) {
                    mPrefs.edit().putString(KEY_DIGITAL_CLOCK_FONT, copiedUri.getPath()).apply();
                }

                AppExecutors.getMainThread().post(() -> {
                    if (copiedUri != null) {
                        CustomToast.show(appContext, R.string.custom_font_toast_message_selected);
                    } else {
                        CustomToast.show(appContext, "Error importing font");
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_clock);

        mClockStylePref = findPreference(KEY_CLOCK_STYLE);
        mClockDialPref = findPreference(KEY_CLOCK_DIAL);
        mClockDialMaterialPref = findPreference(KEY_CLOCK_DIAL_MATERIAL);
        mAnalogClockSizePref = findPreference(KEY_ANALOG_CLOCK_SIZE);
        mDisplayClockSecondsPref = findPreference(KEY_DISPLAY_CLOCK_SECONDS);
        mClockSecondHandPref = findPreference(KEY_CLOCK_SECOND_HAND);
        mDigitalClockFontPref = findPreference(KEY_DIGITAL_CLOCK_FONT);
        mDisplayTextUppercasePref = findPreference(KEY_DISPLAY_TEXT_UPPERCASE);
        mDigitalClockFontSizePref = findPreference(KEY_DIGITAL_CLOCK_FONT_SIZE);
        mSortCitiesPref = findPreference(KEY_SORT_CITIES);
        mEnableCityNotePref = findPreference(KEY_ENABLE_CITY_NOTE);
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
    public void onResume() {
        super.onResume();

        restoreCustomFileDialogIfNeeded(KEY_DIGITAL_CLOCK_FONT, mDigitalClockFontPref, fontPickerLauncher, null);
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mClockStylePref, mClockDialPref, mClockDialMaterialPref, mAnalogClockSizePref, mDisplayClockSecondsPref,
            mClockSecondHandPref, mDigitalClockFontPref, mDisplayTextUppercasePref, mDigitalClockFontSizePref, mSortCitiesPref,
            mEnableCityNotePref, mAutoHomeClockPref, mHomeTimeZonePref, mDateTimePref);

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_CLOCK_STYLE -> {
                final int clockIndex = mClockStylePref.findIndexOfValue((String) newValue);
                mClockStylePref.setSummary(mClockStylePref.getEntries()[clockIndex]);

                boolean isAnalogClock = newValue.equals(mAnalogClock);
                boolean isMaterialAnalogClock = newValue.equals(mMaterialAnalogClock);
                boolean isDigitalClock = newValue.equals(mDigitalClock);

                mClockDialPref.setVisible(isAnalogClock);
                mClockDialMaterialPref.setVisible(isMaterialAnalogClock);
                mAnalogClockSizePref.setVisible(!isDigitalClock);
                mClockSecondHandPref.setVisible(isAnalogClock && SettingsDAO.areClockSecondsDisplayed(mPrefs));
                mDigitalClockFontPref.setVisible(isDigitalClock);
                mDigitalClockFontSizePref.setVisible(isDigitalClock);
            }

            case KEY_CLOCK_DIAL, KEY_CLOCK_DIAL_MATERIAL, KEY_CLOCK_SECOND_HAND, KEY_HOME_TIME_ZONE,
                 KEY_SORT_CITIES -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_DISPLAY_CLOCK_SECONDS -> {
                mClockSecondHandPref.setVisible((boolean) newValue && SettingsDAO.getClockStyle(mPrefs) == DataModel.ClockStyle.ANALOG);

                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_AUTO_HOME_CLOCK -> {
                mHomeTimeZonePref.setEnabled((boolean) newValue);

                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_DISPLAY_TEXT_UPPERCASE -> Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            case KEY_ENABLE_CITY_NOTE -> {
                Utils.performHapticFeedback(getView(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                WidgetUtils.scheduleWidgetUpdate(requireContext(), DigitalAppWidgetProvider.class);
            }
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
                mDigitalClockFontPref, fontPickerLauncher, SettingsDAO.getDigitalClockFont(mPrefs), KEY_DIGITAL_CLOCK_FONT, true, null);
        }

        return true;
    }

    private void setupPreferences() {
        final boolean isAnalogClock = mClockStylePref.getValue().equals(mAnalogClock);
        final boolean isMaterialAnalogClock = mClockStylePref.getValue().equals(mMaterialAnalogClock);
        final boolean isDigitalClock = mClockStylePref.getValue().equals(mDigitalClock);

        mClockStylePref.setSummary(mClockStylePref.getEntry());
        mClockStylePref.setOnPreferenceChangeListener(this);

        mClockDialPref.setVisible(isAnalogClock);
        mClockDialPref.setSummary(mClockDialPref.getEntry());
        mClockDialPref.setOnPreferenceChangeListener(this);

        mClockDialMaterialPref.setVisible(isMaterialAnalogClock);
        mClockDialMaterialPref.setSummary(mClockDialMaterialPref.getEntry());
        mClockDialMaterialPref.setOnPreferenceChangeListener(this);

        mAnalogClockSizePref.setVisible(!isDigitalClock);

        mDisplayClockSecondsPref.setOnPreferenceChangeListener(this);

        mClockSecondHandPref.setVisible(isAnalogClock && SettingsDAO.areClockSecondsDisplayed(mPrefs));
        mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
        mClockSecondHandPref.setOnPreferenceChangeListener(this);

        mDigitalClockFontPref.setVisible(isDigitalClock);
        mDigitalClockFontPref.setTitle(getString(SettingsDAO.getDigitalClockFont(mPrefs) == null
            ? R.string.custom_font_title
            : R.string.custom_font_title_variant));
        mDigitalClockFontPref.setOnPreferenceClickListener(this);

        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);

        mDigitalClockFontSizePref.setVisible(isDigitalClock);

        mSortCitiesPref.setSummary(mSortCitiesPref.getEntry());
        mSortCitiesPref.setOnPreferenceChangeListener(this);

        mEnableCityNotePref.setOnPreferenceChangeListener(this);

        mAutoHomeClockPref.setOnPreferenceChangeListener(this);

        mHomeTimeZonePref.setEnabled(SettingsDAO.getAutoShowHomeClock(mPrefs));
        // Reconstruct the timezone list.
        final TimeZones timezones = SettingsDAO.getTimeZones(requireContext(), System.currentTimeMillis());
        mHomeTimeZonePref.setEntryValues(timezones.timeZoneIds());
        mHomeTimeZonePref.setEntries(timezones.timeZoneNames());
        mHomeTimeZonePref.setSummary(mHomeTimeZonePref.getEntry());
        mHomeTimeZonePref.setOnPreferenceChangeListener(this);

        mDateTimePref.setOnPreferenceClickListener(this);
    }

    private void nullifyAllPrefs() {
        mClockStylePref = null;
        mClockDialPref = null;
        mClockDialMaterialPref = null;
        mAnalogClockSizePref = null;
        mDisplayClockSecondsPref = null;
        mClockSecondHandPref = null;
        mDigitalClockFontPref = null;
        mDisplayTextUppercasePref = null;
        mDigitalClockFontSizePref = null;
        mSortCitiesPref = null;
        mEnableCityNotePref = null;
        mAutoHomeClockPref = null;
        mHomeTimeZonePref = null;
        mDateTimePref = null;

        mClockStyleValues = null;
        mAnalogClock = null;
        mMaterialAnalogClock = null;
        mDigitalClock = null;
    }

}
