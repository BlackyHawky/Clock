// SPDX-License-Identifier: GPL-3.0-only

package com.best.deskclock.settings;

import static android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID;
import static android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID;

import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_BACKGROUND_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR;
import static com.best.deskclock.settings.PreferencesKeys.KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND;

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
import com.best.deskclock.widgets.materialyouwidgets.MaterialYouNextAlarmAppWidgetProvider;

import com.rarepebble.colorpicker.ColorPreference;

public class MaterialYouNextAlarmWidgetSettingsFragment extends ScreenFragment
        implements Preference.OnPreferenceChangeListener {

    private int mAppWidgetId = INVALID_APPWIDGET_ID;

    SwitchPreferenceCompat mShowBackgroundOnDigitalWidgetPref;
    SwitchPreferenceCompat mApplyHorizontalPaddingPref;
    SwitchPreferenceCompat mDefaultBackgroundColorPref;
    ColorPreference mCustomBackgroundColorPref;
    SwitchPreferenceCompat mDefaultTitleColorPref;
    ColorPreference mCustomTitleColorPref;
    SwitchPreferenceCompat mDefaultAlarmTitleColorPref;
    ColorPreference mCustomAlarmTitleColorPref;
    SwitchPreferenceCompat mDefaultAlarmColorPref;
    ColorPreference mCustomAlarmColorPref;

    @Override
    protected String getFragmentTitle() {
        return getString(R.string.material_you_next_alarm_widget);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings_customize_material_you_next_alarm_widget);

        mShowBackgroundOnDigitalWidgetPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND);
        mApplyHorizontalPaddingPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING);
        mDefaultBackgroundColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_BACKGROUND_COLOR);
        mCustomBackgroundColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_BACKGROUND_COLOR);
        mDefaultTitleColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR);
        mCustomTitleColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_TITLE_COLOR);
        mDefaultAlarmTitleColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR);
        mCustomAlarmTitleColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_TITLE_COLOR);
        mDefaultAlarmColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR);
        mCustomAlarmColorPref = findPreference(KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_CUSTOM_ALARM_COLOR);

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

        updateMaterialYouNextAlarmWidget();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        switch (pref.getKey()) {
            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DISPLAY_BACKGROUND -> {
                mDefaultBackgroundColorPref.setVisible((boolean) newValue);
                mCustomBackgroundColorPref.setVisible((boolean) newValue &&
                        !WidgetDAO.isMaterialYouNextAlarmWidgetDefaultBackgroundColor(mPrefs));
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_APPLY_HORIZONTAL_PADDING ->
                    Utils.setVibrationTime(requireContext(), 50);

            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_BACKGROUND_COLOR -> {
                mCustomBackgroundColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_TITLE_COLOR -> {
                mCustomTitleColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_TITLE_COLOR -> {
                mCustomAlarmTitleColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }

            case KEY_MATERIAL_YOU_NEXT_ALARM_WIDGET_DEFAULT_ALARM_COLOR -> {
                mCustomAlarmColorPref.setVisible(!(boolean) newValue);
                Utils.setVibrationTime(requireContext(), 50);
            }
        }

        WidgetUtils.scheduleWidgetUpdate(requireContext(), MaterialYouNextAlarmAppWidgetProvider.class);
        return true;
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof ColorPreference) {
            ((ColorPreference) preference).showDialog(this, 0);
        } else super.onDisplayPreferenceDialog(preference);
    }

    private void setupPreferences() {
        mShowBackgroundOnDigitalWidgetPref.setOnPreferenceChangeListener(this);

        mApplyHorizontalPaddingPref.setOnPreferenceChangeListener(this);

        mDefaultBackgroundColorPref.setVisible(WidgetDAO.isBackgroundDisplayedOnMaterialYouNextAlarmWidget(mPrefs));
        mDefaultBackgroundColorPref.setOnPreferenceChangeListener(this);

        mCustomBackgroundColorPref.setVisible(WidgetDAO.isBackgroundDisplayedOnMaterialYouNextAlarmWidget(mPrefs)
                && !WidgetDAO.isMaterialYouNextAlarmWidgetDefaultBackgroundColor(mPrefs));
        mCustomBackgroundColorPref.setOnPreferenceChangeListener(this);

        mDefaultTitleColorPref.setOnPreferenceChangeListener(this);

        mCustomTitleColorPref.setVisible(!WidgetDAO.isMaterialYouNextAlarmWidgetDefaultTitleColor(mPrefs));
        mCustomTitleColorPref.setOnPreferenceChangeListener(this);

        mDefaultAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mCustomAlarmTitleColorPref.setVisible(!WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor(mPrefs));
        mCustomAlarmTitleColorPref.setOnPreferenceChangeListener(this);

        mDefaultAlarmColorPref.setOnPreferenceChangeListener(this);

        mCustomAlarmColorPref.setVisible(!WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmColor(mPrefs));
        mCustomAlarmColorPref.setOnPreferenceChangeListener(this);
    }

    private void saveCheckedPreferenceStates() {
        mShowBackgroundOnDigitalWidgetPref.setChecked(WidgetDAO.isBackgroundDisplayedOnMaterialYouNextAlarmWidget(mPrefs));
        mApplyHorizontalPaddingPref.setChecked(WidgetDAO.isMaterialYouNextAlarmWidgetHorizontalPaddingApplied(mPrefs));
        mDefaultBackgroundColorPref.setChecked(WidgetDAO.isMaterialYouNextAlarmWidgetDefaultBackgroundColor(mPrefs));
        mDefaultTitleColorPref.setChecked(WidgetDAO.isMaterialYouNextAlarmWidgetDefaultTitleColor(mPrefs));
        mDefaultAlarmTitleColorPref.setChecked(WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmTitleColor(mPrefs));
        mDefaultAlarmColorPref.setChecked(WidgetDAO.isMaterialYouNextAlarmWidgetDefaultAlarmColor(mPrefs));
    }

    private void updateMaterialYouNextAlarmWidget() {
        AppWidgetManager wm = AppWidgetManager.getInstance(requireContext());
        MaterialYouNextAlarmAppWidgetProvider.updateAppWidget(requireContext(), wm, mAppWidgetId);

        Intent result = new Intent();
        result.putExtra(EXTRA_APPWIDGET_ID, mAppWidgetId);
        requireActivity().setResult(Activity.RESULT_OK, result);
    }

}
