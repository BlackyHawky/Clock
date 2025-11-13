// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.best.deskclock.R;
import com.best.deskclock.data.WidgetDAO;
import com.best.deskclock.utils.SdkUtils;
import com.best.deskclock.utils.Utils;
import com.best.deskclock.utils.WidgetUtils;
import com.best.deskclock.widgets.materialyouwidgets.MaterialYouVerticalDigitalAppWidgetProvider;

public class MaterialYouVerticalDigitalWidgetSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mShowBackgroundOnDigitalWidgetPref;
    SwitchPreferenceCompat mCustomizeBackgroundCornerRadiusPref;
    Preference mBackgroundCornerRadiusPref;
    SwitchPreferenceCompat mDisplayDatePref;
    SwitchPreferenceCompat mDisplayNextAlarmPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    SwitchPreferenceCompat mDefaultBackgroundColorPref;
    ColorPickerPreference mCustomBackgroundColorPref;
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
        return getString(R.string.material_you_vertical_digital_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_material_you_vertical_digital_widget);

        mShowBackgroundOnDigitalWidgetPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND);
        mCustomizeBackgroundCornerRadiusPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS);
        mBackgroundCornerRadiusPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_BACKGROUND_CORNER_RADIUS);
        mDisplayDatePref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE);
        mDisplayNextAlarmPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM);
        mApplyHorizontalPaddingPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING);
        mDefaultBackgroundColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR);
        mCustomBackgroundColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_BACKGROUND_COLOR);
        mDefaultHoursColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR);
        mCustomHoursColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_HOURS_COLOR);
        mDefaultMinutesColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR);
        mCustomMinutesColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_MINUTES_COLOR);
        mDefaultDateColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR);
        mCustomDateColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_DATE_COLOR);
        mDefaultNextAlarmColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR);
        mCustomNextAlarmColorPref = findPreference(KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOM_NEXT_ALARM_COLOR);

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

        updateMaterialYouVerticalDigitalWidget();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_BACKGROUND -> {
                boolean displayBackground = (boolean) newValue;
                boolean isCustomColor = !WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultBackgroundColor(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isMaterialYouVerticalDigitalWidgetBackgroundCornerRadiusCustomizable(mPrefs);

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

            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_CUSTOMIZE_BACKGROUND_CORNER_RADIUS -> {
                mBackgroundCornerRadiusPref.setVisible((boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_DATE -> {
                mDefaultDateColorPref.setVisible((boolean) newValue);
                mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible()
                        && !WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultDateColor(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DISPLAY_NEXT_ALARM -> {
                mDefaultNextAlarmColorPref.setVisible((boolean) newValue);
                mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
                        && !WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_APPLY_HORIZONTAL_PADDING ->
                    Utils.setVibrationTime(requireContext(), 50);

            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_BACKGROUND_COLOR -> {
                boolean isCustomColor = !(boolean) newValue;
                boolean displayBackground = WidgetDAO.isBackgroundDisplayedOnMaterialYouVerticalDigitalWidget(mPrefs);
                boolean isRadiusCustomizable = WidgetDAO.isMaterialYouVerticalDigitalWidgetBackgroundCornerRadiusCustomizable(mPrefs);

                mCustomBackgroundColorPref.setVisible(isCustomColor);

                if (!SdkUtils.isAtLeastAndroid12()) {
                    mCustomizeBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground);
                    mBackgroundCornerRadiusPref.setVisible(isCustomColor && displayBackground && isRadiusCustomizable);
                }

                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_HOURS_COLOR -> {
                mCustomHoursColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_MINUTES_COLOR -> {
                mCustomMinutesColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_DATE_COLOR -> {
                mCustomDateColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_VERTICAL_DIGITAL_WIDGET_DEFAULT_NEXT_ALARM_COLOR -> {
                mCustomNextAlarmColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), MaterialYouVerticalDigitalAppWidgetProvider.class);
        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ColorPickerPreference colorPickerPref) {
            colorPickerPref.showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    private void setupPreferences() {
        mShowBackgroundOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        boolean isBackgroundVisible = WidgetDAO.isBackgroundDisplayedOnMaterialYouVerticalDigitalWidget(mPrefs);
        boolean isBackgroundCornerRadiusCustomizable =
                WidgetDAO.isMaterialYouVerticalDigitalWidgetBackgroundCornerRadiusCustomizable(mPrefs);
        boolean isCustomColor = !WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultBackgroundColor(mPrefs);

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

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);

        mDefaultBackgroundColorPref.setVisible(isBackgroundVisible);
        mDefaultBackgroundColorPref.setOnPreferenceChangeListener(this);

        mCustomBackgroundColorPref.setVisible(isBackgroundVisible && isCustomColor);
        mCustomBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultHoursColorPref.setOnPreferenceChangeListener(this);

        mCustomHoursColorPref.setVisible(!WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultHoursColor(mPrefs));
        mCustomHoursColorPref.setOnPreferenceChangeListener(this);

        mDefaultMinutesColorPref.setOnPreferenceChangeListener(this);

        mCustomMinutesColorPref.setVisible(!WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultMinutesColor(mPrefs));
        mCustomMinutesColorPref.setOnPreferenceChangeListener(this);

        mDefaultDateColorPref.setVisible(WidgetDAO.isDateDisplayedOnMaterialYouVerticalDigitalWidget(mPrefs));
        mDefaultDateColorPref.setOnPreferenceChangeListener(this);

        mCustomDateColorPref.setVisible(mDefaultDateColorPref.isVisible()
                && !WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultDateColor(mPrefs));
        mCustomDateColorPref.setOnPreferenceChangeListener(this);

        mDefaultNextAlarmColorPref.setVisible(WidgetDAO.isNextAlarmDisplayedOnMaterialYouVerticalDigitalWidget(mPrefs));
        mDefaultNextAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomNextAlarmColorPref.setVisible(mDefaultNextAlarmColorPref.isVisible()
                && !WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor(mPrefs));
        mCustomNextAlarmColorPref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mShowBackgroundOnDigitalWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnMaterialYouVerticalDigitalWidget(mPrefs));
        mCustomizeBackgroundCornerRadiusPref.setChecked(WidgetDAO.isMaterialYouVerticalDigitalWidgetBackgroundCornerRadiusCustomizable(mPrefs));
        mDisplayDatePref.setChecked(WidgetDAO.isDateDisplayedOnMaterialYouVerticalDigitalWidget(mPrefs));
        mDisplayNextAlarmPref.setChecked(WidgetDAO.isNextAlarmDisplayedOnMaterialYouVerticalDigitalWidget(mPrefs));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isMaterialYouVerticalDigitalWidgetHorizontalPaddingApplied(mPrefs));
        mDefaultBackgroundColorPref.setChecked(WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultBackgroundColor(mPrefs));
        mDefaultHoursColorPref.setChecked(WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultHoursColor(mPrefs));
        mDefaultMinutesColorPref.setChecked(WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultMinutesColor(mPrefs));
        mDefaultDateColorPref.setChecked(WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultDateColor(mPrefs));
        mDefaultNextAlarmColorPref.setChecked(WidgetDAO.isMaterialYouVerticalDigitalWidgetDefaultNextAlarmColor(mPrefs));
    }

    private void updateMaterialYouVerticalDigitalWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        MaterialYouVerticalDigitalAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}
