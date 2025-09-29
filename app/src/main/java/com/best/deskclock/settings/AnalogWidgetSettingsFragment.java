// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_CLOCK_DIAL;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_CUSTOM_DIAL_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_DEFAULT_DIAL_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_CUSTOM_HOUR_HAND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_DEFAULT_HOUR_HAND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_CUSTOM_MINUTE_HAND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_DEFAULT_MINUTE_HAND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_CUSTOM_SECOND_HAND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_DEFAULT_SECOND_HAND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_WITH_SECOND_HAND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_WIDGET_COLOR_CATEGORY;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.standardwidgets.AnalogAppWidgetProvider;

import com.rarepebble.colorpicker.ColorPreference;

public class AnalogWidgetSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    ListPreference mClockDialPref;
    SwitchPreferenceCompat mDisplaySecondsPref;
    ListPreference mClockSecondHandPref;
    PreferenceCategory mWidgetColorCategory;
    SwitchPreferenceCompat mDefaultDialColorPref;
    ColorPreference mDialColorPref;
    SwitchPreferenceCompat mDefaultHourHandColorPref;
    ColorPreference mHourHandColorPref;
    SwitchPreferenceCompat mDefaultMinuteHandColorPref;
    ColorPreference mMinuteHandColorPref;
    SwitchPreferenceCompat mDefaultSecondHandColorPref;
    ColorPreference mSecondHandColorPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.analog_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_analog_widget);

        mClockDialPref = findPreference(KEY_ANALOG_WIDGET_CLOCK_DIAL);
        mDisplaySecondsPref = findPreference(KEY_ANALOG_WIDGET_WITH_SECOND_HAND);
        mClockSecondHandPref = findPreference(KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND);
        mWidgetColorCategory = findPreference(KEY_WIDGET_COLOR_CATEGORY);
        mDefaultDialColorPref = findPreference(KEY_ANALOG_WIDGET_DEFAULT_DIAL_COLOR);
        mDialColorPref = findPreference(KEY_ANALOG_WIDGET_CUSTOM_DIAL_COLOR);
        mDefaultHourHandColorPref = findPreference(KEY_ANALOG_WIDGET_DEFAULT_HOUR_HAND_COLOR);
        mHourHandColorPref = findPreference(KEY_ANALOG_WIDGET_CUSTOM_HOUR_HAND_COLOR);
        mDefaultMinuteHandColorPref = findPreference(KEY_ANALOG_WIDGET_DEFAULT_MINUTE_HAND_COLOR);
        mMinuteHandColorPref = findPreference(KEY_ANALOG_WIDGET_CUSTOM_MINUTE_HAND_COLOR);
        mDefaultSecondHandColorPref = findPreference(KEY_ANALOG_WIDGET_DEFAULT_SECOND_HAND_COLOR);
        mSecondHandColorPref = findPreference(KEY_ANALOG_WIDGET_CUSTOM_SECOND_HAND_COLOR);

        setupPreferences();

        WidgetUtils.addFinishOnBackPressedIfLaunchedFromWidget(this);

        requireActivity().setResult(Activity.RESULT_CANCELED);

        Intent intent = requireActivity().getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mAppWidgetId = extras.getInt(EXTRA_APPWIDGET_ID, INVALID_APPWIDGET_ID);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        saveCheckedPreferenceStates();

        updateAnalogWidget();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_ANALOG_WIDGET_CLOCK_DIAL, KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_ANALOG_WIDGET_WITH_SECOND_HAND -> {
                mClockSecondHandPref.setVisible((boolean) newValue);
                mDefaultSecondHandColorPref.setVisible((boolean) newValue);
                mSecondHandColorPref.setVisible((boolean) newValue
                        && !WidgetDAO.isAnalogWidgetDefaultSecondHandColor(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ANALOG_WIDGET_DEFAULT_DIAL_COLOR -> {
                mDialColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ANALOG_WIDGET_DEFAULT_HOUR_HAND_COLOR -> {
                mHourHandColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ANALOG_WIDGET_DEFAULT_MINUTE_HAND_COLOR -> {
                mMinuteHandColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_ANALOG_WIDGET_DEFAULT_SECOND_HAND_COLOR -> {
                mSecondHandColorPref.setVisible(!(boolean) newValue
                        && WidgetDAO.isSecondHandDisplayedOnAnalogWidget(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), AnalogAppWidgetProvider.class);
        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ColorPreference) {
            ((ColorPreference) preference).showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    private void setupPreferences() {
        final boolean isSecondHandEnabled = SdkUtils.isAtLeastAndroid12()
                && WidgetDAO.isSecondHandDisplayedOnAnalogWidget(mPrefs);

        mClockDialPref.setSummary(mClockDialPref.getEntry());
        mClockDialPref.setOnPreferenceChangeListener(this);

        mDisplaySecondsPref.setVisible(SdkUtils.isAtLeastAndroid12());
        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mClockSecondHandPref.setVisible(isSecondHandEnabled);
        mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
        mClockSecondHandPref.setOnPreferenceChangeListener(this);

        mWidgetColorCategory.setVisible(SdkUtils.isAtLeastAndroid12());

        mDefaultDialColorPref.setOnPreferenceChangeListener(this);

        mDialColorPref.setVisible(!WidgetDAO.isAnalogWidgetDefaultDialColor(mPrefs));
        mDialColorPref.setOnPreferenceChangeListener(this);

        mDefaultHourHandColorPref.setOnPreferenceChangeListener(this);

        mHourHandColorPref.setVisible(!WidgetDAO.isAnalogWidgetDefaultHourHandColor(mPrefs));
        mHourHandColorPref.setOnPreferenceChangeListener(this);

        mDefaultMinuteHandColorPref.setOnPreferenceChangeListener(this);

        mMinuteHandColorPref.setVisible(!WidgetDAO.isAnalogWidgetDefaultMinuteHandColor(mPrefs));
        mMinuteHandColorPref.setOnPreferenceChangeListener(this);

        mDefaultSecondHandColorPref.setVisible(isSecondHandEnabled);
        mDefaultSecondHandColorPref.setOnPreferenceChangeListener(this);

        mSecondHandColorPref.setVisible(isSecondHandEnabled
                && !WidgetDAO.isAnalogWidgetDefaultSecondHandColor(mPrefs));
        mSecondHandColorPref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mDisplaySecondsPref.setChecked(WidgetDAO.isSecondHandDisplayedOnAnalogWidget(mPrefs));
        mDefaultDialColorPref.setChecked(WidgetDAO.isAnalogWidgetDefaultDialColor(mPrefs));
        mDefaultHourHandColorPref.setChecked(WidgetDAO.isAnalogWidgetDefaultHourHandColor(mPrefs));
        mDefaultMinuteHandColorPref.setChecked(WidgetDAO.isAnalogWidgetDefaultMinuteHandColor(mPrefs));
        mDefaultSecondHandColorPref.setChecked(WidgetDAO.isAnalogWidgetDefaultSecondHandColor(mPrefs));
    }

    private void updateAnalogWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        AnalogAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }
}
