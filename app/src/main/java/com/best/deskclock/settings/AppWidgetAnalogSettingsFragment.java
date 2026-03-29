// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.best.deskclock.settings.PreferencesDefaultValues.ANALOG_WIDGET_CLOCK_DIAL_FLOWER;
import static com.best.deskclock.settings.PreferencesDefaultValues.ANALOG_WIDGET_CLOCK_DIAL_SUN;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.AnalogAppWidgetProvider;

public class AppWidgetAnalogSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    String[] mClockDialValues;
    String mClockDialFlower;
    String mClockDialSun;

    ListPreference mClockDialPref;
    SwitchPreferenceCompat mDisplaySecondsPref;
    ListPreference mClockSecondHandPref;
    PreferenceCategory mWidgetColorCategory;
    SwitchPreferenceCompat mDefaultDialColorPref;
    ColorPickerPreference mDialColorPref;
    SwitchPreferenceCompat mDefaultHourHandColorPref;
    ColorPickerPreference mHourHandColorPref;
    SwitchPreferenceCompat mDefaultMinuteHandColorPref;
    ColorPickerPreference mMinuteHandColorPref;
    SwitchPreferenceCompat mDefaultSecondHandColorPref;
    ColorPickerPreference mSecondHandColorPref;

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

        mClockDialValues = getResources().getStringArray(R.array.analog_widget_clock_dial_values);
        mClockDialSun = mClockDialValues[4];
        mClockDialFlower = mClockDialValues[5];

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
            case KEY_ANALOG_WIDGET_CLOCK_DIAL -> {
                final int index = mClockDialPref.findIndexOfValue((String) newValue);
                final boolean isSunOrFlowerClockDial = newValue.equals(mClockDialSun) || newValue.equals(mClockDialFlower);
                final boolean isSecondHandEnabled = SdkUtils.isAtLeastAndroid12()
                    && WidgetDAO.isSecondHandDisplayedOnAnalogWidget(mPrefs);

                mClockDialPref.setSummary(mClockDialPref.getEntries()[index]);

                mClockSecondHandPref.setVisible(isSecondHandEnabled && !isSunOrFlowerClockDial);
            }

            case KEY_ANALOG_WIDGET_CLOCK_SECOND_HAND -> {
                final ListPreference preference = (ListPreference) pref;
                final int index = preference.findIndexOfValue((String) newValue);
                preference.setSummary(preference.getEntries()[index]);
            }

            case KEY_ANALOG_WIDGET_WITH_SECOND_HAND -> {
                final boolean isSecondHandDisplayed = (boolean) newValue;
                final String clockDial = WidgetDAO.getAnalogWidgetClockDial(mPrefs);
                final boolean isSunOrFlowerClockDial = clockDial.equals(ANALOG_WIDGET_CLOCK_DIAL_SUN)
                    || clockDial.equals(ANALOG_WIDGET_CLOCK_DIAL_FLOWER);

                mClockSecondHandPref.setVisible(isSecondHandDisplayed && !isSunOrFlowerClockDial);
                mDefaultSecondHandColorPref.setVisible(isSecondHandDisplayed);
                mSecondHandColorPref.setVisible(isSecondHandDisplayed && !WidgetDAO.isAnalogWidgetDefaultSecondHandColor(mPrefs));
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
                mSecondHandColorPref.setVisible(!(boolean) newValue && WidgetDAO.isSecondHandDisplayedOnAnalogWidget(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), AnalogAppWidgetProvider.class);
        return true;
    }

    private void setupPreferences() {
        final boolean isAtLeastAndroid12 = SdkUtils.isAtLeastAndroid12();
        final boolean isSecondHandDisplayed = isAtLeastAndroid12 && WidgetDAO.isSecondHandDisplayedOnAnalogWidget(mPrefs);
        final boolean isSunOrFlowerClockDial = mClockDialPref.getValue().equals(mClockDialSun)
            || mClockDialPref.getValue().equals(mClockDialFlower);

        mClockDialPref.setSummary(mClockDialPref.getEntry());
        mClockDialPref.setOnPreferenceChangeListener(this);

        mDisplaySecondsPref.setVisible(isAtLeastAndroid12);
        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mClockSecondHandPref.setVisible(isSecondHandDisplayed && !isSunOrFlowerClockDial);
        mClockSecondHandPref.setSummary(mClockSecondHandPref.getEntry());
        mClockSecondHandPref.setOnPreferenceChangeListener(this);

        mWidgetColorCategory.setVisible(isAtLeastAndroid12);

        mDefaultDialColorPref.setOnPreferenceChangeListener(this);

        mDialColorPref.setVisible(!WidgetDAO.isAnalogWidgetDefaultDialColor(mPrefs));
        mDialColorPref.setOnPreferenceChangeListener(this);

        mDefaultHourHandColorPref.setOnPreferenceChangeListener(this);

        mHourHandColorPref.setVisible(!WidgetDAO.isAnalogWidgetDefaultHourHandColor(mPrefs));
        mHourHandColorPref.setOnPreferenceChangeListener(this);

        mDefaultMinuteHandColorPref.setOnPreferenceChangeListener(this);

        mMinuteHandColorPref.setVisible(!WidgetDAO.isAnalogWidgetDefaultMinuteHandColor(mPrefs));
        mMinuteHandColorPref.setOnPreferenceChangeListener(this);

        mDefaultSecondHandColorPref.setVisible(isSecondHandDisplayed);
        mDefaultSecondHandColorPref.setOnPreferenceChangeListener(this);

        mSecondHandColorPref.setVisible(isSecondHandDisplayed && !WidgetDAO.isAnalogWidgetDefaultSecondHandColor(mPrefs));
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
