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
    CustomSliderPreference mBackgroundCornerRadiusPref;
    SwitchPreferenceCompat mDisplayDatePref;
    SwitchPreferenceCompat mDisplayTopDatePref;
    SwitchPreferenceCompat mDisplayNextAlarmPref;
    SwitchPreferenceCompat mDisplayNextAlarmTitlePref;
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
        mBackgroundCornerRadiusPref = findPreference(KEY_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS);
        mDisplayDatePref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_DATE);
        mDisplayTopDatePref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_TOP_DATE);
        mDisplayNextAlarmPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
        mDisplayNextAlarmTitlePref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM_TITLE);
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
    }

    @Override
    public void onResume() {
        super.onResume();

        saveCheckedPreferenceStates();

        updateDigitalWidget();
    }

    @Override
    public void onDestroy() {
        nullifyPreferenceListeners(mDisplayTextUppercasePref, mDisplayTextShadowPref, mDisplaySecondsPref, mHideAmPmPref,
            mShowBackgroundOnDigitalWidgetPref, mCustomizeBackgroundCornerRadiusPref, mBackgroundCornerRadiusPref, mDisplayDatePref,
            mDisplayTopDatePref, mDisplayNextAlarmPref, mDisplayNextAlarmTitlePref, mShowCitiesOnDigitalWidgetPref,
            mApplyHorizontalPaddingPref, mDefaultBackgroundColorPref, mCustomBackgroundColorPref, mDefaultClockColorPref,
            mCustomClockColorPref, mDefaultDateColorPref, mCustomDateColorPref, mDefaultNextAlarmColorPref, mCustomNextAlarmColorPref,
            mDefaultNextAlarmTitleColorPref, mCustomNextAlarmTitleColorPref, mDefaultCityClockColorPref, mCustomCityClockColorPref,
            mDefaultCityNameColorPref, mCustomCityNameColorPref, mDefaultCityNoteColorPref, mCustomCityNoteColorPref,
            mDigitalWidgetMaxClockFontSizePref);

        nullifyAllPrefs();

        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference pref, @NonNull Object newValue) {
        switch (pref.getKey()) {
            case KEY_DIGITAL_WIDGET_DISPLAY_TEXT_UPPERCASE, KEY_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW,
                 KEY_DIGITAL_WIDGET_SECONDS_DISPLAYED, KEY_DIGITAL_WIDGET_HIDE_AM_PM, KEY_DIGITAL_WIDGET_DISPLAY_TOP_DATE,
                 KEY_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING ->
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

            case KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean displayBackground = (boolean) newValue;
                boolean isCustomColor = !WidgetDAO.isDigitalWidgetDefaultBackgroundColor(getPrefs());
                boolean isRadiusCustomizable = WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(getPrefs());

                mCustomizeBackgroundCornerRadiusPref.setVisible(SdkUtils.isAtLeastAndroid12()
                    ? displayBackground
                    : displayBackground && isCustomColor);
                mBackgroundCornerRadiusPref.setVisible(SdkUtils.isAtLeastAndroid12()
                    ? displayBackground && isRadiusCustomizable
                    : displayBackground && isCustomColor && isRadiusCustomizable);
                mDefaultBackgroundColorPref.setVisible(displayBackground);
                mCustomBackgroundColorPref.setVisible(displayBackground && isCustomColor);
            }

            case KEY_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mBackgroundCornerRadiusPref.setVisible((boolean) newValue);
            }

            case KEY_DIGITAL_WIDGET_DISPLAY_DATE -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isDateDisplayed = (boolean) newValue;

                mDisplayTopDatePref.setVisible(isDateDisplayed);
                mDefaultDateColorPref.setVisible(isDateDisplayed);
                mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible()
                    && !WidgetDAO.isDigitalWidgetDefaultDateColor(getPrefs()));
            }

            case KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isNextAlarmDisplayed = (boolean) newValue;
                boolean isNextAlarmTitleDisplayed = WidgetDAO.isNextAlarmTitleDisplayedOnDigitalWidget(getPrefs());

                mDisplayNextAlarmTitlePref.setVisible(isNextAlarmDisplayed);
                mDefaultNextAlarmTitleColorPref.setVisible(isNextAlarmDisplayed && isNextAlarmTitleDisplayed);
                mCustomNextAlarmTitleColorPref.setVisible(isNextAlarmDisplayed
                    && isNextAlarmTitleDisplayed
                    && !WidgetDAO.isDigitalWidgetDefaultNextAlarmTitleColor(getPrefs()));

                mDefaultNextAlarmColorPref.setVisible(isNextAlarmDisplayed);
                mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
                    && !WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(getPrefs()));
            }

            case KEY_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM_TITLE -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isNextAlarmTitleDisplayed = (boolean) newValue;

                mDefaultNextAlarmTitleColorPref.setVisible(isNextAlarmTitleDisplayed);
                mCustomNextAlarmTitleColorPref.setVisible(isNextAlarmTitleDisplayed
                    && !WidgetDAO.isDigitalWidgetDefaultNextAlarmTitleColor(getPrefs()));
            }

            case KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean areWorldCitiesDisplayed = (boolean) newValue;
                boolean isCityNoteEnabled = SettingsDAO.isCityNoteEnabled(getPrefs());

                mDefaultCityClockColorPref.setVisible(areWorldCitiesDisplayed);
                mCustomCityClockColorPref.setVisible(areWorldCitiesDisplayed
                    && !WidgetDAO.isDigitalWidgetDefaultCityClockColor(getPrefs()));
                mDefaultCityNameColorPref.setVisible(areWorldCitiesDisplayed);
                mCustomCityNameColorPref.setVisible(areWorldCitiesDisplayed
                    && !WidgetDAO.isDigitalWidgetDefaultCityNameColor(getPrefs()));
                mDefaultCityNoteColorPref.setVisible(areWorldCitiesDisplayed && isCityNoteEnabled);
                mCustomCityNoteColorPref.setVisible(areWorldCitiesDisplayed
                    && isCityNoteEnabled
                    && !WidgetDAO.isDigitalWidgetDefaultCityNoteColor(getPrefs()));

                mDigitalWidgetMaxClockFontSizePref.setEnabled(!areWorldCitiesDisplayed);
                if (areWorldCitiesDisplayed) {
                    mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.digital_widget_message_summary);
                } else {
                    mDigitalWidgetMaxClockFontSizePref.setTitle(R.string.max_clock_font_size_title);
                }
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);

                boolean isCustomColor = !(boolean) newValue;
                boolean displayBackground = WidgetDAO.isBackgroundDisplayedOnDigitalWidget(getPrefs());
                boolean isRadiusCustomizable = WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(getPrefs());

                mCustomBackgroundColorPref.setVisible(isCustomColor);

                if (!SdkUtils.isAtLeastAndroid12()) {
                    mCustomizeBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground);
                    mBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground && isRadiusCustomizable);
                }
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mCustomClockColorPref.setVisible(!(boolean) newValue);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR -> {
                mCustomDateColorPref.setVisible(!(boolean) newValue);
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mCustomNextAlarmColorPref.setVisible(!(boolean) newValue);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_TITLE_COLOR -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mCustomNextAlarmTitleColorPref.setVisible(!(boolean) newValue);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mCustomCityClockColorPref.setVisible(!(boolean) newValue);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mCustomCityNameColorPref.setVisible(!(boolean) newValue);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_CITY_NOTE_COLOR -> {
                Utils.performHapticFeedback(getView(), isVibrationsEnabled(), HapticFeedbackConstantsCompat.VIRTUAL_KEY);
                mCustomCityNoteColorPref.setVisible(!(boolean) newValue);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), DigitalAppWidgetProvider.class);
        return true;
    }

    private void setupPreferences() {
        final boolean areWorldCitiesDisplayed = WidgetDAO.areWorldCitiesDisplayedOnDigitalWidget(getPrefs());
        List<City> selectedCities = getDataModel().getSelectedCities();
        final boolean showHomeClock = SettingsDAO.getShowHomeClock(requireContext(), getPrefs());
        final boolean isCityNoteEnabled = SettingsDAO.isCityNoteEnabled(getPrefs());
        final boolean isNextAlarmDisplayed = WidgetDAO.isNextAlarmDisplayedOnDigitalWidget(getPrefs());
        final boolean isNextAlarmTitleDisplayed = WidgetDAO.isNextAlarmTitleDisplayedOnDigitalWidget(getPrefs());

        mDisplayTextUppercasePref.setOnPreferenceChangeListener(this);

        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mHideAmPmPref.setVisible(!DateFormat.is24HourFormat(requireContext()));
        mHideAmPmPref.setOnPreferenceChangeListener(this);

        mShowBackgroundOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        boolean isBackgroundVisible = WidgetDAO.isBackgroundDisplayedOnDigitalWidget(getPrefs());
        boolean isBackgroundCornerRadiusCustomizable = WidgetDAO.isDigitalWidgetBackgroundCornerRadiusCustomizable(getPrefs());
        boolean isCustomColor = !WidgetDAO.isDigitalWidgetDefaultBackgroundColor(getPrefs());

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

        mDisplayTopDatePref.setVisible(WidgetDAO.isDateDisplayedOnDigitalWidget(getPrefs()));
        mDisplayTopDatePref.setOnPreferenceChangeListener(this);

        mDisplayNextAlarmPref.setOnPreferenceChangeListener(this);

        mDisplayNextAlarmTitlePref.setVisible(isNextAlarmDisplayed);
        mDisplayNextAlarmTitlePref.setOnPreferenceChangeListener(this);

        mShowCitiesOnDigitalWidgetPref.setVisible(SettingsDAO.isClockTabVisible(getPrefs()) && (!selectedCities.isEmpty() || showHomeClock));
        mShowCitiesOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);

        mDefaultBackgroundColorPref.setVisible(isBackgroundVisible);
        mDefaultBackgroundColorPref.setOnPreferenceChangeListener(this);

        mCustomBackgroundColorPref.setVisible(isBackgroundVisible && isCustomColor);
        mCustomBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultClockColorPref.setOnPreferenceChangeListener(this);

        mCustomClockColorPref.setVisible(!WidgetDAO.isDigitalWidgetDefaultClockColor(getPrefs()));
        mCustomClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultDateColorPref.setVisible(WidgetDAO.isDateDisplayedOnDigitalWidget(getPrefs()));
        mDefaultDateColorPref.setOnPreferenceChangeListener(this);

        mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible() && !WidgetDAO.isDigitalWidgetDefaultDateColor(getPrefs()));
        mCustomDateColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmColorPref.setVisible(isNextAlarmDisplayed);
        mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
            && !WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(getPrefs()));
        mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmTitleColorPref.setVisible(isNextAlarmDisplayed && isNextAlarmTitleDisplayed);
        mDefaultNextAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmTitleColorPref.setVisible(isNextAlarmDisplayed
            && isNextAlarmTitleDisplayed
            && !WidgetDAO.isDigitalWidgetDefaultNextAlarmTitleColor(getPrefs()));
        mCustomNextAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible() && areWorldCitiesDisplayed);
        mDefaultCityClockColorPref.setOnPreferenceChangeListener(this);

        mCustomCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
            && areWorldCitiesDisplayed
            && !WidgetDAO.isDigitalWidgetDefaultCityClockColor(getPrefs()));
        mCustomCityClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible() && areWorldCitiesDisplayed);
        mDefaultCityNameColorPref.setOnPreferenceChangeListener(this);

        mCustomCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
            && areWorldCitiesDisplayed
            && !WidgetDAO.isDigitalWidgetDefaultCityNameColor(getPrefs()));
        mCustomCityNameColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityNoteColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible() && areWorldCitiesDisplayed && isCityNoteEnabled);
        mDefaultCityNoteColorPref.setOnPreferenceChangeListener(this);

        mCustomCityNoteColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isVisible()
            && areWorldCitiesDisplayed
            && isCityNoteEnabled
            && !WidgetDAO.isDigitalWidgetDefaultCityNoteColor(getPrefs()));
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

    private void nullifyAllPrefs() {
        mDisplayTextUppercasePref = null;
        mDisplayTextShadowPref = null;
        mDisplaySecondsPref = null;
        mHideAmPmPref = null;
        mShowBackgroundOnDigitalWidgetPref = null;
        mCustomizeBackgroundCornerRadiusPref = null;
        mBackgroundCornerRadiusPref = null;
        mDisplayDatePref = null;
        mDisplayTopDatePref = null;
        mDisplayNextAlarmPref = null;
        mDisplayNextAlarmTitlePref = null;
        mShowCitiesOnDigitalWidgetPref = null;
        mApplyHorizontalPaddingPref = null;
        mDefaultBackgroundColorPref = null;
        mCustomBackgroundColorPref = null;
        mDefaultClockColorPref = null;
        mCustomClockColorPref = null;
        mDefaultDateColorPref = null;
        mCustomDateColorPref = null;
        mDefaultNextAlarmColorPref = null;
        mCustomNextAlarmColorPref = null;
        mDefaultNextAlarmTitleColorPref = null;
        mCustomNextAlarmTitleColorPref = null;
        mDefaultCityClockColorPref = null;
        mCustomCityClockColorPref = null;
        mDefaultCityNameColorPref = null;
        mCustomCityNameColorPref = null;
        mDefaultCityNoteColorPref = null;
        mCustomCityNoteColorPref = null;
        mDigitalWidgetMaxClockFontSizePref = null;
    }

}
