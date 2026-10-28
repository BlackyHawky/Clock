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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.HapticFeedbackConstantsCompat;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.base.BaseSettingsScreenFragment;
import com.best.deskclock.data.City;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.settings.custompreference.ColorPickerPreference;
import com.best.deskclock.settings.custompreference.CustomSliderPreference;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.DigitalAppWidgetProvider;

import java.util.List;

public class AppWidgetDigitalSettingsFragment extends BaseSettingsScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplayTextUppercasePref;
    SwitchPreferenceCompat mDisplayTextShadowPref;
    SwitchPreferenceCompat mDisplaySecondsPref;
    SwitchPreferenceCompat mHideAmPmPref;
    SwitchPreferenceCompat mShowBackgroundOnDigitalWidgetPref;
    SwitchPreferenceCompat mCustomizeBackgroundCornerRadiusPref;
    SwitchPreferenceCompat mDisplayDatePref;
    SwitchPreferenceCompat mDisplayTopDatePref;
    SwitchPreferenceCompat mDisplayNextAlarmPref;
    SwitchPreferenceCompat mDisplayNextAlarmTitlePref;
    SwitchPreferenceCompat mShowCitiesOnDigitalWidgetPref;
    SwitchPreferenceCompat mEnableCityFlagPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    SwitchPreferenceCompat mDefaultBackgroundColorPref;
    ColorPickerPreference mCustomBackgroundColorPref;
    SwitchPreferenceCompat mDefaultClockColorPref;
    ColorPickerPreference mCustomClockColorPref;
    SwitchPreferenceCompat mDefaultDateColorPref;
    ColorPickerPreference mCustomDateColorPref;
    SwitchPreferenceCompat mDefaultNextAlarmColorPref;
    ColorPickerPreference mCustomNextAlarmColorPref;
    SwitchPreferenceCompat mDefaultNextAlarmTitleColorPref;
    ColorPickerPreference mCustomNextAlarmTitleColorPref;
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_digital_widget);

        mDisplayTextUppercasePref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_TEXT_UPPERCASE);
        mDisplayTextShadowPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW);
        mDisplaySecondsPref = findPreference(KEY_DIGITAL_WIDGET_SECONDS_DISPLAYED);
        mHideAmPmPref = findPreference(KEY_DIGITAL_WIDGET_HIDE_AM_PM);
        mShowBackgroundOnDigitalWidgetPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
        mCustomizeBackgroundCornerRadiusPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
        mDisplayDatePref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_DATE);
        mDisplayTopDatePref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_TOP_DATE);
        mDisplayNextAlarmPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
        mDisplayNextAlarmTitlePref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM_TITLE);
        mShowCitiesOnDigitalWidgetPref = findPreference(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
        mEnableCityFlagPref = findPreference(KEY_DIGITAL_WIDGET_ENABLE_CITY_FLAG);
        mApplyHorizontalPaddingPref = findPreference(KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING);
        mDefaultBackgroundColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR);
        mCustomBackgroundColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_BACKGROUND_COLOR);
        mDefaultClockColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR);
        mCustomClockColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR);
        mDefaultDateColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR);
        mCustomDateColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR);
        mDefaultNextAlarmColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR);
        mCustomNextAlarmColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR);
        mDefaultNextAlarmTitleColorPref = findPreference(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_TITLE_COLOR);
        mCustomNextAlarmTitleColorPref = findPreference(KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_TITLE_COLOR);
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

        updateMaxClockFontSizeState();

        invalidatePreferencesVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();

        saveCheckedPreferenceStates();

        updateDigitalWidget();

        updateMaxClockFontSizeState();

        invalidatePreferencesVisibility();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        switch (pref.getKey()) {
            case KEY_DIGITAL_WIDGET_DISPLAY_TEXT_UPPERCASE, KEY_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW,
                 KEY_DIGITAL_WIDGET_SECONDS_DISPLAYED, KEY_DIGITAL_WIDGET_HIDE_AM_PM, KEY_DIGITAL_WIDGET_DISPLAY_TOP_DATE,
                 KEY_DIGITAL_WIDGET_ENABLE_CITY_FLAG, KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING,
                 KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND, KEY_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS,
                 KEY_DIGITAL_WIDGET_DISPLAY_DATE, KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM,
                 KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM_TITLE, KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED,
                 KEY_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR, KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR,
                 KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR,
                 KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_TITLE_COLOR, KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR,
                 KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, KEY_DIGITAL_WIDGET_DEFAULT_CITY_NOTE_COLOR ->
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), DigitalAppWidgetProvider.class);

        if (KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED.equals(pref.getKey())) {
            updateMaxClockFontSizeState();
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
    protected boolean isPreferenceVisible(@NonNull Preference pref, @NonNull String key) {
        final boolean displayBackground = mShowBackgroundOnDigitalWidgetPref.isChecked();
        final boolean isCustomBgColor = !mDefaultBackgroundColorPref.isChecked();
        final boolean isRadiusCustomizable = mCustomizeBackgroundCornerRadiusPref.isChecked();

        final boolean displayDate = mDisplayDatePref.isChecked();

        final boolean displayNextAlarm = mDisplayNextAlarmPref.isChecked();
        final boolean displayNextAlarmTitle = displayNextAlarm && mDisplayNextAlarmTitlePref.isChecked();

        List<City> selectedCities = getDataModel().getSelectedCities();
        final boolean showHomeClock = SettingsDAO.getShowHomeClock(requireContext(), getPrefs());
        final boolean canShowCitiesCategory = SettingsDAO.isClockTabVisible(getPrefs()) && (!selectedCities.isEmpty() || showHomeClock);
        final boolean areWorldCitiesDisplayed = canShowCitiesCategory && mShowCitiesOnDigitalWidgetPref.isChecked();

        return switch (key) {
            case KEY_DIGITAL_WIDGET_HIDE_AM_PM -> !DateFormat.is24HourFormat(requireContext());

            case KEY_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> SdkUtils.isAtLeastAndroid12()
                ? displayBackground
                : (displayBackground && isCustomBgColor);

            case KEY_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS -> SdkUtils.isAtLeastAndroid12()
                ? (displayBackground && isRadiusCustomizable)
                : (displayBackground && isCustomBgColor && isRadiusCustomizable);

            case KEY_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR -> displayBackground;

            case KEY_DIGITAL_WIDGET_CUSTOM_BACKGROUND_COLOR -> displayBackground && isCustomBgColor;

            case KEY_DIGITAL_WIDGET_DISPLAY_TOP_DATE, KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR -> displayDate;

            case KEY_DIGITAL_WIDGET_CUSTOM_DATE_COLOR -> displayDate && !mDefaultDateColorPref.isChecked();

            case KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM_TITLE, KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> displayNextAlarm;

            case KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR -> displayNextAlarm && !mDefaultNextAlarmColorPref.isChecked();

            case KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_TITLE_COLOR -> displayNextAlarmTitle;

            case KEY_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_TITLE_COLOR -> displayNextAlarmTitle && !mDefaultNextAlarmTitleColorPref.isChecked();

            case KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED -> canShowCitiesCategory;

            case KEY_DIGITAL_WIDGET_ENABLE_CITY_FLAG, KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR,
                 KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR -> areWorldCitiesDisplayed;

            case KEY_DIGITAL_WIDGET_CUSTOM_CITY_CLOCK_COLOR -> areWorldCitiesDisplayed && !mDefaultCityClockColorPref.isChecked();

            case KEY_DIGITAL_WIDGET_CUSTOM_CITY_NAME_COLOR -> areWorldCitiesDisplayed && !mDefaultCityNameColorPref.isChecked();

            case KEY_DIGITAL_WIDGET_DEFAULT_CITY_NOTE_COLOR -> areWorldCitiesDisplayed && SettingsDAO.isCityNoteEnabled(getPrefs());

            case KEY_DIGITAL_WIDGET_CUSTOM_CITY_NOTE_COLOR -> areWorldCitiesDisplayed && SettingsDAO.isCityNoteEnabled(getPrefs())
                && !mDefaultCityNoteColorPref.isChecked();

            case KEY_DIGITAL_WIDGET_CUSTOM_CLOCK_COLOR -> !mDefaultClockColorPref.isChecked();

            default -> super.isPreferenceVisible(pref, key);
        };
    }

    private void updateMaxClockFontSizeState() {
        List<City> selectedCities = getDataModel().getSelectedCities();
        final boolean showHomeClock = SettingsDAO.getShowHomeClock(requireContext(), getPrefs());
        final boolean canShowCitiesCategory = SettingsDAO.isClockTabVisible(getPrefs()) && (!selectedCities.isEmpty() || showHomeClock);
        final boolean areWorldCitiesDisplayed = canShowCitiesCategory && mShowCitiesOnDigitalWidgetPref.isChecked();

        if (areWorldCitiesDisplayed) {
            mDigitalWidgetMaxClockFontSizePref.setEnabled(false);
            mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.digital_widget_message_summary);
        } else {
            mDigitalWidgetMaxClockFontSizePref.setEnabled(true);
            mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.max_clock_font_size_title);
        }
    }

    private void setupPreferences() {
        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mHideAmPmPref.setOnPreferenceChangeListener(this);

        mShowBackgroundOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mCustomizeBackgroundCornerRadiusPref.setOnPreferenceChangeListener(this);

        mDefaultBackgroundColorPref.setOnPreferenceChangeListener(this);

        mCustomBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDisplayDatePref.setOnPreferenceChangeListener(this);

        mDisplayTopDatePref.setOnPreferenceChangeListener(this);

        mDefaultDateColorPref.setOnPreferenceChangeListener(this);

        mCustomDateColorPref.setOnPreferenceChangeListener(this);

        mDisplayNextAlarmPref.setOnPreferenceChangeListener(this);

        mDisplayNextAlarmTitlePref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mShowCitiesOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mEnableCityFlagPref.setOnPreferenceChangeListener(this);

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);

        mDefaultClockColorPref.setOnPreferenceChangeListener(this);

        mCustomClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityClockColorPref.setOnPreferenceChangeListener(this);

        mCustomCityClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityNameColorPref.setOnPreferenceChangeListener(this);

        mCustomCityNameColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityNoteColorPref.setOnPreferenceChangeListener(this);

        mCustomCityNoteColorPref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mDisplayTextUppercasePref.setChecked(WidgetDAO.isTextUppercaseDisplayedOnDigitalWidget(getPrefs()));
        mDisplayTextShadowPref.setChecked(WidgetDAO.isTextShadowDisplayedOnDigitalWidget(getPrefs()));
        mDisplaySecondsPref.setChecked(WidgetDAO.areSecondsDisplayedOnDigitalWidget(getPrefs()));
        mHideAmPmPref.setChecked(WidgetDAO.isAmPmHiddenOnDigitalWidget(getPrefs()));
        mShowBackgroundOnDigitalWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnDigitalWidget(getPrefs()));
        mCustomizeBackgroundCornerRadiusPref.setChecked(WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(getPrefs()));
        mDisplayDatePref.setChecked(WidgetDAO.isDateDisplayedOnDigitalWidget(getPrefs()));
        mDisplayTopDatePref.setChecked(WidgetDAO.isTopDateDisplayedOnDigitalWidget(getPrefs()));
        mDisplayNextAlarmPref.setChecked(WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(getPrefs()));
        mDisplayNextAlarmTitlePref.setChecked(WidgetDAO.isNextAlarmTitleDisplayedOnDigitalWidget(getPrefs()));
        mShowCitiesOnDigitalWidgetPref.setChecked(WidgetDAO.areWorldCitiesDisplayedOnDigitalWidget(getPrefs()));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isDigitalWidgetHorizontalPaddingApplied(getPrefs()));
        mDefaultBackgroundColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultBackgroundColor(getPrefs()));
        mDefaultClockColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultClockColor(getPrefs()));
        mDefaultDateColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultDateColor(getPrefs()));
        mDefaultNextAlarmColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(getPrefs()));
        mDefaultNextAlarmTitleColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultNextAlarmTitleColor(getPrefs()));
        mDefaultCityClockColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultCityClockColor(getPrefs()));
        mDefaultCityNameColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultCityNameColor(getPrefs()));
        mDefaultCityNoteColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultCityNoteColor(getPrefs()));
    }

    private void updateDigitalWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        DigitalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}
