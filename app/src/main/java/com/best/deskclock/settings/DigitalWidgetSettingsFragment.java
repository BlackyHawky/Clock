// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_DIGITAL_WIDGET_DISPLAY_DATE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_WIDGETS_DEFAULT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_BACKGROUND_COLOR;
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
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DISPLAY_DATE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_DISPLAY_SECONDS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.alarmclock.WidgetUtils;
import com.best.alarmclock.standardwidgets.DigitalAppWidgetProvider;
import com.best.deskclock.R;
import com.best.deskclock.data.City;
import com.best.deskclock.data.DataModel;
import com.best.deskclock.data.SettingsDAO;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.Utils;
import com.rarepebble.colorpicker.ColorPreference;

import java.util.ArrayList;
import java.util.List;

public class DigitalWidgetSettingsFragment extends ScreenFragment implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    ColorPreference mBackgroundColorPref;
    ColorPreference mCustomClockColorPref;
    ColorPreference mCustomDateColorPref;
    ColorPreference mCustomNextAlarmColorPref;
    ColorPreference mCustomCityClockColorPref;
    ColorPreference mCustomCityNameColorPref;
    EditTextPreference mDigitalWidgetMaxClockFontSizePref;
    SwitchPreferenceCompat mDisplaySecondsPref;
    SwitchPreferenceCompat mDisplayDatePref;
    SwitchPreferenceCompat mShowBackgroundOnDigitalWidgetPref;
    SwitchPreferenceCompat mShowCitiesOnDigitalWidgetPref;
    SwitchPreferenceCompat mDefaultClockColorPref;
    SwitchPreferenceCompat mDefaultDateColorPref;
    SwitchPreferenceCompat mDefaultNextAlarmColorPref;
    SwitchPreferenceCompat mDefaultCityClockColorPref;
    SwitchPreferenceCompat mDefaultCityNameColorPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.digital_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_digital_widget);

        mDisplaySecondsPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_SECONDS);
        mDisplayDatePref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_DATE);
        mShowBackgroundOnDigitalWidgetPref = findPreference(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
        mBackgroundColorPref = findPreference(KEY_DIGITAL_WIDGET_BACKGROUND_COLOR);
        mShowCitiesOnDigitalWidgetPref = findPreference(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);
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
        mDigitalWidgetMaxClockFontSizePref = findPreference(KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE);

        setupPreferences();

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

        refresh();
        updateDigitalWidget();
    }

    @Override
    public void onStop() {
        super.onStop();

        WidgetUtils.resetLaunchFlag();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_DIGITAL_WIDGET_DISPLAY_SECONDS -> Utils.setVibrationTime(requireContext(), 50);

            case KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND -> {
                if (mShowBackgroundOnDigitalWidgetPref.getSharedPreferences() != null) {
                    final boolean isNotBackgroundDisplayed = mShowBackgroundOnDigitalWidgetPref.getSharedPreferences()
                            .getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_BACKGROUND, DEFAULT_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
                    mBackgroundColorPref.setVisible(!isNotBackgroundDisplayed);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED -> {
                if (mShowCitiesOnDigitalWidgetPref.getSharedPreferences() != null
                        && mDefaultCityClockColorPref.getSharedPreferences() != null
                        && mDefaultCityNameColorPref.getSharedPreferences() != null) {

                    final boolean areCitiesDisplayed = mShowCitiesOnDigitalWidgetPref.getSharedPreferences()
                            .getBoolean(KEY_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED, DEFAULT_DIGITAL_WIDGET_WORLD_CITIES_DISPLAYED);

                    final boolean isCityClockDefaultColors = mDefaultCityClockColorPref.getSharedPreferences()
                            .getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);

                    final boolean isCityNameDefaultColor = mDefaultCityNameColorPref.getSharedPreferences()
                            .getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);

                    mDefaultCityClockColorPref.setVisible(!areCitiesDisplayed);
                    mCustomCityClockColorPref.setVisible(!areCitiesDisplayed && !isCityClockDefaultColors);
                    mDefaultCityNameColorPref.setVisible(!areCitiesDisplayed);
                    mCustomCityNameColorPref.setVisible(!areCitiesDisplayed && !isCityNameDefaultColor);
                    mDigitalWidgetMaxClockFontSizePref.setEnabled(areCitiesDisplayed);
                    if (!areCitiesDisplayed) {
                        mDigitalWidgetMaxClockFontSizePref.setSummary(
                                requireContext().getString(R.string.digital_widget_message_summary));
                    } else {
                        mDigitalWidgetMaxClockFontSizePref.setSummary(
                                requireContext().getString(R.string.widget_max_clock_font_size_summary)
                                        + WidgetDAO.getDigitalWidgetMaxClockFontSize(mPrefs));
                    }
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR -> {
                if (mDefaultClockColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultClockColorPref.getSharedPreferences()
                            .getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
                    mCustomClockColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DISPLAY_DATE -> {
                if (mDisplayDatePref.getSharedPreferences() != null) {
                    final boolean isDateHidden = mDisplayDatePref.getSharedPreferences()
                            .getBoolean(KEY_DIGITAL_WIDGET_DISPLAY_DATE, DEFAULT_DIGITAL_WIDGET_DISPLAY_DATE);
                    mDefaultDateColorPref.setVisible(!isDateHidden);
                    mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible() && !mDefaultDateColorPref.isChecked());
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR -> {
                if (mDefaultDateColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultDateColorPref.getSharedPreferences()
                            .getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_DATE_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
                    mCustomDateColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> {
                if (mDefaultNextAlarmColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultNextAlarmColorPref.getSharedPreferences()
                            .getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
                    mCustomNextAlarmColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR -> {
                if (mDefaultCityClockColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultCityClockColorPref.getSharedPreferences()
                            .getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_CLOCK_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
                    mCustomCityClockColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR -> {
                if (mDefaultCityNameColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultCityNameColorPref.getSharedPreferences()
                            .getBoolean(KEY_DIGITAL_WIDGET_DEFAULT_CITY_NAME_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
                    mCustomCityNameColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_DIGITAL_WIDGET_MAX_CLOCK_FONT_SIZE -> {
                final EditTextPreference digitalWidgetMaxClockFontSizePref = (EditTextPreference) pref;
                digitalWidgetMaxClockFontSizePref.setSummary(
                        requireContext().getString(R.string.widget_max_clock_font_size_summary)
                                + newValue.toString());
            }
        }

        requireContext().sendBroadcast(new Intent(ACTION_APPWIDGET_UPDATE));
        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ColorPreference) {
            ((ColorPreference) preference).showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    private void setupPreferences() {
        mShowBackgroundOnDigitalWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnDigitalWidget(mPrefs));
        mBackgroundColorPref.setVisible(mShowBackgroundOnDigitalWidgetPref.isChecked());

        List<City> selectedCities = new ArrayList<>(DataModel.getDataModel().getSelectedCities());
        final boolean showHomeClock = SettingsDAO.getShowHomeClock(requireContext(), mPrefs);
        mShowCitiesOnDigitalWidgetPref.setVisible(!selectedCities.isEmpty() || showHomeClock);
        mDefaultCityClockColorPref.setVisible(!selectedCities.isEmpty() || showHomeClock);
        mCustomCityClockColorPref.setVisible(!selectedCities.isEmpty() || showHomeClock);
        mDefaultCityNameColorPref.setVisible(!selectedCities.isEmpty() || showHomeClock);
        mCustomCityNameColorPref.setVisible(!selectedCities.isEmpty() || showHomeClock);

        mShowCitiesOnDigitalWidgetPref.setChecked(WidgetDAO.areWorldCitiesDisplayedOnDigitalWidget(mPrefs));
        if (mShowCitiesOnDigitalWidgetPref.isShown()) {
            mDefaultCityClockColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultCityClockColor(mPrefs));
            mDefaultCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked());
            mCustomCityClockColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked()
                    && !mDefaultCityClockColorPref.isChecked());

            mDefaultCityNameColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultCityNameColor(mPrefs));
            mDefaultCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked());
            mCustomCityNameColorPref.setVisible(mShowCitiesOnDigitalWidgetPref.isChecked()
                    && !mDefaultCityNameColorPref.isChecked());

            if (mShowCitiesOnDigitalWidgetPref.isChecked()) {
                mDigitalWidgetMaxClockFontSizePref.setEnabled(false);
                mDigitalWidgetMaxClockFontSizePref.setSummary(
                        requireContext().getString(R.string.digital_widget_message_summary));
            } else {
                mDigitalWidgetMaxClockFontSizePref.setEnabled(true);
                mDigitalWidgetMaxClockFontSizePref.setSummary(
                        requireContext().getString(R.string.widget_max_clock_font_size_summary)
                                + WidgetDAO.getDigitalWidgetMaxClockFontSize(mPrefs));
            }
        } else {
            mDigitalWidgetMaxClockFontSizePref.setEnabled(true);
            mDigitalWidgetMaxClockFontSizePref.setSummary(
                    requireContext().getString(R.string.widget_max_clock_font_size_summary)
                            + WidgetDAO.getDigitalWidgetMaxClockFontSize(mPrefs));
        }

        mDefaultClockColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultClockColor(mPrefs));
        mCustomClockColorPref.setVisible(!mDefaultClockColorPref.isChecked());

        mDisplayDatePref.setChecked(WidgetDAO.isDateDisplayedOnDigitalWidget(mPrefs));
        mDefaultDateColorPref.setVisible(mDisplayDatePref.isChecked());

        mDefaultDateColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultDateColor(mPrefs));
        mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible() && !mDefaultDateColorPref.isChecked());

        mDefaultNextAlarmColorPref.setChecked(WidgetDAO.isDigitalWidgetDefaultNextAlarmColor(mPrefs));
        mCustomNextAlarmColorPref.setVisible(!mDefaultNextAlarmColorPref.isChecked());
    }

    private void refresh() {
        mDisplaySecondsPref.setChecked(WidgetDAO.areSecondsDisplayedOnDigitalWidget(mPrefs));
        mDisplaySecondsPref.setOnPreferenceChangeListener(this);

        mDisplayDatePref.setOnPreferenceChangeListener(this);

        mShowBackgroundOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mBackgroundColorPref.setOnPreferenceChangeListener(this);

        mShowCitiesOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mDefaultClockColorPref.setOnPreferenceChangeListener(this);

        mCustomClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultDateColorPref.setOnPreferenceChangeListener(this);

        mCustomDateColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityClockColorPref.setOnPreferenceChangeListener(this);

        mCustomCityClockColorPref.setOnPreferenceChangeListener(this);

        mDefaultCityNameColorPref.setOnPreferenceChangeListener(this);

        mCustomCityNameColorPref.setOnPreferenceChangeListener(this);

        mDigitalWidgetMaxClockFontSizePref.setOnPreferenceChangeListener(this);
        mDigitalWidgetMaxClockFontSizePref.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.selectAll();
        });
    }

    private void updateDigitalWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        DigitalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}
