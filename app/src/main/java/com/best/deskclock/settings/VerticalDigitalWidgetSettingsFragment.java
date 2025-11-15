// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_WIDGET_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_VERTICAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.standardwidgets.VerticalDigitalAppWidgetProvider;

public class VerticalDigitalWidgetSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mDisplayTextShadowPref;
    SwitchPreferenceCompat mShowBackgroundOnVerticalDigitalWidgetPref;
    SwitchPreferenceCompat mCustomizeBackgroundCornerRadiusPref;
    Preference mBackgroundCornerRadiusPref;
    SwitchPreferenceCompat mDisplayDatePref;
    SwitchPreferenceCompat mDisplayNextAlarmPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    ColorPickerPreference mBackgroundColorPref;
    SwitchPreferenceCompat mDefaultHoursColorPref;
    ColorPickerPreference mCustomHoursColorPref;
    SwitchPreferenceCompat mDefaultMinutesColorPref;
    ColorPickerPreference mCustomMinutesColorPref;
    SwitchPreferenceCompat mDefaultDateColorPref;
    ColorPickerPreference mCustomDateColorPref;
    SwitchPreferenceCompat mDefaultNextAlarmColorPref;
    ColorPickerPreference mCustomNextAlarmColorPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.vertical_digital_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_vertical_digital_widget);

        mDisplayTextShadowPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW);
        mShowBackgroundOnVerticalDigitalWidgetPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
        mCustomizeBackgroundCornerRadiusPref = findPreference(KEY_VERTICAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
        mBackgroundCornerRadiusPref = findPreference(KEY_VERTICAL_WIDGET_BACKGROUND_CORNER_RADIUS);
        mDisplayDatePref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE);
        mDisplayNextAlarmPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
        mApplyHorizontalPaddingPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING);
        mBackgroundColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_BACKGROUND_COLOR);
        mDefaultHoursColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR);
        mCustomHoursColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR);
        mDefaultMinutesColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR);
        mCustomMinutesColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR);
        mDefaultDateColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR);
        mCustomDateColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR);
        mDefaultNextAlarmColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR);
        mCustomNextAlarmColorPref = findPreference(KEY_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR);

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

        updateVerticalDigitalWidget();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_TEXT_SHADOW,
                 KEY_VERTICAL_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING ->
                    Utils.setVibrationTime(requireContext(), 50);

            case KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND -> {
                mCustomizeBackgroundCornerRadiusPref.setVisible((boolean) newValue);
                mBackgroundCornerRadiusPref.setVisible((boolean) newValue
                        && WidgetDAO.isVerticalWidgetBackgroundCornerRadiusCustomizable(mPrefs));
                mBackgroundColorPref.setVisible((boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> {
                mBackgroundCornerRadiusPref.setVisible((boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE -> {
                mDefaultDateColorPref.setVisible((boolean) newValue);
                mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible()
                        && !WidgetDAO.isVerticalDigitalWidgetDefaultDateColor(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM -> {
                mDefaultNextAlarmColorPref.setVisible((boolean) newValue);
                mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
                        && !WidgetDAO.isVerticalDigitalWidgetDefaultNextAlarmColor(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR -> {
                mCustomHoursColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR -> {
                mCustomMinutesColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR -> {
                mCustomDateColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> {
                mCustomNextAlarmColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), VerticalDigitalAppWidgetProvider.class);
        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ColorPickerPreference colorPickerPref) {
            colorPickerPref.showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    private void setupPreferences() {
        mDisplayTextShadowPref.setOnPreferenceChangeListener(this);

        mShowBackgroundOnVerticalDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mCustomizeBackgroundCornerRadiusPref.setVisible(WidgetDAO.isBackgroundDisplayedOnVerticalDigitalWidget(mPrefs));
        mCustomizeBackgroundCornerRadiusPref.setOnPreferenceChangeListener(this);

        mBackgroundCornerRadiusPref.setVisible(WidgetDAO.isBackgroundDisplayedOnVerticalDigitalWidget(mPrefs)
                && WidgetDAO.isVerticalWidgetBackgroundCornerRadiusCustomizable(mPrefs));

        mDisplayDatePref.setOnPreferenceChangeListener(this);

        mDisplayNextAlarmPref.setOnPreferenceChangeListener(this);

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);

        mBackgroundColorPref.setVisible(WidgetDAO.isBackgroundDisplayedOnVerticalDigitalWidget(mPrefs));
        mBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultHoursColorPref.setOnPreferenceChangeListener(this);

        mCustomHoursColorPref.setVisible(!WidgetDAO.isVerticalDigitalWidgetDefaultHoursColor(mPrefs));
        mCustomHoursColorPref.setOnPreferenceChangeListener(this);

        mDefaultMinutesColorPref.setOnPreferenceChangeListener(this);

        mCustomMinutesColorPref.setVisible(!WidgetDAO.isVerticalDigitalWidgetDefaultMinutesColor(mPrefs));
        mCustomMinutesColorPref.setOnPreferenceChangeListener(this);

        mDefaultDateColorPref.setVisible(WidgetDAO.isDateDisplayedOnVerticalDigitalWidget(mPrefs));
        mDefaultDateColorPref.setOnPreferenceChangeListener(this);

        mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible()
                && !WidgetDAO.isVerticalDigitalWidgetDefaultDateColor(mPrefs));
        mCustomDateColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmColorPref.setVisible(WidgetDAO.isNextAlarmDisplayedOnVerticalDigitalWidget(mPrefs));
        mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
                && !WidgetDAO.isVerticalDigitalWidgetDefaultNextAlarmColor(mPrefs));
        mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mDisplayTextShadowPref.setChecked(WidgetDAO.isTextShadowDisplayedOnVerticalDigitalWidget(mPrefs));
        mShowBackgroundOnVerticalDigitalWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnVerticalDigitalWidget(mPrefs));
        mCustomizeBackgroundCornerRadiusPref.setChecked(WidgetDAO.isVerticalWidgetBackgroundCornerRadiusCustomizable(mPrefs));
        mDisplayDatePref.setChecked(WidgetDAO.isDateDisplayedOnVerticalDigitalWidget(mPrefs));
        mDisplayNextAlarmPref.setChecked(WidgetDAO.isNextAlarmDisplayedOnVerticalDigitalWidget(mPrefs));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isVerticalDigitalWidgetHorizontalPaddingApplied(mPrefs));
        mDefaultHoursColorPref.setChecked(WidgetDAO.isVerticalDigitalWidgetDefaultHoursColor(mPrefs));
        mDefaultMinutesColorPref.setChecked(WidgetDAO.isVerticalDigitalWidgetDefaultMinutesColor(mPrefs));
        mDefaultDateColorPref.setChecked(WidgetDAO.isVerticalDigitalWidgetDefaultDateColor(mPrefs));
        mDefaultNextAlarmColorPref.setChecked(WidgetDAO.isVerticalDigitalWidgetDefaultNextAlarmColor(mPrefs));
    }

    private void updateVerticalDigitalWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        VerticalDigitalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}
