// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static com.best.deskclock.settings.PreferencesKeys.KEY_ANALOG_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_ANALOG_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.best.deskclock.R;

public class WidgetSettingsFragment extends ScreenFragment implements Preference.OnPreferenceClickListener {

    Preference mAnalogWidgetCustomizationPref;
    Preference mDigitalWidgetCustomizationPref;
    Preference mVerticalDigitalWidgetCustomizationPref;
    Preference mNextAlarmWidgetCustomizationPref;
    Preference mMaterialYouAnalogWidgetCustomizationPref;
    Preference mMaterialYouDigitalWidgetCustomizationPref;
    Preference mMaterialYouVerticalDigitalWidgetCustomizationPref;
    Preference mMaterialYouNextAlarmWidgetCustomizationPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.widgets_settings);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_widgets);

        mAnalogWidgetCustomizationPref = findPreference(KEY_ANALOG_WIDGET_CUSTOMIZATION);
        mDigitalWidgetCustomizationPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOMIZATION);
        mVerticalDigitalWidgetCustomizationPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION);
        mNextAlarmWidgetCustomizationPref = findPreference(KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION);
        mMaterialYouAnalogWidgetCustomizationPref = findPreference(KEY_MATERIAL_YOU_ANALOG_WIDGET_CUSTOMIZATION);
        mMaterialYouDigitalWidgetCustomizationPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZATION);
        mMaterialYouVerticalDigitalWidgetCustomizationPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION);
        mMaterialYouNextAlarmWidgetCustomizationPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZATION);

        setupPreferences();
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference pref) {
        switch (pref.getKey()) {
            case KEY_ANALOG_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new AnalogWidgetSettingsFragment());

            case KEY_DIGITAL_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new DigitalWidgetSettingsFragment());

            case KEY_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new VerticalDigitalWidgetSettingsFragment());

            case KEY_NEXT_ALARM_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new NextAlarmWidgetSettingsFragment());

            case KEY_MATERIAL_YOU_ANALOG_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new MaterialYouAnalogWidgetSettingsFragment());

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new MaterialYouDigitalWidgetSettingsFragment());

            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new MaterialYouVerticalDigitalWidgetSettingsFragment());

            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOMIZATION ->
                    animateAndShowFragment(new MaterialYouNextAlarmWidgetSettingsFragment());
        }

        return true;
    }

    private void setupPreferences() {
        mAnalogWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mVerticalDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mNextAlarmWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mMaterialYouAnalogWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mMaterialYouDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mMaterialYouVerticalDigitalWidgetCustomizationPref.setOnPreferenceClickListener(this);

        mMaterialYouNextAlarmWidgetCustomizationPref.setOnPreferenceClickListener(this);
    }

}
