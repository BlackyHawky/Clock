// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_DATE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_HIDE_AM_PM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_SECONDS_DISPLAYED;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.materialyouwidgets.MaterialYouDigitalAppWidgetProvider;

import com.rarepebble.colorpicker.ColorPreference;

import java.util.List;

public class MaterialYouDigitalWidgetSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplaySecondsPref;
    SwitchPreferenceCompat mHideAmPmPref;
    SwitchPreferenceCompat mShowBackgroundOnDigitalWidgetPref;
    SwitchPreferenceCompat mDisplayDatePref;
    SwitchPreferenceCompat mDisplayNextAlarmPref;
    SwitchPreferenceCompat mShowCitiesOnDigitalWidgetPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    SwitchPreferenceCompat mDefaultBackgroundColorPref;
    ColorPreference mCustomBackgroundColorPref;
    SwitchPreferenceCompat mDefaultClockColorPref;
    ColorPreference mCustomClockColorPref;
    SwitchPreferenceCompat mDefaultDateColorPref;
    ColorPreference mCustomDateColorPref;
    SwitchPreferenceCompat mDefaultNextAlarmColorPref;
    ColorPreference mCustomNextAlarmColorPref;
    SwitchPreferenceCompat mDefaultCityClockColorPref;
    ColorPreference mCustomCityClockColorPref;
    SwitchPreferenceCompat mDefaultCityNameColorPref;
    ColorPreference mCustomCityNameColorPref;
    CustomSeekbarPreference mDigitalWidgetMaxClockFontSizePref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.material_you_digital_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_material_you_digital_widget);

        mDisplaySecondsPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_SECONDS_DISPLAYED);
        mHideAmPmPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_HIDE_AM_PM);
        mShowBackgroundOnDigitalWidgetPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
        mDisplayDatePref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_DATE);
        mDisplayNextAlarmPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
        mShowCitiesOnDigitalWidgetPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
        mApplyHorizontalPaddingPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING);
        mDefaultBackgroundColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR);
        mCustomBackgroundColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_BACKGROUND_COLOR);
        mDefaultClockColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR);
        mCustomClockColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR);
        mDefaultDateColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR);
        mCustomDateColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_DATE_COLOR);
        mDefaultNextAlarmColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR);
        mCustomNextAlarmColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR);
        mDefaultCityClockColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR);
        mCustomCityClockColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR);
        mDefaultCityNameColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR);
        mCustomCityNameColorPref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR);
        mDigitalWidgetMaxClockFontSizePref = findPreference(KEY_MATERIAL_YOU_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE);

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

        updateMaterialYouDigitalWidget();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
        case KEY_MATERIAL_YOU_DIGITAL_WIDGET_SECONDS_DISPLAYED, KEY_MATERIAL_YOU_DIGITAL_WIDGET_HIDE_AM_PM,
             KEY_MATERIAL_YOU_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING ->
                Utils.setVibrationTime(requireContext(), 50);

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_BACKGROUND -> {
                mDefaultBackgroundColorPref.setVisible((boolean) newValue);
                mCustomBackgroundColorPref.setVisible((boolean) newValue &&
                        !WidgetDAO.isMaterialYouDigitalWidgetDefaultBackgroundColor(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_DATE -> {
                mDefaultDateColorPref.setVisible((boolean) newValue);
                mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible()
                        && !WidgetDAO.isMaterialYouDigitalWidgetDefaultDateColor(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM -> {
                mDefaultNextAlarmColorPref.setVisible((boolean) newValue);
                mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
                        && !WidgetDAO.isMaterialYouDigitalWidgetDefaultNextAlarmColor(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED -> {
                mDefaultCityClockColorPref.setVisible((boolean) newValue);
                mCustomCityClockColorPref.setVisible((boolean) newValue
                        && !WidgetDAO.isMaterialYouDigitalWidgetDefaultCityClockColor(mPrefs));
                mDefaultCityNameColorPref.setVisible((boolean) newValue);
                mCustomCityNameColorPref.setVisible((boolean) newValue
                        && !WidgetDAO.isMaterialYouDigitalWidgetDefaultCityNameColor(mPrefs));
                mDigitalWidgetMaxClockFontSizePref.setEnabled(!(boolean) newValue);
                if ((boolean) newValue) {
                    mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.digital_widget_message_summary);
                } else {
                    mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.max_clock_font_size_title);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR -> {
                mCustomBackgroundColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR -> {
                mCustomClockColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_DATE_COLOR -> {
                mCustomDateColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> {
                mCustomNextAlarmColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR -> {
                mCustomCityClockColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR -> {
                mCustomCityNameColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), MaterialYouDigitalAppWidgetProvider.class);
        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ColorPreference) {
            ((ColorPreference) preference).showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    private void setupPreferences() {
        final boolean areWorldCitiesDisplayed = WidgetDAO.areWorldCitiesDisplayedOnMaterialYouDigitalWidget(mPrefs);
        List<City> selectedCities = DataModel.getDataModel().getSelectedCities();
        final boolean showHomeClock = SettingsDAO.getShowHomeClock(requireContext(), mPrefs);

        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mHideAmPmPref.setVisible(!DateFormat.is24HourFormat(requireContext()));
        mHideAmPmPref.setOnPreferenceChangeListener(this);

        mShowBackgroundOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mDisplayDatePref.setOnPreferenceChangeListener(this);

        mDisplayNextAlarmPref.setOnPreferenceChangeListener(this);

        mShowCitiesOnDigitalWidgetPref.setVisible(!selectedCities.isEmpty() || showHomeClock);
        mShowCitiesOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);

        mDefaultBackgroundColorPref.setVisible(WidgetDAO.isBackgroundDisplayedOnMaterialYouDigitalWidget(mPrefs));
        mDefaultBackgroundColorPref.setOnPreferenceChangeListener(this);

        mCustomBackgroundColorPref.setVisible(WidgetDAO.isBackgroundDisplayedOnMaterialYouDigitalWidget(mPrefs)
                && !WidgetDAO.isMaterialYouDigitalWidgetDefaultBackgroundColor(mPrefs));
        mCustomBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultClockColorPref.setOnPreferenceChangeListener(this);

        mCustomClockColorPref.setVisible(!WidgetDAO.isMaterialYouDigitalWidgetDefaultClockColor(mPrefs));
        mCustomClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultDateColorPref.setVisible(WidgetDAO.isDateDisplayedOnMaterialYouDigitalWidget(mPrefs));
        mDefaultDateColorPref.setOnPreferenceChangeListener(this);

        mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible()
                && !WidgetDAO.isMaterialYouDigitalWidgetDefaultDateColor(mPrefs));
        mCustomDateColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmColorPref.setVisible(WidgetDAO.isNextAlarmDisplayedOnMaterialYouDigitalWidget(mPrefs));
        mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
                && !WidgetDAO.isMaterialYouDigitalWidgetDefaultNextAlarmColor(mPrefs));
        mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
                && areWorldCitiesDisplayed);
        mDefaultCityClockColorPref.setOnPreferenceChangeListener(this);

        mCustomCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
                && areWorldCitiesDisplayed
                && !WidgetDAO.isMaterialYouDigitalWidgetDefaultCityClockColor(mPrefs));
        mCustomCityClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
                && areWorldCitiesDisplayed);
        mDefaultCityNameColorPref.setOnPreferenceChangeListener(this);

        mCustomCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
                && areWorldCitiesDisplayed
                && !WidgetDAO.isMaterialYouDigitalWidgetDefaultCityNameColor(mPrefs));
        mCustomCityNameColorPref.setOnPreferenceChangeListener(this);

        if (mShowCitiesOnDigitalWidgetPref.isVisible() && areWorldCitiesDisplayed) {
            mDigitalWidgetMaxClockFontSizePref.setEnabled(false);
            mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.digital_widget_message_summary);
        } else {
            mDigitalWidgetMaxClockFontSizePref.setEnabled(true);
            mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.max_clock_font_size_title);
        }
    }

    private void saveCheckedPreferenceStates() {
        mDisplaySecondsPref.setChecked(WidgetDAO.areSecondsDisplayedOnMaterialYouDigitalWidget(mPrefs));
        mHideAmPmPref.setChecked(WidgetDAO.isAmPmHiddenOnMaterialYouDigitalWidget(mPrefs));
        mShowBackgroundOnDigitalWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnMaterialYouDigitalWidget(mPrefs));
        mDisplayDatePref.setChecked(WidgetDAO.isDateDisplayedOnMaterialYouDigitalWidget(mPrefs));
        mDisplayNextAlarmPref.setChecked(WidgetDAO.isNextAlarmDisplayedOnMaterialYouDigitalWidget(mPrefs));
        mShowCitiesOnDigitalWidgetPref.setChecked(WidgetDAO.areWorldCitiesDisplayedOnMaterialYouDigitalWidget(mPrefs));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isMaterialYouDigitalWidgetHorizontalPaddingApplied(mPrefs));
        mDefaultBackgroundColorPref.setChecked(WidgetDAO.isMaterialYouDigitalWidgetDefaultBackgroundColor(mPrefs));
        mDefaultClockColorPref.setChecked(WidgetDAO.isMaterialYouDigitalWidgetDefaultClockColor(mPrefs));
        mDefaultDateColorPref.setChecked(WidgetDAO.isMaterialYouDigitalWidgetDefaultDateColor(mPrefs));
        mDefaultNextAlarmColorPref.setChecked(WidgetDAO.isMaterialYouDigitalWidgetDefaultNextAlarmColor(mPrefs));
        mDefaultCityClockColorPref.setChecked(WidgetDAO.isMaterialYouDigitalWidgetDefaultCityClockColor(mPrefs));
        mDefaultCityNameColorPref.setChecked(WidgetDAO.isMaterialYouDigitalWidgetDefaultCityNameColor(mPrefs));
    }

    private void updateMaterialYouDigitalWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        MaterialYouDigitalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}
