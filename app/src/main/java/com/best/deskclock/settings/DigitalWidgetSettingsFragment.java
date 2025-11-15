// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_HIDE_AM_PM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DISPLAY_DATE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DISPLAY_SECONDS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED;

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
import com.best.deskclock.widgets.standardwidgets.DigitalAppWidgetProvider;

import java.util.List;

public class DigitalWidgetSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplayTextShadowPref;
    SwitchPreferenceCompat mDisplaySecondsPref;
    SwitchPreferenceCompat mHideAmPmPref;
    SwitchPreferenceCompat mShowBackgroundOnDigitalWidgetPref;
    SwitchPreferenceCompat mCustomizeBackgroundCornerRadiusPref;
    Preference mBackgroundCornerRadiusPref;
    SwitchPreferenceCompat mDisplayDatePref;
    SwitchPreferenceCompat mDisplayNextAlarmPref;
    SwitchPreferenceCompat mShowCitiesOnDigitalWidgetPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    ColorPickerPreference mBackgroundColorPref;
    SwitchPreferenceCompat mDefaultClockColorPref;
    ColorPickerPreference mCustomClockColorPref;
    SwitchPreferenceCompat mDefaultDateColorPref;
    ColorPickerPreference mCustomDateColorPref;
    SwitchPreferenceCompat mDefaultNextAlarmColorPref;
    ColorPickerPreference mCustomNextAlarmColorPref;
    SwitchPreferenceCompat mDefaultCityClockColorPref;
    ColorPickerPreference mCustomCityClockColorPref;
    SwitchPreferenceCompat mDefaultCityNameColorPref;
    ColorPickerPreference mCustomCityNameColorPref;
    CustomSeekbarPreference mDigitalWidgetMaxClockFontSizePref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.digital_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_digital_widget);

        mDisplayTextShadowPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW);
        mDisplaySecondsPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_SECONDS);
        mHideAmPmPref = findPreference(KEY_DIGITAL_WIDGET_HIDE_AM_PM);
        mShowBackgroundOnDigitalWidgetPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
        mCustomizeBackgroundCornerRadiusPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
        mBackgroundCornerRadiusPref = findPreference(KEY_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS);
        mDisplayDatePref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_DATE);
        mDisplayNextAlarmPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
        mShowCitiesOnDigitalWidgetPref = findPreference(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
        mApplyHorizontalPaddingPref = findPreference(KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING);
        mBackgroundColorPref = findPreference(KEY_DIGITAL_WIDGET_BACKGROUND_COLOR);
        mDefaultClockColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR);
        mCustomClockColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR);
        mDefaultDateColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR);
        mCustomDateColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR);
        mDefaultNextAlarmColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR);
        mCustomNextAlarmColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR);
        mDefaultCityClockColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR);
        mCustomCityClockColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR);
        mDefaultCityNameColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR);
        mCustomCityNameColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR);
        mDigitalWidgetMaxClockFontSizePref = findPreference(KEY_DIGITAL_WIDGET_MAXIMUM_CLOCK_FONT_SIZE);

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

        updateDigitalWidget();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW, KEY_DIGITAL_WIDGET_DISPLAY_SECONDS,
                 KEY_DIGITAL_WIDGET_HIDE_AM_PM, KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING ->
                    Utils.setVibrationTime(requireContext(), 50);

            case KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND -> {
                mCustomizeBackgroundCornerRadiusPref.setVisible((boolean) newValue);
                mBackgroundCornerRadiusPref.setVisible((boolean) newValue
                        && WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(mPrefs));
                mBackgroundColorPref.setVisible((boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> {
                mBackgroundCornerRadiusPref.setVisible((boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DISPLAY_DATE -> {
                mDefaultDateColorPref.setVisible((boolean) newValue);
                mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible()
                        && !WidgetDAO.isDigitalWidgetDefaultDateColor(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM -> {
                mDefaultNextAlarmColorPref.setVisible((boolean) newValue);
                mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
                        && !WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED -> {
                mDefaultCityClockColorPref.setVisible((boolean) newValue);
                mCustomCityClockColorPref.setVisible((boolean) newValue
                        && !WidgetDAO.isDigitalWidgetDefaultCityClockColor(mPrefs));
                mDefaultCityNameColorPref.setVisible((boolean) newValue);
                mCustomCityNameColorPref.setVisible((boolean) newValue
                        && !WidgetDAO.isDigitalWidgetDefaultCityNameColor(mPrefs));
                mDigitalWidgetMaxClockFontSizePref.setEnabled(!(boolean) newValue);
                if ((boolean) newValue) {
                    mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.digital_widget_message_summary);
                } else {
                    mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.max_clock_font_size_title);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR -> {
                mCustomClockColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR -> {
                mCustomDateColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> {
                mCustomNextAlarmColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR -> {
                mCustomCityClockColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR -> {
                mCustomCityNameColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), DigitalAppWidgetProvider.class);
        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ColorPickerPreference colorPickerPref) {
            colorPickerPref.showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    private void setupPreferences() {
        final boolean areWorldCitiesDisplayed = WidgetDAO.areWorldCitiesDisplayedOnDigitalWidget(mPrefs);
        final boolean showHomeClock = SettingsDAO.getShowHomeClock(requireContext(), mPrefs);
        List<City> selectedCities = DataModel.getDataModel().getSelectedCities();

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mHideAmPmPref.setVisible(!DateFormat.is24HourFormat(requireContext()));
        mHideAmPmPref.setOnPreferenceChangeListener(this);

        mShowBackgroundOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mCustomizeBackgroundCornerRadiusPref.setVisible(WidgetDAO.isBackgroundDisplayedOnDigitalWidget(mPrefs));
        mCustomizeBackgroundCornerRadiusPref.setOnPreferenceChangeListener(this);

        mBackgroundCornerRadiusPref.setVisible(WidgetDAO.isBackgroundDisplayedOnDigitalWidget(mPrefs)
                && WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(mPrefs));

        mDisplayDatePref.setOnPreferenceChangeListener(this);

        mDisplayNextAlarmPref.setOnPreferenceChangeListener(this);

        mShowCitiesOnDigitalWidgetPref.setVisible(!selectedCities.isEmpty() || showHomeClock);
        mShowCitiesOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);

        mBackgroundColorPref.setVisible(WidgetDAO.isBackgroundDisplayedOnDigitalWidget(mPrefs));
        mBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultClockColorPref.setOnPreferenceChangeListener(this);

        mCustomClockColorPref.setVisible(!WidgetDAO.isDigitalWidgetDefaultClockColor(mPrefs));
        mCustomClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultDateColorPref.setVisible(WidgetDAO.isDateDisplayedOnDigitalWidget(mPrefs));
        mDefaultDateColorPref.setOnPreferenceChangeListener(this);

        mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible()
                && !WidgetDAO.isDigitalWidgetDefaultDateColor(mPrefs));
        mCustomDateColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmColorPref.setVisible(WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(mPrefs));
        mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
                && !WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(mPrefs));
        mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
                && areWorldCitiesDisplayed);
        mDefaultCityClockColorPref.setOnPreferenceChangeListener(this);

        mCustomCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
                && areWorldCitiesDisplayed
                && !WidgetDAO.isDigitalWidgetDefaultCityClockColor(mPrefs));
        mCustomCityClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
                && areWorldCitiesDisplayed);
        mDefaultCityNameColorPref.setOnPreferenceChangeListener(this);

        mCustomCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
                && areWorldCitiesDisplayed
                && !WidgetDAO.isDigitalWidgetDefaultCityNameColor(mPrefs));
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
        mDisplayTextShadowPref.setChecked(WidgetDAO.isTextShadowDisplayedOnDigitalWidget(mPrefs));
        mDisplaySecondsPref.setChecked(WidgetDAO.areSecondsDisplayedOnDigitalWidget(mPrefs));
        mHideAmPmPref.setChecked(WidgetDAO.isAmPmHiddenOnDigitalWidget(mPrefs));
        mShowBackgroundOnDigitalWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnDigitalWidget(mPrefs));
        mCustomizeBackgroundCornerRadiusPref.setChecked(WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(mPrefs));
        mDisplayDatePref.setChecked(WidgetDAO.isDateDisplayedOnDigitalWidget(mPrefs));
        mDisplayNextAlarmPref.setChecked(WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(mPrefs));
        mShowCitiesOnDigitalWidgetPref.setChecked(WidgetDAO.areWorldCitiesDisplayedOnDigitalWidget(mPrefs));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isDigitalWidgetHorizontalPaddingApplied(mPrefs));
        mDefaultClockColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultClockColor(mPrefs));
        mDefaultDateColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultDateColor(mPrefs));
        mDefaultNextAlarmColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(mPrefs));
        mDefaultCityClockColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultCityClockColor(mPrefs));
        mDefaultCityNameColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultCityNameColor(mPrefs));
    }

    private void updateDigitalWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        DigitalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}
