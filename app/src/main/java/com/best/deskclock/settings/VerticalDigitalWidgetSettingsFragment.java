// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE;
import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE;
import static com.best.deskclock.settings.PreferencesDefaultValues.DEFAULT_WIDGETS_DEFAULT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.alarmclock.WidgetUtils;
import com.best.alarmclock.standardwidgets.VerticalDigitalAppWidgetProvider;
import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.Utils;

import com.rarepebble.colorpicker.ColorPreference;

public class VerticalDigitalWidgetSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    ColorPreference mBackgroundColorPref;
    ColorPreference mCustomHoursColorPref;
    ColorPreference mCustomMinutesColorPref;
    ColorPreference mCustomDateColorPref;
    ColorPreference mCustomNextAlarmColorPref;
    SwitchPreferenceCompat mShowBackgroundOnVerticalDigitalWidgetPref;
    SwitchPreferenceCompat mDisplayDatePref;
    SwitchPreferenceCompat mDefaultHoursColorPref;
    SwitchPreferenceCompat mDefaultMinutesColorPref;
    SwitchPreferenceCompat mDefaultDateColorPref;
    SwitchPreferenceCompat mDefaultNextAlarmColorPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.vertical_digital_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_vertical_digital_widget);

        mShowBackgroundOnVerticalDigitalWidgetPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
        mDisplayDatePref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE);
        mBackgroundColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR);
        mDefaultHoursColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR);
        mCustomHoursColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR);
        mDefaultMinutesColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR);
        mCustomMinutesColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR);
        mDefaultDateColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR);
        mCustomDateColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR);
        mDefaultNextAlarmColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR);
        mCustomNextAlarmColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR);

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
        updateVerticalDigitalWidget();
    }

    @Override
    public void onStop() {
        super.onStop();

        WidgetUtils.resetLaunchFlag();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND -> {
                if (mShowBackgroundOnVerticalDigitalWidgetPref.getSharedPreferences() != null) {
                    final boolean isNotBackgroundDisplayed = mShowBackgroundOnVerticalDigitalWidgetPref.getSharedPreferences()
                            .getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND,
                                    DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
                    mBackgroundColorPref.setVisible(!isNotBackgroundDisplayed);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR -> {
                if (mDefaultHoursColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultHoursColorPref.getSharedPreferences()
                            .getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
                    mCustomHoursColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR -> {
                if (mDefaultMinutesColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultMinutesColorPref.getSharedPreferences()
                            .getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
                    mCustomMinutesColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE -> {
                if (mDisplayDatePref.getSharedPreferences() != null) {
                    final boolean isDateHidden = mDisplayDatePref.getSharedPreferences()
                            .getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE, DEFAULT_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE);
                    mDefaultDateColorPref.setVisible(!isDateHidden);
                    mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible() && !mDefaultDateColorPref.isChecked());
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR -> {
                if (mDefaultDateColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultDateColorPref.getSharedPreferences()
                            .getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DATE_DEFAULT_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
                    mCustomDateColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> {
                if (mDefaultNextAlarmColorPref.getSharedPreferences() != null) {
                    final boolean isNotDefaultColors = mDefaultNextAlarmColorPref.getSharedPreferences()
                            .getBoolean(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR, DEFAULT_WIDGETS_DEFAULT_COLOR);
                    mCustomNextAlarmColorPref.setVisible(isNotDefaultColors);
                }
                Utils.setVibrationTime(requireContext(), 50);
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
        mShowBackgroundOnVerticalDigitalWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnVerticalDigitalWidget(mPrefs));
        mBackgroundColorPref.setVisible(mShowBackgroundOnVerticalDigitalWidgetPref.isChecked());

        mDefaultHoursColorPref.setChecked(WidgetDAO.isVerticalDigitalWidgetDefaultHoursColor(mPrefs));
        mCustomHoursColorPref.setVisible(!mDefaultHoursColorPref.isChecked());

        mDefaultMinutesColorPref.setChecked(WidgetDAO.isVerticalDigitalWidgetDefaultMinutesColor(mPrefs));
        mCustomMinutesColorPref.setVisible(!mDefaultMinutesColorPref.isChecked());

        mDisplayDatePref.setChecked(WidgetDAO.isDateDisplayedOnVerticalDigitalWidget(mPrefs));
        mDefaultDateColorPref.setVisible(mDisplayDatePref.isChecked());

        mDefaultDateColorPref.setChecked(WidgetDAO.isVerticalDigitalWidgetDefaultDateColor(mPrefs));
        mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible() && !mDefaultDateColorPref.isChecked());

        mDefaultNextAlarmColorPref.setChecked(WidgetDAO.isVerticalDigitalWidgetDefaultNextAlarmColor(mPrefs));
        mCustomNextAlarmColorPref.setVisible(!mDefaultNextAlarmColorPref.isChecked());
    }

    private void refresh() {
        mDisplayDatePref.setOnPreferenceChangeListener(this);

        mShowBackgroundOnVerticalDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultHoursColorPref.setOnPreferenceChangeListener(this);

        mCustomHoursColorPref.setOnPreferenceChangeListener(this);

        mDefaultMinutesColorPref.setOnPreferenceChangeListener(this);

        mCustomMinutesColorPref.setOnPreferenceChangeListener(this);

        mDefaultDateColorPref.setOnPreferenceChangeListener(this);

        mCustomDateColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);
    }

    private void updateVerticalDigitalWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        VerticalDigitalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}
