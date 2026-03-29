// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.best.deskclock.settings.PreferencesKeys.*;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.DigitalAppWidgetProvider;

import java.util.List;

public class AppWidgetDigitalSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplayTextUppercasePref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    SwitchPreferenceCompat mDisplaySecondsPref;
    SwitchPreferenceCompat mHideAmPmPref;
    SwitchPreferenceCompat mShowBackgroundOnDigitalWidgetPref;
    SwitchPreferenceCompat mCustomizeBackgroundCornerRadiusPref;
    CustomSliderPreference mBackgroundCornerRadiusPref;
    SwitchPreferenceCompat mDisplayDatePref;
    SwitchPreferenceCompat mDisplayNextAlarmPref;
    SwitchPreferenceCompat mShowCitiesOnDigitalWidgetPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    SwitchPreferenceCompat mDefaultBackgroundColorPref;
    ColorPickerPreference mCustomBackgroundColorPref;
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
    SwitchPreferenceCompat mDefaultCityNoteColorPref;
    ColorPickerPreference mCustomCityNoteColorPref;
    CustomSliderPreference mDigitalWidgetMaxClockFontSizePref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.digital_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_digital_widget);

        mDisplayTextUppercasePref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_TEXT_UPPERCASE);
        mDisplayTextShadowPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW);
        mDisplaySecondsPref = findPreference(KEY_DIGITAL_WIDGET_SECONDS_DISPLAYED);
        mHideAmPmPref = findPreference(KEY_DIGITAL_WIDGET_HIDE_AM_PM);
        mShowBackgroundOnDigitalWidgetPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
        mCustomizeBackgroundCornerRadiusPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
        mBackgroundCornerRadiusPref = findPreference(KEY_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS);
        mDisplayDatePref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_DATE);
        mDisplayNextAlarmPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
        mShowCitiesOnDigitalWidgetPref = findPreference(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
        mApplyHorizontalPaddingPref = findPreference(KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING);
        mDefaultBackgroundColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR);
        mCustomBackgroundColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_BACKGROUND_COLOR);
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
        mDefaultCityNoteColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NOTE_COLOR);
        mCustomCityNoteColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_CITY_NOTE_COLOR);
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
            case KEY_DIGITAL_WIDGET_DISPLAY_TEXT_UPPERCASE, KEY_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW,
                 KEY_DIGITAL_WIDGET_SECONDS_DISPLAYED, KEY_DIGITAL_WIDGET_HIDE_AM_PM,
                 KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING -> Utils.setVibrationTime(requireContext(), 50);

            case KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND -> {
                boolean displayBackground = (boolean) newValue;
                boolean isCustomColor = !WidgetDAO.isDigitalWidgetDefaultBackgroundColor(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(mPrefs);

                mCustomizeBackgroundCornerRadiusPref.setVisible(SdkUtils.isAtLeastAndroid12()
                    ? displayBackground
                    : displayBackground && isCustomColor);
                mBackgroundCornerRadiusPref.setVisible(SdkUtils.isAtLeastAndroid12()
                    ? displayBackground && isRadiusCustomizable
                    : displayBackground && isCustomColor && isRadiusCustomizable);
                mDefaultBackgroundColorPref.setVisible(displayBackground);
                mCustomBackgroundColorPref.setVisible(displayBackground && isCustomColor);
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
                boolean areWorldCitiesDisplayed = (boolean) newValue;
                boolean isCityNoteEnabled = SettingsDAO.isCityNoteEnabled(mPrefs);

                mDefaultCityClockColorPref.setVisible(areWorldCitiesDisplayed);
                mCustomCityClockColorPref.setVisible(areWorldCitiesDisplayed
                    && !WidgetDAO.isDigitalWidgetDefaultCityClockColor(mPrefs));
                mDefaultCityNameColorPref.setVisible(areWorldCitiesDisplayed);
                mCustomCityNameColorPref.setVisible(areWorldCitiesDisplayed
                    && !WidgetDAO.isDigitalWidgetDefaultCityNameColor(mPrefs));
                mDefaultCityNoteColorPref.setVisible(areWorldCitiesDisplayed && isCityNoteEnabled);
                mCustomCityNoteColorPref.setVisible(areWorldCitiesDisplayed
                    && isCityNoteEnabled
                    && !WidgetDAO.isDigitalWidgetDefaultCityNoteColor(mPrefs));

                mDigitalWidgetMaxClockFontSizePref.setEnabled(!areWorldCitiesDisplayed);
                if (areWorldCitiesDisplayed) {
                    mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.digital_widget_message_summary);
                } else {
                    mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.max_clock_font_size_title);
                }

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR -> {
                boolean isCustomColor = !(boolean) newValue;
                boolean displayBackground = WidgetDAO.isBackgroundDisplayedOnDigitalWidget(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(mPrefs);

                mCustomBackgroundColorPref.setVisible(isCustomColor);

                if (!SdkUtils.isAtLeastAndroid12()) {
                    mCustomizeBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground);
                    mBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground && isRadiusCustomizable);
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

            case KEY_DIGITAL_WIDGET_DEFAULT_CITY_NOTE_COLOR -> {
                mCustomCityNoteColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), DigitalAppWidgetProvider.class);
        return true;
    }

    private void setupPreferences() {
        final boolean areWorldCitiesDisplayed = WidgetDAO.areWorldCitiesDisplayedOnDigitalWidget(mPrefs);
        List<City> selectedCities = DataModel.getDataModel().getSelectedCities();
        final boolean showHomeClock = SettingsDAO.getShowHomeClock(requireContext(), mPrefs);
        final boolean isCityNoteEnabled = SettingsDAO.isCityNoteEnabled(mPrefs);

        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mHideAmPmPref.setVisible(!DateFormat.is24HourFormat(requireContext()));
        mHideAmPmPref.setOnPreferenceChangeListener(this);

        mShowBackgroundOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        boolean isBackgroundVisible = WidgetDAO.isBackgroundDisplayedOnDigitalWidget(mPrefs);
        boolean isBackgroundCornerRadiusCustomizable = WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(mPrefs);
        boolean isCustomColor = !WidgetDAO.isDigitalWidgetDefaultBackgroundColor(mPrefs);

        if (SdkUtils.isAtLeastAndroid12()) {
            mCustomizeBackgroundCornerRadiusPref.setVisible(isBackgroundVisible);
            mBackgroundCornerRadiusPref.setVisible(isBackgroundVisible && isBackgroundCornerRadiusCustomizable);
        } else {
            mCustomizeBackgroundCornerRadiusPref.setVisible(isBackgroundVisible && isCustomColor);
            mBackgroundCornerRadiusPref.setVisible(isBackgroundVisible
                && isCustomColor
                && isBackgroundCornerRadiusCustomizable);
        }

        mCustomizeBackgroundCornerRadiusPref.setOnPreferenceChangeListener(this);

        mDisplayDatePref.setOnPreferenceChangeListener(this);

        mDisplayNextAlarmPref.setOnPreferenceChangeListener(this);

        mShowCitiesOnDigitalWidgetPref.setVisible(!selectedCities.isEmpty() || showHomeClock);
        mShowCitiesOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);

        mDefaultBackgroundColorPref.setVisible(isBackgroundVisible);
        mDefaultBackgroundColorPref.setOnPreferenceChangeListener(this);

        mCustomBackgroundColorPref.setVisible(isBackgroundVisible && isCustomColor);
        mCustomBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultClockColorPref.setOnPreferenceChangeListener(this);

        mCustomClockColorPref.setVisible(!WidgetDAO.isDigitalWidgetDefaultClockColor(mPrefs));
        mCustomClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultDateColorPref.setVisible(WidgetDAO.isDateDisplayedOnDigitalWidget(mPrefs));
        mDefaultDateColorPref.setOnPreferenceChangeListener(this);

        mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible() && !WidgetDAO.isDigitalWidgetDefaultDateColor(mPrefs));
        mCustomDateColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmColorPref.setVisible(WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(mPrefs));
        mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
            && !WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(mPrefs));
        mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible() && areWorldCitiesDisplayed);
        mDefaultCityClockColorPref.setOnPreferenceChangeListener(this);

        mCustomCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
            && areWorldCitiesDisplayed
            && !WidgetDAO.isDigitalWidgetDefaultCityClockColor(mPrefs));
        mCustomCityClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible() && areWorldCitiesDisplayed);
        mDefaultCityNameColorPref.setOnPreferenceChangeListener(this);

        mCustomCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
            && areWorldCitiesDisplayed
            && !WidgetDAO.isDigitalWidgetDefaultCityNameColor(mPrefs));
        mCustomCityNameColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityNoteColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible() && areWorldCitiesDisplayed && isCityNoteEnabled);
        mDefaultCityNoteColorPref.setOnPreferenceChangeListener(this);

        mCustomCityNoteColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
            && areWorldCitiesDisplayed
            && isCityNoteEnabled
            && !WidgetDAO.isDigitalWidgetDefaultCityNoteColor(mPrefs));
        mCustomCityNoteColorPref.setOnPreferenceChangeListener(this);

        if (mShowCitiesOnDigitalWidgetPref.isVisible() && areWorldCitiesDisplayed) {
            mDigitalWidgetMaxClockFontSizePref.setEnabled(false);
            mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.digital_widget_message_summary);
        } else {
            mDigitalWidgetMaxClockFontSizePref.setEnabled(true);
            mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.max_clock_font_size_title);
        }
    }

    private void saveCheckedPreferenceStates() {
        mDisplayTextUppercasePref.setChecked(WidgetDAO.isTextUppercaseDisplayedOnDigitalWidget(mPrefs));
        mDisplayTextShadowPref.setChecked(WidgetDAO.isTextShadowDisplayedOnDigitalWidget(mPrefs));
        mDisplaySecondsPref.setChecked(WidgetDAO.areSecondsDisplayedOnDigitalWidget(mPrefs));
        mHideAmPmPref.setChecked(WidgetDAO.isAmPmHiddenOnDigitalWidget(mPrefs));
        mShowBackgroundOnDigitalWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnDigitalWidget(mPrefs));
        mCustomizeBackgroundCornerRadiusPref.setChecked(WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(mPrefs));
        mDisplayDatePref.setChecked(WidgetDAO.isDateDisplayedOnDigitalWidget(mPrefs));
        mDisplayNextAlarmPref.setChecked(WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(mPrefs));
        mShowCitiesOnDigitalWidgetPref.setChecked(WidgetDAO.areWorldCitiesDisplayedOnDigitalWidget(mPrefs));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isDigitalWidgetHorizontalPaddingApplied(mPrefs));
        mDefaultBackgroundColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultBackgroundColor(mPrefs));
        mDefaultClockColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultClockColor(mPrefs));
        mDefaultDateColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultDateColor(mPrefs));
        mDefaultNextAlarmColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(mPrefs));
        mDefaultCityClockColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultCityClockColor(mPrefs));
        mDefaultCityNameColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultCityNameColor(mPrefs));
        mDefaultCityNoteColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultCityNoteColor(mPrefs));
    }

    private void updateDigitalWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        DigitalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}
